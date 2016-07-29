//Discrete Event Simulation: API and Usage (Optional) (10:57)
//https://class.coursera.org/reactive-002/lecture/45
//
//TOC
//– Wire, inverter, andGate, orGate: implementation based on API for discrete event simulation
//– DES performs actions at a given moment; type Action = () => Unit // Unit => Unit
//– the time is simulated
//– trait Similation: currentTime, afterDelay, run
//– class diagram for app: layers  Simulation; Gates; Circuits; MySimulaton
//– gates layer: Wire – getSignal, setSignal, addAction
//– Wire implementation : state = value of the signal, list of actions
//– Inverter implementation: installing an action on its input wire – inverse and output after a delay
//– andGate/orGate is similar to Inverter

//digital circuit simulator
// based on framework for discrete event simulation
// how assignments and higher-order functions can be combined

// gates and wires implementation

//discrete event simulation framework
//simulator performs 'actions' specified by the user at a given 'moment'
type Action = () => Unit
// all work in side effects

// the 'time' is simulated; it has nothing to do with the actual time

// simulation happens inside an derived object
// simulator API
trait Simulation {
    // get current simulated time
    def currentTime: Int

    // register an action to perform after a delay
    def afterDelay(delay: Int)(block: => Action): Unit
    // can you see CBN parameter?

    // perform the simulation until there are no more actions waiting
    def run(): Unit
}

// simulator in flesh would be a objects hierarchy like
// Simulation >: Gates >: Circuits >: MyCircuit01

// Gates will consist of Wire class, 3 gates

// Wire API
// getSignal: Boolean
// setSignal(sig: Boolean): Unit
// addAction(a: Action): Unit
// attached actions are executed at each change of the transported signal
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

// gates API

// inverter
// action on its input wire, after delay produce inverted signal in output wire
def inverter (input: Wire, output: Wire): Unit = {

    def invertAction(): Unit = {
        val inSig = input.getSignal
        afterDelay(InverterDelay) { output setSignal !inSig }
    }

    input addAction invertAction
}

// AND gate
def andGate(in1: Wire, in2: Wire, out: Wire): Unit = {

    def andAction(): Unit = {
        val in1Sig = in1.getSignal
        val in2Sig = in2.getSignal
        afterDelay(AndGateDelay) { out setSignal (in1Sig & in2Sig) }
    }

    in1 addAction andAction
    in2 addAction andAction
}

// OR gate
def orGate(in1: Wire, in2: Wire, out: Wire): Unit = {

    def action(): Unit = {
        val in1Sig = in1.getSignal
        val in2Sig = in2.getSignal
        afterDelay(OrGateDelay) { out setSignal (in1Sig | in2Sig) }
    }

    in1 addAction action
    in2 addAction action
}
// n.b: getSignal get value from wire before delay
// why? after dalay it can be different signal
