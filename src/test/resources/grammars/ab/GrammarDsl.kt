package grammars.ab

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.times

class GrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "a" * "b"
    }
}