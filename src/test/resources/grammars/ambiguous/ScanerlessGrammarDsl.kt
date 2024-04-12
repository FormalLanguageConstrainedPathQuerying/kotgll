package grammars.ambiguous

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.or
import org.ucfs.grammar.combinator.regexp.*

class ScanerlessGrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "a" or S or S * S or S * S * S
    }
}