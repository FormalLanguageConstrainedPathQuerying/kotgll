package grammars.g2

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.or
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Nt

class ScanerlessGrammarDsl : Grammar() {
    val S by Nt().asStart()

    init {
        S /= "subClassOf" or "subClassOf_r" * S * "subClassOf"
    }
}