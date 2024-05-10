package grammars.multi_dyck

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.*

class ScanerlessGrammarDsl: Grammar() {
    val S by Nt().asStart()
    val S1 by Nt("(" * S * ")" * S)
    val S2 by Nt("{" * S * "}" * S)
    val S3 by Nt("[" * S * "]" * S)

    init {
        S /= Epsilon or S1 or S2 or S3
    }
}