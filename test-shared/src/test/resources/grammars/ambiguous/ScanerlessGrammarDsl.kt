package grammars.ambiguous

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.or
import org.ucfs.grammar.combinator.regexp.*

class ScanerlessGrammarDsl: Grammar() {
    val S by Nt().asStart()

    init {
        S /= "a" or S or S * S or S * S * S
    }
}