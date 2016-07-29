//The Actor Model (13:43)
//https://class.coursera.org/reactive-002/lecture/75

//TOC
//– Actors are objects, model represents their interactions
//– like: info exchange between 2 persons
//– speaking – send a message, hearing – receive a message
//– messages delivery takes time
//– rely only on messages
//– Actor: object with identity; has a behaviour; interacts using async message passing
//– message processed in a context different from his origin
//– trait Actor : describes the behaviour of an actor (method receive)
//– method receive: Receive // PartialFunction Any => Unit ; describes reaction to a message
//– reaction can include sending message back: case ('get', cli: ActorRef) => cli ! count
//– 'client tell count'
//– implicit argument 'sender' = self
//– actors can create other actors, change behaviour : trait ActorContext; implicit val context
//– execution machinery provided by actor context
//– each actor has a stack of behaviours, active top one
//– context methods 'become' and 'unbecome' for push/pop actor behaviours
//– become example: stateful counter w/o explicit var
//– similar to 'async tail-recursion'; state is scoped to current behaviour
//– another thing: create an actor, stop (often self)
//– context 'actorOf', 'stop' methods
//– actors created by actors – hierarchy of actors
//– example: runnable app – counter // eclipse setup for running app
//– bottom line: upon reception of a message actor can do:
//send message; create actor; designate the behaviour for the next message

import akka.actor.Actor._
import akka.actor._

// interactions between objects: humans model
// excange messages, async
// Actor: object with identity, has behaviour, interact using async messages
// Actor can: send messages, create/stop actors, change behaviour

// messages

// the Actor trait
{
    trait Actor {
        def receive: Receive
    }
    type Receive = PartialFunction[Any, Unit]
}

// Counter actor
{
    class Counter extends Actor {
        private var count = 0
        def receive = {
            case "incr" => count += 1
        }
    }
}
// no output, not really statefull

// actor address: ActorRef
// stateful actor: Counter
{
    class Counter extends Actor {
        private var count = 0
        def receive = {
            case "incr" => count += 1
            case ("get", customer: ActorRef) => customer ! count
            // customer 'tell' count
        }
    }
}

// implicit sender, how messages are sent
{
    trait Actor {
        implicit val self: ActorRef
        def sender: ActorRef
    }
    abstract class ActorRef {
        def !(msg: Any)(implicit sender: ActorRef = Actor.noSender): Unit
        def tell(msg: Any, sender: ActorRef) = this.!(msg)(sender)
    }
}

// rewrite counter using 'sender'
{
    class Counter extends Actor {
        private var count = 0
        def receive = {
            case "incr" => count += 1
            case "get" => sender ! count
        }
    }
}

// behaviour

// actor have an implicit context
// context is used to redefine behaviour, stack push/pop
{
    trait ActorContext {
        def become(behaviour: Receive, discardOld: Boolean = true): Unit
        def unbecome(): Unit
    }
    trait Actor {
        implicit val context: ActorContext
    }
}

// example: Counter can maintain state changing context (recursion, async tailrec)
// advantages: explicit state change (only one point of change);
// state is scoped to current behaviour
{
    class Counter extends Actor {
        def counter(n: Int): Receive = {
            case "incr" => context.become(counter(n + 1))
            case "get" => sender ! n
        }
        def receive = counter(0)
    }
}

// actor can create/stop actor

// hierarchy of actors
{
    trait ActorContext {
        def actorOf(p: Props, name: String): ActorRef
        def stop(a: ActorRef): Unit
    }
}

// example: application 'main extends actor'
{
    class CounterMain extends Actor {
        val counter = context.actorOf(Props[Counter], "counter")

        override def receive: Receive = {
            case count: Int => // get count
                println(s"count was $count")
                context.stop(self)
        }

        counter ! "incr"
        counter ! "incr"
        counter ! "incr"
        counter ! "get"
    }

    class Counter extends Actor {
        def counter(n: Int): Receive = {
            case "incr" => context.become(counter(n + 1))
            case "get" => sender ! n
        }
        def receive = counter(0)
    }
}
// jvm args: main class: akka.Main; -Dakka.loglevel=WARNING
// prog.arg: week5.CounterMain;
