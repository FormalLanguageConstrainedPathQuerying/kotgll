package grammars.bracket_star_x

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.or
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Nt

class ScanerlessGrammarDsl : Grammar() {
    val List by Nt().asStart()
    val Elem by Nt("x" or List)

    init {
        List /= "[" * Elem
    }
}