//Loops (8:25)
//https://class.coursera.org/reactive-002/lecture/35
//
//TOC
//– another imperative prog. property: loop – control statement
//– variables are enough to model all imperative programs? yes.
//– while (i > 0) exp ; 'while' – keyword
//– how to emulate loop using function?
//– function WHILE using call-by-name params and tail-recursion
//– function REPEAT emulate 'do-until'
//– 'for' loop in Scala // similar to For-Expression
//– for-loop translates using 'foreach' combinator, not map/flatMap

// modeling imperative programs: state, control structures (loops)

// how to emulate loop

def power (x: Double, exp: Int): Double = {
    var r = 1.0
    var i = exp
    while(i > 0) { r = r*x; i = i - 1 }
    r
}
// while is a keyword

// we can define 'while' using a function
def _while(condition: => Boolean )(command: => Unit ): Unit =
    if(condition) {
        command
        _while(condition)(command)
    }
    else ()
// the condition and the command are call-by-name
// so that they're reevaluated in each iteration
// and, _while is tail recursive

// what about repeat { command } ( condition )
def _repeat(command: => Unit)(condition: => Boolean): Unit = {
    command
    if (condition) ()
    else _repeat(command)(condition)
}

// can you do this?
// repeat { command } until ( condition )

// for-loop can't be modeled simply by a higher-order function
// reason: in Java, for(int i = 1; i < 3; i = i+1) { do-someth )
// declared var i is visible in other arguments and in the body

// in Scala we have another for-loop
for(i <- 1 until 3) { System.out.print(i + " ") }
// print 1 2

// is it for-expression?
// no, it is a for-loop

// for-loops translate similarly to for-expr, but using
// the foreach combinator instead of map/flatMap
def foreach(func: T => Unit): Unit

// example
for(i <- 1 until 3; j <- "abc") println(i + " " + j)
// translates to
(1 until 3) foreach (i => "abc" foreach (j => println(i + " " + j)))
