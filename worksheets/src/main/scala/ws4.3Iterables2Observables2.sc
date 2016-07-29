//From Iterables to Observables 2 (9:44)
//https://class.coursera.org/reactive-002/lecture/139

//TOC
//we need callbacks
//– magic dualisation trick
//type Iterable[T] = () => (() => Try[Option[T]])
//– factory of a factory to generate values of type T
//– it's essence of the Iterable , sync
//– ok, trick: flip the arrows
//() => (() => Try[Option[T]])
//// flip
//(Try[Option[T]] => Unit) => Unit
//– seems like we need a callback around it, for push-based iterations
//– complexify : callback that takes a cb that takes a value
//type Observable[T] = (Try[Option[T]] => Unit) => Unit
//– Try/Option require pattern matching, so lets pull out condition branches
//type Observable[T] = (Throwable=>Unit, ()=>Unit, T=>Unit) => Unit
//// 3 cases: exception, nothing, value
//– encapsulate 3 cases into its own type : Observer
//type Observable[T] = Observer[T] => Unit
//type Observer[T] = (Throwable=>Unit, ()=>Unit, T=>Unit)
//– observer will have 3 functions, make it trait
//trait Observer[T] {
//    def onError(err: Throwable): Unit
//    def onCompleted(): Unit // empty collection
//    def onNext(value: T): Unit }
//– observable becomes trait
//trait Observable[T] {
//    def subscribe(observer: Observer[T]): Unit }
//– and what it means? Observable get a callback
//– callback have 3 cases-functions
//– Observable vs. Future: obs.callback on each call get another value; future get the same value
//– little more: subscribe returns subscription
//trait Observable[T] { def subscribe(observer: Observer[T]): Subscription }
//trait Subscription { def unsubscribe...; defisUnsubscribed... }
//– Iterable and Observable are dual
//– we had seen all 4 effects, connected via category theory

import scala.util.Try

// continue flipping arrows: Iterable to Observable derivation
// magic of Category Theory

type Iterable[T] =
    () => (() => Try[Option[T]])
// factory of factories of values :)
// 3 kind of values: exception, none, some

// flip the arrows, get a callback (push-based type)
type Observable[T] =
    (Try[Option[T]] => Unit) => Unit
// setter of a setter

// and complixify it back, grow functions to full-size traits

// unwrap Try[Option[T]] to 3 cases
type Observable[T] =
    (Throwable=>Unit, ()=>Unit, T=>Unit) => Unit

// move 3 functions to type
type Observer[T] = (Throwable=>Unit, ()=>Unit, T=>Unit)
// then
type Observable[T] = Observer[T] => Unit

// move to trait

trait Observer[T] { // vs Iterator
    def onError(err: Throwable): Unit
    def onCompleted(): Unit // empty collection, EOF
    def onNext(value: T): Unit
}

trait Observable[T] { // vs Iterable
    def subscribe(observer: Observer[T]): Subscription
}
// observable: something that I can feed a callback
// cb with 3 functions, all side-effected

// each time cb is called, it have a new data (vs Future)

// subscription can be cancelled (cancellation token)
// callback can be removed
trait Subscription {
    def unsubscribe(): Unit
    def isUnsubscribed: Boolean
}

// n.b. Iterable and Observable are dual
