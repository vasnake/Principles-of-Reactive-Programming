//Extended Example: Discrete Event Simulation (Optional) (10:54)
//https://class.coursera.org/reactive-002/lecture/37
//
//TOC
//– application example, mutable variables as real world quantifiable properties model
//– structure system as system of layers, DSL layers
//– digital circuit simulator based on a framework for discrete event simulation
//– change system state, on timer ticks: data model + event handlers/actions
//– digital circuits: composed of wires, wires transport signals, signals transformed by components
//– signals: true, false
//– base components – gates: inverter, AND gate, OR gate
//– component have a reaction time – delay ; connected by wires
//– DC diagram for half-adder: sum and carry
//– class Wire
//– components as functions with side effects (to wires?): inverter, andGate, orGate – Wire in, Wire out
//– construct function halfAdder from 4 Wires
//– functon fullAdder with 5 params — wires: 2 halfAdders, 1 orGate
//– function 'notEq': 3 params wires, 2 inverters, 2 andGates, 1 orGate

//digital circuit simulator
// based on framework for discrete event simulation
// how assignments and higher-order functions can be combined

//wires and functional components (gates)
//– inverter: inverse of its input
//– AND gate: conjunction of its inputs
//– OR gate: disjunction

// wires transport signals
// signals transformed by components
// signals: true/false

// components have a reaction time, delay:
// output don't change immediately

//inverter (not)
// in --^inv-- out

// AND gate
// in1 --
//       &and-- out
// in2 --

// OR gate
// in1 --
//       |or-- out
// in2 --

// Half Adder HA
// S = (a | b) & ^(a & b) // sum
// C = a & b              // carry

// class Wire, connects components
// for HA we have wires a,b,d,e
class Wire

// component as functions
// as a side effect: creates a gate
def inverter(input: Wire, output: Wire): Unit
def andGate(inp1: Wire, inp2: Wire, output: Wire): Unit
def orGate(inp1: Wire, inp2: Wire, output: Wire): Unit

// half-adder as a function
// a, b: input; s, c: output
def halfAdder(a: Wire, b: Wire, s: Wire, c: Wire): Unit = {
    val d, e = new Wire
    orGate(a, b, d)  // a|b = d
    andGate(a, b, c) // a&b = c
    inverter(c, e)   // ^c = e
    andGate(d, e, s) // d&e = s
}
// propagate signal in order

// full-adder
// sum, cout: output
// cin: carry in
// cout: carry out
// a, b, cin: input
def fullAdder(a: Wire, b: Wire, cin: Wire, sum: Wire, cout: Wire): Unit = {
    val s, c1, c2 = new Wire
    halfAdder(b, cin, s, c1) // b, cin => ha => s, c1
    halfAdder(a, s, sum, c2) // a, s => ha => sum, c2
    orGate(c1, c2, cout)     // c1|c2 = cout
}

// exercise
// what logical function is it
def func(a: Wire, b: Wire, c: Wire): Unit = {
    val d, e, f, g = new Wire
    inverter(a, d)      // ^a = d
    inverter(b, e)      // ^b = e
    andGate(a, e, f)    // a&e = f
    andGate(b, d, g)    // b&d = g
    orGate(f, g, c)     // f&g = c
}
// f|g == (a&e) | (b&d) == (a & ^b) | (b & ^a)
// 1,1 = 0
// 0,0 = 0
// 1,0 = 1
// 0,1 = 1
// a != b
