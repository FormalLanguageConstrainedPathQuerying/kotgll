package grammars.bracket_star_x

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.or
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Nt

class ScanerlessGrammarDsl : Grammar() {
    var List by Nt()
    var Elem by Nt()

    init {
        setStart(List)
        List = "[" * Elem
        Elem = "x" or List
    }
}