package grammars.geo

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class GrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "broaderTransitive" * Option(S) * "broaderTransitive_r"
    }
}