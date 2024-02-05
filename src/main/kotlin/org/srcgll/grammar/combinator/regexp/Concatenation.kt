package org.srcgll.grammar.combinator.regexp

data class Concat(
    internal val head: Regexp,
    internal val tail: Regexp,
) : Regexp {

    /*
    D[s](h.t) = acceptEps(h).D[s](t) | D[s](h).t
     */
    override fun derive(symbol: DerivedSymbol): Regexp {
        val newHead = head.derive(symbol)

        if (!head.acceptEpsilon()) {
            return when (newHead) {
                Empty -> Empty
                Epsilon -> tail
                else -> Concat(newHead, tail)
            }
        }
        return when (newHead) {
            Empty -> tail.derive(symbol)
            Epsilon -> Alternative.makeAlternative(tail, tail.derive(symbol))
            else -> Alternative.makeAlternative(Concat(newHead, tail), tail.derive(symbol))
        }
    }
}

infix operator fun Regexp.times(other: Regexp): Concat = Concat(head = this, other)
infix operator fun String.times(other: Regexp): Concat = Concat(head = Term(this), other)
infix operator fun Regexp.times(other: String): Concat = Concat(head = this, Term(other))
infix operator fun String.times(other: String): Concat = Concat(head = Term(this), Term(other))