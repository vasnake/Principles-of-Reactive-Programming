//Designing Actor Systems (38:10) / web crawler
//https://class.coursera.org/reactive-002/lecture/159

//TOC
//– room full of 'people'
//– split you problem to small tasks, assign tasks to actors
//– draw a communication diagrams
//– actors have a very little overhead
//– optimize comunications path
//– actors can have short life
//– example: web crawler, collect all links from start URL
//– 1 actor: Receptionist – accept incoming requests from client
//– Client ask for start // to Receptionist Get url
//– Controller: what was visited, what to visit // from Controller Check (url, depth) // one controller per user request
//– Getter: go to URL, get doc // from Controller Get url // multiple getters
//– AsyncHttpClient, write 'get(url)' using Java Future converted to Scala Future using Promise
//– write async code top to bottom
//– parser 'findLinks' using Jsoup
//– class Getter(url, depth) extends Actor // val fut = get(url); fut onComplete { … }
//– using future.pipeTo(actor)
//– implicit execution context
//– Getter message: case body: String => parse, send links to parent/controller
//– actors are run by a dispatcher – potentially shared – which can also run Futures // futures in actors
//– ActorLogging, logging includes IO which can block; Akka's logging passes that task to dedicated actors
//– class Controller extends Actor with ActorLogging { ….
//var cache = Set.empty[String] // shared cache, var pointed to immutable structure
//var children = Set …
//– prefer immutable data structures, they can be safely shared // thread safe
//– timeouts: class Controller … context.setReceiveTimeout(10 seconds) … // timeout is reset by every received message
//– Akka timer service: trait Scheduler // not terribly precise
//– scheduleOnce … children foreach … // it is not thread safe, it's run in a context of a scheduler, not actor
//– scheduleOnce ... self Timeout … // sending message to self is good and safe
//– same problem with caching getter results // pipeTo self, use unwrapped sender – avoid accessing data in other context
//– do not refer to actor state from code running async // outside self – Future combinators | scheduler
//// that breaks actor incapsulation
//– class Receptionist extends Actor …
//– waiting or running behaviour with jobs
//– jobs: case class Job(client: ActorRef, url: String) // jobs queue
//– prefer context.become for different states, with data local to the behaviour

import java.util.concurrent.Executor

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, ReceiveTimeout, Status}
import akka.dispatch.sysmsg.Failed
import com.ning.http.client.AsyncHttpClient

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

// take a bunch of 'people' and give them subtasks
// draw a diagram of communications
// 'people' can be short-lived

// example: the Link Checker (web crawler)
// input: url, maxdepth
// output: all links founded in pages

// client -> receptionist (Get(url), back: Result(url, links))
// receptionist -> controller, set of urls (Check(url, depth), back: Result(links))
// controller -> getter, process one url (Get(url, currDepth), back: Links, Done)

// plan
// async web client: "com.ning" % "async-http-client" % "1.7.19"
// write getter for processing the body: "org.jsoup" % "jsoup" % "1.8.1"
// write a controller which spawns getters
// write a receptionist managing one controler per user request

// web client, sync version, not so good
// actor can't react to other messages; wastes one thread -- a finite resource
{
    val webclient = new AsyncHttpClient
    def get(url: String): String = {
        val resp = webclient.prepareGet(url).execute().get // sync
        if (resp.getStatusCode < 400) resp.getResponseBodyExcerpt(131072)
        else sys.error(s"bad status ${resp.getStatusCode}")
    }
}

// web client, async version: using Promise & Future
// much better, reactive: non-blocking and event-driven
{
    private val webclient = new AsyncHttpClient
    def get(url: String)(implicit exec: Executor): Future[String] = {
        val fut = webclient.prepareGet(url).execute()
        val prom = Promise[String]()
        fut.addListener(new Runnable {
            override def run(): Unit = {
                val resp = fut.get
                if (resp.getStatusCode < 400)
                    prom.success(resp.getResponseBodyExcerpt(131072))
                else prom.failure(sys.error(s"bad status ${resp.getStatusCode}"))
            }
        }, exec)
        prom.future
    }
}

trait WebClient {
    def get(url: String)(implicit exec: Executor): Future[String]
}

case class BadStatus(status: Int) extends RuntimeException

object AsyncWebClient extends WebClient {
    private val client = new AsyncHttpClient

