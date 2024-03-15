package grammars.cAPlusBStar

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class cAPlusBStar : Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = Term("c") * Some(Term("a")) * Many(Term("b"))
    }

}