package kvstore

import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.FunSuiteLike
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import akka.testkit.ImplicitSender
import akka.testkit.TestProbe
import scala.concurrent.duration._
import kvstore.Persistence.{ Persisted, Persist }
import kvstore.Replica.OperationFailed
import kvstore.Replicator.{ Snapshot }
import scala.util.Random
import scala.util.control.NonFatal
import org.scalactic.ConversionCheckedTripleEquals

class Step1_PrimarySpec extends TestKit(ActorSystem("Step1PrimarySpec"))
    with FunSuiteLike
        with BeforeAndAfterAll
    with Matchers
    with ConversionCheckedTripleEquals
    with ImplicitSender
    with Tools {

  override def afterAll(): Unit = {
    system.shutdown()
  }

  import Arbiter._

  test("case1: Primary (in isolation) should properly register itself to the provided Arbiter") {
    val arbiter = TestProbe()
        system.actorOf(Replica.props(arbiter.ref, Persistence.props(flaky = false)), "case1-primary")
    
    arbiter.expectMsg(Join)
  }

  test("case2: Primary (in isolation) should react properly to Insert, Remove, Get") {
    val arbiter = TestProbe()
        val primary = system.actorOf(Replica.props(arbiter.ref, Persistence.props(flaky = false)), "case2-primary")
        val client = session(primary)

    arbiter.expectMsg(Join)
    arbiter.send(primary, JoinedPrimary)

    client.getAndVerify("k1")
    client.setAcked("k1", "v1")
    client.getAndVerify("k1")
    client.getAndVerify("k2")
    client.setAcked("k2", "v2")
    client.getAndVerify("k2")
    client.removeAcked("k1")
    client.getAndVerify("k1")
  }

  
}
