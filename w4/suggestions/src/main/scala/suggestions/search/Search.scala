package suggestions
package search

import org.json4s._
import scala.concurrent.{ ExecutionContext, Future, Promise }
import ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.collection._
import scala.collection.JavaConverters._
import scala.util.Try
import scala.async.Async._

import rx.lang.scala.Observable
import observablex.{SchedulerEx, ObservableEx}
import ObservableEx._

import dispatch._
import org.json4s.native._
import retrofit.http.{GET, Query}
import retrofit.Callback
import retrofit.client.Response
import retrofit.{RetrofitError, Callback, RestAdapter}
import com.google.gson.annotations.SerializedName

object Search {

  /* implementation using Json */

  implicit val formats = org.json4s.DefaultFormats

  def wikipediaSuggestionJson(term: String): Future[List[String]] = {
    async {
      log("querying: " + term)
      val search = "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&limit=15&search="
      val response = await { Http(url(search + term).OK(as.String)) }
      val json = JsonParser.parse(response)
      val words = json(1)
      words.extract[List[String]]
    }
  }

  def wikipediaPageJson(term: String): Future[String] = {
    async {
      val search = "http://en.wikipedia.org/w/api.php?action=parse&format=json&prop=text&section=0&page="
      val response = await { Http(url(search + term).OK(as.String)) }
      val json = JsonParser.parse(response)
      val text = for {
        JObject(child) <- json
        JField("parse", JObject(fields)) <- child
        JField("text", JObject(tfields)) <- fields
        JField("*", JString(text)) <- tfields
      } yield text
      text.head
    }
  }

  /* alternative implementation using Retrofit */

  class Page {
    var parse: Content = _
  }

  class Content {
    var title: String = _
    var text: Text = _
  }

  class Text {
    @SerializedName("*")
    var all: String = _
  }

  trait WikipediaService {
    @GET("/w/api.php??action=opensearch&format=json&limit=15")
    def suggestions(@Query("search") term: String, callback: Callback[Array[AnyRef]]): Unit

    @GET("/w/api.php??action=parse&format=json&prop=text&section=0")
    def page(@Query("page") term: String, callback: Callback[Page]): Unit
  }

  val restAdapter = new RestAdapter.Builder().setServer("http://en.wikipedia.org").build()

  val service = restAdapter.create(classOf[WikipediaService])

  def callbackFuture[T]: (Callback[T], Future[T]) = {
    val p = Promise[T]()
    val cb = new Callback[T] {
      def success(t: T, response: Response) = {
        p success t
      }
      def failure(error: RetrofitError) = {
        p failure error
      }
    }

    (cb, p.future)
  }

  def wikipediaSuggestionRetrofit(term: String): Future[List[String]] = {
    async {
      val (cb, f) = callbackFuture[Array[AnyRef]]
      service.suggestions(term, cb)
      val result = await { f }
      val arraylist = result(1).asInstanceOf[java.util.List[String]]
      
      arraylist.asScala.toList
    }
  }

  def wikipediaPageRetrofit(term: String): Future[String] = {
    async {
      val (cb, f) = callbackFuture[Page]
      service.page(term, cb)
      val result = await { f }
      result.parse.text.all
    }
  }

  def wikipediaSuggestion(term: String): Future[List[String]] = wikipediaSuggestionRetrofit(term)

  def wikipediaPage(term: String): Future[String] = wikipediaPageRetrofit(term)

}

