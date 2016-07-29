import rx.lang.scala.Observable
import scala.language.postfixOps
import scala.concurrent.duration._

//RX Operators (11:39)
//https://class.coursera.org/reactive-002/lecture/143

//TOC
//Reactive Extensions
//– monad Observable ops: filter, take, …
//– map marble diagram
//– flatMap marble diagram : map(f).merge
//– Iterable flatMap vs Observable flatMap: sync vs. async ; values pushing into obs.flatMap
//– different implementations for flatMap, output order can be mixed/interleaved
//– example: nested streams, flatten
//val xs: Observable.from(List(3, 2, 1))
//val yss: xs.map(x => Observable.interval(x seconds).map(_ => x).take(2))
//val zs: yss.flatten
//– merge/flatten: 2 input streams, 1 output stream : nondeterministic merge
//– concat: add to tail (wait for tail)
//– perfect example: earthquakes: is async and don't support backpressure
//– showcase for 'concat' usability
//class EarthQuake {
//    def magnitude: Double
//    def location: GeoCoord }
//object Magnitude extends Enumeration {
//    def apply(magn: Double): Magnitude = {...}
//    type Magnitude = Value
//    val Micro, Minor, Light, Moderate, ... }
//val quakes: Observable = usgs()
//val major = quakes.map(q=>(q.Location, Magnitude(q.Magnitude))).
//    filter({ case (loc, mag) => mag >= Major})
//major.subscribe({ case (loc, mag) => { println($'Magnitude $mag quake at $loc') }})
//– add another service: geocode
//def geoCode(c: GeoCoord): Future[Country] = ...
//val withCountry = usgs().map(q => { val country = geoCode(q.Location)
//    Observable.from(country.map(c => (q,c)))})
//val m = withCountry.flatten()
//val c = withCountry.concat()
//– future & observable together // obs.from make observable from feature
//– in this case, flatten change earthquakes order
//– we have to use concat for saving earthquakes order
//– groupBy operator: lets group earthquakes by country
//val merged: withCountry.merge()
//val byCountry = merged.groupBy({ case (quake, country) => country})

//http://reactivex.io/documentation/operators/merge.html

//Reactive Extensions
// operators

// Observable: collections monad, higher-order functions: map, flatMap, ...

// flatMap

// flatMap over async streams (Observable) is nondeterministic
// collection order can't be predicted
def flatMapImpl[R](f: T => Observable[R]): Observable[R] = { map(f).merge }
// every time value arrives, f is called; derived async stream will be produced;
// then merged into output stream

val ints = Observable.interval(3 second)
val outints = ints.flatMap(n => Observable.interval(n second))
// output can be mixed out-of-order
// async two times

// flatten

// flattening nested streams
// how nested streams are merged: async, timeline define everything
// nondeterministic!

val xs: Observable[Int] = Observable(3 to 1 by -1)
val yss: Observable[Observable[Int]] =
    xs.map(x => Observable.interval(x seconds).map(_ => x).take(2))
// yss: 3 collections, each emit number x (1 or 2 or 3) every x seconds, only 2 times

// output order defined by time intervals, not the input ordr
val zs: Observable[Int] = yss.flatten
// 1,2,1,3,2,3

// concat

// wait and then aggregate nested streams, by input order
// has to buffer internally: not a stream really, you don't want to use it
val zsI = yss.concat
// 3,3,2,2,1,1

// practical example
// Earthquakes: async, don't support backpressure (they go out anyhow)

def quakes(): Observable[Feature] = Usgs()

def major(): Observable[(Point, Magnitude)] =
    quakes() // map to tuple (location, magnitude)
        .map(quake => (quake.geometry, Magnitude(quake.properties.magnitude)))
        // select only major events
        .filter { case (location, magnitude) => magnitude >= Major }
// start processing
major.subscribe( {
        case (loc, mag) => { println(s"Magnitude: $mag, guake at: $loc") } })

// map coordinates to location names, using remote service, async
// Observable and Future

val withCountry: Observable[Observable[(EarthQuake, Country)]] =
    usgs().map(quake => {
        val country: Future[Country] = reverseGeocode(quake.Location)
        Observable.from(country.map(country => (quake, country)))
    })
// quake's order will be broken, mixed with geocoder service latency
val merged: Observable[(EarthQuake, Country)] = withCountry.flatten()
// records in order but, we need buffer and time
val concated: Observable[(EarthQuake, Country)] = withCountry.concat()

// groupBy operator

// group quakes by country
val byCountry: Observable[(Country, Observable[(EarthQuake, Country)])] =
    merged.groupBy({ case (quake, country) => country })
