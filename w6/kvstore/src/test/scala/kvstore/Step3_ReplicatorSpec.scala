package kvstore

import akka.testkit.{ TestProbe, TestKit, ImplicitSender }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.FunSuiteLike
import akka.actor.ActorSystem
import scala.concurrent.duration._
import kvstore.Arbiter.{ JoinedSecondary, Join }
import kvstore.Persistence.{ Persisted, Persist }
import kvstore.Replicator.{ SnapshotAck, Snapshot, Replicate }
import org.scalactic.ConversionCheckedTripleEquals

class Step3_ReplicatorSpec extends TestKit(ActorSystem("Step3ReplicatorSpec"))
    with FunSuiteLike
        with BeforeAndAfterAll
    with Matchers
    with ConversionCheckedTripleEquals
    with ImplicitSender
    with Tools {

  override def afterAll(): Unit = {
    system.shutdown()
  }

  test("case1: Replicator should send snapshots when asked to replicate") {
    val secondary = TestProbe()
    val replicator = system.actorOf(Replicator.props(secondary.ref), "case1-replicator")

    replicator ! Replicate("k1", Some("v1"), 0L)
    secondary.expectMsg(Snapshot("k1", Some("v1"), 0L))
    secondary.ignoreMsg({ case Snapshot(_, _, 0L) => true })
    secondary.reply(SnapshotAck("k1", 0L))

    replicator ! Replicate("k1", Some("v2"), 1L)
    secondary.expectMsg(Snapshot("k1", Some("v2"), 1L))
    secondary.ignoreMsg({ case Snapshot(_, _, 1L) => true })
    secondary.reply(SnapshotAck("k1", 1L))

    replicator ! Replicate("k2", Some("v1"), 2L)
    secondary.expectMsg(Snapshot("k2", Some("v1"), 2L))
    secondary.ignoreMsg({ case Snapshot(_, _, 2L) => true })
    secondary.reply(SnapshotAck("k2", 2L))

    replicator ! Replicate("k1", None, 3L)
    secondary.expectMsg(Snapshot("k1", None, 3L))
    secondary.reply(SnapshotAck("k1", 3L))
  }

  test("case2: Replicator should retry until acknowledged by secondary") {
    val secondary = TestProbe()
    val replicator = system.actorOf(Replicator.props(secondary.ref), "case2-replicator")

    replicator ! Replicate("k1", Some("v1"), 0L)
    secondary.expectMsg(Snapshot("k1", Some("v1"), 0L))
    secondary.expectMsg(300.milliseconds, Snapshot("k1", Some("v1"), 0L))
    secondary.expectMsg(300.milliseconds, Snapshot("k1", Some("v1"), 0L))

    secondary.reply(SnapshotAck("k1", 0L))
  }

}
