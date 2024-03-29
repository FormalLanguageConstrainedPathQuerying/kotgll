package grammars.ab

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.extension.StringExtension.times
import org.srcgll.grammar.combinator.regexp.Nt

class ScanerlessGrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "a" * "b"
    }
}