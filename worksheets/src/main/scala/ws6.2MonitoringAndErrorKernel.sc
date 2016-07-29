//Lifecycle Monitoring and the Error Kernel (24:07)
//https://class.coursera.org/reactive-002/lecture/89

//TOC
//– restarts are not externally visible
//– if you have ActorRef, actor was alive
//– if you have no more responces, actor – may be – dead
//– or, may be, network is down?
//– you can't track restart
//– enters Akka Lifecycle Monitoring a.k.a. DeathWatch
//– context.watch(target) : Terminated(target) message
//– Terminated, existenceConfirmed == true if message was sent by actor, not system
//– example: Getter, eliminate 'Done' message
//list of actor children in the context // actor/child names are unique
//Controller: add case Terminated(_) => … check children.isEmpty
//– monitoring for fail-over : class Manager extends Actor { …. case Terminated(db) => context.become(backup()) …
//– The Error Kernel pattern: keep important data near the root, delegate risk to the leaves
//– restarts are recursive (childs are part of the state)
//– avoid restarting top actors
//– in Akka you are forced to create actors hierarchy / mandatory parental supervision
//– example: apply E.Kernel to Receptionist: // getters at the bottom
//  always stop Controller if it has a problem
//  react to Terminated to catch cases where no Result was sent
//  discard Terminated after Result was sent
//class Receptionist … supervisorStrategy = stoppingStrategy
//  ...
//context.watch(controller)
//…
//case Terminated(_) => …
//
//– interjection: the EventStream: every actor can optionally subscribe to the EventStream
//– context.system.eventStream : one shout, many listen // broadcast messages
//– unhandled Terminated messages: dangerous …
//def unhandled( … ) … case Terminated … throw new DeathPactException
//– on default supervisor call 'stop' command after that exception

import akka.actor.Actor.Receive
import akka.actor._
import akka.dispatch.sysmsg.Failed
import akka.event.Logging.LogEvent

// check actor is valid? alive?
// only by exchanging messages

// Akka DeathWatch: register: context.watch(ref)
// and react to
// case Terminated(ref) => log(node ref is dead)

// DeathWatch API
{
  trait ActorContext {
    def watch(target: ActorRef): ActorRef
    def unwatch(target: ActorRef): ActorRef
  }
  case class Terminated private[akka] (actor: ActorRef)
                                      (val existenceConfirmed: Boolean,
                                       val addressTerminated: Boolean)
    extends AutoReceiveMessage with PossiblyHarmful
}
// case class Terminated is private except for Akka: you can't create this message
// system will prohibit constructor call
// existenceConfirmed: true if watch was registered on alive actor;
// false if try watch on already dead actor.
// addressTerminated: is message synthesised by the system?

// example: Controller, Getter: end-of-conversation marker
{
  class Getter(url: String, depth: Int) extends Actor {
    override def receive: Receive = {
      case _: Status.Failure => context.stop(self) // Terminated sent to controller
      case body: String =>
        for(link <- findLinks(body)) context.parent ! Controller.Check(link, depth)
        context.stop(self) // Terminated sent to controller
    }
  }
}
// Getter: replace message 'Done' with context.stop(self)

// list of children in context;
// child will be removed from list before Terminated arrive
{
  trait ActorContext {
    def children: Iterable[ActorRef]
    def child(name: String): Option[ActorRef]
  }
}
// we can check it to be sure: the work is done

// Controller rewrited to use 'watch'
{
  class Controller extends Actor {
    override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 5) {
      case _: Exception => SupervisorStrategy.Restart // restart getter if it fails
    }
    override def receive: Receive = {
      case Check(url, depth) =>
        if(!cache(url) && depth > 0) context.watch(newGetter(url, depth-1)) // wait for Terminated
        cache += url
      case ReceiveTimeout => context.children foreach context.stop // kill getter if it hung
      case Terminated(_) => if(context.children.isEmpty) context.parent ! Result(cache)
    }
  }
}

