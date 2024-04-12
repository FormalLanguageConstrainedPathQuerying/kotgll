package org.srcgll.grammar.combinator.regexp


open class Alternative(
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
    }

    override fun derive(symbol: DerivedSymbol): Regexp {
        return makeAlternative(left.derive(symbol), right.derive(symbol))
    }
}

class Optional private constructor(val exp: Regexp): Alternative(Epsilon, exp){
    companion object{
        fun create(exp: Regexp): Alternative = Optional(exp)
    }
}

infix fun Regexp.or(other: Regexp): Regexp = Alternative.makeAlternative(left = this, other)

fun opt(exp: Regexp): Alternative = Optional.create(exp)