//Functions and State (15:28)
//https://class.coursera.org/reactive-002/lecture/47
//
//TOC
//– for pure functions the concept of time is not important // no side effects
//– this reflects on the Substitution Model of computation
//– SM: rewrite, reduce expressions to values due to program evaluating; essential: function application rewriting.
//– rewriting can be done anywhere in a term, all rewritings lead to the same solution – result of the lambda-calculus // confluence, aka:
//– Church-Rosser theorem:  the ordering in which the reductions are chosen does not make a difference to the eventual result
//– it does not apply to stateful objects
//– object has a state if its behaviour is influenced by its history; state changes over the course of time
//– example: bank account
//– state constructed from variables: var x = 'olala'; x = 'boo' // versus 'val' // change through an assignment
//– class with variable members : BankAccoun … var balance ...
//– statefulness connected with having variables; how strong? not strong at all.
//– lets see, stream implementation using 'variable tail', not 'lazy val tail' – still not stateful.
//– that implementation, just caching tail value, is not stateful, but variable was used allright
//– another example, BankAccountProxy, w/o variable: is stateful because uses stateful BankAccoun object

// in pure FP, w/o mutable state, the concept of time wasn't important

// reminder: substitution model
// program evaluating by rewriting

// example
def iterate(n: Int, func: Int => Int, x: Int): Int =
    if(n == 0) x else iterate(n-1, func, func(x))

def square(x: Int) = x*x

iterate(1, square, 3) // 9

// call gets rewritten as
// take right-hand side, replace arguments
if(1 == 0) 3 else iterate(1-1, square, square(3))
// as
iterate(0, square, square(3))
// as
iterate(0, square, 9)
// as
if(0 == 0) 9 else iterate(0-1, square, square(9))

// lambda-calculus: rewriting can be done anywhere in a term,
// solution won't change from it
// -- no state!

// enters state,
// an object has a state if its behaviour is influenced by its history
// e.g bank account

// every form of mutable state is constructed from variables
var x: String = "abc"
var count = 111

// association name-value can be change later through an assignment
count = count+1

// state in objects
class BankAccount {
    private var balance = 0

    def deposit(amount: Int) =
        if(amount > 0) balance += amount

    def withdraw(amount: Int): Int =
        if(0 < amount && amount <= balance) {
            balance -= amount
            balance
        }
        else throw new Error("insufficient funds")
}

val acc = new BankAccount
acc deposit 50
acc withdraw 20
acc withdraw 20
acc withdraw 25 // java.lang.Error: insufficient funds
// same operations -- different result: depends on history

// statefullness connected to having variables, right?
// let's check it

// Stream, replace lazy val with var
def cons[T](hd: T, tl: => Stream[T]) = new Stream[T] {
    def head = hd

    private var tlOpt: Option[Stream[T]] = None

    def tail: T = tlOpt match {
        case Some(x) => x
        case None => tlOpt = Some(tl); tail
    }
}
// in that case, is the result of 'cons' a stateful object?
// if you see it as a black box, then no, not stateful
// behaviour don't depends on history

// another example
class BankAccountProxy(ba: BankAccount) {
    def deposit(amount: Int) = ba.deposit(amount)
    def withdraw(amount: Int) = ba.withdraw(amount)
}
// are instances of BankAccountProxy stateful objects?
// yes, behaviour depends on history
