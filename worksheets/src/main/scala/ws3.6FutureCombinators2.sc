//Combinators on Futures 2
//https://class.coursera.org/reactive-002/lecture/125

//TOC
//– Future monad, recovery combinator: fallbackTo
//– def fallbackTo(that: =>Future[T]): Future[T] = {
//    if this future fails take the successful result of that future
//    if that future fails too, take the error of this future }
//def sendToSafe(pack: Array): Future[Array] =
//    sendTo(mailServer.eur, pack) fallbackTo {
//        sendTo(mailServer.usa, pack) } recover {
//        case eurError => eurError.msg.toByteArray }
//– implemented as
//def fallbackTo(that: => Future[T]): Future[T] = { this recoverWith {
//    case _ => that recoverWith { case _ => this } } }
//– beautiful
//– async where possible, blocking where necessary
//– two methods allow to block execution, don't do it ever
//trait Awaitable … def ready … def result …
//val packet: Future = sock.readFromMemory()
//val confirm: Future = pack.flatMap(sock.sendToSafe(_))
//val debug = Await.result(confirm, 2 seconds); println(debug.toText)
//– postfixOps
//object Duration { def apply(len: Long, unit: TimeUnit): Duration }
//val fiveYears = 1826 minutes
//– combinators over Future[T]

import java.net.URL
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, TimeUnit}

// Lecture 4.7 - Combinators on Futures 2

// example was: send packet resiliently
// problem with error message
trait Socket {

    def sendTo(url: URL, packet: Array[Byte]): Future[Array[Byte]] =
        Http(url, Request(packet))
            .filter(response => response.isOK)
            .map(response => response.toByteArray)

    // if error, send to second server
    def sendToSafe(packet: Array[Byte]): Future[Array[Byte]] =
    // send to europe ...
        sendTo(mailServer.europe, packet)
            .recoverWith {
                case europeError => sendTo(mailServer.usa, packet)
                    .recover {
                        // get error message from usa, not really good
                        case usaError => usaError.getMessage.toByteArray
                    }
            }
}
// can we do better?

// lets try to write a better recovery combinator
{
    trait Future[T] {

        // with fallbackTo combinator we can write a nice code for Socket
        def fallbackTo(that: => Future[T]): Future[T] = {
            // if 'this' future fails, take the successful result of 'that' future ...
            // if 'that' future fails too, take the error of 'this' future
            this recoverWith { case _ => that recoverWith { case _ => this }}
        } // nice!

        def recover(func: PartialFunction[Throwable, T]): Future[T]
        def recoverWith(func: PartialFunction[Throwable, Future[T]]): Future[T]
    }

// nice socket.sendToSafe
    trait Socket {

        def sendTo(url: URL, packet: Array[Byte]): Future[Array[Byte]] =
            Http(url, Request(packet))
                .filter(response => response.isOK)
                .map(response => response.toByteArray)

        // if error, send to second server
        def sendToSafe(packet: Array[Byte]): Future[Array[Byte]] =
        // send to europe ... if-err: try usa ... if-err: message from europe
            sendTo(mailServer.europe, packet)
                .fallbackTo { sendTo(mailServer.usa, packet) }
                .recover { case europeError => europeError.getMessage.toByteArray }
    }
}
// good enough.

// what else?

// blocking methods, never-ever do this!
// use it wisely, only if necessary, for debug, may be
{
    trait Awaitable[T] extends AnyRef {
        // dangerous code, bloody murder!!!
        abstract def ready(atMost: Duration): Unit
        abstract def result(atMost: Duration): T
    }
    trait Future[T] extends Awaitable[T] {
        ???
    }

    // blocking example
    val sock = Socket()
    val pack = sock.readFromMemory() // Future
    val confirm = pack.flatMap(sock.sendToSafe(_)) // Future
    // bloody murder!!!
    // block and wait up to 2 seconds
    val infomsg = Await.result(confirm, 2 seconds)
    println(infomsg.toText)
}

//aside: postfixOps
{
    object Duration {
        def apply(length: Long, unit: TimeUnit): Duration = ???
    }
    val fiveYears = 1826 minutes
}
