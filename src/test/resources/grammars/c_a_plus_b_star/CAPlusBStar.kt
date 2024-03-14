package grammars.c_a_plus_b_star

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class CAPlusBStar : Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = Term("c") * Some(Term("a")) * Many(Term("b"))
    }

}