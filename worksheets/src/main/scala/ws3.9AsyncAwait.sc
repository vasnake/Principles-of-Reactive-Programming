//Async Await
//    https://class.coursera.org/reactive-002/lecture/131

//TOC
//Future; get rid of it
//– making effects implicit , in a limited scope, internally
//– say: T => Future[S]
//– mean: T => Try[S] or even T => S
//– Async await magic
//def async[T](body: =>T) (implicit context: ExecutionContext): Future[T]
//def await[T](fut: Future[T]): T
//async{ … await{ … } … }
//– await allows to forget about Future and use type T
//– illegal uses of await: under a try/catch; in expression passed as an argument to a by-name param, …
//– lets taste it, example
//def retry(nTimes: Int)(block: =>Future[T]): Future[T] = async {
//    var i = 0; var result: Try[T] = Failure(new Exception))
//    while (result.isFailure && i < nTimes) {
//        result = await { Try(block); i += 1 }
//        result.get }
//– example: Future filter
//    def filter(p: T => Boolean): Future[T] = async {
//        val x = await{ this }
//        if( !p(x) ) { throw new NoSuchElementException }
//        else { x } }
//– example: flatMap
//    def flatMap[S](f: T=>Future[S]): Future[S] = async {
//        val x: T = await { this }
//        await { f(x) } }
//– what about filter w/o await? Enters Promise // async await more preferrable
//    def filter(pred: T=> Boolean): Future [T] = {
//        val p = Promise[T]()
//        this onComplete {
//            case Failure(e) => p.failure(e)
//            case Success(x) => if( !pred(x) ) p.failure( new NoSuch … )
//            else p.success(x) }
//        p.future }

// Future, get rid of it
// can we write things w/o boilerplate?

// we want T => S, not T => Future[S]
// make effect (latency) implicit
{
    def async[T](body: => T): Future[T] = ???
    def await[T](future: Future[T]): T = ???

    val smth = async { ??? await { ??? } ... }
}
// a lots of limitations, but it will pay off

// example
def retry[T](nTimes: Int)(block: => Future[T]): Future[T] =
    async { // block return Future
        var i: Int = 0
        var result: Try[T] = Failure(new Exception("Oops"))

        while (i < nTimes && result.isFailure) {
            result = await { Try(block) } // Future[Try[T]], result is Try[T]
            i += 1
        }

        result.get
    }
// call retry, get future and use it.

// example
def filterI[T](future: Future[T], p: T => Boolean): Future[T] =
    async{
        val x: T = await{ future }
        if(!p(x)) { // predicate
            throw new NoSuchElementException("No such element")
        } else {
            x
        }
    }

// example
def flatMap[T,S](future: Future[T], op: T => Future[S]): Future[S] =
    async{
        val x: T = await{ future }
        await{ op(x) }: S
    }

// next: Promise
// example
def filterII[T](future: Future[T], p: T => Boolean): Future[T] = {
    val p = Promise[T]()

    future.onComplete { // callback
        case Success(s) => {
            if(p(s)) p.success(s) // set promise
            else p.failure(new NoSuchElementException("No such element"))
        }
        case Failure(f) => p.failure(f)
    }
    p.future // get future from promise
}
// you see: async ... await much better
