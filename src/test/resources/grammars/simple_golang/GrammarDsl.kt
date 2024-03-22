package grammars.simple_golang

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class GrammarDsl: Grammar() {
    var Program by Nt()
    var Block by Nt()
    var Statement by Nt()
    var IntExpr by Nt()

    init {
        setStart(Program)
        Program = Block
        Block = Many(Statement)
        Statement = IntExpr * ";" or "r" * IntExpr * ";"
        IntExpr = "1" or "1" * "+" * "1"
    }
}