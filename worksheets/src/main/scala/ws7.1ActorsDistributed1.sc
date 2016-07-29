//Actors are Distributed (36:30)
//https://class.coursera.org/reactive-002/lecture/95
//TOC
//– Akka cluster
//– the impact of network communication: data sharing only by value; bandwidth; latency; failures; data corruption
//– serialization/deserialization, history of copy != history of origin => only immutable objects can be shared
//– comm.failures: message can be lost; only part of the message can arrive
//– one corrupted event per terabyte
//– distributed computing breaks assumptions made by the sync programming model
//– actors are distributed: comm is async, one-way, not guaranteed;
//actors are location transparent, hidden behind ActorRef
//– actors take the network model and brings it to local machine
//– actors are designed around distributed model of computations
//– how actors communicate ower network?
//– Address for every actor system, scheme and authority of a hierarchical URI // tree of actors – path
//– actor names form the URI's path elements: ref.path = 'akka://HelloW/user/greeter'
//val system = ActorSystem('HelloW')
//val ref = system.actorOf(Props[Greeter], 'greeter')
//– authority part: 'akka://HelloW' // address of the (local) actor system
//– path: 'user/greeter'
//– remote address example: 'akka.tcp://HelloW@host.net:6565/user/greeter'
//– ActorRef vs. ActorPath: path is a full name, whether the actor exists or not
//ref points to started actor; an incarnation
//– ActorPath can only optimistically send a message
//– ActorRef can be watched
//– ActorRef example: 'akka://HelloW/user/greeter#9385793' // #uid
//– actor names are unique within a parent, but can be reused
//– bottom line: file path vs. file handler
//– path2ref: Resolver: context.actorSelection(path) ! Identify((path, sender))
//– relative actor paths: like FS paths, even with wildcards
//– cluster: a set of nodes (actor systems) about which all members are in agreement
//nodes can collaborate on a common task
//– inductive: a single node can declare itself a cluster (join itself)
//– single node can join a cluster: request is sent to any member; info is spread using a gossip protocol;
//once all members know about the new node it's declared part of the cluster
//– no single leader in cluster // gossip protocol
//– example: start up a cluster // TCP port 2552
//config: '-Dakka.actor.provider=akka.cluster.ClusterActorRefProvider'
//app: val cluster = Cluster(context.system)
//cluster.subscribe(self, classOf[ClusterEvent.MemberUp])
//cluster.join(cluster.selfAddress) ; def receive = { …
//– class ClusterWorker extends Actor {
//val cluster = Cluster(context.system)
//cluster.subscribe(self, classOf[ClusterEvent.MemberRemoved])
//val main = cluster.selfAddress.copy(port = Some(2552))
//cluster.join(main) ; def receive = { …
//– utilizing cluster, example: class ClusterReceptionist extends Actor { // web crawler/link checker
//behaviours: awaitingMembers; active(addresses: Vector) // list of workers
//– receptionist active behaviour: case MemberUp … ; case MemberRemoved …
//case Get(url) => val client = sender; val address = pick(addresses);
//makeNewActor(new Customer(client, url, address)) …
//– class Customer( client, url, node: Address ) extends Actor { // deploy Controller actor on a given node
//implicit val s = context.parent // controller would think: a messages came from receptionist
//val props = Props[Controller].withDeploy(Deploy(scope = RemoteScope(node)))
//val controler = context.actorOf(props, 'controller')
//controller ! Check(url, 2)
//– cluster worker have a 'remote' actors root; 'deploy' send a message to that 'remote'
//remote creates 'akka.tcp:// … /user/app/ … ' thunk and under it – Controller actor
//  logically, controller.parent = customer
//– Java VM arguments for cluster nodes

import akka.actor.Actor.Receive
import akka.actor._
import akka.cluster.{Cluster, ClusterEvent}
import akka.cluster.ClusterEvent.{ClusterDomainEvent, MemberRemoved, MemberUp}
import akka.dispatch.sysmsg.Failed
import akka.remote.RemoteScope

import scala.concurrent.duration._

// distributed actors: group of people on different continents
// eventual consistency: that group of people agree on some state

// distributed: network communication impact
// share by value, bandwidth/latency, data corruption/delivery failure

