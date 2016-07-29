//Functional Reactive Programming (20:24)
//https://class.coursera.org/reactive-002/lecture/109
//
//TOC
//– reactive programming is about reacting to sequences of events that happen in time
//– functional: aggregate sequence into a signal // wrap and hide callbacks mechanics
//– signal: monadic approach: value that changes over time ; as a function (time domain => value domain)
//– replace (hide) updates of mutable state to this: define new signals in terms of existing ones
//– example: mouse positions
//– event-based: event MouseMoved(to: Position) – execute procedure
//– FRP: a signal mousePosition: Signal[Position] – at any time represents current position – generate some data
//– signal as function (time => position) ; client call a function that 'recomputes' all chain of signals
//– FRP origins: 1997 – Fran lib, event streaming, frp.Signal extends Scala.react // Deprecating the Observer Pattern
//– signal operations: get the current value – signal.apply(); define a signal – Signal constructor
//– constant signals : val sig = Signal(42)
//– signal that varies in time: externally defined signals; or can use a 'Var extends Signal' ;
//– Var provides an 'update' operation: sig.update(5)
//– sintactic shugar for update: arr(i) = 3 == arr.update(i, 3)
//– sig() = 5 == sig.update(5)
//– signals vs variables: we can map over signals (producing a relation that maintained automagically),
//but we have to propagate all changes for variables manually
//– example: BankAccount with signals : val balance = Var(0)
//– mutable state saved in object Signal[Int]
//– much cleaner solution if compare to publisher/subscriber
//– for vars: v = v+1 // ok
//– for signals: s() = s() + 1 // s to be at all points in time one larger than itself, make no sense
//– be careful : signal updates != variables assignments

// events, signals
// FP magic pill: Feature
// FRP: Functional Reactive Programming

// Lecture 4.2 - Functional Reactive Programming

// pull versus push?

// reactive programming: about reacting to sequences of events
// that happen in time

// FP: aggregate an event sequence into one signal
// signal: value that changes over time // monad? no
// represented as a function time => domain value.
// define new signals in terms of existing ones
// instead of propagating updates to mutable state

// example: mouse positions

// events style
// fire event
def MouseMoved(newpos: Position)

// FRP style
// signal, represents current mouse position
def mousePosition: Signal[Position]

// well, Signal, what is it?
// monadic way to process events?
// 2 fundamental operations over signals:
// -- obtain the value of the signal at the current time
// implemented as 'apply': val curpos = mousePosition()
// -- define a new signal in terms of other signals
// implemented by using the Signal constructor

def inRectangle(LL: Position, UR: Position): Signal[Boolean] =
    Signal {
        val pos = mousePosition()
        LL <= pos && pos <= UR
    }

// but, how do we define a signal that varies in time?

// externally defined signals, such as mousePosition
// and 'map' over them

// or, we can use a 'Var': subclass of Signal
// Var provides an 'update' operation
val sig = Var(3)
sig.update(5) // sig() = 5
// like 'event generated', signal changed

// step aside, about syntax
val arr = Array(1,2,3); val i = 0
// FYI: in Scala, 'update' operation can be written as assignment
// actually
arr(i) = 0
// translated to
arr.update(i, 0)
// thus, 'sig.update(5)' equal 'sig() = 5'

// OK, back to Var // it look like a var? isn't it?

// crucial difference between var and Var is:
// pull vs push?
// we have to push updates (manually) if we use mutable var
// and we can pull updates from signals
// using newSig = sig map doStuffWithEventValue
// !!! relation maintained automatically !!!
// in other words: functions vs state

// lets see on example: BankAccount with signals
// note: take a look to BankAccount with pub/sub


class BankAccount {
    // make balance a signal
    val balance = Var(0)

    def deposit(amount: Int): Unit =
        if(amount > 0) {
            // update signal
            // balance() = balance() + amount // cycle, a no-no
            val b = balance()
            balance() = b + amount
        }

    def withdraw(amount: Int): Unit =
        if(0 < amount && amount <= balance()) {
            // update signal
            val b = balance()
            balance() = b - amount
        }
        else throw new Error("insufficient funds")
}

// new observer
def consolidated(accs: List[BankAccount]): Signal[Int] =
// create signal from sum of all accounts
    Signal(accs.map(_.balance()).sum)

// play
val a, b = new BankAccount
val c = consolidated(List(a,b))
c() // 0
a deposit 20
c() // 20
b deposit 30
c() // 50

// a few more signals

val xchange = Signal(246.0)
val inUsd = Signal(c() * xchange())
inUsd() // 12300.0
b withdraw 10
inUsd() // 9840.0

// you can see, how higher-order functions do the trick
// pulling actual values from signals

// exercise

// 1
{
    val num = Var(1)
    val twice = Signal(num() * 2)
    num() = 2
}
// 2
{
    var num = Var(1)
    val twice = Signal(num() * 2)
    num = Var(2)
}
// do they yield the same final value for twice()?
// no, in case 2 we have num become a new signal, old didn't update
