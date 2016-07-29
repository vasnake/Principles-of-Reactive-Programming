import scala.collection.immutable.Queue
import scala.util._

//Latency as an Effect 2
//https://class.coursera.org/reactive-002/lecture/121

//TOC
//– non-blocking calls and sequential composition of actions: monad Future[T]
//– Future[T] handles latency and exceptions! happy path
//trait Future[T] { … def onComplete(callback: Try[T] => Unit)(implicit executor: ExecutionContext): Unit …
//– callback needs to use pattern matching: case Success … case Failure … ? or use high-order functions?
//– alternatives
//trait Future[T] { def onComplete(success: T => Unit, failed: Throwable => Unit): Unit
//def onComplete(callback: Observer[T]): Unit
//trait Observer[T] { def on Next(value: T): Unit; def onError(err: Throwable): Unit …
//– also it's called 'continuations'
//– ok, app with Future looks like
//trait Socket { def readFromMemory(): Future[Array[Byte]]; def sendToEurope(packet: Array[Byte]): Future[Array[Byte]]
//– callback with match … case … seems ugly
//– monad: happy path, flatMap .. next lecture
//– creating Futures
//object Future { def apply(body: =>T)(implicit context: ExecutionContext): Future[T] …
//val q = Queue[EMailMessage](EmailMessage(from = 'me', to = 'you'), EmailMessage(...
//def readFromMemory(): Future[Array[Byte]] = Future {
//val em = q.dequeue(); var ser = serialization.findSerializerFor(em); ser.toBinary(em) }
//– this code executes only once, even if we will register two (or more) callbacks

// now: computations can take time
// wrap it into Future monad

// Future[T]
// a monad that handles exceptions and latency
// explicit latency
{
    trait Future[T] {
        // essence: callback
        // will be called 'at most once'
        def onComplete(callback: Try[T] => Unit)
    }

    // callback need pattern matching but,
    // that design not only possible
    def myCallback(ts: Try[Int]): Unit = ts match {
        case Success(n) => onNext(n)
        case Failure(e) => onError(e)
    }
    // but, we can embed onNext & onError to some type
}

// alternative design, OO like

trait Observer[T] {
    // two callbacks, aka continuations
    def onNext(value: T): Unit
    def onError(err: Throwable): Unit
}
// pass that to Future
trait Future[T] {
    // two callbacks
    def onComplete(success: T => Unit, failed: Throwable => Unit): Unit
    // or, callback type
    def onComplete(callback: Observer[T]): Unit
}

// example, socket returns future

trait Socket {
    def readFromMemory(): Future[Array[Byte]]
    def send2Europe(packet: Array[Byte]): Future[Array[Byte]]
}

val sock = Socket()
val packet = sock.readFromMemory() // Future
val confirm = packet.onComplete { // callback: Unit, we want Future
        case Success{p} => sock.send2Europe(p) // Future
        case Failure(t) => ??? // ???
    }
// isn't it looks nasty? yep
// callbacks hell, a no-no, we need higher-order functions
// monad.flatMap is our friend, see the next lecture: combinators on futures

// for now lets talk about future construction

// companion object
object Future {
    // constructor, call-by-name expression with long-time computations
    def apply[T](body: => T) = ???
}

// usage
val queue = Queue[EMailMessage](
    EMailMessage(from = "Erik", to = "Roland"),
    EMailMessage(from = "Martin", to = "Erik"))

def readFromMemory(): Future[Array[Byte]] =
    Future {
        // body will be executed async, exactly once no matter how many callbacks
        val email = queue.dequeue()
        val serializer = serialization.findSerializerFor(email)
        serializer.toBinary(email)
    }
