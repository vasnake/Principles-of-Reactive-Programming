import scala.util.Try
import scala.util.control.NonFatal

// type with flatMap and unit, obeying 3 laws: monad

// a monad M is a parametric type M[T]
// with 2 operations
// flatMap, unit
trait M[T] {
    def flatMap[U](func: T => M[U]): M[U]
}
def unit[T](x: T): M[T]

// flatMap is called 'bind'

// List is a monad, with unit(x) = List(x)
// Option is a monad, with unit(x) = Some(x)
// Generator is a monad, unit(x) = single(x)

// map can be defined for every monad as a combination of
// flatMap and unit
/*
m map f == m flatMap(x => unit(f(x)))
    == m flatMap (f andThen unit)
*/

// 3 laws of a monad
// 1: associativity
// m flatMap f flatMap g == m flatMap (x => f(x) flatMap g)
// 2: left unit
// unit(x) flatMap f == f(x)
// 3: right unit
// m flatMap unit == m

// monad laws for Option
abstract class Option[+T] {
    def flatMap[U](func: T => Option[U]): Option[U] = this match {
        case None => None
        case Some(x) => func(x)
    }
}

// check the left unit law
// unit(x) flatMap f == f(x)
Some(x) flatMap f == f(x)

// right unit law
// m flatMap unit == m
opt flatMap Some == opt

// associative law
// m flatMap f flatMap g == m flatMap (x => f(x) flatMap g)
opt flatMap f flatMap g == opt flatMap (x => f(x) flatMap g)
==
opt match {case Some(x) => f(x) case None => None }
    match {case Some(y) => g(y) case None => None }
==
opt match {
    case Some(x) => f(x) match {case Some(y) => g(y) ... }
    ...
}
==
opt match {
    case Some(x) => f(x) flatMap g
    ...
}

// what these laws meaning?
// associativity says essentially that one can 'inline' nested for-expr
for(y <- for(x <- m; y <- f(x)) yield y
    z <- g(y)) yield z
==
for(x <- m;
    y <- f(x)
    z <- g(y)) yield z

// right unit law says
for (x <- m) yield x
==
m

// left unit not used in for-expr

// Try monad (like Option)
// Success/Failure
// actually, Try is not a monad: left unit law fails for Try (see below)

abstract class Try[+T]
case class Success[T](x: T) extends Try[T]
case class Failure(ex: Exception) extends Try[Nothing]

// for wrapping exceptions
// note: Option wraps nulls

// how to wrap?

Try(expr) // Success(someVal) or Failure(someExc)

// how Try is implemented?
 object Try {

    def apply[T](expr: => T): Try[T] =
    // can you see call-by-name parameter?
        try Success(expr)
        catch { case NonFatal(ex) => Failure(ex) }

}

// composing Try
// it's the beauty of monads

for {
    x <- computeX
    y <- computeY
} yield f(x,y)
// on success it will be Success(f(x,y))
// on error this will return Failure(ex)

// for this we need map, flatMap
abstract class Try[T] {

    def flatMap[U](func: T => Try[U]): Try[U] = this match {
        case fail: Failure => fail // do nothing on failed object
        case Success(x) =>
            try func(x)
            catch { case NonFatal(ex) => Failure(ex) }
    }

    def map[U](func: T => U): Try[U] = this match {
        case fail: Failure => fail // do nothing on failed object
        case Success(x) => Try( func(x) )
    }
}

// or, defining map using flatMap and unit
t map f
==
t flatMap (x => Try(f(x)))
==
t flatMap (f andThan Try)

// Try is not a monad, left unit law is fails
// 2: left unit
// unit(x) flatMap f == f(x)

Try(expr) flatMap f != f(elxpr)
// left-hand side will never rise a non-fatal exception
// whereas right-hand side will

// but, it's a win:
// an expression composed from 'Try', 'map', 'flatMap' will never
// throw a non-fatal exception

