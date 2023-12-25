package org.srcgll.grammar.combinator.regexp

data class Many
(
    val exp : Regexp,
)
    : Regexp
{
    override fun derive(symbol : DerivedSymbol) : Regexp
    {

        return when (val newReg = exp.derive(symbol)) {
            Epsilon -> Many(exp)
            Empty   -> Empty
            else    -> Concat(newReg, Many(exp))
        }
    }
}

fun some(exp: Regexp) = (exp * Many(exp))