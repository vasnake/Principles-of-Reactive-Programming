//Composing Futures 1
//https://class.coursera.org/reactive-002/lecture/127

//TOC
//– combinators over Future[T] : once we moved to future land we can't use regular control flow
//– we need high-order functions over Future[T] to make life easier
//– flatMap // for-comprehension
//val packet: Future = sock.readFromMemory()
//val confirm: Future = pack.flatMap(sock.sendToSafe(_))
//val confirm: Future = for { pack ← sock.readFromMemory(); confirm ← sock.sendToSafe(pack) } yield confirm
//– lets write another combinator
//def retry(nTimes: Int)(block: =>Future[T]): Future[T] = {
//    retry nTimes at most and give up }
//– using recursion
//if(nTimes == 0) { Future.failed(new Exception('oops')) } else {
//    block fallbackTo { retry(nTimes-1){ block } } }
//– some people say: recursion is a GOTO for FP, use high-order functions! ...

import scala.concurrent.{Awaitable, Future}

// Lecture 4.8 - Composing Futures 1
// for-expression on Future

// control structures in Future land? no way
// only higher-order functions, combinators

// last version of example was
{
    val sock = Socket()
    val pack = sock.readFromMemory() // Future
    val confirm = pack.flatMap(sock.sendToSafe(_)) // Future
}

// you could rewrite it in for-expr
{
    val sock = Socket()
    val confirmation: Future[Array[Byte]] = for {
        pack <- sock.readFromMemory()
        confirm <- sock.send2Safe(pack)
    } yield confirm
}

// lets try another approach to enhance resilience
{
    trait Future[T] extends Awaitable[T] {

        def retry(nTimes: Int)(block: => Future[T]): Future[T] = {
            // retry block at most nTimes
            // and give up after that
            if(nTimes <= 0) {
                Future.failed(new Exception("can't do"))
            } else {
                block fallbackTo { retry(nTimes-1){ block }}
            }
        }
    }
}
// can you do this w/o recursion?
