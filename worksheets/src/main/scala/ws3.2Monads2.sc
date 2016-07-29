//Monads and Effects 2
//https://class.coursera.org/reactive-002/lecture/117
//
//TOC
//first effect: failures/exceptions. deal with failures using Try[T] monad
//– express possible failure
//– change 'T => S' to 'T => Try[S]'
//– abstract class Try[T] … case class Success … extends Try[T] … case class Failure … extends Try[Nothing]
//– trait Adventure … def collectCoins(): Try[List[Coin]] …
//– dealing with failure explicitly:
//val treasure = coins match { case Success … case failure@Failure(e) => failure }
//– not so good looking
//– will use high-order functions over Try (flatMap) – happy path, forget about exceptions
//– it's possible because Try[T] is a monad! that handles exceptions
//val treasure = adv.collectCoins().flatMap(coins => { adv.buyTreasure(coins) })
//or, using comprehension syntax
//val treasure = for { coins ← adv.collectCoins(); treasure ← buyTreasure(coins) } yield treasure
//– Try[T] design: flatMap and constructor
//def map[S](f: T => S): Try[S] = this match { case Success(val) => Try(f(val)); case failure@Failure(t) => failure }
//object Try { def apply[T](r: =>T): Try[T] = { try { Success(r) } catch { case t => Failure(t) } }

abstract class Try[T]
case class Success[T](elem: T) extends Try[T]
case class Failure(t: Throwable) extends Try[Nothing]

trait Adventure {
    def collectCoins(): Try[List[Coin]] = ???
    def buyTreasure(coins: List[Coin]): Try[Treasure] = ???
}

// two possible outcomes from actions, ok

// this is ugly
def PlayI(): Unit = {
    val adventure = Adventure()
    val coins: Try[List[Coin]] = adventure.collectCoins()
    val treasure: Try[Treasure] = coins match {
        case Success(cs)          => adventure.buyTreasure(cs)
        case Failure(t)           => Failure(t)
    }
}

// higher-order functions comes to resque
// monad helps to take the happy path
def PlayII(): Unit = {
    val adventure = Adventure()
    val coins: Try[List[Coin]] = adventure.collectCoins()
    val treasure: Try[Treasure] =
        coins.flatMap(cs => adventure.buyTreasure(cs))
}

// or, using for-expression (comprehension) syntax
def PlayIII(): Unit = {
    val adventure = Adventure()
    val treasure: Try[Treasure] = for {
        coins <- adventure.collectCoins()
        treasure <- buyTreasure(coins)
    } yield treasure
}

// how it works?
trait Try[T] {
    def map[S](op: T => S): Try[S] = this match {
        case Success(value) => Try(op(value))
        case failure@Failure(t) => failure
    }
}
object Try {
    def apply[T](expr: => T): Try[T] = {
        try Success(expr)
        catch { case t => Failure(t) }
    }
}