    def get(url: String)(implicit exec: Executor): Future[String] = {
        val prom = Promise[String]()
        val fut = client.prepareGet(url).execute()
        fut.addListener(new Runnable {
            override def run(): Unit = {
                val resp = fut.get
                if (resp.getStatusCode < 400)
                    prom.success(resp.getResponseBodyExcerpt(131072))
                else prom.failure(BadStatus(resp.getStatusCode))
            }
        }, exec)
        prom.future
    }
}

// parse html
// "org.jsoup" % "jsoup" % "1.8.1"
{
    import org.jsoup.Jsoup
    import scala.collection.JavaConverters._

    def findLinks(body: String): Iterator[String] = {
        val doc = Jsoup.parse(body, url)
        val links = doc.select("a[href]")
        for {
            link <- links.iterator().asScala
        } yield link.absUrl("href")
    }
}

// class Getter extends Actor
{
    class Getter(url: String, depth: Int) extends Actor {
        // actors context dispatcher: can run Futures
        implicit val exec = context.dispatcher

        // pattern: future.onComplete => future.pipeTo
        val fut: Future[Try[String]] = WebClient.get(url)
        fut.pipeTo(self)
//        fut onComplete {
//            case Success(body) => self ! body
//            case Failure(err) => self ! Status.Failure(err)
//        }
    }
}
// rewrited:
{
    class Getter(url: String, depth: Int) extends Actor {
        implicit val exec = context.dispatcher
        // webclient future use that dispatcher
        WebClient get url pipeTo self // send message to self: html body or Failure(err)

        override def receive: Receive = {
            case body: String => getLinks(body); stop()
            case _: Status.Failure => stop()
        }

        def getLinks(body: String) = {
            for (link <- findLinks(body))
                context.parent ! Controller.Check(link, depth) // ask to check a new link
        }
        def stop() = {
            context.parent ! Done
            context.stop(self)
        }
    }
}

// step aside: actor-based logging
// IO can block, do it async in other actors
{
    // akka.loglevel=DEBUG
    class A extends Actor with ActorLogging {
        override def receive: Receive = {
            case msg => log.debug("received msg: '{}'", msg)
        }
    }
}

// class Controller extends Actor with ActorLogging
{
    class Controller extends Actor with ActorLogging {
        // n.b. shared cache of urls: we use 'var cache = immutable.Set'
        // not 'val cache = mutable.Set'
        // can't share mutable data!
        var cache = Set.empty[String]

        var children = Set.empty[ActorRef]

        override def receive: Receive = {
            case Check(url, depth) => check(url, depth)
            case Getter.Done => done()
        }
        def done() = {
            children -= sender
            if(children.isEmpty) context.parent ! Result(cache)
        }
        def check(url: String, depth: Int) = {
            log.debug("{} checking {}", depth, url)
            if(!cache(url) && depth > 0)
                children += context.actorOf(Props(new Getter(url, depth-1)))
            cache += url
        }
    }
}
// manage timeouts: Controller ... context.setReceiveTimeout
// timeout is reset by every received message
{
    import scala.concurrent.duration._
    class Controller extends Actor with ActorLogging {
        context.setReceiveTimeout(10 seconds)
        var children = Set.empty[ActorRef]
        override def receive: Receive = {
            case Check(url, depth) => check(url, depth)
            case Getter.Done => done()
            case ReceiveTimeout => children foreach (_ ! Getter.Abort)
        }
    }
}

// handle Abort in the Getter
{
    class Getter(url: String, depth: Int) extends Actor {
        override def receive: Receive = {
            case body: String => getLinks(body); stop()
            case _: Status.Failure => stop()
            case Abort => stop()
        }
        def stop() = {
            context.parent ! Done
            context.stop(self)
        }
    }
}

// step aside: don't refer to actor state from async code!

// example: Akka Scheduler, timer service
// can say: use message passing, not lambdas or runnable
{
    trait Scheduler {
        def scheduleOnce(delay: FiniteDuration,
                         target: ActorRef,
                         msg: Any)(implicit ec: ExecutionContext): Cancellable
        def scheduleOnce(delay: FiniteDuration)(block: => Unit)
                        (implicit ec: ExecutionContext): Cancellable
        def scheduleOnce(delay: FiniteDuration, run: Runnable)
                        (implicit ec: ExecutionContext): Cancellable
        // and so on ...
    }
}
// controller timeout example: not thread-safe
{
    class Controller extends Actor with ActorLogging {
        import context.dispatcher
        var children = Set.empty[ActorRef]
        // block of code run in scheduler context, not actor context => sync problem
        context.system.scheduler.scheduleOnce(10 seconds) { children foreach(_ ! Getter.Abort) }
    }
}
// safe timeout: send a message to actor after a timeout
{
    class Controller extends Actor with ActorLogging {
        import context.dispatcher
        var children = Set.empty[ActorRef]
        // it's ok, message passing
        context.system.scheduler.scheduleOnce(10 seconds, self, Timeout)

        override def receive: Receive = {
            case Timeout => children foreach(_ ! Getter.Abort)
        }
    }
}

