//Monads and Effects 1
//https://class.coursera.org/reactive-002/lecture/115
//Erik Meijer

//TOC
//– RxScala >= 0.23
//– 'monad' == constructor + flatMap // vs 'unit', 'flatMap' and 3 laws: associativity, left unit, right unit.
//– 4 essential effects in programming: (sync, async) * (one, many)
//– Synchronous: one: T/Try[T]; many: Iterable[T] // monadic way
//– Asynchronous: one: Future[T]; many: Observable[T]
//– 2 cases: monad Try[T] for sync. exceptions; monad Future[T] for async. latency and exceptions.
//– first effect: exceptions : we need to deal with failures: monad Try[T]
//– example (sync): trait Adventure { def collectCoins(): List[Coin]; def buyTreasure(coins: List[Coin]): Treasure }
//– not that simple: actions may fail: def collectCoins … throw new GameOverExeption(«eaten by monster»)...
//– and sequential composition of actions may fail
//– failures undeclared
//– we need to expose possibility of failure
//– replace 'T => S' by 'T => Try[S]'

// toy example

trait Adventure {
    def collectCoins(): List[Coin] = ???
    def buyTreasure(coins: List[Coin]): Treasure = ???
}

val adventure = Adventure()
val coins = adventure.collectCoins()
val treasure = adventure.buyTreasure(coins)

// what if 'throw...'? actions may fail
def collectCoins(): List[Coin] = {
    if (eatenByMonster) throw new GameOver("Ooops")
    List(Gold(), Gold(), Silver())
}
// return type is a lie
def buyTreasure(coins: List[Coin]): Treasure = {
    if (coins.sumBy(x => x.value) < treasureCost)
        throw new GameOver("Nice try!")
    Diamond()
}
// we need to declare it; we need a robust code

// wrap type to a monad (railroad oriented programming: two inputs, two outputs
// vs one input, two outputs)
// T => S
// T => Try[S]
// may be Success, may be not: honestly expose possibility of failure
