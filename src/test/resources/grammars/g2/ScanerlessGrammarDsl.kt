package grammars.g2

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.or
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.*

class ScanerlessGrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "subClassOf" or "subClassOf_r" * S * "subClassOf"
    }
}