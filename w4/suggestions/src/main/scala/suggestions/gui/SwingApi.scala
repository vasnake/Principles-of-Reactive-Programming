package suggestions
package gui

import scala.language.reflectiveCalls
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{ Try, Success, Failure }
import scala.swing.Reactions.Reaction
import scala.swing.event.Event
import rx.lang.scala.Observable

/** Basic facilities for dealing with Swing-like components.
*
* Instead of committing to a particular widget implementation
* functionality has been factored out here to deal only with
* abstract types like `ValueChanged` or `TextField`.
* Extractors for abstract events like `ValueChanged` have also
* been factored out into corresponding abstract `val`s.
*/
trait SwingApi {

  type ValueChanged <: Event

  val ValueChanged: {
    def unapply(x: Event): Option[TextField]
  }

  type ButtonClicked <: Event

  val ButtonClicked: {
    def unapply(x: Event): Option[Button]
  }

  type TextField <: {
    def text: String
    def subscribe(r: Reaction): Unit
    def unsubscribe(r: Reaction): Unit
  }

  type Button <: {
    def subscribe(r: Reaction): Unit
    def unsubscribe(r: Reaction): Unit
  }

  implicit class TextFieldOps(field: TextField) {

    /** Returns a stream of text field values entered in the given text field.
      *
      * @param field the text field
      * @return an observable with a stream of text field updates
      */
    def textValues: Observable[String] = ???

  }

  implicit class ButtonOps(button: Button) {

    /** Returns a stream of button clicks.
     *
     * @param field the button
     * @return an observable with a stream of buttons that have been clicked
     */
    def clicks: Observable[Button] = ???
  }

}
