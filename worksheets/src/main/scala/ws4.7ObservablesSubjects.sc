import rx.lang.scala.subjects.{PublishSubject, ReplaySubject}

import scala.concurrent.Promise

//Promises and Subjects (8:55)
//https://class.coursera.org/reactive-002/lecture/147

//TOC
//– Promise for Future → Subjects for Observables
//– Promise for Future – promise 'hasa' future
//def map[S](f: t => S)(implicit context): Future[S] = {
//    val p = Promise[S](); this.onComplete {
//        case t => try { p.success(f(t)) } catch { case e => p.failure(e) } }
//    p.future }
//– Subject for Observable – subject 'isa' observable
//– subject do nothing if you call 'onComleted' twice, but promise throw error
//– subject are like channel: on one side pump in values, on the other – subscribers get data
//– subject are like combination of observer and observable
//val channel = PublishSubject[Int]()
//val a = channel.subscribe(x => println(s'a: $x'))
//val b = channel.subscribe(x => println(s'b: $x'))
//channel.onNext(42)
//a.unsubscribe()
//channel.onNext(4711)
//channel.onCompleted()
//val c = channel.subscribe(x => println(s'c: $x'))
//channel.onNext(13)
//– in that example sub 'c' only get empty observable, but
//– ReplaySubject: has history/memory, caches all values – sub 'c' will get all values
//– other subjects:
//    async subject: caches final value
//behaviour subj: caches latest value
//– bad subjects: like mutable variables; not work well in backpressure;
//– in most cases you don't need subjects

// creating streams, Subject vs Promise

// promise works like this
def map[T, S](fut: Future[T], op: T => S): Future[S] = {
    val p = Promise[S]()

    fut.onComplete { // delayed future complete
        case t =>
            try { p.success(op(t)) }
            catch { case e => p.failure(e) }
    }

    p.future
}
// promise 'hasa' future

// subject 'isa' observable, like a channel in-out
// you can push values to channel (like complete future in promise)

trait Subject[T] {
    // Observer
    def onNext
    def onCompleted
    def onError

    // Observable
    def subscribe
}

// kinds of subjects
{
    val channel = PublishSubject[Int]() // simplest
    val a = channel.subscribe(x => println(s"a: $x")) // 42
    val b = channel.subscribe(x => println(s"b: $x")) // 42, 4711, !
    channel.onNext(42) // push value into channel
    a.unsubscribe()
    channel.onNext(4711)
    channel.onCompleted() // close !
    val c = channel.subscribe(x => println(s"c: $x")) // !
    channel.onNext(13) // dropped on the floor
}
{
    val channel = ReplaySubject[Int]() // has memory
    val a = channel.subscribe(x => println(s"a: $x")) // 42
    val b = channel.subscribe(x => println(s"b: $x")) // 42, 4711, !
    channel.onNext(42)
    a.unsubscribe()
    channel.onNext(4711)
    channel.onCompleted() // close !
    val c = channel.subscribe(x => println(s"c: $x")) // 42, 4711, !
    channel.onNext(13) // dropped on the floor
}
// others: async, behaviour

// try not to use subjects, it's imperative


