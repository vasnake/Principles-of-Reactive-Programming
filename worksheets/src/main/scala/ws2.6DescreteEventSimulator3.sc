//Discrete Event Simluation: Implementation and Test (Optional) (18:12)
//https://class.coursera.org/reactive-002/lecture/41
//
//TOC
//– trait Simulation implementation
//– type Agenda = List[Event] mutable list of actions, event loop – propagate signals (added using 'afterDelay')
//– case class Event(time: Int, action: Action);
//– agenda sorted by time
//– time handling: var curtime; afterDelay(delay)(block) = agenda.add(Event(curtime+delay, () => block) )
//– event loop: agenda match { case first :: rest => … loop() }
//– run: start the event loop
//– a way to examine the changes of the signals: def probe(name: String, wire: Wire): Unit = … print
//– parameters (delays) mixin: trait Parameters { def InverterDelay = 2 …
//– object sim extends Circuits with Parameters
//– test simulation in worksheet
//– OR-gate alternative : inverter, andGate, inverter
//– time to stabilize the circuit – all signals processed
//– bottom line: state and assignments make our mental model of computations more complicated
//– we lose referential transparency ; but, assignments allow us to formulate certain programs in an elegant way
//– trade-off: provability/reasoning vs concise implementation / more expressiveness

    // Lecture 3.6 - Discrete Event Simulation: Implementation and Test

// Simulation implementation, get all pieces together

// discrete event simulation framework
trait Simulation {
    // idea: keep an 'agenda' in every simulation instance
    // agenda: sorted list of events
    // event: action, time-when-to-start-action

    type Action = () => Unit // gate action function

    case class Event(time: Int, action: Action) // when signal will reach the gate output

    private type Agenda = List[Event]

    private var agenda: Agenda = List() // mutable state, no events so far
    // sorted by event.time

    private var curtime = 0 // state, current clock tick

    private def insert(ag: Agenda, item: Event): Agenda = ag match {
        // maintain sorted list
        case first :: rest if first.time <= item.time =>
            first :: insert(rest, item)
        case _ => item :: ag
    }

    // running loop
    private def loop(): Unit = agenda match {
        // pop event, run its action (generate out signal for gate)
        case Nil => // termination
        case first :: rest =>
            agenda = rest
            curtime = first.time
            first.action()
            loop()
    }

    // API

    def currentTime: Int = curtime

    // insert event into agenda
    def afterDelay(delay: Int)(actionblock: => Unit): Unit = {
        // n.b. CBN action block
        // create new event, update agenda
        val e = Event(currentTime + delay, () => actionblock)
        agenda = insert(agenda, e)
    }

    def run(): Unit = {
        afterDelay(0) { println("*** simulation strted, time: " + currentTime + " ***") }
        loop()
    }
}

// gates and wires layer
trait Gates extends Simulation {

    def InverterDelay: Int
    def AndGateDelay: Int
    def OrGateDelay: Int

    class Wire {

        private var sigVal = false // mutable state: default signal = 0
        private var actions: List[Action] = List() // attached gates, actually

        def getSignal: Boolean = sigVal

        // side effects (result: Unit)
        def setSignal(s: Boolean): Unit = if (s != sigVal) {
            sigVal = s
            actions foreach(_()) // propagate signal to attached gates
        }

        def addAction(a: Action): Unit = {
            actions = a :: actions
            a() // kick start
        }
    }
    // inverter
    // action on its input wire, after delay produce inverted signal in output wire
    def inverter (input: Wire, output: Wire): Unit = {

        def action(): Unit = {
            val inSig = input.getSignal
            afterDelay(InverterDelay) { output setSignal !inSig }
        }

        input addAction action
    }

    // AND gate
    def andGate(in1: Wire, in2: Wire, out: Wire): Unit = {

        def action(): Unit = {
            val in1Sig = in1.getSignal
            val in2Sig = in2.getSignal
            afterDelay(AndGateDelay) { out setSignal (in1Sig & in2Sig) }
        }

        in1 addAction action
        in2 addAction action
    }

    // OR gate
    def orGate(in1: Wire, in2: Wire, out: Wire): Unit = {

        def action(): Unit = {
            val in1Sig = in1.getSignal
            val in2Sig = in2.getSignal
            afterDelay(OrGateDelay) { out setSignal (in1Sig | in2Sig) }
            // n.b: action get value from wire before delay
            // why? after dalay it can be different signal
        }

        in1 addAction action
        in2 addAction action
    }

    // we need a way to examine the changes of the signals on the wires
    // component 'probe' sit on wire, called when signal changes
    def probe(name: String, wire: Wire): Unit = {

        def action(): Unit = {
            println(s"$name $currentTime value: ${wire.getSignal}")
        }

        wire addAction action
    }
}

// circuits layer
trait Circuits extends Gates {

    def halfAdder(a: Wire, b: Wire, s: Wire, c: Wire): Unit = {
        val d, e = new Wire
        orGate(a, b, d)  // a|b = d
        andGate(a, b, c) // a&b = c
        inverter(c, e)   // ^c = e
        andGate(d, e, s) // d&e = s
        // propagate signal in order
    }

    // sum, cout (carry out): output
    // a, b, cin (carry in): input
    def fullAdder(a: Wire, b: Wire, cin: Wire, sum: Wire, cout: Wire): Unit = {
        val s, c1, c2 = new Wire
        halfAdder(b, cin, s, c1) // b, cin => ha => s, c1
        halfAdder(a, s, sum, c2) // a, s => ha => sum, c2
        orGate(c1, c2, cout)     // c1|c2 = cout
    }
}

// parameters for simulator mixin
trait Parameters {
    def InverterDelay = 2
    def AndGateDelay = 3
    def OrGateDelay = 5
}


// simulator instance
object sim extends Circuits with Parameters
import sim._

//4 wires for half-adder
val in1, in2, sum, carry = new Wire
// connect wires with HA
halfAdder(in1, in2, sum, carry)
// put probe on wires
probe("sum", sum)
probe("carry", carry)

// change the signal on wire
in1 setSignal true
// run simulation
run()
// result
//*** simulation strted, time: 0 ***
//    sum 8 value: true

in2 setSignal true
run()
//*** simulation strted, time: 8 ***
//    carry 11 value: true
//    sum 16 value: false

// A variant, lets do some funny staff

//an alternative version of the OR-gate can be defined in terms of AND and INV
def orGateAlt(in1: Wire, in2: Wire, out: Wire): Unit = {
    val notIn1, notIn2, notOut = new Wire
    inverter(in1, notIn1)           // ^a = na
    inverter(in2, notIn2)           // ^b = nb
    andGate(notIn1, notIn2, notOut) // na & nb = no
    inverter(notOut, out)           // ^no = out
    // ^(na & nb) = ^(^a & ^b) = out
}
// if we replace orGate by orGateAlt in our sim?
// time will be different
// additional events may be produced
// why? more components, take more time to stabilize

// bottom line

// state and assignments make our mental model of computation more complicated
// we lose referential transparency

// but, assignments allow us to formulate certain programs in an elegant way
// (links to mutable objects, chain of commands, make propagating signals easy)

// mutable list of actions
// effect of actions change state of objects and can install other actions

// with higher-order functions and lazy evaluation we can combine very
// powerful and concise programs

// avoid (mutable) state if you can
// use it if you must
