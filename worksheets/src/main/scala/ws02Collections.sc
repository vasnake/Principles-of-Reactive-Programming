// recap: collections

//core collections methods
//mep, flatMap, filter
//foldLeft, foldRight

// idealized implementation of map on lists
/*
abstract class List[+T] {

def map[U](func: T => U): List[U] = this match {
    case Nil => Nil
    case x :: xs => func(x) :: xs.map(func)
}

// flatMap
def flatMap[U](func: T => List[U]): List[U] = this match {
    case Nil => Nil
    case x :: xs => func(x) ++ xs.flatMap(func)
}

// filter
def filter(p: T => Boolean): List[T] = this match {
    case Nil => Nil
    case x :: xs => if(p(x)) x :: xs.filter(p) else xs.filter(p)
}

}
*/

val n = 5
def isPrime(n: Int) = (2 until n) forall (d => n % d != 0)

// for expressions

// this ugly 2 nested loops
(1 until n) flatMap (i =>
    (1 until i) filter (j =>
        isPrime(i + j)) map (j => (i, j)))

// can be rewritten as
for {
    i <- 1 until n
    j <- 1 until i
    if isPrime(i + j)
} yield (i, j)
// compiler will rewrite it back to flatMap, filter, map

// rules
// 1.
// for(x <- e1) yield e2
// to
// e1.map( x => e2)
// 2.
// for (x <- e1 if f; s) yield e2
// to
// for (x <- e1.withFilter(x=>f); s) yield e2
// 3.
// for (x <- e1; y <- e2; s) yield e3
// to
// e1.flatMap(x => for(y <- e2; s) yield e3)

// left-hand side of a generator may also be a pattern

for {
    JObj(bindings) <- data
    JSeq(phones) = bindings("phoneNumbers")
    JObj(phone) <- phones
    JStr(digits) = phone("number")
    if digits startsWith "212"
} yield (bindings("firstName"), bindings("lastName"))

// pattern 'pat' with a single variable 'x' translated
// pat <- expr
// to
x <- expr withFilter {
    case pat => true
    case _ => false
} map {
    case pat => x
}

// exercise
val N = 7
for {
    x <- 2 to N
    y <- 2 to x
    if (x % y == 0)
} yield (x, y)

// expands to ?
(2 to N) flatMap (x =>
    (2 to x) withFilter (y =>
        x % y == 0) map (y => (x, y)))
