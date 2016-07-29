//Hello World Observables (6:29)
//https://class.coursera.org/reactive-002/lecture/141

//TOC
//– simple example, observables
//– stream of values, every 1 sec.; filter them; group in chunks of 2, shifted by 1; print
//val ticks: Observable[Long] = Observable.interval(1 seconds)
//val evens: Observable[Long] = ticks.filter(_ % 2 == 0)
//val bufs = Observable[Seq[Long]] = evens.slidingBuffer(count=2, skip=1)
//val s = bufs.subscribe(println(_))
//....
//s.unsubscribe()
//– marble diagram
//

import rx.lang.scala.{Observable, Subscription}
import scala.language.postfixOps
import scala.concurrent.duration._

def ticks(): Unit = {

    val ticks: Observable[Long]        = Observable.interval(1 second)
    // Observable is a collection monad!
    val evens: Observable[Long]        = ticks.filter(s => s%2 == 0)
    val buffers: Observable[Seq[Long]] = evens.buffer(2, 1)

    // run the program for a while
    val subscription: Subscription     = buffers.subscribe(println(_))

    readLine()

    // stop the stream
    subscription.unsubscribe()
}
