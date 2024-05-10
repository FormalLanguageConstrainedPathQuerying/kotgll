package grammars.g1

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.Option
import org.ucfs.grammar.combinator.regexp.or

class ScanerlessGrammarDsl : Grammar() {
    val S by Nt().asStart()

    init {
        S /= "subClassOf_r" * Option(S) * "subClassOf" or
                "type_r" * Option(S) * "type"
    }
}