package grammars.a

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.rsm.symbol.Term

class ScanerlessGrammarDsl : Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = Term("a")
    }
}