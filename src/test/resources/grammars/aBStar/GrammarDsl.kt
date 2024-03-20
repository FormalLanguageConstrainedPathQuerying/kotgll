package grammars.aBStar

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Many
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.times
import org.srcgll.rsm.symbol.Term

class GrammarDsl : Grammar(){
    var S by Nt()
    init{
        setStart(S)
        S = Many(Term(Token.A) * Term(Token.B))
    }

}