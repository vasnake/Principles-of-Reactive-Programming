//Persistent Actor State (40:05)
//https://class.coursera.org/reactive-002/lecture/161

//TOC
//– we need to not lose actors state
//– shall not lose (important) state due to (system) failure
//– persistent state, recover state at (re)start (log + storage + snapshots)
//– save, load
//– persistent state: updates or append-only
//– updates vs. changes/append
//updates: recovery in constant time; data volume depends on number of records only, not their change rate
//changes/appends: saved history (audit); retroactive error correction; logs analyses benefits for business;
//less IO; changes are immutable => can freely be replicated
//– combination: actor write changes/events to log; log-reader restore actor state and write it to DB;
//actor restore state from DB (after restart) in constant time
//– immutable snapshots can be used to bound recovery time / upper bound
//collection of states + log of events after snapshot
//– in Akka: 'persist' method: persist(MyEvent(...)) { event => … // ok, confirmation from journal arrived … }
//– actor send a message to journal (another actor) for 'persist' event; journal save it …
//after, journal will reply 'persisted' and actor can do 'post-commit' ops
//– blogpost example: messages, events, state with method updated that map events to state
//class UserProcessor extends PersistentActor
//– applying after persisting leaves actor in stale state: // messages serialization
//  after 'persist' was called, actor won't process any other inputs until journal reply.
//– we can use 'persistAsync' method if we don't care about continuation
//– only one source of truth: persistent storage with saved states: knowledge about processed messages/blogposts
//– at-least-once delivery design: retrying until successful reply from recipient
//  sender keep state: waiting for confirmation-for-id
//– example: class UserProcessor(publisher: ActorPath) extends PersistentActor with AtLeastOnceDelivery { … }
//case NewPost(text, id) => persist(PostCreated(text)) { e => deliver(publisher, PublishPost(text, _)) }
//case PostPublished(id) => confirmDelivery(id)
//– ActorPath: full path to the actor, works even after system cold start (unlike ActorRef)
//– deliver: send blogpost to some publisher, at-least-once
//– exactly-once delivery design: usually it's what we want
//  at-least-once: responsibility on the sender
//  exactly-once: responsibility on the recipient – keep state: id-was-confirmed
//– example: class Publisher extends PersistentActor { … } // in fact: at-most-once
//deduplication (UserProcessor used at-least-once) saving published posts count as expectedId
//– we see that what we really want is confirmation of effects (website modifications), not confirmation of messages
//  we want atomicity for website update + message delivery
//– when to perform External Effects? choice needs to be made based on the business model
//  performing the effect and persisting that cannot be atomic // not in messaging system
//– perform EE before persisting: at-least-once semantics
//– perform EE after persisting: at-most-once semantics
//– if processing is idempotent than using at-least-once semantics achieves effectively exactly-once processing
//– summary: actors persist data/state/updates; events/snapshots can be replicated; recovery replays logged events.

import akka.actor.ActorPath
import akka.persistence.{AtLeastOnceDelivery, PersistentActor}

// actors must persist state as needed;
// must recover state at (re)start

// 2 ways: in-place updates: mutable stored state (e.g. in DB);
// append-only log, changes/events/state diff (Event Sourcing?)

// in-place updates:
// recovery in constant time
// data volume depends on number of records, not change rate

// persist changes:
// replay, audit, restore of history
// retroactive recovery from errors
// buisness value from log (user activity analytics)
// optimized IO, but lots of space needed
// changes are immutable, easily replicated/shared

// combined approach: some actor save changes to log;
// writer actor read the log and write snapshot to DB (periodically);
// on (re)start, actor recover from DB in constant time

// snapshots are immutable: easy sharing, replicate.
// n.b. we talking about actors state, not business logic state.

// ok, lets see how actor save changes: 'persist'

// persist(MyEvent ...
{
  persist(MyEvent(data)) { event =>
    // event was saved, persisted
    doSomethingWith(event)
  }
}
// persist: send message to journal, set a callback;
// journal reply 'persisted', call the callback;
// and then we can 'doSomethingWith' that event

// example: Blog posts

