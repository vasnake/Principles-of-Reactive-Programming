//Identity and Change (8:12)
//https://class.coursera.org/reactive-002/lecture/31
//
//TOC
//– are objects the same or different: identity
//– referential transparency: val x = E; val y = E; == val x = E; val y = x // key property for substitution model
//– is two BankAccoun the same? define 'being the same'
//– property: operational equivalence – if no possible test can distinguish between them
//– tests: arbitrary sequence of ops on (x, y) observing outcomes
//– then, repeat, renaming 'y' by 'x' in tests code. if results are different = x and y are different
//– ok, test operational equivalence for BankAccount : x deposit 30; y withdraw 20 // x deposit 30; x withdraw 20 – different
//– that difference leads to : substitution model cannon be used
//– SM can be adapted if we introduce a store, but this way to complicated
//– good bye, substitution model

// exclude assignments, get referential transparency
val E = "some"

{ val x = E; val y = E }
// x and y are the same
// we could have also written
{ val x = E; val y = x }

// but if assignment is allowed, we can't have referential transparency
val x = new BankAccount
val y = new BankAccount
// x and y are different

// what it means, to being the same

// meaning is defined by the property of
// operational equivalence

// x and y are op.eq if 'no possible test' can distinguish
// between them

// to test if x and y are the same
// execute arbitrary sequence 'f' of operations that involves x,y
// observe outcomes
{
    val x = new BankAccount
    val y = new BankAccount
    f(x, y)
}
// then, execute sequence, renaming y by x
{
    val x = new BankAccount
    val y = new BankAccount
    f(x, x)
}
// if you see the difference, then expressions are different

// example
{ // 1 half of the experiment
val x = new BankAccount
    val y = new BankAccount
    x deposit 30 // 30
    y withdraw 20 // insufficient funds
}
{ // 2 half of the experiment
val x = new BankAccount
    val y = new BankAccount
    x deposit 30 // 30
    x withdraw 20 // 10
}
// you can see the difference

// objects are the same
{
    val x = new BankAccount
    val y = x
}
// but, to prove it, we should exec infinite number of operations

// can't use substitution model in  mutable world
{
    val x = new BankAccount
    val y = x
        // and we rewrite y as
    { val y = new BankAccount }
    // no, we can't: it wont be the same object
}
