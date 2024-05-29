package org.ucfs.grammar.combinator.regexp

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

fun many(some: Regexp): Many {
    return Many(some)
}
val Regexp.many: Many
    get() = Many(this)

fun some(exp: Regexp) = exp * Many(exp)