//Observable Contract (14:19)
//https://class.coursera.org/reactive-002/lecture/151

//TOC
//– contract that not visible in the type
//– never implement observable yourself, use rx-lib: contract you dont fully know
//– same for all RX types: use lib factories (lib is fragile and overcomlicated)
//– create observable, concept
//object Observable {
//    def create(s: Observer => Subscription) = new Observable {
//        def subscribe(observer: Observer): Subscription = {
//            Implementation(s(observer)) }}}
//val s = Observable.create(f).subscribe(observer) = // conceptually
//val s = Implementation(f(observer))
//– subscribe/auto-unsubscribe : observer.onCompleted unsubscribe s automagically
//val s = Observable(f).subscribe(observer) = // conceptually
//val s = Implementation(f(Wrap(observer))
//– auto-unsubscribe behaviour : onCompleted/onError auto unsubscribe sub
//val empty = Observable(sub => { sub.onCompleted() })
//val s = empty.subscribe(); println(s.isUnsubscribed)
//– call sequence: (onNext)*(onCompleted+onError)? // 0..n 0..1 times
//    no calls after onCompleted/onError
//    calls always serialized, no overlaps
//    https://www.google.com/search?q=rx+design+guidelines

import jdk.internal.dynalink.linker.LinkerServices.Implementation
import rx.lang.scala.{Observable, Observer, Subscription}

// Observable conventions
// hidden, not obvious things in contract

// never implement observable yourself, use rx-lib: contract you dont fully know
// it's not trivial

// naive approach to observable creation
{
    object Observable {

        def create[T](subs: Observer[T] => Subscription): Observable[T] = {

            def subscribe(obs: Observer[T]): Subscription =
                Implementation(subs(obs)) // loads of things in implementation
        }
    }

    val s = Observable.create(func).subscribe(observer)
    // conceptually ==
    val s = Implementation(func(observer)) // loads of things in implementation
}

// auto-unsubscribe contract
val s = Observable(func).subscribe(observer)
// conceptually ==
val s = Implementation(f(wrap(observer))) // loads of things in implementation
// auto-unsubscribe when onCompleted, onError

// check this
val empty = Observable(subs => { subs.onCompleted() }) // close emmidiately
val s = empty.subscribe()
println(s.isUnsubscribed)

// Rx contract
// call sequence: (onNext)*(onCompleted+onError)?
// 0..n onNext, 0..1 onCompleted or 0..1 onError

// never implement observable yourself, use rx-lib: contract you dont fully know
// same for all RX types: use lib factories (lib is fragile and overcomlicated)