// Fail-over monitoring using 'watch'
// example
{
  class Manager extends Actor {
    override def receive: Receive = prime()
    def backup(): Receive = { ??? }
    def prime(): Receive = {
      val db = context.actorOf(Props[DBActor], "db")
      context.watch(db)

      // return partial func:
      {
        case Terminated(`db`) => context.become(backup())
      }
    }
  }
}
// switch to backup DB if main was failed

// 'stop' action was considered, 'watch' is the ansver.
// what about 'restart'?

// The Error Kernel pattern:
// keep important data near the root, delegate risk to the leaves
// children nodes are part of the state

// mandatory parental supervision: in Akka you forced to create hierarchy

// Error Kernel example

// Receptionist/Controller/Getter
// -- stop controller if it has a problem
// -- react to Terminated to catch cases where no Result was sent
// -- discard Terminated after Result was sent
{
  class Receptionist extends Actor {
    override def supervisorStrategy = SupervisorStrategy.stoppingStrategy
    def runNext(queue: Vector[Job]): Receive = {
      reqNo += 1
      if (queue.isEmpty) waiting
      else {
        val controller = context.actorOf(controllerProps, s"c$reqNo")
        context.watch(controller) // wait for Terminated
        controller ! Controller.Check(queue.head.url, 2)
        running(queue)
      }
    }
    def running(queue: Vector[Job]): Receive = {
      case Controller.Result(links) => {
        val job = queue.head
        job.client ! Result(job.url, links)
        context.stop(context.unwatch(sender)) // no more Terminated messages
        context.become(runNext(queue.tail))
      }
      case Terminated(_) => // controller failed
        val job = queue.head
        job.client ! Failed(job.url) // send notification
        context.become(runNext(queue.tail))
      case Get(url) => context.become(enqueueJob(queue, Job(sender, url)))
    }
  }
}
// stoppingStrategy; context.watch(controller);
// context.stop(context.unwatch(sender);
// case Terminated => client ! Failed

// why Terminated is dangerous?
// unhandled Terminated will stop parent actor. Why?

// step aside: context.system.EventStream, broadcast messages
{
  trait EventStream {
    def subscribe(subscriber: ActorRef, topic: Class[_]): Boolean
    def unsubscribe(subscriber: ActorRef, topic: Class[_]): Boolean
    def unsubscribe(subscriber: ActorRef): Unit
    def publish(event: AnyRef): Unit
  }

  class Listener extends Actor {
    context.system.eventStream.subscribe(self, classOf[LogEvent])

    override def receive: Receive = {
      case e: LogEvent => ???
    }

    override def postStop(): Unit = context.system.eventStream.unsubscribe(self)
  }
}

// unhandled message are passed to the 'unhandled' method
{
  trait Actor {
    def unhandled(message: Any): Unit = message match {
      case Terminated(target) => throw new DeathPactException(target)
      case msg => context.system.eventStream.publish(UnhandledMessage(msg, sender, self))
    }
  }
}
// by default, system will stop parent of the 'Terminated' sender
// and that why Terminated messages can be harmful

// example: shutdown system if Receptionist terminated
// watch(receptionist); not handle Terminated from Controller leads to
// receptionist stop => system shut down
{
  class Main extends Actor {
    val receptionist = context.actorOf([Receptionist], "receptionist")
    context.watch(receptionist) // sign death pact!
    // unhandled Terminated go to parent and stop it

    // yada yada
    context.setReceiveTimeout(10 seconds)

    override def receive: Receive = {
      case Result(url, set) => println("result")
      case Failed(url) => println("failed")
      case ReceiveTimeout => context.stop(self)
    }
  }

  class Receptionist extends Actor {
    def runNext(queue: Vector[Job]): Receive = {
      reqNo += 1
      // hack to demo Terminated effect
      if(reqNo == 3) context.stop(self)

      if (queue.isEmpty) waiting
      else {
        val controller = context.actorOf(controllerProps, s"c$reqNo")
        context.watch(controller)
        controller ! Controller.Check(queue.head.url, 2)
        running(queue)
      }
    }
  }
}
