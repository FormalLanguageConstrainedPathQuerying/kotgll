package org.srcgll.grammar.combinator.regexp


data class Alternative
    (
    internal val left: Regexp,
    internal val right: Regexp,
) : Regexp {
    companion object {
        fun makeAlternative(left: Regexp, right: Regexp): Regexp {
            if (left is Empty) return right
            if (right is Empty) return left

            if (left is Alternative && (right == left.left || right == left.right)) {
                return left
            }
            if (right is Alternative && (left == right.left || left == right.right)) {
                return right
            }
            return if (left == right) left else Alternative(left, right)
        }

        fun makeAlternative(literals: Iterable<String>): Regexp {
            val terms = literals.map { Term(it) }
            val initial: Regexp = terms[0] or terms[1]

            return terms.subList(2, terms.size)
                .fold(initial) { acc: Regexp, i: Term<String> -> Alternative.makeAlternative(acc, i) }
        }
    }

    override fun derive(symbol: DerivedSymbol): Regexp {
        return makeAlternative(left.derive(symbol), right.derive(symbol))
    }

}

infix fun Regexp.or(other: Regexp): Regexp = Alternative.makeAlternative(left = this, other)

fun opt(exp: Regexp): Regexp = Alternative.makeAlternative(exp, Epsilon)