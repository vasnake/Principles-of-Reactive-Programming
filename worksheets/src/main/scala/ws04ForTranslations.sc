implicit val pref = "log" //> pref  : String = log
def log(x: Any)(implicit pref: String) = println(s"${pref}: $x")
log("for-expression")("translation to map, filter")

// for-expressions is just a syntactic shugar
// for map/flatMap/filter

// syntax of 'for' is closely related to the higher-order functions
// map, flatMap, filter
{
    def map[T, U](xs: List[T], func: T => U): List[U] =
        for (x <- xs) yield func(x)

    def flatMap[T, U](xs: List[T], func: T => Iterable[U]): List[U] =
        for (x <- xs; y <- func(x)) yield y

    def filter[T](xs: List[T], p: T => Boolean): List[T] =
        for (x <- xs if p(x)) yield x

    ""
}

// only it goes the other way
// for(x <- e1) yield e2
// to
// e1.map(x => e2)

// for(x <- e1 if f; s) yield e2
// to
// for(x <- e1.withFilter(x => f); s) yield e2

// for(x <- e1; y <- e2; s) yield e3
// to
// e1.flatMap(x => for(y <- e2; s) yield e3)

// example
val n = 3
def isPrime(n: Int) = (2 until n) forall (x => n % x != 0)

for {
    i <- 1 until n
    j <- 1 until i
    if isPrime(i + j)
} yield (i, j)

// translates to
(1 until n).flatMap(i =>
    (1 until i).withFilter(j =>
        isPrime(i + j)).map(j => (i, j)))

// exercise
// translate next 'for' to higher-order functions

case class Book(title: String, authors: List[String])

val books = List(
    Book(title = "SICP", authors = List("Abelson, Harald", "Sussman, Gerald")),
    Book(title = "Effective Java", authors = List("Bloch, Joshua")),
    Book(title = "Java Puzzlers", authors = List("Bloch, Joshua")))

for {
    b <- books
    a <- b.authors
    if a contains "Bloch"
} yield b.title

// translates to

books.flatMap {
    b =>
        b.authors.withFilter {
            a => a contains "Bloch"
        } map {
            a => b.title
        }
}

// any data type can be used in 'for'
// if map, flatMap, withFilter defined

// databased, XML, arrays, ...
// ScalaQuery, Slick
// MS LINQ