{
  // messages
  case class NewPost(text: String, id: Long)
  case class BlogPosted(id: Long)
  case class BlogNotPosted(id: Long, reason: String)
  // events for 'persistent'
  sealed trait Event
  case class PostCreated(test: String) extends Event
  case object QuotaReached extends Event
  // actor state
  case class State(posts: Vector[String], disabled: Boolean) {
    def updated(e: Event): State = e match {
      case PostCreated(text) => copy(posts = posts :+ text)
      case QuotaReached => copy(disabled = true)
    }
  }

// UserProcessor extends PersistentActor: receiveCommand and receiveRecover
  class UserProcessor extends PersistentActor {
    var state = State(Vector.empty, false)
    def updateState(e: Event) { state = state.updated(e) }

  // receiveRecover will replay all persisted events after crash-restart mishap.
  def receiveRecover = { case e: Event => updateState(e) }

  override def receiveCommand: Receive = {
      case NewPost(text, id) => {
        if (state.disabled) sender ! BlogNotPosted(id, "quota reached")
        else {
          // persist synchronously, then update internal state -- 'apply'
          persist(PostCreated(text)) { e => updateState(e); sender ! BlogPosted(id) }
          persist(QuotaReached)(updateState)
        }
      }
    }
  }
}

// while persist-persisted runs, actor is deaf, persist runs 'synchronously';
// updating state after persisted: that called 'persist then apply',
// pessimistic blocking, change internal state after writing to storage.
// high consistency, low throughput.

// persistAsync: apply then persist, optimistic blocking, change internal state
// before writing to storage. high throughput, high complexity in transactions
// logic (need to serialize all external effects).

// example: apply, persistAsync: optimistic locking
{
  case NewPost(text, id) => {
    if (!state.disabled) {
      val created = PostCreated(text)
      update(created)
      update(QuotaReached)
      persistAsync(created)(sender ! BlogPosted(id))
      persistAsync(QuotaReached)(_ => ())
    } else sender ! BlogNotPosted(id, "quota reached")
  }
}
// will sender repeat request if confirmation lost?

// another aspect: confirmation (persisted) may never come: power outage, link broken, whatnot
// but, state may be stored on disk (or, may be not).
// if not: ok, no confirmation, no save: repeat request.
// if state was stored: trouble -- should we repeat the request?

// exactly-once semantic?

// Akka in-the-box semantic: at-most-once (confirmation, no repeats)

// at-least-once semantic: sender retries until receipt arrive;
// duplicate deliveries are possible. recipient need to be idempotent to satisfy
// 'exactly-once' semantic

// retrying means we need persist 'message x needs to be sent' at the client.
// acknowledgement means we need persist 'confirmation x was sent' at the server.
// => client & server need to persist some states

// example: AtLeastOnceDelivery BlogPost (publisher: ActorPath)
{
  class UserProcessor(publisher: ActorPath)
    extends PersistentActor with AtLeastOnceDelivery {

    override def receiveRecover: Receive = {
      case PostCreated(text) => deliver(publisher, PublishPost(text, _))
      case PostPublished(id) => confirmDelivery(id)
    }

    override def receiveCommand: Receive = {
      case NewPost(text, id) => {
        persist(PostCreated(text)) { e =>
          deliver(publisher, PublishPost(text, _))
          sender ! BlogPosted(id) }}
      case PostPublished(id) => confirmDelivery(id)
    }
  }
}
// call 'deliver' to publisher method, at least once. Correlation id is passed.
// pablisher is transparent between system restarts (thanks to ActorPath).
// PostPublished => confirmDelivery -- inform AtLeastOnceDelivery that delivery was confirmed.

// for exactly-once delivery publisher needs to remember all published id.
// be idempotent
// example: Publisher, PersistentActor
{
  class Publisher extends PersistentActor {
    var expectedId = 0L

    override def receiveRecover: Receive = { case PostPublished(id) => expectedId = id + 1 }

    override def receiveCommand: Receive = {
      case PublishPost(text, id) => {
        if (id > expectedId) () // ignore, not yet ready for that
        else if (id < expectedId) sender ! PostPublished(id) // resend confirmation
        else persist(PostPublished(id)) { e =>
          sender ! e // state saved, send confirmation PostPublished
          deployPostToWebsite()
          expectedId += 1
        }}
    }
  }
}
// save expectedId, don't publish if post id < expectedId.
// id generated by AtLeastOnceDelivery.
// problem: external effects can be lost. website may not be modified.

// exactly-once messages delivery is not what we wants.
// we want exactly-once processing of the effects.

// that is a fundamental issue for message processing systems and external effects.
// choice:
// -- perform EE before persisting event: at-least-once semantics.
// -- EE  after persisting: at-most-once semantics.

// I choose at-least-once + idempotency of external system

// in this lecture we consider persistence of internal state of the components.
// not business logic.
