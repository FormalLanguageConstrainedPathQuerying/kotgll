package grammars.c_a_star_b_star

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class GrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = Term("c") * Many(Term("a")) * Many(Term("b"))
    }
}