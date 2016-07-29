//Testing Actor Systems (17:16)
//https://class.coursera.org/reactive-002/lecture/81

//TOC
//– verify externally observable effects: messages only
//– TestProbe, Akka's remote-controlled actor
//    tp.send(... ); tp.expectMsg( ... )
//– or, using ImplicitSender
//    new TestKit(ActorSystem...) with ImplicitSender { …
//    tst ! «How are you?»// sender = testActor
//    expectMsg(...)
//– mocking DB? dependency injection
//– overridable factory methods – mocking communication peers, isolate tested objects
//– test Getter
//– mock 'parent' with custom class StepParent …
//– parent-child proxy pattern or class FosterParent for parent-child communication // I think explicit sender are better
//– test Receptionist: fakeController
//– testing actor hierarchy: test bottom-up, start with leaves: 'reverse onion testing'

import java.util.concurrent.Executor

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.actor.Actor.Receive
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.pattern.pipe
import com.ning.http.client.AsyncHttpClient
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._

// testing tools in Akka toolbelt

// test observing messages flow (the only way)

// example: how to test actor Toggle?
class Toggle extends Actor {
    // two states, first: happy
    override def receive: Receive = happy

    def happy: Receive = {
        case "How are you?" => sender ! "happy"; context become sad
    }
    def sad: Receive = {
        case "How are you?" => sender ! "sad"; context become happy
    }
}
// only one way: send messages and check responces

// TestProbe in action, testing Toggle
// TestProbe can buffer messages in the internal queue, for inspection
// n.b. ActorSystem creation for tests (can't use Main)
{
    implicit val system = ActorSystem("TestSys")
    val toggle = system.actorOf(Props[Toggle])
    val p = TestProbe()

    p.send(toggle, "How are you?")
    p.expectMsg("happy")
    p.send(toggle, "How are you?")
    p.expectMsg("sad")
    p.send(toggle, "unknown")
    p.expectNoMsg(1 second)
    system.shutdown()
}

// TestKit: same test, another flavour: ImplicitSender
// TestProbe as a context, implicit actor name: testActor
{
    new TestKit(ActorSystem("TestSys")) with ImplicitSender {
        val toggle = system.actorOf(Props[Toggle])
        toggle ! "How are you?"; expectMsg("happy")
        toggle ! "How are you?"; expectMsg("sad")
        toggle ! "unknown"; expectNoMsg(1 second)
        system.shutdown(); // testActor.tell("shutup", toggle)
    }
}

// dependencies, external systems (DB, ...)

// you can mock dependencies, use Dependency Injection pattern
// add overridable factory methods

// example: web-crawler Receptionist
{
    class Receptionist extends Actor {

        def runNext(queue: Vector[Job]): Receive = {
            if (queue.isEmpty) waiting
            else {
                // v1: controller creation hardwired
                val controller = context.actorOf(Props[Controller], s"controller$reqNo")
            }
        }
    }
}
// replace it with fabric method, any controller can be injected
{
    class Receptionist extends Actor {
        // default implementation
        def controllerProps: Props = Props[Controller]
        // yada yada ...
        val controller = context.actorOf(controllerProps, "controller")
    }
}

// detailed example: web-crawler Getter, WebClient can be created using DI pattern
{
    class Getter extends Actor {
        // default implementation
        def client: WebClient = AsyncWebClient
        client get url pipeTo self
    }
}

// testing infrastructure:

// trait WebClient
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

// getter adapted for webclient injection
class Getter(url: String, depth: Int) extends Actor {
    implicit val exec = context.dispatcher
    // default implementation
    def client: WebClient = AsyncWebClient

    client get url pipeTo self
    override def receive: Receive = ???
}

// FakeWebClient (GetterSpec.scala)
val firstLink = "http://ya.ru"
val bodies = Map(
    firstLink ->
        """ <html> <head> <title> page 1 </title> </head> <body> <h1> A link </h1>
          | <a href="http://ya.ru/1">click</a>
          | </body>
        """.stripMargin)
