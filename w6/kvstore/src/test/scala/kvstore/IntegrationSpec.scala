/**
 * Copyright (C) 2013-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package kvstore

import akka.actor.{ Actor, Props, ActorRef, ActorSystem }
import akka.testkit.{ TestProbe, ImplicitSender, TestKit }
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }
import scala.concurrent.duration._
import org.scalatest.FunSuiteLike
import org.scalactic.ConversionCheckedTripleEquals

class IntegrationSpec(_system: ActorSystem) extends TestKit(_system)
    with FunSuiteLike
        with Matchers
    with BeforeAndAfterAll
    with ConversionCheckedTripleEquals
    with ImplicitSender
    with Tools {

  import Replica._
  import Replicator._
  import Arbiter._

  def this() = this(ActorSystem("ReplicatorSpec"))

  override def afterAll: Unit = system.shutdown()

  /*
   * Recommendation: write a test case that verifies proper function of the whole system,
   * then run that with flaky Persistence and/or unreliable communication (injected by
   * using an Arbiter variant that introduces randomly message-dropping forwarder Actors).
   */
  }
