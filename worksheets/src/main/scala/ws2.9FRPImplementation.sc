//A Simple FRP Implementation (19:32)
//https://class.coursera.org/reactive-002/lecture/111
//
//TOC
//– FRP API implementation : Signal, Var // automating and hiding observers logic // propagate updates behind the scene
//– Signal: constructor, apply
//– Var extends Signal: update
//– each signal maintains: current value; current expression; set of observers
//– how do we record dependencies in observers? detect who call sig.apply() – observer, save it
//– Signal constructor/update push 'this' into global stack; then base sig. 'apply' pop 'caller' from stack.
//– who call sig() // apply() – value getter? maintain global data struture – stack
//– class StackableVariable[T] … { var values: List[T] … def withValue[R](newValue: T)(op: => R): R ...} // DynamicVariable
//– object NoSignal extends Signal[Nothing] as stack 'sentinel' (or initial value) for signal expressions evaluation
//– object Signal { … var caller = new StackableVariable[Signal][_](NoSignal) …
//– class Signal[T](expr: => T) { var myExpr: () => T = _; var myValue: T = _; var observers: Set[Signal[_]] = Set(); update(expr) … }
//– reevaluating callers : after signal update, call other observers // def computeValue
//– NoSignal …  def computeValue() = () // disabled
//– class Var[T](expr: => T) extends Signal[T](expr) { override update … }
//– and this is it. built on worst kind of state: global state
//– try to replace global state by thread-local state: scala.util.DynamicVariable
//– object Signal { … var caller = new DynamicVariable[Signal][_](NoSignal) …
//– its still not very good: imperative nature of state; JDK implementation not good; multiplexed threads …
//– we have better solution: involving implicit parameters
//– instead of maintaining global state, pass its current value into a signal expression as an implicit parameter
//– this is purely functional, but requires more boilerplate
//– we only cover teaser of  one style of FRP: discrete signals changed by events
//– also existst: FRP variants dealing with continuous signals computed by sampling, not event propagation

// after Scala.react

// Lecture 4.3 - A Simple FRP Implementation
// functional reactive programming

// Signal/Var implementation

import scala.util.DynamicVariable

// API
{
    class Signal[T](expr: => T) {
        // call-by-name expression, evaluated when event 'pulled' from signal
        def apply(): T = ???

        // evaluate expression and get event value
    }

    object Signal {
        // companion constructor
        def apply[T](expr: => T) = new Signal(expr)
    }

    class Var[T](expr: => T) extends Signal[T](expr) {
        // myvar() = 5 // setter with side-effect
        def update(expr: => T): Unit = ???
    }

    object Var {
        def apply[T](expr: => T) = new Var(expr)
    }
}

// idea: each signal maintains:
// -- its current value
// -- the current expression that defines the signal value
// -- a set of observers: the other signals that depend on its value

// how do we record dependencies in observers?
// when evaluating a signal-valued expression, need to know which signal caller
// gets defined or updated by the expression: identify caller.
// executing a 'sig()' means adding caller to the observers of 'sig'.
// when sig's value changes, all obesrvers are re-evaluated and the set
// of observers is cleared.
// re-evaluation of observer signal will re-enter that signal back to the set of observers,
// as long as caler's value still depends on 'sig'

// who's calling?
// lets start with global data structure, stack of signals calls

class StackableVariable[T](init: T) { // DynamicVariable
    // stack
    private var values: List[T] = List(init)

    // value on top of the stack
    def value: T = values.head

    // push value on stack while evaluating the 'op'
    def withValue[R](newValue: T)(op: => R): R = {
        // operation: call-by-name expression
        values = newValue :: values
        try op finally values = values.tail
    }
}

// usage example
// first signal, stays in stack
val caller = new StackableVariable(iniSig)
// next signal, 'context' concept
caller.withValue(otherSig) {
    ???
}

// stack + sentinel object NoSignal lead to that
{
    class Signal[T](expr: => T) {
        def apply(): T = ???
    }

    // sentinel signal on top of stack of callers
    object NoSignal extends Signal[Nothing](???) {
        ???
    }

    object Signal {
        // global! stack of signal calls
        private val caller = new StackableVariable[Signal[_]](NoSignal)

        def apply[T](expr: => T) = new Signal(expr)
    }

}

