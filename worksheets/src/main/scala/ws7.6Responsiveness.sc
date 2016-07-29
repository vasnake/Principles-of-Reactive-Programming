//Responsiveness (11:41)
//https://class.coursera.org/reactive-002/lecture/163

//TOC
//– actors = distribution
//– event-driven, responsive, scalable, resilient = reactive
//– the goal of resilience is to be available
//– responsiveness : ability to respond to input in time // if not = not available
//– responsiveness implies resilience to overload scenarios
//– responsiveness in cases: normal, fail, overload
//– exploit parallelism: class PostSummary changed from sequential to parallel
//– next step: responsiveness of each component – reduce latency there
//– avoid dependency of processing cost on load: O(N) your best frend, O(N^2) your enemy
//– add parallelism elastically (resizing routers)
//– on overload: requests will pile up, processing gets backlogged, clients timeout
//– CircuitBreaker pattern: retries n times and then don't ask service again until resetTimeout
//– Bulkheading pattern: separate computing parts from client parts // segregate the resources
//  not only actor isolation: exec.mechanisms needs to be isolated
//Props[MyActor].withDispatcher('compute-jobs')
//– akka.actor.default-dispatcher : fork-join-executor // configuration file
//– failures vs. responsiveness : detecting failure takes time, timeout
//– immediate fail-over requires the hot-swap backup to be ready available
//– instant fail-over is possible in active-active configurations // send request to all nodes
//– summary:
//  message-driven systems can be scaled horizontally and vertically
//  responsiveness demands resilience and elasticity
//decoupling components ; location transparency ;
//– message-driven is the basis for reactive systems; responsiveness ties the whole story together

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.CircuitBreaker
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import scala.util.Failure

// responsiveness : ability to respond to input in time
// if not = system not available, not resilient (to overload)
// responsiveness implies resilience to overload scenarios
// the goal of resilience: be available

// exploit parallelizm

// example: PostSummary with latency (queries performed sequentially)
// latency = s + t + a
{
  class PostSummary extends Actor {
    implicit val timeout = Timeout(500 millis)

    override def receive: Receive = {
      case Get(postId, user, password) =>
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

// parallel version of PostSummary, latency = max(s,t,a)
{
  class PostSummary extends Actor {
    implicit val timeout = Timeout(500 millis)

    override def receive: Receive = {
      case Get(postId, user, password) =>
        val status = (publisher ? GetStatus(postId)).mapTo[PostStatus]
        val text = (postStore ? Get(postId)).mapTo[Post]
        val auth = (authService ? Login(user, password)).mapTo[AuthStatus]
        val resp = for (s <- status; t <- text; a <- auth) yield {
          if (a.successful) Result(s, t) else Failure("not authorized")
        }
        resp pipeTo sender
    }
  }
}

// optimize every component, reduce latency
// algorithms & data structures: linear or logarithmic complexity on number of requests

// add parallelizm elastically (resizing routers, adding nodes)

// avoid backlogging: Circuit Breaker pattern
// call n times then wait resetTimeout (let system breathe)
{
  class Retriever(service: ActorRef) extends Actor {
    implicit val timeout = Timeout(2 seconds)
    val cb = CircuitBreaker(context.system.scheduler,
      maxFailures = 3, callTimeout = 1 second, resetTimeout = 30 seconds)

    override def receive: Receive = {
      case Get(user) =>
        val res = cb.withCircuitBreaker(service ? user).mapTo[String]
        // yada yada
    }
  }
}

// Bulkheading:
// separate execution context: client interface should be
// isolated from computing context/nodes/dispatchers.
// segregate resources
val compProps = Props[MyActor].withDispatcher("compute-jobs")
/*
dispatcher config, application.conf

akka.actor.default-dispatcher {
  executor = "fork-join-executor"
  fork-join-executor {
    parallelizm-min = 8
    parallelizm-max = 64
    parallelizm-factor = 3.0
  }
}
compute-jobs.fork-join-executor {
    parallelizm-min = 4
    parallelizm-max = 4
}
 */

// failures:
// detecting failures takes time; await for timeout.
// if you can't afford to wait:
// active-active configuration makes possible instant fail-over
/*
send request to 3 nodes, wait for 2 matching replies.
if one node don't respond -- recover it
 */

// summary
// message-driven leads to decoupling, enabling resilience;
// and enables elasticity (using location transparency);
// responsiveness require resilience, elasticity;
// altogether: reactive
