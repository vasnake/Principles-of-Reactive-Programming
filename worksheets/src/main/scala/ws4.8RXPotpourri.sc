//RX potpourri (11:30)
//https://class.coursera.org/reactive-002/lecture/149

//TOC
//– creating observables: 'from' Future , using AsyncSubject
//– computation runs only once, side effects is subtle
//object Observable {
//    def apply[T](fut: Future[T]): Observable[T] = {
//        val subj = AsyncSubject[T]()
//        fut.onComplete {
//            case Failure(e) => { subj.onError(e) }
//            case Success(t) => { subj.onNext(t); subj.onCompleted() } }
//        subj } }
//– Notification data structure for materializing 3 cases/callbacks: onNext,onError,onCompleted
//def materialize: Observable[Notification[T]] = {...}
//– observable.toBlocking.forEach(... // bad practice
//– reduce: alike in SQL – reducing you get table with one row, there you get observable with single value
//    only one way to get scalar/iterable: toBlocking // don't do it
//– observable 'from' iterable
//object Observable {
//    def apply[T](subscribe: Subscriber[T] => Unit): Observable[T] }
//def from[T](ts: Iterable[T]): Observable[T] = Observable(
//    s => { ts.foreach(t =>
//    { if(s.isUnsubscribed) { break } s.onNext(t) })
//        s.onCompleted() } )
//– backpressure: https://github.com/ReactiveX/RxJava/wiki/Backpressure

import rx.lang.scala.Observable
import rx.lang.scala.subjects.AsyncSubject
import scala.util.control.Breaks._

// creating Observable

// Observable 'from' Future
// using AsyncSubject: create channel, push value to channel, once
{
    object Observable {
        def apply[T](fut: Future[T]): Observable[T] = {
            val subj = AsyncSubject[T]() // like promise

            fut.onComplete { // single value, once
                case Failure(e) => { subj.onError(e) }
                case Success(t) => { subj.onNext(t); subj.onCompleted() }
            }
            // return subsject
            subj
        }
    }
}

// from iterable
{
    def from[T](ts: Iterable[T]): Observable[T] =
        Observable(subs => {
        // constructor: object Observable { def apply[T](subs: Subscriber[T] => Unit): Observable[T] = ??? }
            ts.foreach(t => {
                // check if unsubscribed
                if (subs.isUnsubscribed) { break }
                subs.onNext(t) // push next value
            })
            subs.onCompleted() // close
        })
}

// observable notifications
// unwrap 3 callback cases (instead of pattern matching)
{ // can switch from pattern matching to 3callbacks (and other way around)
    def materialize: Observable[Notification[T]] = ???

    abstract class Notification[+T] // covariant
    case class onNext[T](elem: T) extends Notification[T]
    case class onError(err: Throwable) extends Notification[Nothing]
    case class onCompleted extends Notification[Nothing]
}

// blocking observable: bad practice
{
    val ts: Observable[T] = obs.toBlocking // wait to complete
    ts.forEach(t => {???})

    // check this
    val xs: Observable[Long] = Observable.interval(1 second).take(5)

    // bad
    val ys: List[Long] = xs.toBlockingObservable.toList
    println(ys)

    // ok
    val zs: Observable[Long] = xs.sum
    val s: Long = zs.toBlockingObservable.single
    println(s)
}

// converting observable to scalar
// reduce: like a SQL: you get not a scalar, but a table with a single row
// you get Observable with a single value
{
    def reduce(op: (T,T) => T): Observable[T]
}

// consumer is slower than producer
// backpressure support: https://github.com/ReactiveX/RxJava/wiki/Backpressure
