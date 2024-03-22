package grammars.bracket_star_x

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.or
import org.srcgll.grammar.combinator.regexp.times

class GrammarDsl: Grammar() {
    var List by Nt()
    var Elem by Nt()

    init {
        setStart(List)
        List = "[" * Elem
        Elem = "x" or List
    }
}