val links = Map(firstLink -> Seq("http://ya.ru/1"))

object FakeWebClient extends WebClient {
    def get(url: String)(implicit exec: Executor): Future[String] =
        bodies get url match {
            case None => Future.failed(BadStatus(404))
            case Some(body) => Future.successful(body)
        }
}

// fakeGetter: create Getter with overrided WebClient
def fakeGetter(url: String, depth: Int): Props = {
    Props(new Getter(url, depth) {
        override def client = FakeWebClient
    })
}
// a problem:
// what about this: Getter send messages to 'parent'?
// in TestKit it will be the 'guardian' actor, bad idea.
// what can we do?

// we need StepParent: a proxy from child to probe
class StepParent(child: Props, probe: ActorRef) extends Actor {
    context.actorOf(child, "child")

    override def receive: Receive = {
        case msg => probe.tell(msg, sender)
    }
}

// advanced version of proxy: two-way Parent-Child communications, transparent
// 'probe' is a logger/monitor
class FosterParent(childprops: Props, probe: ActorRef) extends Actor {
    val child = context.actorOf(childprops, "child")

    override def receive: Receive = {
        case msg if sender == context.parent => // from parent/tester
            probe forward msg; child forward msg // 'forward' passes original sender
        case msg => // from child, I think
            probe forward msg; context.parent forward msg // to probe and to parent/tester
    }
}

// example
// testing Getter with mocked parent (controller) and mocked webclient.
// tests in WordSpecLile format (ScalaTest)
class GetterSpec extends TestKit(ActorSystem("GetterSpec")) with ImplicitSender
    with WordSpecLike with BeforeAndAfterAll {
    import GetterSpec._ // from object, see above

    override def afterAll(): Unit = {
        system.shutdown()
    }

    "A Getter" must {

        "return the right body" in {
            val getter = system.actorOf(Props(new FosterParent(
                fakeGetter(firstLink, 2), // child props
                testActor)), // probe
                "rightBody") // getter name

            for(link <- links(firstLink))
                expectMsg(Controller.Check(link, 2)) // getter found next link
            expectMsg(Getter.Done) // getter done
        }

        "properly finish in case of errors" in {
            val getter = system.actorOf(Props(new FosterParent(
                fakeGetter("unknown", 2), // child props
                testActor)), // probe
                "wrongLink") // getter name
            expectMsg(Getter.Done)
        }
    }
}

// similarly: test Reseptionist in isolation
// inject fake controller (ReceptionistSpec.scala)
object ReceptionistSpec {

    class FakeController extends Actor {
        import context.dispatcher // for scheduler
        override def receive: Receive = {
            case Controller.Check(url, depth) =>
                context.system.scheduler.scheduleOnce(
                    1 second, sender, Controller.Result(Set(url)))
// Thread.Sleep()??? no way, change to scheduleOnce...
//                Thread.sleep(1000)
//                sender ! Controller.Result(Set(url))
        }
    }

    def fakeReceptionist: Props =
        Props(new Receptionist {
            override def controllerProps = Props[FakeController]
        })
}

class ReceptionistSpec extends TestKit(ActorSystem("ReceptionistSpec")) with ImplicitSender
    with WordSpecLike with BeforeAndAfterAll {
    import ReceptionistSpec._
    import Receptionist._

    override def afterAll(): Unit = system.shutdown()

    "A Receptionist" must {

        "reply with a result" in {
            val rec = system.actorOf(fakeReceptionist, "sendResult")
            rec ! Get("myURL")
            expectMsg(Result("myURL", Set("myURL")))
        }

        "reject request flood" in {
            val rec = system.actorOf(fakeReceptionist, "rejectFlood")
            for (i <- 1 to 5) rec ! Get(s"myURL$i")
            expectMsg(Failed("myURL5"))
            for (i <- 1 to 4) expectMsg(Result(s"myURL$i", Set(s"myURL$i")))
        }
    }
}
// test in isolation:
// reverse onion testing: start from bottom modules/classes, move up to root of hierarchy
