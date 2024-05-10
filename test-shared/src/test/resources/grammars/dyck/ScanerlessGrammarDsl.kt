package grammars.dyck

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.*

class ScanerlessGrammarDsl: Grammar() {
    val S by Nt().asStart()

    init {
        S /= Epsilon or  "(" * S * ")" * S
    }
}