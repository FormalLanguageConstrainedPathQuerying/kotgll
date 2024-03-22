package grammars.ambiguous

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class GrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "a" or S or S * S or S * S * S
    }
}