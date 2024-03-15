package grammars.aStar

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Many
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.Term

class aStar : Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = Many(Term("a"))
    }
}