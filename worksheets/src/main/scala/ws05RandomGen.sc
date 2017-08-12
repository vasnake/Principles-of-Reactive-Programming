// for-expressions
// is good not only for lists

//any object with map, flatMap, withFilter can be used in for-expression

//generation of random values

// what is a systematic way to get random values for other domains?

// generators concept first

trait Generator[+T] {
    def generate: T
}

val integers = new Generator[Int] {
    val rand = new java.util.Random

    def generate = rand.nextInt()
}

val booleans = new Generator[Boolean] {
    def generate = integers.generate > 0
}

val pairs = new Generator[(Int, Int)] {
    def generate = (integers.generate, integers.generate)
}

// and without 'new Generator' boilerplate?
// we would like to write
{
    val booleans = for (x <- integers) yield x > 0

    // or
    def pairs[T, U](t: Generator[T], u: Generator[U]) = for {
        x <- t
        y <- u
    } yield (x, y)
}
// obviously, we need to add map, flatMap, withFilter to Generator

// expanded version
{
    val booleans = integers map (x => x > 0)

    def pairs[T, U](t: Generator[T], u: Generator[U]) =
        t flatMap (x => u map (y => (x, y)))
}

// monoid Generator
{
    trait Generator[+T] {
        // an alias for this
        self =>

        def generate: T

        def map[S](func: T => S): Generator[S] =
            new Generator[S] {
                def generate = func(self.generate)
            }

        def flatMap[S](func: T => Generator[S]): Generator[S] =
            new Generator[S] {
                def generate = func(self.generate).generate
            }
    }

    val integers = new Generator[Int] {
        val rand = new java.util.Random

        def generate = rand.nextInt()
    }

    val booleans = for (x <- integers) yield x > 0
    // expanded to
    val booleans2 = integers map (x => x > 0)
    // expanded to
    /*
        val booleans3 = new Generator[Boolean] {
            def generate = (x: Int => x > 0) (integers.generate)
        }
        // simplified to
        val booleans4 = new Generator[Boolean] {
            def generate = integers.generate > 0
        }
    */
}

// the pairs generator
/*
def pairs[T, U](t: Generator[T], u: Generator[U]) = t flatMap (
    x => u map (y => (x, y)))
// expanded to
def pairs[T, U](t: Generator[T], u: Generator[U]) = t flatMap {
    x => new Generator[(T,U)] { def generate = (x, u.generate) }}
// expanded to
def pairs[T, U](t: Generator[T], u: Generator[U]) = new Generator[(T,U)] {
    def generate = (new Generator[(T,U)] {
        def generate = (t.generate, u.generate)
    }).generate }
// simplified to
def pairs[T, U](t: Generator[T], u: Generator[U]) = new Generator[(T,U)] {
    def generate = (t.generate, u.generate) }
*/

// generator examples
{
    trait Generator[+T] {
        // an alias for this
        self =>

        def generate: T

        def map[S](func: T => S): Generator[S] =
            new Generator[S] {
                def generate = func(self.generate)
            }

        def flatMap[S](func: T => Generator[S]): Generator[S] =
            new Generator[S] {
                def generate = func(self.generate).generate
            }
    }

    val integers = new Generator[Int] {
        val rand = new java.util.Random

        def generate = rand.nextInt()
    }

    val booleans = for (x <- integers) yield x > 0

    // only one
    def single[T](x: T): Generator[T] = new Generator[T] {
        def generate = x
    }

    // choose from interval
    def choose(lo: Int, hi: Int): Generator[Int] =
        for (x <- integers) yield lo + math.abs(x) % (hi - lo)

    // choose one of list
    def oneOf[T](xs: T*): Generator[T] =
        for (idx <- choose(0, xs.length)) yield xs(idx)

    // lists
    def lists: Generator[List[Int]] = for {
        isEmpty <- booleans
        list <- if (isEmpty) emptyLists else nonEmptyLists
    } yield list

    def emptyLists = single(Nil)
    def nonEmptyLists = for {
        head <- integers
        tail <- lists
    } yield head :: tail

    // tree generator?
    // leaf or an inner node
    trait Tree
    case class Inner(left: Tree, right: Tree) extends Tree
    case class Leaf(x: Int) extends Tree

    def leafs: Generator[Leaf] = for {
        x <- integers
    } yield Leaf(x)

    def inners: Generator[Inner] = for {
        l <- trees
        r <- trees
    } yield Inner(l, r)

    def trees: Generator[Tree] = for {
        isLeaf <- booleans
        tree <- if (isLeaf) leafs else inners
    } yield tree

    // we will need pairs generator below
    def pairs[T, U](t: Generator[T], u: Generator[U]) = for {
        x <- t
        y <- u
    } yield (x, y)

    // application: random testing – random test inputs

    def test[T](g: Generator[T], numTimes: Int = 100)
               (test: T => Boolean): Unit = {
        for (i <- 0 until numTimes) {
            val value = g.generate
            assert(test(value), "test failed for " + value)
        }
        println("passed " + numTimes + " tests")
    }
    // usage : generate pairs of lists, test func for true 100 times
    test(pairs(lists, lists)) {
        case (xs, ys) => (xs ++ ys).length > xs.length
    }
    // test should fail: list can be Nil
}

// ScalaCheck tool idea: write 'properties'
// or invariants
forAll {
    (l1: List[Int], l2: List[Int]) =>
        l1.size + l2.size == (l1 ++ l2).size }
// and tool do the heavylifting with random values and checks









