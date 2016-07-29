//Latency as an Effect 1
//https://class.coursera.org/reactive-002/lecture/119
//
//TOC
//– next effect: latency
//– computation can take time : we need async: Future[T]
//– Future[T] – monad for latency
//– networking script: Adventure game => socket transfer
//trait Socket { def readFromMemory(): Array[Byte]; def sendToEurope(packet: Array[Byte]): Array[Byte]
//val sock = Socket(); val pack = sock.readFromMemory(); val confirmation = sock.sendToEurope(pack)
//– time for ops: fetch from main mem = 100 nanosec; from L1 cache = 0.5 nanosec
//send 2Kb over 1Gbps net = 20 000 nanosec
//– readFromMemory: block for 50 000 ns, continue if there is no exception
//– sendToEurope: block for 150 000 000 ns, continue if no exception
//– in other scale: read = 3 day, send = 5 year
//– express that latency, make it explicit
//– allow non-blocking call, we can't wait 5 years: use callback
//– ok, non-blocking actions: we need sequential composition of actions
//– we need a monad for that composition

// previous: exceptions as effect: computations can fail
// wrap it into Try monad

// now: computations can take time
// wrap in into Future monad

// previous toy example
trait Coin { ??? }
trait Treasure { ??? }
trait Adventure {
    def collectCoins(): List[Coin] = ???
    def buyTreasure(coins: List[Coin]): Treasure = ???
}
val adventure = Adventure()
val coins = adventure.collectCoins()
val treasure = adventure.buyTreasure(coins)

// morph this into network app
trait Socket {
    def readFromMemory(): Array[Byte]
    def send2Europe(packet: Array[Byte]): Array[Byte]
}

val socket = Socket()
val packet = socket.readFromMemory()
val confirm = socket.send2Europe(packet)

// takes a lot of time
//val packet = socket.readFromMemory()
//val confirm = socket.send2Europe(packet)

// lets say: 1 nanosec = 1 sec
// read 1 MB from RAM: 3 days
// send packet from US to Europe and back: 5 years
// and this ops can fail!

// we want to express it explicitly
// thus, we won't wait days and years, we wanna do some useful stuff
// Async ops: call, bind callback, do next thing
