implicit val pref = "log"
def log(x: Any)(implicit pref: String) = println(s"${pref}: $x")
log("for-expression")("queries")

//> queries: for-expression

// 'for' notation is essentially equivalent to the common
// operations of DB query languages

// example
case class Book(title: String, authors: List[String])

val books = List(
    Book(title = "SICP", authors = List("Abelson, Harald", "Sussman, Gerald")),
    Book(title = "Effective Java", authors = List("Bloch, Joshua")),
    Book(title = "Java Puzzlers", authors = List("Bloch, Joshua")))

// find books whose author's name is 'Bloch'
for {
    b <- books
    a <- b.authors
    if a contains "Bloch"
} yield b.title

// names of all authors with 2 books at least
// author name repeats, not good
for {
    b1 <- books
    b2 <- books
    if b1 != b2
    a1 <- b1.authors
    a2 <- b2.authors
    if a1 == a2
} yield a1

// can we avoid it?
// combinations: 2, filter out: 1
for {
    b1 <- books
    b2 <- books
    if b1.title < b2.title // check each book once
    a1 <- b1.authors
    a2 <- b2.authors
    if a1 == a2
} yield a1

// what if an author has published 3 books?
val bkz = Book(title = "test", authors = List("Bloch, Joshua")) :: books

// author is printed 3 times
// combinations: 6, filter out: 3
val ators = for {
    b1 <- bkz
    b2 <- bkz
    if b1.title < b2.title // check each book once
    a1 <- b1.authors
    a2 <- b2.authors
    if a1 == a2
} yield a1

// we can use 'distinct'
ators.distinct
// or, set: val books = Set ... like a real DB
ators.toSet
