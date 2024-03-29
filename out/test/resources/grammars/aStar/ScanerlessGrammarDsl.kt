package grammars.aStar

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Many
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.rsm.symbol.Term

class ScanerlessGrammarDsl : Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = Many(Term("a"))
    }
}