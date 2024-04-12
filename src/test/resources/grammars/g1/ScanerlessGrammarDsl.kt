package grammars.g1

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.extension.StringExtension.times
import org.srcgll.grammar.combinator.regexp.*

class ScanerlessGrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "subClassOf_r" * opt(S) * "subClassOf" or
                "type_r" * opt(S) * "type"
    }
}