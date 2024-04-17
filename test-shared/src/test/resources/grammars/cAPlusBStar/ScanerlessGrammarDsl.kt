package grammars.cAPlusBStar

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.*
import org.ucfs.rsm.symbol.Term

class ScanerlessGrammarDsl : Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = Term("c") * Some(Term("a")) * Many(Term("b"))
    }

}