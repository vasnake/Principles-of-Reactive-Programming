import rx.lang.scala.subscriptions.{CompositeSubscription, MultipleAssignmentSubscription}

//Subscriptions (10:34)
//https://class.coursera.org/reactive-002/lecture/145

//TOC
//– what happens when x.unsubscribe? (x = q.subscribe; y = q.subscribe )
//– x will not receive any items but y still 'live'
//– subscription causes side effect: each subscriber has its own source == Cold Observable
//– Hot Observable: same source shared by all subscribers, subscription w/o side effect
//– unsubscribing != cancellation : unsubscribing don't cancel stream or other subscriptions
//– subscriptions form an interesting algebra (state: isUnsubscribed; ops: unsubscribe, subscribe; collection)
//collection of subscriptions, unsubscribe all it once: CompositeSubscription
//MultiAssignmentSubscription: allow replace/swap subscription source
//    SerialSubscription: subscribing to next source if unsubscribe from prev.
//– idempotent subscriptions: call 'unsubscribe' many times – action only once
//– unsubscribe composite: all subs in collection will be unsubscribed
//– adding sub to unsubscribed composite: sub will be unsubscribed
//– also multi
//– unsubscribe inner item do nothing with collection / has no effect on container
//– subscriptions used in observables in ops implementations
//– this algebra used inside other ops

// unsubscribing

val quakes: Observable[EarthQuake] = ???
val s1: Subscription = quakes.Subscribe(f1)
val s2: Subscription = quakes.Subscribe(f2)

s1.unsubscribe()
// what happens to s2?
// s2 keep recieving messages
// unsubscribing != cancellation

// cold vs hot observables

// cold observable: each subscriber has its own source
// subscription side-effect: create a new source

// hot observable: same source shared by all subscribers

// basics

trait Subscription {
    def unsubscribe(): Unit
    def isUnsubscribed: Boolean
}
object Subscription {
    def apply(unsubscribe: => Unit): Subscription = ???
}

// unsubscribe is idempotent: can't break anything by repeatedly calling
// unsubscribe

// Subscription algebra
// 3 kinds of subscriptions

// collections of subscriptions, unsubscribe alltogether
class CompositeSubscription extends Subscription {
    def +=(s: Subscription): this.type
    def -=(s: Subscription): this.type
}

// swap underlying subscription
class MultiAssignmentSubscription extends Subscription {
    def subscription: Subscription
    def subscription_=(that: Subscription): this.type
}

// special case of MultiAssignment: unsubscribed when swapping
class SerialSubscription extends Subscription {
    def subscription: Subscription
    def subscription_=(that: Subscription): this.type
}

// composite demo
val a = Subscription { println("A") }
val b = Subscription { println("B") }
val c = Subscription { println("C") }
val composite = CompositeSubscription(a, b)

println(composite.isUnsubscribed) // no
composite.unsubscribe()
println(composite.isUnsubscribed) // yes
println(a.isUnsubscribed) // yes
println(c.isUnsubscribed) // no
composite += c
println(c.isUnsubscribed) // yes

// multi demo
val multi = MultipleAssignmentSubscription()
multi.subscription = a
multi.subscription = b
multi.unsubscribe() // b unsubscribe
multi.subscription = c // c unsubscribe

// subscriptions algebra is used not in client code, but in operators/combiners
