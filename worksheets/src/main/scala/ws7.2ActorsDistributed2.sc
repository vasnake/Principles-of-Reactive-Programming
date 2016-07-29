//Actors are Distributed Part II (18:17 — optional)
//https://class.coursera.org/reactive-002/lecture/97

//TOC
//– main node states: joining, up, leaving, exiting, removed, // down
//– cluster needs failure detection
//– every node is monitored (heartbeats) from several others // see: cluster membership state
//– a node unreachable from one other is considered unreachable for all // inconsistent cluster
//– to restore the cluster consensus, node can be removed
//– if we have a lot of nodes, number of communications can be quadratic (1/2N^2), not good …
//– enters 'neighbors' comm. model : ring, 2 or three next nodes clockwise
//– if node become unreachable, it enters to pseudostate 'down'
//– moving node to that state is a policy decision
//– transitions between that states seen as events/messages: MemberUp, MemberRemoved, …
//– DeathWatch: actors on nodes which are removed must be dead // no node, no actors
//– Terminated message: delivery is guaranteed, actor cannot come back, clean-up of remote-deployed childs
//– removed node needs to be completely restarted
//– Terminated message guaranteed, how come? It's a special message, it can be synthesised
//– example: class ClusterWorker extends Actor … { … context.watch(main_receptionist_ref)
//– using cluster is not complicated: subscribe to ClusterEvent.MemberUp, …
//– use context.watch to get Terminated message; stop actor when it's time

import akka.actor.Actor.Receive
import akka.actor._
import akka.cluster.{Cluster, ClusterEvent}

// failure detection, monitoring, watch-Terminate

// main node states
// joining -> up -> leaving -> exiting -> removed
// up -- MemberUp; removed -- MemberRemoved.

// worker node have another state (not really a state, a flag more likely): unreachable;
// node can become unreachable at any state.
// After node marked as unreachable, it moved to state 'down', by some policy.
// After reaching consistency, cluster set down node to removed state
// unreachable -> down -> removed

// node that unreachable must be detected (heartbeat) and removed from cluster

// neighbors monitoring: array of addressed sorted and considered as circle;
// node 1 monitors 2,3; 2 monitors 3,4; ...

// if one node detect unreachable neighbour, that information gossiping to
// all other nodes.
// unreachable node will be removed from cluster; until that there is no cluster,
// it's inconsistent

// example:
/*
main stopped; worker detect main as unreachable;
autodown conf. makes unreachable node go down;
worker become leader; leader move main node to removed;
that cause shutdown the program.
 */

// Cluster and DeathWatch

// actors on removed nodes must be dead (killed properly) to achieve cluster consistency.
// or, on live nodes, children actors of dead parent must be stopped.
// It's possible to handle that requirement, using Terminated message (watch).
// delivery of Terminated is guaranteed (it can be synthesized).

// terminated is the last message from actor.
// That means, node can't come back. It must be restarted.

// apply that to ClusterWorker
// find receptionist ActorRef, watch it, stop(self) if receptionist Terminated
{
  class ClusterWorker extends Actor with ActorLogging {
    val cluster = Cluster(context.system)
    cluster.subscribe(self, classOf[ClusterEvent.MemberUp])
    // get main node address
    val main = cluster.selfAddress.copy(port = Some(2552))
    // join to main cluster
    cluster.join(main)

    override def receive: Receive = {
      case ClusterEvent.MemberUp(member) =>
        if (member.address == main)
          context.actorSelection(RootActorPath(main) / "user" / "app" / "receptionist") ! Identify("42")
      case ActorIdentity("42", None) => context.stop(self)
      case ActorIdentity("42", Some(ref)) =>
        log.info("receptionist is at {}", ref)
        context.watch(ref)
      case Terminated(_) => context.stop(self)
    }

    override def postStop(): Unit = AsyncWebClient.shutdown()
  }
}
