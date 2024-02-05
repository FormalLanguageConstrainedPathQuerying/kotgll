package org.srcgll.grammar.combinator.regexp

data class Many(
    val exp: Regexp,
) : Regexp {
    override fun derive(symbol: DerivedSymbol): Regexp {
        val newReg = exp.derive(symbol)

        return when (newReg) {
            Epsilon -> Many(exp)
            Empty -> Empty
            else -> Concat(newReg, Many(exp))
        }
    }
}

val Regexp.many: Many
    get() = Many(this)

fun Some(exp: Regexp) = exp * Many(exp)
fun Some(exp: String) = Term(exp) * Many(Term(exp))