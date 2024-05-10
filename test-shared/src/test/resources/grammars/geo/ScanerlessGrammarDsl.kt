package grammars.geo

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.Option

class ScanerlessGrammarDsl : Grammar() {
    val S by Nt().asStart()

    init {
        S /= "broaderTransitive" * Option(S) * "broaderTransitive_r"
    }
}