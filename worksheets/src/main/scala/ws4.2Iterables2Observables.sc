//From Iterables to Observables 1 (8:06)
//https://class.coursera.org/reactive-002/lecture/137

//TOC
//– second half of effects: Iterable and Observable
//– trait Iterable is a base trait for all Scala collections: def iterator() : Iterator[T]
//– trait Iterator { def hasNext: Boolean; def next(): T
//– pull-based collections , synchronous
//– Iterable is a monad!
//– example, read files from disk (slow, sync)
//def ReadLinesFromDisk(path: String): Iterator[String] = {
//Source.fromFile(path).getLines() }
//val lines = ReadLinesFromDisk('/tmp/foobar')
//for (line <- lines) { ... }
//– 2 weeks for line – no way, go async
//– enters async streams
//– convert the pull model into a push model
//– first: simplify, get rid of 'hasNext' and change def next
//trait Iterator[T] { ... def next(): Option[T] // Some[T] or None[Nothing], just like Try/Success/Failure
//– trait with a single def = type
//type Iterator[T] = () => Option[T] // func void/Unit => Option
//– or, even more
//trait Iterable[T] { def iterator(): ()=>Option[T] ...
//– or, even more
//type Iterable[T] = () => (() => Try[Option[T]])
//– and we get HO function
//– lets flip the arrows, find duality

// base trait for all Scala collections
trait Iterable[T] { def iterator(): Iterator[T] }

trait Iterator[T] { def hasNext: Boolean; def next(): T }

//pull-based collections , synchronous

// it's a monad
def flatMap[B](op: A => Iterable[B]): Iterable[B]
def map[B](op: A => B): Iterable[B]
// constructor and other HO functions
// ...  and it's a functor (endofunctor)

// example: slow sync reading

def readLinesFromDisk(path: String): Iterator[String] =
    Source.fromFile(path).getLines()
val lines = readLinesFromDisk("/dev/rnd")
for (line <- lines) {
    println(line)
}

// can we derive async (dual) monad for Iterable?
// duality: let's convert the pull model into a push model

// rewrite things for simplicity
{
    // remove hasNext, change T to Option[T]
    trait Iterable[T] { def iterator(): Iterator[T] }
    trait Iterator[T] { def next(): Option[T] }

}
{
    // trait with 1 function? it's a type
    type Iterator[T] =  () => Option[T]
}
{
    // change iterable
    trait Iterable[T] { def iterator(): () => Option[T] }
}
{
    // again, trait with one func, rewrite it
    type Iterable[T] =  () => (()=>Option[T])
    // HO function
}
// next step: flip arrows
