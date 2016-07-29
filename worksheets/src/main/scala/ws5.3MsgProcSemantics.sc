//Message Processing Semantics (27:28)
//https://class.coursera.org/reactive-002/lecture/77

//TOC
//– access to Actor by exchanging messages, only
//– to send message you need address: ActorRef
//– own address: self
//– creating actor you get his address, not instance
//– address in message: sender
//– every actor are completely independent agent of computation
//– all actors run fully concurrently; local execution
//– no notion of global sync
//– message: one-way communication
//– the most encapsulated form of OO
//– inside an actor is effectively single-threaded
//– messages are received sequenially
//– processing one message is the atomic unit of execution
//– blocking is replaced by enqueueing a message
//– example: BankAccount : actor; messages as case classes/objects
//deposit, withdraw: messages completely serialized/one-at-a-time
//– what about transferring money acc → acc? Alice to Bob, coordinator Tom
//– Tom to Alice: withdraw; Tom to Bob: deposit – 3 actors
//    2 BankAccount, 1 WireTransfer with message Transfer(from, to, amount)
//Transfer send Withdraw to Alice and 'become' awaitWithdraw
//awaitWithdraw process 2 cases: Done and Failed
//if done: send Deposit to Bob and 'become' awaitDeposit
//else: context.stop // kill Tom
//– debugging/logging events in Eclipse
//– delivery guarantees : communications unreliable
//– we need a protocol: A-doSometh, B-ok; if-not-ok: A-haveYouHeard?
//– delivery requires eventual availability of channel and recipient
//– keep talking: efforts classification:
//at-most-once: delivers [0, 1] times – w/o any state, sending once
//    at-least-once: [1, inf] – sender keep message and resend it until confirmed
//    exactly-once: 1 time – most costly, receiver also must have state: processed messages
//– messages support reliability
//messages can be persistent // save it to DB
//    can include token/id
//delivery can be repeated/restarted
//– reliability can only be ensured by business-level acknowledgement // business-logic
//– how to apply this principles to BankAccount app? keep/save the state
//log activities to persistent storage
//each transfer has a unique ID
//    add ID to Withdraw and Deposit
//    store IDs of completed actions within BankAccount
//– message ordering: Akka-specific – arrive in order to same destination // triangle route example
//    mostly undefined // need coordinator in business-level

import akka.actor.{Actor, ActorRef, Props}
import akka.actor.Actor.Receive
import akka.event.LoggingReceive

// messages, actor address (self, sender): only this is available
// no communication but messages

// local execution, fully concurrently, isolated
// synchronisation:

// messages are received sequentially;
// processing one message is the atomic unit of execution;
// an actor is effectively single-threaded;
// blocking is replaced by message queue

// example: BankAccount
{
    object BankAccount {
        // messages as case classes
        case class Deposit(amount: BigInt) { require(amount > 0) }
        case class Withdraw(amount: BigInt) { require(amount > 0) }
        case object Done
        case object Failed
    }
    class BankAccount extends Actor {
        import BankAccount._
        var balance = BigInt(0)

        override def receive: Receive = {
            case Deposit(amount) => { balance += amount; sender ! Done }
            case Withdraw(amount) if amount <= balance => {
                balance -= amount; sender ! Done
            }
            case _ => sender ! Failed
        }
    }
}
// no messages overlapping, sync style, atomic message processing

// collaboration: money transfer from A to B, with manager C

// example: WireTransfer
{
    object BankAccount {
        // messages as case classes
        case class Deposit(amount: BigInt) { require(amount > 0) }
        case class Withdraw(amount: BigInt) { require(amount > 0) }
        case object Done
        case object Failed
    }

    // n.b. akka.event.LoggingReceive
    class BankAccount extends Actor {
        import BankAccount._
        var balance = BigInt(0)

        override def receive: Receive = LoggingReceive {
            case Deposit(amount) =>
                balance += amount
                sender ! Done
            case Withdraw(amount) if amount <= balance =>
                balance -= amount
                sender ! Done
            case _ => sender ! Failed
        }
    }

    object WireTransfer {
        // messages
        case class Transfer(from: ActorRef, to: ActorRef, amount: BigInt)
        case object Done
        case object Failed
    }

    class WireTransfer extends Actor {
        import WireTransfer._

        override def receive: Receive = LoggingReceive {
            case Transfer(from, to, amount) => {
                from ! BankAccount.Withdraw(amount)
                context.become(awaitWithdraw(to, amount, sender))
            }
        }
        def awaitWithdraw(to: ActorRef, amount: BigInt, initiator: ActorRef): Receive = LoggingReceive {
            case BankAccount.Done =>
                to ! BankAccount.Deposit(amount)
                context.become(awaitDeposit(initiator))
            case BankAccount.Failed =>
                initiator ! Failed
                context.stop(self)
        }
        def awaitDeposit(initiator: ActorRef): Receive = LoggingReceive {
            case BankAccount.Done =>
                initiator ! Done
                context.stop(self)
        }
    }

    class TransferMain extends Actor {
        val accA = context.actorOf(Props[BankAccount], "accA")
        val accB = context.actorOf(Props[BankAccount], "accB")
        accA ! BankAccount.Deposit(100)

        override def receive: Receive = LoggingReceive {
            case BankAccount.Done => transfer(50)
        }
        def transfer(amount: BigInt) = {
            val trans = context.actorOf(Props[WireTransfer], "transfer")
            trans ! WireTransfer.Transfer(accA, accB, amount)
            context.become(LoggingReceive{
                case WireTransfer.Done =>
                    println("success")
                    context.stop(self)
            })
        }
    }
}
// run configuration:
// main class: akka.Main
// prog.args: week5.TransferMain
// VM args: -Dakka.loglevel=DEBUG -Dakka.actor.debug.receive=on

// communications are unreliable

// how you can guarantee messages delivery?
// use a protocol with confirmations

// semantics:
// -- at most once: no state
// -- at least once: state on sender
// -- exactly once: most complicated, need to maintain state on receiver

// messages support reliability (event sourcing?):
// all messages can be persistent;
// can include UID
// delivery can be retries until successful

// but, only business-level can ensure reliability, by acknowledgement:
// 'yes, I've done it'

// example: BankAccount
// write-ahead log: WireTransfer activities
// each transfer with tUID
// withdraw and deposit store tUID in BankAccount (maintain idempotent behaviour)

// message ordering: mostly undefined

// A sends multiple messages to B
// B receive messages in order
// this is Akka-specific