// Actor and Future, another async problem
// another off-context example: class Cache extends Actor
{
    class Cache extends Actor {
        var cache = Map.empty[String, String]
        override def receive: Receive = {
            case Get(url) => {
                if(cache contains url) sender ! cache(url)
                // web-client Future callback updates the cache variable -- a no-no
                else WebClient get url foreach { body =>
                    cache += url -> body
                    sender ! body
                }
            }
        }
    }
}
// safe cache update: send a message when future completes
{
    class Cache extends Actor {
        var cache = Map.empty[String, String]
        override def receive: Receive = {
            case Get(url) => {
                if(cache contains url) sender ! cache(url)
                // sender must be dereferenced, Future ahead!
                else WebClient get url map (Result(sender, url, _)) pipeTo self
                // message passing, ok
            }
            case Result(from, url, body) =>
                cache += url -> body
                client ! body
        }
    }
}
// deref sender before forming a callback, or else webclient can be fucked by wrong reference
{
    def receive: Receive = {
        case Get(url) => {
            if(cache contains url) sender ! cache(url)
            else {
                val from = sender
                WebClient get url map (Result(from, url, _)) pipeTo self
            }
        }
    }
}
// bottom line: don't refer to actor state from async code!

// class Receptionist extends Actor: 2 states: waiting and running
// one user request: one controller
// one user request at a time
{
    class Receptionist extends Actor {
        override def receive: Receive = waiting
        val waiting: Receive = {
            // upon Get(url) start a traversal, become running
        }
        def running(queue: Vector[Job]): Receive = {
            // upon Get(url) append job to queue
            // upon Controller.Result(links) ship that to client, run next job
        }
    }
}
{
    class Receptionist extends Actor {
        case class Job(client: ActorRef, url: String)
        var reqNo = 0

        def runNext(queue: Vector[Job]): Receive = {
            reqNo += 1
            if (queue.isEmpty) waiting // goto waiting state
            else {
                val controller = context.actorOf(Props[Controller], s"controller$reqNo")
                controller ! Controller.Check(queue.head.url, 2)
                running(queue) // goto running state
            }
        }

        override def receive: Receive = waiting

        val waiting: Receive = {
            // upon Get(url) start a traversal, become running
            case Get(url) => context.become(
                runNext(Vector(Job(sender, url))))
        }

        def running(queue: Vector[Job]): Receive = {
            // upon Get(url) append job to queue
            // upon Controller.Result(links) ship that to client, run next job
            case Controller.Result(links) => {
                val job = queue.head
                job.client ! Result(job.url, links)
                context.stop(sender) // stop controller
                context.become(runNext(queue.tail))
            }
            case Get(url) => context.become(enqueueJob(queue, Job(sender, url)))
        }

        // 3 requests in processing, no more
        def enqueueJob(queue: Vector[Job], job: Job): Receive = {
            if (queue.size > 3) {
                sender ! Failed(job.url)
                running(queue)
            }
            else running(queue :+ job)
        }
    }
}

// class Main extends Actor
{
    class Main extends Actor {
        import Receptionist._
        val rec = context.actorOf(Props[Receptionist], "receptionist")
        context.setReceiveTimeout(10 seconds)

        rec ! Get("http://ya.ru")
        rec ! Get("http://ya.ru/1")
        rec ! Get("http://ya.ru/2")
        rec ! Get("http://ya.ru/3")
        rec ! Get("http://ya.ru/4")

        override def receive: Receive = {
            case Result(url, set) =>
                println(set.toVector.sorted.mkString(s"Results for '$url':\n", "\n", "\n"))
            case Failed(url) => println(s"Failed to fetch '$url'\n")
            case ReceiveTimeout => context.stop(self)
        }

        override def postStop(): Unit = {
            WebClient.shutdown()
            super.postStop()
        }
    }
}

// summary
// a reactive apps: non-blocking (async) & event-driven
// actors run by a dispatcher, which can also run futures
// prefer immutable data structures: they can be shared
// prefer context.become with data local to the behaviour, for different states
// don't refer to actor state from async code (scheduler, future)
