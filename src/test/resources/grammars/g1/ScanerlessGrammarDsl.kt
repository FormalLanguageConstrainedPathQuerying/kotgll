package grammars.g1

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.*

class ScanerlessGrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "subClassOf_r" * Option(S) * "subClassOf" or
                "type_r" * Option(S) * "type"
    }
}