// stateful object: behaviour depends on history
// object copied over network is not the same, its history lost
// only immutable object can be shared with sense

// you can rely on messages with confirmations, async.

// actor path, URI
{
  val ref: ActorRef = newActor()
  print(ref.path) // akka://HelloWorld/user/greeter
  print(ref) // akka://HelloWorld/user/greeter#43428347
}
// like filesystem tree
// authority: akka://HelloWorld
// path: user/greeter
// remote address example: akka.tcp://HelloW@host.net:6565/user/greeter
// ActorRef uid: #43428347

// Resolving an ActorPath: context.actorSelection(path) ! Identify((path, sender))
{
  // resolver custom messages
  case class Resolve(path: ActorPath)
  case class Resolved(path: ActorPath, ref: ActorRef)
  case class NotResolved(path: ActorPath)

  // utility actor
  class Resolver extends Actor {
    override def receive: Receive = {
      case Resolve(path) => context.actorSelection(path) ! Identify((path, sender))
      case ActorIdentity((path, client), Some(ref)) => client ! Resolved(path, ref)
      case ActorIdentity((path, client), None) => client ! NotResolved(path)
    }
  }
}

// like filesystem, actor path can be relative
{
  // grand-child
  context.actorSelection("child/grandchild")
  // sibling
  context.actorSelection("../sibling")
  // from local root
  context.actorSelection("/user/app")
  // broadcasting using wildcards
  context.actorSelection("/user/controllers/*")
}

// akka.cluster, decentralized

// set of actor systems (nodes) about which all members are in agreement
// about who is in cluster, and who is not

// start a cluster: a single node declared itself a cluster (join)
// a node can join a cluster, sending a request to any node
// once all current members know about the new node, it's declared part of the cluster
// gossip protocol (EventStream)

// example, startup a cluster

// configuration (application.conf) : akka.actor.provider = ClusterActorRefProvider
{
  // sbt dependencies "com.typesafe.akka" %% "akka-cluster" % "2.2.1"

  // application.conf
  akka { actor { provider = akka.cluster.ClusterActorRefProvider }}

  // command line
  -Dakka.actor.provider=akka.cluster.ClusterActorRefProvider
}

// main program: class ClusterMain extends Actor
// listen tcp port 2552 by default.
// will hold Receptionist actor locally (and Customer)
{
  class ClusterMain extends Actor { // main class for node
    val cluster = Cluster(context.system)
    cluster.subscribe(self, classOf[ClusterEvent.MemberUp])
    cluster.subscribe(self, classOf[ClusterEvent.MemberRemoved])

    cluster.join(cluster.selfAddress) // join to self, main cluster

    val receptionist = context.actorOf(Props[ClusterReceptionist], "receptionist")
    context.watch(receptionist) // sign death pact

    override def receive: Receive = {
      case ClusterEvent.MemberUp(member) =>
        // another node joined, worker maybe
        if(member.address != cluster.selfAddress) {
          getLater(1 seconds, "http://ya.ru")
          getLater(2 seconds, "http://ya.ru/0")
          getLater(2 seconds, "http://ya.ru/1")
          getLater(3 seconds, "http://ya.ru/2")
          getLater(4 seconds, "http://ya.ru/3")
          context.setReceiveTimeout(3 seconds)
      }
      case Result(url, set) => println(set.toVector.sorted.mkString(s"Results for '$url':\n, \n, \n"))
      case Failed(url, reason) => println(s"Failed to fetch '$url', $reason\n")
      case ReceiveTimeout =>
        // network problems? go down
        cluster.leave(cluster.selfAddress)
      case ClusterEvent.MemberRemoved(m, _) =>
        // worker down, stop the test program
        context.stop(self)
    }

    // start fetching
    def getLater(d: FiniteDuration, url: String) = {
      import context.dispatcher
      context.system.scheduler.scheduleOnce(d, receptionist, Get(url))
    }
    getLater(Duration.Zero, "http://ya.ru")
  }
}
// cluster main configuration:
/*
program arguments: linkchecker.ClusterMain
VM arguments:
  -Dakka.loglevel=INFO
  -Dakka.actor.provider=akka.cluster.ClusterActorRefProvider
  -Dakka.cluster.min-nr-of-members=2
 */

