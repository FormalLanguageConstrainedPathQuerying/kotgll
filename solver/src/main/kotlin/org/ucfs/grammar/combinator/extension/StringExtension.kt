package org.ucfs.grammar.combinator.extension

import org.ucfs.grammar.combinator.regexp.*
import org.ucfs.rsm.symbol.Term

object StringExtension {
    fun makeAlternative(literals: Iterable<String>): Regexp {
        val terms = literals.map { Term(it) }
        val initial: Regexp = terms[0] or terms[1]

        return terms.subList(2, terms.size)
            .fold(initial) { acc: Regexp, i: Term<String> -> Alternative.makeAlternative(acc, i) }
    }
    infix operator fun Regexp.times(other: String): Concat = Concat(head = this, Term(other))
    infix operator fun String.times(other: String): Concat = Concat(head = Term(this), Term(other))
    infix operator fun String.times(other: Regexp): Concat = Concat(head = Term(this), other)

    infix fun String.or(other: String): Regexp = Alternative.makeAlternative(left = Term(this), Term(other))
    infix fun String.or(other: Regexp): Regexp = Alternative.makeAlternative(left = Term(this), other)
    infix fun Regexp.or(other: String): Regexp = Alternative.makeAlternative(left = this, Term(other))


    fun many(some: String): Regexp {
        return many(Term(some))
    }
    fun Option(exp: String) = Alternative.makeAlternative(Epsilon, Term(exp))

}