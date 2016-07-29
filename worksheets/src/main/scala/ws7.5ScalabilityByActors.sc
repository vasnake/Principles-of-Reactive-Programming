//Scalability (17:00)
//https://class.coursera.org/reactive-002/lecture/103

//TOC
//– favor scalability over single-threaded performance
//– low performance: system is slow for a single client
//– low scalability: system is fast for 1 client, but slow for many
//– req/sec limit as high as possible / resp.time is constant
//– it means (for Actor System): many stateless actors (replicas) running concurrently
//– one actor = one message at a time
//– async message passing gives as a possibility for scaling up
//– worker pool, message routing: stateful (round robin, smallest queue, …);
//  stateless(random, consistent hashing, …)
//– stateless router: can be more than one, no need to share state
//– round-robin routing: imbalances lead to larger spread in latency spectrum
//– smallest mailbox routing: priority, routees need to be local for inspection of queue; hight routing cost
//– shared work queue: required routees to be local; most homogenous latency; effectively a pull model.
//– adaptive routing: requires feedback about proc.times, latencies, queue sizes
//  feedback control theory // oscillations, over-dampling
//– random routing: can stochastically lead to imbalance ; stateless
//– consistent hashing: can exhibit systematic imbalances based on hashing function
//  sticky sessions is possible
//– consistent hashing can be used if substreams correspond to independent parts of the state
//  replication of stateful actors // actors need to communicate
//  sharding and stuff?
//– multiple writers to the same state require appropriate data structures and are eventually consistent
//  distributed store example
//– persistent stateful actors can be replicated
//– not only scalability, fault tolerance as well
//  persistent state, recover failed actor (on othe node maybe)
//  can start many instances, only one is active
//  consistent routing to the active instance // session
//  message buffering
//– vertical scalability by async messages
//– horizontal scalability by location transparency

// features of actors
// scale up: async messages;
// scale out: more actors (stateless), location transparency.

// we favor scalability over performance (for single client)
// req/sec should grow linearly with number of clients; constant resp.time

// it means: stateless concurrent actor replicas processing messages,
// one actor -- one message at a time.
// more clients -- more actors.

// Routing messages to worker pools:
// -- stateful: round robin, smallest queue, adaptive, ...
// -- stateless: random, consistent hashing, ...

// stateless is less accurate but more scalable (don't have to share anything)

// stateful

// round-robin: equal distribution of messages, does not give a fuck about node workload,
// node mailbox may be full

// smallest mailbox routing: evens out imbalances; high routing cost

// shared work queue: requires routees to be local; effectively a pull model;
// most homogenous latency

// adaptive routing, good choice: requires feedback from workers; steering the routing weights
// (feedback control theory: oscillations, over-dampening)

// stateless

// random routing: equal (almost) distribution of messages, low routing overhead,
// may have several distributed routers;
// nodes workload are ignored.

// consistent hashing: 'all red to 1, all blue to 2';
// nodes workload are ignored; hashing function must be good;
// substreams bundling, sticky sessions, CDN functionality

// sticky session means: we can use stateful actors, user will see updated state
// working with the same actor on every request.
// It's possible if substreams (of requests) correspond to independent parts of the state.

// consistent hashing routing can be used to replicate stateful actors:
// input stream splits in such a fashion that a state affected by substreams are independent.
// Appropriate data structure required: CRDT: Convergent and Commutative Replicated Data Types
// for eventual consistency.
// In such a case replication used more for fault tolerance than scalability.

// replication of persisted stateful actors

// based on persisted state (persist-persisted)
// only one instance is active
// consistent routing to the active instance
// buffering messages during recovery
// migration: recovery at a different location
