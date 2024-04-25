package org.ucfs.examples.golang

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.or
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Many
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.or

class SimpleGolang : Grammar() {
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
