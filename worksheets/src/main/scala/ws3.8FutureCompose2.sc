//Composing Futures 2
//https://class.coursera.org/reactive-002/lecture/129

//TOC
//some people say: recursion is a GOTO for FP, use high-order functions!
//– avoid recursion ; use foldRight and foldLeft
//– refreshing memory:
//    (a,b,c).foldRight(acc)(f) = f(a, f(b, f(c, acc)))
//(a,b,c).foldLeft(acc)(f) = f(f(f(acc, a), b), c)
//– retry using foldLeft
//def retry(nTimes: Int)(block: =>Future[T]): Future[T] = {
//    val ns = (1 to nTimes).toList
//    val attempts = ns.map(_ => ()=>block)
//    val failed = Future.failed(new Exception..
//    val result = attempts.foldLeft(failed)( (a,block) => a recoverWith { block() } )
//    result }
//retry(3) { block } = unfolds to ((failed recoverWith{block()})
//    recoverWith{block()} )
//recoverWith{block()}
//– retry using foldRight
//def retry(nTimes: Int)(block: =>Future[T]): Future[T] = {
//    val ns = (1 to nTimes).toList
//    val attempts = ns.map(_ => ()=>block)
//    val failed = Future.failed(new Exception..
//    val result = attempts.foldRight( () =>failed) ((block, a) => () => {block() fallbackTo{a()}} )
//    result() }
//retry(3) {block} = unfolds to
//    block fallbackTo { block fallbackTo { block fallbackTo {failed} } }
//– in this case recursive solution is a simpliest one
//– you need to know where use recursion, foldLeft, foldRight and even more HO functions: mastering FP

import scala.concurrent.{Awaitable, Future}

// Lecture 4.10 - Composing Futures 2
// Future 'retry' with 'foldLeft' and 'foldRight'

// recursive retry was
{
    trait Future[T] {

        def retry(nTimes: Int)(block: => Future[T]): Future[T] = {
            // retry block at most nTimes
            // and give up after that
            if(nTimes <= 0) {
                Future.failed(new Exception("can't do"))
            } else {
                block fallbackTo { retry(nTimes-1){ block } }
            }
        }
    }
}
// can you do this w/o recursion?

// foldLeft, foldRight reminder:
List(a,b,c).foldRight(acc)(func)
// expands to
func(a, func(b, func(c, acc)))

List(a,b,c).foldLeft(acc)(func)
// expands to
func(func(func(acc, a), b), c)

// foldLeft retry solution
{
    trait Future[T] {
        def recover(func: PartialFunction[Throwable, T]): Future[T]
        def recoverWith(func: PartialFunction[Throwable, Future[T]]): Future[T]

        def retry(nTimes: Int)(block: => Future[T]): Future[T] = {
            val attempts = (1 to nTimes).map(_ => ()=>block) // n anon functions
            val failed = Future.failed(new Exception("oops")) // neutral item, accum
            // loop: start with fail, recoverWith block
            val result = attempts.foldLeft(failed)(
                (fut, block) => fut.recoverWith { block() })

            result
        }
    }
}

// foldRight retry solution
{
    trait Future[T] {
        def recover(func: PartialFunction[Throwable, T]): Future[T]
        def recoverWith(func: PartialFunction[Throwable, Future[T]]): Future[T]
        def fallbackTo(that: => Future[T]): Future[T] = {
            this recoverWith { case _ => that recoverWith { case _ => this }}
        }

        def retry(nTimes: Int)(block: => Future[T]): Future[T] = {
            val attempts = (1 to nTimes).map(_ => ()=>block) // n anon functions
            val failed = Future.failed(new Exception("oops"))
            // loop: call block, fallbackTo failure
            val result = attempts.foldRight( ()=>failed ) // delayed execution all the time
            ((block, fut) => ()=>{ block().fallbackTo{ fut() }})
            // blk1 fallbackto { blk2 fallbackto { blk3 fallbackto { failed }}}
            result()
        }
    }
}

// strait recursion wins!

// bonus

import scala.util.{Failure, Success, Try}

// Lecture 4.9 - Implementation of flatMap on Future
// how can flatMap be used instead of onComplete?

// simplified implementation, demo
// flatMap in terms of onComplete
{
    trait Future[T] {
        def onComplete(callback: Try[T] => Unit): Unit = ???
        def flatMap[S](func: T => Future[S]): Future[S] = ???
    }
}

// follow the types ...
// rewrite onComplete method to apply HO function and callback to result of async op.
{
    trait Future[T] {
        self =>
        // alias for this

        def onComplete(callback: Try[T] => Unit): Unit = println("done")

        def flatMap[S](func: T => Future[S]): Future[S] =
        // 1. return future
            new Future[S] {
                // 2. define onComplete
                override def onComplete(callback: Try[S] => Unit): Unit =
                    self onComplete {
                        case Success(x) => func(x).onComplete(callback) // Unit
                        case Failure(e) => callback(Failure(e)) // Unit
                    }
            }
    }
}
