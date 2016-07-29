//Promises, promises
//https://class.coursera.org/reactive-002/lecture/133

//TOC
//Future, Promise
//– Promise – a way to create Future creating value outside
//– example: filter w/o async await
//def filter(pred: T=> Boolean): Future [T] = {
//    val p = Promise[T]()
//    this onComplete {
//        case Failure(e) => p.failure(e)
//        case Success(x) => if( !pred(x) ) p.failure( new NoSuch … )
//        else p.success(x) }
//    p.future }
//trait Promise[T] { def future: Future[T]; def tryComplete(result: Try[T]): Boolean
//– Promise is very imperative concept: like a mailbox with mutable variable: callback was executed
//– like a variable where you get the result via callback
//– example: racing
//    def race[T](left: Future[T], right: Future[T]): Future[T] = {
//        val p = Promise[T]()
//        left onComplete { p.tryComplete(_) }
//        right onComplete { p.tryComplete(_) }
//        p.future }
//– helpers
//    trait Promise { def success(val: T): Unit = this.complete(Success(val)); def failure …
//– example: zip using Promise
//        def zip[S, R](that: Future[S], f: (T, S) => R): Future[R] = {
//            val p = Promise[R]()
//            this onComplete {
//                case Failure(e) => p.failure(e); case Success(x) => that onComplete {
//                    case Failure(e) => p.failure(e); case Success(y) => p.success(f(x, y)) } }
//            p.future }
//– and with async await
//        def zip[S, R](that: Future[S], f: (T, S) => R): Future[R] = async {
//            f( await { this }, await { that } ) }
//– much better
//– recursive solution for Future list
//        def sequence[T](fts: List[Future[T]]): Future[List[T]] = {
//            fts match {
//                case Nil => Future(Nil)
//                case (ft::fts) => ft.flatMap( t => sequence(fts).flatMap(ts => Future(t::ts)) ) } }
//– async await solution
//        def sequence[T](fs: List[Future[T]]): Future[List[T]] = async {
//            var _fs = fs ;	val r = ListBuffer[T]()
//            while ( _fs != Nil ) { r += await { _fs.head }
//                _fs = _fs.tail }
//            r.toList }
//– and using Promise ???
//done with effect: async Future // latency + failure

//Promise – a way to create Future creating value outside
// like a mailbox: create a mailbox and wait until somehow letter appears in it
// like a mutable (once!) variable, very imperative concept
// single assignment variable

// example
def filterII[T](future: Future[T], p: T => Boolean): Future[T] = {
    val p = Promise[T]()

    future.onComplete { // callback, get the value, async
        case Success(s) => {
            if(p(s)) p.success(s) // complete future
            else p.failure(new NoSuchElementException("No such element"))
            // success, failure: helpers for 'complete'
        }
        case Failure(f) => p.failure(f)
    }

    p.future // get future from promise
    // after some time it will be ready
}

// when Promise is useful?

// situation: race, who came first?
// example, very nice
def race[T](left: Future[T], right: Future[T]): Future[T] = {
    val p = Promise[T]()

    left  onComplete { p.tryComplete(_) } // try complete future
    right onComplete { p.tryComplete(_) }

    p.future // first actor
}

// when not to use Promise

// example

// async-await
def zipI[T, S, R](future: Future[T], other: Future[S], combine: (T, S) => R): Future[R] =
    async {
        combine(await{ future }: T, await{ other }: S)
    }

// promise // fugly
def zipII[T, S, R](future: Future[T], other: Future[S], combine: (T, S) => R): Future[R] = {
    val p = Promise[R]()

    future onComplete {
        case Failure(f) => { p.failure(f) }
        case Success(t) => { other onComplete {
            case Failure(f) => { p.failure(f) }
            case Success(s) => p.success(combine(t,s))
        }}
    }

    p.future
}

// when to use clear Future and flatMap

// example
def sequenceI[T](fts: List[Future[T]]): Future[List[T]] = fts match {
    case Nil => Future(Nil)
    case h::t => h.flatMap(a => sequence(t))
        .flatMap(lst => Future(a::lst))
}

// and async-await // fugly
def sequenceII[T](fts: List[Future[T]]): Future[List[T]] = async {
    val r = ListBuffer[T]()

    var _fs = fts
    while(_fs != Nil) {
        r += await { _fs.head }
        _fs = _fs.tail
    }

    r.toList
}
