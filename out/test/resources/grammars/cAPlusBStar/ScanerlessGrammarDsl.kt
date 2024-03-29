package grammars.cAPlusBStar

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.rsm.symbol.Term

class ScanerlessGrammarDsl : Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = Term("c") * Some(Term("a")) * Many(Term("b"))
    }

}