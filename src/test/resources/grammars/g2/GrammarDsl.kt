package grammars.g2

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class GrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "subClassOf" or "subClassOf_r" * S * "subClassOf"
    }
}