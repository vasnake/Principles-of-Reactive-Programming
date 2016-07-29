// case classes, JSON example
// functions are objects
// subclassing functions
// partial matches
// partial functions

// case classes
// JSON in Scala

abstract class JSON

case class JSeq(elems: List[JSON]) extends JSON

case class JObj(bindings: Map[String, JSON]) extends JSON

case class JNum(num: Double) extends JSON

case class JStr(txt: String) extends JSON

case class JBool(b: Boolean) extends JSON

case object JNull extends JSON

// JSON data example

val data = JObj(Map(
    "firstName" -> JStr("John"),
    "lastName" -> JStr("Smith"),
    "address" -> JObj(Map(
        "streetAddress" -> JStr("21 2nd Street"),
        "state" -> JStr("NY")
    ))
))

// pattern matching application

def show(json: JSON): String = json match {
    case JSeq(elems) => "[" + (elems map show mkString ", ") + "]"
    case JObj(bind) => {
        val assocs = bind map {
            case (key, value) => "\"" + key + "\": " + show(value)
        }
        "{" + (assocs mkString ", ") + "}"
    }
    case JNum(num) => num.toString
    case JStr(str) => '\"' + str + '\"'
    case JBool(b) => b.toString
    case JNull => "null"
}

// functions are objects

// function in
/*
        val assocs = bind map {
            case (key, value) => "\"" + key + "\": " + show(value)
        }
*/
// what is the type of that function?
// {case (key, value) => key + ": " + value}
// taken by itself, the expression is not typable
// expected type is
// (String, Json) => String
type JBinding = (String, JSON)
// JBinding => String

// function type
type f1 = JBinding => String
// is just a shorthand for type
// scala.Function1[JBinding, String]
// trait with its type arguments

// so, pattern matching block
// {case (key, value) => key + ": " + value}
// expands to the Function1 instance
new Function1[JBinding, String] {
    def apply(x: JBinding) = x match {
        case (key, value) => key + ": " + value
    }
}

// functions = classes, so we can subclassing functions
// e.g. maps are functions
trait _Map[key, Value] extends (key => Value)

// or sequences are functions from Int to values
trait _Seq[Elem] extends (Int => Elem)

// so, seq(i) rewrites to 'apply'

// partial matches

// ok, PM block like
// case "ping" => "pong"
// can be given function type
// String => String
val f2: String => String = {
    case "ping" => "pong"
}
// but the function is not defined on all its domain!
// f2("pong") // MatchError
// is there a way to find out whether the function can be applied to a
// given argument beforehand?

// yes, there is: trait PartialFunction extends Function1
val f3: PartialFunction[String, String] = {
    case "ping" => "pong"
}
f3.isDefinedAt("ping") // true
f3.isDefinedAt("pong") // false

// if the expected type is a PartialFunction
// the Scala compiler will expand
// { case "ping" => "pong" }
// as follows
new PartialFunction[String, String] {
    def apply(x: String) = x match {
        case "ping" => "pong"
    }

    def isDefinedAt(x: String) = x match {
        case "ping" => true
        case _ => false
    }
}

// exercise

val f4: PartialFunction[List[Int], String] = {
    case Nil => "one"
    case x :: xs => xs match {
        case Nil => "two"
    }
}
f4.isDefinedAt(List(1, 2, 3)) // true
f4(List(1, 2, 3)) // MatchError
// PF can't control nested functions
