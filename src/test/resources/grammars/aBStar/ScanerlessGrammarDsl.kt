package grammars.aBStar

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Many
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.rsm.symbol.Term
import org.srcgll.grammar.combinator.regexp.times

class ScanerlessGrammarDsl : Grammar(){
    var S by Nt()
    init{
        setStart(S)
        S = Many(Term("a") * Term("b"))
    }

}