// having a tool for detecting signal caller, signal implementation
// could be like that
{
    class Signal[T](expr: => T) {

        // anonimous function, uninitialized yet
        private var myExpr: () => T = _

        // current value, uninitialized yet
        private var myValue: T = _

        // set of signal observers
        private var observers: Set[Signal[_]] = Set()

        // initialize
        update(expr)

        // n.b: update is 'protected', not public
        protected def update(expr: => T): Unit = {
            myExpr = () => expr // myExpr is a function
            //call it
            computeValue()
        }

        protected def computeValue(): Unit = {
            myValue = caller.withValue(this)(myExpr())
            ???
        }

        def apply(): T = {
            // caller signal added to observers
            observers += caller.value
            // check for applications like: s() = s() + 3
            assert(!caller.value.observers.contains(this), "cyclic signal definition")
            // return current value
            myValue
        }
    }
}

// as you can see, previous conclusions about 'push vs pull' was wrong.
// signal implementation does push updates along caller chain,
// exactly as traditional Observer pattern.
// Signal value updated when event source is updated,
// apply method just return the current value.
// The difference is that we have implicit list of observers.

// OK, how we propagate updates?
// in 'computeValue' method
{

    class Signal[T](expr: => T) {

        private var myExpr: () => T = _

        private var myValue: T = _

        private var observers: Set[Signal[_]] = Set()

        update(expr)

        protected def update(expr: => T): Unit = {
            myExpr = () => expr
            computeValue()
        }

        // signal was updated directly or by observers chain
        protected def computeValue(): Unit = {
            // calculate a new value
            val newValue = caller.withValue(this)(myExpr())
            if (myValue != newValue) {
                // signal changed
                myValue = newValue
                // update observers and clean the set
                val obs = observers
                observers = Set()
                obs.foreach(_.computeValue())
            }
        }
    }

    // sentinel signal on top of stack of callers
    object NoSignal extends Signal[Nothing](???) {
        // sentinel do nothing
        override def computeValue() = ()
    }

}

// about Var: that kind of signals can be updated by client code
{
    class Signal[T](expr: => T) {

        private var myExpr: () => T = _
        private var myValue: T = _
        private var observers: Set[Signal[_]] = Set()

        update(expr)

        protected def update(expr: => T): Unit = {
            myExpr = () => expr
            computeValue()
        }

        protected def computeValue(): Unit = {
            val newValue = caller.withValue(this)(myExpr())
            if (myValue != newValue) {
                myValue = newValue
                val obs = observers
                observers = Set()
                obs.foreach(_.computeValue())
            }
        }
    }

    class Var[T](expr: => T) extends Signal[T](expr) {

        // public 'update'
        override def update(expr: => T): Unit =
            super.update(expr)
    }

    object Var {
        def apply[T](expr: => T) = new Var(expr)
    }
}

// and thats it.

// but, we have at least one major issue: global state
// that prohibited parallel execution
{
    object Signal {
        private val caller = new StackableVariable[Signal[_]](NoSignal)
    }
}
// one workaround is to replace global state by thread-local state
// each thread have a personal variable
// it is supported in Scala through class
// scala.util.DynamicVariable

// API of DynamicVariable matches the one of StackableVariable
// so we can simply swap it into
{
    object Signal {
        private val caller = new DynamicVariable[Signal[_]](NoSignal)
    }
}

// but, that state still have a number of disadvantages
// -- imperative nature produces hidden dependencies which are hard to manage
// -- its implementation on the JDK involves a global hash table lookup,
// which can be a performance problem
// -- it does not play well in situations where threads are multiplexed
// between several tasks (thread pool)

// implicit parameters can solve this problems
// passing state current value to a signal as an implicit parameter
// can be a saviour.
// this is purely functional, but it requires more boilerplate
// Future versions of Scala might solve that

// bottom line
// we considered one particular style of functional reactive programming
// discrete signals changed by events
// -- set of variables that updates by chain of callers/observers, wrapped in Signal class
// of objects.
// some variants of FRP also treat continuous signals
// by 'sampling' instead of event propagation
