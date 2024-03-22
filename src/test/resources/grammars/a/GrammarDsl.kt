package grammars.a

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.Term

class GrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = Term("a")
    }
}