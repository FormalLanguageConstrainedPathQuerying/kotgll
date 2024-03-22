package grammars.abc

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class GrammarDsl: Grammar() {
    var S by Nt()
    var A by Nt()
    var B by Nt()

    init {
        setStart(S)
        S = "a" * B * "c" or A * "c"
        A = "a" * "b"
        B = Term("b")
    }
}