//Combinators on Futures 1
//https://class.coursera.org/reactive-002/lecture/123

//TOC
//– Future monad: happy path, compose/combine actions
//– flatMap
//– val packet = sock.readFromMemory() // Future
//val confirmation = packet.flatMap(p => sock.sendToEurope(p)) // Future
//– sendToEurope : request can fail if net is down
//Http(url, Request(packet)).filter(resp => resp.isOK).map(resp => resp.toByteArray)
//– lets send it twice
//def sendToAndBackup(pack: Array[Byte]): Future[(Array, Array)] =
//val eur = sendTo(mailServer.eur, packet)
//val usa = sendTo(mailServer.usa, packet)
//eur.zip.usa // conforms to shortest, no good
//– send packets robustly , Future methods recover* are like map/flatMap for error case
//– allow us to compose both channels: Success and Failure
//def recover(f: PartialFunction[Throwable, T]): Future[T]
//def recoverWith(f: PartialFunction[Throwable, Future[T]]): Future[T]
//def sendToSafe(pack: Array): Future[Array] =
//    sendTo(mailServer.eur, pack) recoverWith {
//        case europeError => sendTo(mailServer.usa, pack) recover {
//            case usaError => usaError.msg.toByteArray} }
//– not really good, err.message from Europe gone … next lecture

// Lecture 4.6 - Combinators on Futures 1

// example was: socket returns future

trait Socket {
    def readFromMemory(): Future[Array[Byte]]
    def send2Europe(packet: Array[Byte]): Future[Array[Byte]]
}

val sock = Socket()
val packet = sock.readFromMemory() // Future
val confirm = packet.onComplete { // ??? we need future, get Unit?
        case Success{p} => sock.send2Europe(p) // Future
        case Failure(t) => ??? // ???
    }

// can we do better?
// yes, monad combinators can help
// how?

// take future monad
trait Awaitable[T] extends AnyRef {
    abstract def ready(atMost: Duration): Unit
    abstract def result(atMost: Duration): T
}
trait Future[T] extends Awaitable[T] {
    // happy path
    def filter(p: T => Boolean): Future[T]
    def flatMap[S](func: T => Future[S]): Future[S]
    def map[S](func: T => S): Future[S]

    // domain specific method, similar to flatMap/map (look at signatures)
    // only for 'error' channel
    def recoverWith(func: PartialFunction[Throwable, Future[T]]): Future[T] // flatMap
    def recover(func: PartialFunction[Throwable, T]): Future[T] // map
}
object Future {
    // constructor, call-by-name expression with long-time computations
    def apply[T](body: => T): Future[T] = ???
}

// and rewrite example as
{
    // happy path
    val sock = Socket()
    val packet = sock.readFromMemory() // Future
    val confirm = packet.flatMap(data => sock.send2Europe(data)) // Future
    // or, using for-expr
    val confirmI = for(data <- packet) yield sock.send2Europe(data)
    // or
    val confirmII = for {
        p <- sock.readFromMemory() // flatMap
        c <- sock.send2Europe(p) // map
    } yield c
}
// looks good.
// what about error handling?
// you can use recover* methods

// toy implementation of send2Europe
{
    trait Socket {
        // happy path
        def send2Europe(packet: Array[Byte]): Future[Array[Byte]] =
            Http(URL("mail.server.eu"), Request(packet))
                .filter(response => response.isOK)
                .map(response => response.toByteArray)
    }
    object Http {
        // http request runs async
        def apply(url: URL, req: Request): Future[Response] = ???
    }
}
// looks good but, what about error processing?
// can we write more resilient code?

// yes, using recover{With}
// it's like map/flatMap only for error channel of Future
{
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
                    // if failed, sent to usa
                    case europeError => sendTo(mailServer.usa, packet)
                        // if failed, error from the usa
                        .recover {
                            // get error message from usa, not really good
                            case usaError => usaError.getMessage.toByteArray
                        }
                }
        // if error, call recover ...
        // fugly again
    }
}
// not really good: send to europe, get message from usa
// see next lecture ...
