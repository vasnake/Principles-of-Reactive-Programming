//Eventual Consistency (15:49)
//https://class.coursera.org/reactive-002/lecture/99

//TOC
//– everything takes time: async concept
//– cluster consistency, eventually
//– strong consistency: access to data is serialized => execution thread is blocked
//def update(...) = synchronized {…}
//def read(...) = synchronized { … }
//– it's easy enough for local system – same JVM, use locks
//– weak consistency: after an update conditions need to be met until reads return the updated value
//inconsistency window ; access require to perform synchronization
//private @volatile var field = 0
//def update(...) = Future { synchronized {…}}
//def read() = field
//– lock moved into another execution context;
//https://en.wikipedia.org/wiki/Weak_consistency
//there can be no access to a synchronization variable if there are pending write operations.
//And there can not be any new read/write operation started if system is performing any synchronization operation
//– eventual consistency: special type of a weak consistency :
//there is a time after which all reads return the last written value (if no more updates are made)
//– example: eventually consistent Store
//case class Sync(x, timestamp)
//class DistributedStore extends Actor { … var lastUpdate = currentTimeMillis() …
//def recieve = { … case Update(x) => peers foreach (_ ! Sync(field, lastUpdate)) …
//– actors and eventual consistency : deeply related concepts
//limited: at most can be eventually consistent // see CAP theorem
//e.c. requires eventual dissemination of all updates; protocol that can garantee message delivery
//actors are not auto e.c.;
//needs to employ suitable data structures, e.g. CRDT (Convergent and Commutative Replicated Data Types)
//– A comprehensive study of Convergent and Commutative Replicated Data Types by
//Marc Shapiro, Nuno Preguica, Carlos Baquero, Marek Zawirski
//– actor itself : sequentially consistent, serialized
//– example: the cluster membership state is a convergent data type / CRDT // joining, up, leaving, ...
//DAG of states // graph
//conflicts can always be resolved locally
//conflict resolution is commutative // a+b = b+a

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef}

import scala.concurrent.Future

// communications take time; async events;
// decisions require consistency, wait for it, eventually it's there

// cluster is eventually consistent

// example: strong consistency
// immediately after update, all reads will return the updated value
{
  class BancAccount {
    private var field = 0
    def update(op: Int => Int): Int = synchronized {
      field = op(field)
      field
    }
    def read(): Int = synchronized { field }
  }
}
// all accesses to variable are serialized, potential deadlocks; performance impact

// example: weak consistency
// after an update (async), conditions need to be met until reads return the updated value:
// inconsistency window.
// volatile is a hidden 'synchronized' and a 'don't use cache' instructions
{
  class BancAccount {
    private @volatile var field = 0
    def update(op: Int => Int): Future[Int] = Future {
      synchronized {
        field = op(field)
        field
      }
    }
    def read(): Int = field
  }
}
// async update, performance is good; still potential deadlocks

// eventual consistency: special kind of weak consistency:
// once no more updates are made to an object,
// there is a time after which all reads return the last written value.

// example: eventually consistent distributed store.
// Update, Get, Result: external interface
{
  object DistributedStore {
    case class Update(x: Int)
    case object Get
    case class Result(x: Int)
    case class Sync(x: Int, timestamp: Long)
    case object Hello
  }

  class DistributedStore extends Actor {
    import DistributedStore._

    var peers: List[ActorRef] = Nil
    var field = 0
    var lastUpdate = System.currentTimeMillis()

    override def receive: Receive = {
      case Update(x) =>
        field = x
        lastUpdate = System.currentTimeMillis()
        peers foreach (_ ! Sync(field, lastUpdate))
      case Get => sender ! Result(field)
      case Sync(x, timestamp) if timestamp > lastUpdate =>
        field = x
        lastUpdate = timestamp
      case Hello =>
        peers ::= sender
        sender ! Sync(field, lastUpdate)
    }
  }
}
// convergence achieved using ordering by timestamp: lastUpdate value

// testing that e.c.d.s in repl
{
  import akka.actor._
  implicit val system = ActorSystem("distributed")
  import ActorDSL._
  implicit val sender = actor(new Act { become { case msg => println(msg) }})
  import DistributedStore._

  val a = system.actorOf(Props[DistributedStore])
  val b = system.actorOf(Props[DistributedStore])

  a ! Get
  b ! Get
  a ! Update(42); a ! Get
  b ! Get
  a.tell(Hello, b); b ! Get
  b ! Update(43); b ! Get
  a ! Get
  b.tell(Hello, a); a ! Get
  system.shutdown()
}

// one actor itself have strong consistency;
// collaborating actors can at most be eventually consistent (messages take time)
// delivery at-least-once required.
// shared data structure need to be convergent and commutative (updates in any order)
// CRDT: Convergent and Commutative Replicated Data Types

// optional: how CRDT works

// example: cluster node state is CRDT
// -- directed acyclic graph of states
// -- conflicts can always be resolved locally
// -- conflict resolution is commutative
/*
  joining - up - leaving - exiting - removed
  plus: unreachable - down - removed

  consider this: node a got info about node b from nodes c and d.
  c info: b is down; d info: b is leaving.
  In the state DAG we can see, that down 'after' leaving, so we can assume that
  b is down.
  conflict resolved locally.
  conflict resolution is commutative: no matter what signal comes first, merged result
  always the same.
 */
