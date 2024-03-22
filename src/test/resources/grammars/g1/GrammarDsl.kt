package grammars.g1

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class GrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "subClassOf_r" * Option(S) * "subClassOf" or
                "type_r" * Option(S) * "type"
    }
}