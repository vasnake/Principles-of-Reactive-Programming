//Actor Composition (20:14)
//https://class.coursera.org/reactive-002/lecture/101

//TOC
//– type of the actor defined by its accepted messages / actor interface
//– actor type is structural
//– this structure may change over time / behaviour defined by a protocol
//– it's possible to statically verify that sending message would be processed, with probability
//– Actor Systems are composed like human organizations
//– Actors are composed on a protocol level // split task on subtasks; compose results // e.g. webcrawler
//– an actor can: translate, forward, split, aggregate requests/replies
//– less type safety => more freedom
//– The Customer pattern = request-reply: most fundamental
//– allows dynamic composition of actor systems by including customer address in the request
//– another pattern: one-way proxy: Interceptors: 'target forward msg' // sender preserved
//– another pattern: exactly one reply, Result|Failure: Ask pattern (akka.pattern.ask): // adapter or facade
//example: '(userService ? FindByEmail(email))...' // ? => 'ask'
//creates pseudoactor with ActorRef linked to a Promise and takes an implicit timeout
//– Ask get handy if you need result aggregation from multiple actors
//example: 'class PostSummary … ' for-expression on futures from several asks: 'service ? operation'
//– risk delegation pattern: create subordinate to perform dangerous task
//  apply lifecycle monitoring ; report success/failure back ; shut down ephemeral actors
//  example: FileWriter => FileWorker(s)
//– composing actors: Facade: translation, validation, rate limitation, access control // or Decorator?
//  http://stackoverflow.com/questions/3489131/difference-between-the-facade-proxy-adapter-and-decorator-design-patterns

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, SupervisorStrategy, Terminated}
import org.scalatest.concurrent.PatienceConfiguration.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import akka.pattern.{ask, pipe}

// actor type is a protocol: set of messages

// it's possible, in theory, to check actor types statically, with some probability

// actors are composed on a protocol level
// translate, forward, split, aggregate messages/tasks/subtasks

// also, actors can manipulate with messages time properties: frequency, exact time, ...
// more freedom, less safety

// patterns

// customer pattern: request-reply,
// sender parameter allows dynamic composition of actors
// alice-bob-alice; a-b-c-a // forward to charlie

// interceptor pattern
// one-way proxy
{
  class AuditTrail(target: ActorRef) extends Actor with ActorLogging {
    override def receive: Receive = {
      case msg =>
        log.info("sent from {} to {} msg {}", sender, target, msg)
        target forward msg
    }
  }
}

// the Ask pattern: exactly one reply
// Ask operator creates a tiny actor linked to a Promise.
// To guarantee a result, ask op takes an implicit timeout.
// after 3 seconds future will be completed with timeout and tiny actor will be stopped.
// another point of failure: cast to UserInfo.
{
  case class Get(email: String)
  case class FindByEmail(email: String)

  trait Answer
  case class Result(posts: List[Post]) extends Answer
  case class Failure(err: Throwable) extends Answer

  case class Post(email: String)
  case class UserInfo(posts: List[Post])

  class PostsByEmail(service: ActorRef) extends Actor {
    implicit val timeout = Timeout(3 seconds)

    override def receive: Receive = {
      case Get(email) => {
        // ask return a Future; get info from it or recover with Failure; pipeTo client.
        // Future[Any] but we expect UserInfo.
        // quite common pattern, kinda Facade pattern for getting result from a service.
        // If logic inside becomes too complicated: create a custom actor.
        val fut = (service ? FindByEmail(email))
          .mapTo[UserInfo]
          .map(info => Result(info.posts.filter(_.email == email)))
          .recover { case ex => Failure(ex) }
        fut pipeTo sender
      }
    }
  }
}

// ask pattern is handy for results aggregating
// example: results aggregation
{
  class PostSummary extends Actor {
    implicit val timeout = Timeout(500 millis)

    override def receive: Receive = {
      case Get(postId, user, password) =>
        // flatMap, map over Futures, yield future, pipeTo sender.
        val resp = for {
          status <- (publisher ? GetStatus(postId)).mapTo[PostStatus]
          text <- (postStore ? Get(postId)).mapTo[Post]
          auth <- (authService ? Login(user, password)).mapTo[AuthStatus]
        } yield {
          if (auth.successful) Result(status, text)
          else Failure("not authorized")
        }
        resp pipeTo sender
    }
  }
}

// risk delegation pattern

// create subordinate worker to perform dangerous task
// apply lifecycle monitoring (watch-Terminate, supervisor)
// report results back to client
// shut down worker
{
  class FileWriter extends Actor {
    var clients = Map.empty[ActorRef, ActorRef]
    override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

    override def receive: Receive = {
      case Write(contents, file) =>
        val worker = context.actorOf(props(new FileWorker(contents, file, self)))
        context.watch(worker)
        clients += worker -> sender
      case Done => clients.get(sender).foreach(_ ! Done); clients -= sender
      case Terminated(worker) => clients.get(sender).foreach(_ ! Failed); clients -= sender
    }
    // timeout + ask?
    // unwatch; stop(worker)?
  }
}

// and lot of others: Facade
// translation
// validation
// rate limitaton
// access control
