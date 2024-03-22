package grammars.multi_dyck

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class GrammarDsl: Grammar() {
    var S by Nt()
    var S1 by Nt()
    var S2 by Nt()
    var S3 by Nt()

    init {
        setStart(S)
        S = Epsilon or S1 or S2 or S3
        S1 = "(" * S * ")" * S
        S2 = "{" * S * "}" * S
        S3 = "[" * S * "]" * S
    }
}