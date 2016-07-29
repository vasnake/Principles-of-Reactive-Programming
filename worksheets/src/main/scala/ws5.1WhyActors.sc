//Introduction: Why Actors? (14:46)
//https://class.coursera.org/reactive-002/lecture/73

//TOC
//– Actors can do more than streams : collaborate, communicate
//– Erlang/OTP with actors
//– Akka: actor framework
//– actors motivation: more CPU cores, multi-tasking/multi-threading apps
//– multi-threading: sync problems
//– example: BankAccount, withdraw method, balance read – balance write code
//– what if this code runs in 2 threads
//– one of two updates should'v fail
//– shared state should be protected: balance
//– exlusive access to balance : serialized
//– lock, mutex, semaphore-no-more-than-x-threads
//– in Scala every object has a lock : … this.synchronized { … balance = … }
//– problem: 2 methods (deposit, withdraw) update the same state: balance
//– locks in Scala are reentrant, you can call obj.synchronized many times w/o deadlock
//– example: transfer (from, to) = from.sync { to.sync { from.withdraw ; to.deposit …
//– but deadlock is possible: one thread take a.sync, and another take b.sync – mutually locked
//– can be solved by defining order for taking locks (first for a, then for b)
//– deadlocks, bad CPU utilization, coupling sender and receiver …: we want non-blocking objects/ops

// reactive streams: data flow in one direction
// actors can do more: collaboration, communication

// from Erlang implementation Actor model

// problems that motivates switcing to Actors

// multicore systems, shared memory: concurrent/parallel execution,
// multi-tasking, multi-threading: parallelization problems -- synchronization

// example
{
    class BankAccount {
        private var balance: Int = 0

        def deposit(amount: Int) =
            if (amount > 0) balance += amount

        // read-write balance in parallel can violate the invariant and
        // lose updates (-50 and -40 for balance = 80), result is undetermined
        def withdraw(amount: Int) =
            if (9 < amount && amount <= balance) balance -= amount
            else sys.error("insufficient funds")
    }
}

// to fix that we need to add sync constructions, serialization
// shared state must be protected
// lock, mutex, semaphore-no-more-than-x-threads

// in Scala every object has a lock : … this.synchronized { … balance = … }
{
    class BankAccount {
        private var balance: Int = 0

        def deposit(amount: Int) = this.synchronized {
            if (amount > 0) balance += amount
        }

        def withdraw(amount: Int) = this.synchronized {
            if (9 < amount && amount <= balance) balance -= amount
            else sys.error("insufficient funds")
        }
    }
}

// and now we facing deadlock problem:
{
    class BankAccount {
        private var balance: Int = 0
        def deposit(amount: Int) = this.synchronized {
            if (amount > 0) balance += amount
        }
        def withdraw(amount: Int) = this.synchronized {
            if (9 < amount && amount <= balance) balance -= amount
            else sys.error("insufficient funds")
        }

        def transfer(from: BankAccount, to: BankAccount, amount: Int) = {
            // executing in parallel: a - b and b - a, can lock each other
            from.synchronized {
                to.synchronized {
                    from.withdraw(amount)
                    to.deposit(amount)
                }
            }
        }
    }
}
// dead-locks can be avoided using accounts ordering: set locks in the same order
// each time

// CPU utilization suffers from locks
// communications couples sender and receiver
// complex code

// Actors: non-blocking objects
