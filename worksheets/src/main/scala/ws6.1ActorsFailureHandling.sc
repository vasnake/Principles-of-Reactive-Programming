//Failure Handling with Actors (22:38)
//https://class.coursera.org/reactive-002/lecture/87

//TOC
//– Failures, supervision, actor state and persistence
//– Actor: where failure go? async: messages
//reify exception and send to … sender? no, parent/supervisor
//– actors work as a team (system): failure is handled by the team leader/manager
//– escalating problem up until failure get handled
//– Supervision: resilience (recovery from a deformation) demands
//  containment (failure are isolated) and delegation of failure
//– actor model take care about containment
//– failed actor should be terminated or restarted / recreated
//– decision must be taken by one other actor
//– supervised actors form a tree structure
//– mandatory parental supervision: top actor not only supervisor, but a parent
//– supervision strategy: manager override val supervisorStrategy = ...
//exception cases and actions: Stop, Restart, Escalate …
//– strategy works as message processor, message = exception
//– AllForOneStrategy: decision applies to all children
//simple rate trigger included: finite number of restarts, … in time window, …
//– OneForOneStrategy(maxNrOfRestarts = 10, withinTimeRange = 1 minute) { … }
//– restarted actor have the same ID, in Akka the ActorRef stays valid after a restart
//other actors can continue communications with restarted actor
//– failure: unexpected error: indicate invalidated actor state
//– restart will install initial – valid – behaviour/state, recreate actor
//– actor lifecycle: start, (restart)*, stop
//– preStart, preRestart, postRestart, postStop callbacks
//  and children actors considered as an actor state
//– callbacks example
//  db: postStop: db.close
//– actor-local state vs external state (class Listener(source: ActorRef) extends Actor … )
//– context recursively restart all actor childs that not stopped in preRestrt

import akka.actor.FSM.Reason
import akka.actor.SupervisorStrategy.{Escalate, Restart, Stop}
import akka.actor.{Actor, ActorKilledException, ActorRef, OneForOneStrategy, Props}
import org.iq80.leveldb.DBException

// Actor, failure go to supervisor, restart failed node
// state handling while restart? error kernel pattern
// persistent state can survive a failure

// failure: reify as messages, send to supervisor (teamlead)

// tree structure, hierarchy of actors
// supervisor == parent (create his childs): mandatory parental supervision

// containment, isolate failure
// delegation, go to supervisor

// how it's implemented?

// supervisorStrategy (val, not def! efficiency matter)
// example: one for one strategy, 3 cases:
// 1 reconnect DB by restarting actor,
// 2 node ! Kill -- intension to stop => stop node;
// 3 service down, can't handle, escalate
{
  class Manager extends Actor {
    override val supervisorStrategy = OneForOneStrategy() {
      case _: DBException => Restart
      case _: ActorKilledException => Stop // db ! Kill
      case _: ServiseDownException => Escalate
    }
    context.actorOf(Props[DBActor], "db")
    context.actorOf(Props[VIServiceActor], "service")
  }
}
// default strategy just restarts failed childs

// restarts limits example
// stop child node if limits exceeded
{
  class Manager extends Actor {
    var restarts = Map.empty[ActorRef, Int].withDefaultValue(0)

    override val supervisorStrategy = OneForOneStrategy() {
      case _: DBException => {
        restarts(sender) match {
          case toomany if toomany > 10 => restarts -= sender; Stop
          case n => restarts = restarts.updated(sender, n+1); Restart
        }
      }
    }

  }
}

// all for one Strategy (restart all if one failed)
// stop all childs when restriction (limits) violated

// rate trigger can be included in all for ... and one for ...
// allow a finite number of restarts (in a time window)
{
  OneForOneStrategy(maxNrOfRetries=10, withinTimeRange = 1 minute) {
    case _: DBException => Restart // will turn into stop
  }
}

// actor identity
// in akka ActorRef stays valid after a restart

// what restart mean?
// we talk about unexpected errors, exceptions
// that means, actor state become invalid (childs considered as part of state)
// supervisor, restarting node, install initial state/behaviour

// actor lifecycle: Start, Restart*, Stop (supervisor end)
// Start: new instance
// preStart callback -- messages -- failure -- Restart -- preRestart callback -- die
// new instance, state cleared
// postRestart callback -- messages -- wanna stop -- Stop -- postStop callback -- die

{ // default callbacks, you can redefine them
  trait Actor {
    // once
    def preStart(): Unit = {}

    //  0..many times
    def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      context.children foreach (context.stop(_))
      postStop()
    }
    def postRestart(reason: Throwable): Unit = {
      preStart()
    }

    // once
    def postStop(): Unit = {}
  }
}

// example: default lifecycle, DBActor, just reconnect to DB
{
  class DBActor extends Actor {
    val db = DB.openConnection()

    override def postStop(): Unit = db.close()
  }
}

// example: external state, hook/unhook to ext.system;
// on restart do nothing.
// children restarted recursively
{
  class Listener(source: ActorRef) extends Actor {
    override def preStart() { source ! RegisterListener(self) }
    override def preRestart(reason: Throwable, msg: Option[Any]) {}
    override def postRestart(reason: Throwable): Unit = {}
    override def postStop(): Unit = { source ! UnregisterListener(self) }
  }
}
