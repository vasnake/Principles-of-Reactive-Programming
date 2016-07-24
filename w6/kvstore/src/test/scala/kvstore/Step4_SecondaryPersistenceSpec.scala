package kvstore

import akka.testkit.TestKit
import akka.testkit.ImplicitSender
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.FunSuiteLike
import akka.actor.ActorSystem
import akka.testkit.TestProbe
import scala.concurrent.duration._
import Arbiter._
import Persistence._
import org.scalactic.ConversionCheckedTripleEquals

class Step4_SecondaryPersistenceSpec extends TestKit(ActorSystem("Step4SecondaryPersistenceSpec"))
    with FunSuiteLike
        with BeforeAndAfterAll
    with Matchers
    with ConversionCheckedTripleEquals
    with ImplicitSender
    with Tools {

  override def afterAll(): Unit = {
    system.shutdown()
  }

  test("case1: Secondary should not acknowledge snapshots until persisted") {
    import Replicator._

    val arbiter = TestProbe()
    val persistence = TestProbe()
    val replicator = TestProbe()
    val secondary = system.actorOf(Replica.props(arbiter.ref, probeProps(persistence)), "case1-secondary")
    val client = session(secondary)

    arbiter.expectMsg(Join)
    arbiter.send(secondary, JoinedSecondary)

    client.get("k1") should ===(None)

    replicator.send(secondary, Snapshot("k1", Some("v1"), 0L))
    val persistId = persistence.expectMsgPF() {
      case Persist("k1", Some("v1"), id) => id
    }

    withClue("secondary replica should already serve the received update while waiting for persistence: ") {
      client.get("k1") should ===(Some("v1"))
    }

    replicator.expectNoMsg(500.milliseconds)

    persistence.reply(Persisted("k1", persistId))
    replicator.expectMsg(SnapshotAck("k1", 0L))
    client.get("k1") should ===(Some("v1"))
  }

  test("case2: Secondary should retry persistence in every 100 milliseconds") {
    import Replicator._

    val arbiter = TestProbe()
    val persistence = TestProbe()
    val replicator = TestProbe()
    val secondary = system.actorOf(Replica.props(arbiter.ref, probeProps(persistence)), "case2-secondary")
    val client = session(secondary)

    arbiter.expectMsg(Join)
    arbiter.send(secondary, JoinedSecondary)

    client.get("k1") should ===(None)

    replicator.send(secondary, Snapshot("k1", Some("v1"), 0L))
    val persistId = persistence.expectMsgPF() {
      case Persist("k1", Some("v1"), id) => id
    }

    withClue("secondary replica should already serve the received update while waiting for persistence: ") {
      client.get("k1") should ===(Some("v1"))
    }

    // Persistence should be retried
    persistence.expectMsg(200.milliseconds, Persist("k1", Some("v1"), persistId))
    persistence.expectMsg(200.milliseconds, Persist("k1", Some("v1"), persistId))

    replicator.expectNoMsg(500.milliseconds)

    persistence.reply(Persisted("k1", persistId))
    replicator.expectMsg(SnapshotAck("k1", 0L))
    client.get("k1") should ===(Some("v1"))
  }

}
