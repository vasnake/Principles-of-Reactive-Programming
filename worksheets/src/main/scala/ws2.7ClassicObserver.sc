//Imperative Event Handling: The Observer Pattern (12:27)
//https://class.coursera.org/reactive-002/lecture/107
//
//TOC
//– user interface, classic event handlig: observer pattern
//– views need to react to changes in a model : Observer pattern / publish-subscribe / MVC
//– model: state of application
//– view subscribe, model publish
//– trait Publisher { var subscribers: Set[Subscriber]; def subscribe; def publish …
//– trait Subscriber { def handler(p: Publisher) }
//– example, class BankAccoun extends Publisher { … }
//– class Consolidator(observed: List[BankAccount]) extends Subscriber { … }
//– observer pattern, the good: view decoupled from state/model; any number of views; simple
//– the bad: handlers are Unit-type, forces imperative style; many moving parts;
//– don't fit in concurrency; views are still tightly bound to one state, update happens immediately.
//– Adobe code (2008): 1/3 – event handling; ½ of the bugs
//– bottom line: not good enough
//– reactive: events => signals, messages, functions, futures, observables, actors

// events, signals
// FP magic pill: Feature
// FRP: Functional Reactive Programming

// Lecture 4.1 - Imperative Event Handling: The Observer Pattern
// publish/subscribe, MVC

// view need to react to changes in a model/state of an app
// view subscribe on changes in a model
// model publish its changes to subscribers

trait Publisher {
    // mutable state!
    private var subscribers: Set[Subscriber] = Set()

    def subscribe(sub: Subscriber): Unit =
        subscribers += sub

    def unsubscribe(sub: Subscriber): Unit =
        subscribers -= sub

    def publish(): Unit =
        subscribers.foreach(_.handler(this))
}

trait Subscriber {
    def handler(pub: Publisher): Unit = println("callback ...")
}

// pub/sub intertwined, not good

// take a look to a BankAccount as a publisher
class BankAccount extends Publisher {
    private var balance = 0

    // public getter for subscribers
    def currentBalance: Int = balance

    def deposit(amount: Int) =
        if(amount > 0) {
            balance += amount
            // call subscribers
            publish()
        }

    def withdraw(amount: Int): Unit =
        if(0 < amount && amount <= balance) {
            balance -= amount
            // call subscribers
            publish()
        }
        else throw new Error("insufficient funds")
}

// observer, view that will be called when bank acc changes
class Consolidator(observed: List[BankAccount]) extends Subscriber {
    // add this to the list of subscribers in bank account
    observed.foreach(_.subscribe(this)) // call handler on acc change

    // mutable state!
    private var total: Int = _ // null
    compute()

    private def compute() =
        total = observed.map(_.currentBalance).sum

    override def handler(pub: Publisher) = compute()

    def totalBalance = total
}

// lets play
val a, b = new BankAccount
val c = new Consolidator(List(a,b))
c.totalBalance
a deposit(20)
c.totalBalance
b deposit(30)
c.totalBalance

// observer pattern, good part
// -- decouples views from state
// -- allows to have any number of views
// -- simple

// bad part
// -- forces imperative style, side effects
// -- many parts that need to be co-ordinated
// -- concurrency make things more complicated
// -- views updates immediately; tightly bound to state

// we can do better?
