package grammars.abc

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.*
import org.ucfs.rsm.symbol.Term

class ScanerlessGrammarDsl: Grammar() {
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