// another node: worker, class ClusterWorker extends Actor; main program, entry point
// set random tcp port: config: akka.remote.netty.tcp.port = 0.
// will hold Controller actor locally (and Getters)
{
  class ClusterWorker extends Actor { // main class for node
    val cluster = Cluster(context.system)
    cluster.subscribe(self, classOf[ClusterEvent.MemberRemoved])

    val main = cluster.selfAddress.copy(port = Some(2552))
    cluster.join(main)  // join to main cluster

    override def receive: Receive = {
      case ClusterEvent.MemberRemoved(member, _) =>
        if (member.address == main) context.stop(self)
    }

    override def postStop(): Unit = AsyncWebClient.shutdown()
  }
}
// cluster worker config:
/*
program arguments: linkchecker.ClusterWorker
VM arguments:
  -Dakka.loglevel=INFO
  -Dakka.actor.provider=akka.cluster.ClusterActorRefProvider
  -Dakka.remote.netty.tcp.port=0
  -Dakka.cluster.auto-down=on
 */


// add actor that do some stuff: ClusterReceptionist extends Actor, inside main node;
// work with Controllers on remote nodes (not spawn them locally).
{
  class ClusterReceptionist extends Actor {
    val cluster = Cluster(context.system)
    cluster.subscribe(self, classOf[MemberUp])
    cluster.subscribe(self, classOf[MemberRemoved])

    override def postStop(): Unit = { cluster.unsubscribe(self) }

    override def receive: Receive = awaitingMembers

    val awaitingMembers: Receive = {
      case state: ClusterEvent.CurrentClusterState => {
        // reply to subscribe, list of Address of nodes in cluster
        val addresses = state.members.toVector map (_.address)
        val nodes = addresses filter (_ != cluster.selfAddress) // not self
        // if cluster have workers
        if (nodes.nonEmpty) context.become(active(nodes))
      }
      case MemberUp(member) if member.address != cluster.selfAddress =>
        context.become(active(Vector(member.address))) // first worker is up
      // wait for worker nodes
      case Get(url) => sender ! Failed(url, "no nodes available")
    }

    def active(nodes: Vector[Address]): Receive = {
      case MemberUp(member) if member.address != cluster.selfAddress =>
        // add another worker to list
        context.become(active(nodes :+ member.address))
      case MemberRemoved(member, _) => {
        // remove worker from list
        val workers = nodes filter (_ != member.address)
        if (workers.isEmpty) context.become(awaitingMembers)
        else context.become(active(workers))
      }
      // children.size == num of concurrent requests
      case Get(url) if context.children.size < nodes.size => {
        // unemployed worker in cluster
        val client = sender
        val address = pick(nodes)
        // create a local actor, Controller facade
        context.actorOf(Props(new Customer(client, url, address)))
      }
      case Get(url) => sender ! Failed(url, "to many parallel queries")
    }
  }
}

// class Customer extends Actor, created by Receptionist, locally;
// Controller manipulator, abstracting all network particularities
{
  class Customer(client: ActorRef, url: String, node: Address) extends Actor {
    // set implicit 'sender' = parent, i.e. receptionist; all messages sent from Customer
    // will be seen as sent from receptionist.
    implicit val s = context.parent

    // deploy controller on remote node
    // with stopping supervisor strategy
    // then watch it.
    // set messages timeout = 5 seconds

    // deploy controller on remote node
    val props = Props[Controller].withDeploy(Deploy(scope = RemoteScope(node)))

    override val supervisorStrategy = SupervisorStrategy.stoppingStrategy
    val controller = context.actorOf(props, "controller")
    context.watch(controller)

    context.setReceiveTimeout(5 seconds)
    controller ! Check(url, 2)

    override def receive: Receive = ({
      case ReceiveTimeout =>
        context.unwatch(controller) // Terminated messages are unwelcome now
        client ! Failed(url, "controller timed out")
      case Terminated(_) => client ! Failed(url, "controller died")
      case Controller.Result(links) =>
        // work is done
        context.unwatch(controller)
        client ! Receptionist.Result(url, links)
      // and then stop Customer-Controller pipe
    }: Receive) andThen (_ => context.stop(self))
  }
}
