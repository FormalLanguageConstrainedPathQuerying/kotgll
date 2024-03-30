package grammars.aBStar

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.extension.StringExtension.times
import org.srcgll.grammar.combinator.regexp.Many
import org.srcgll.grammar.combinator.regexp.Nt

class ScanerlessGrammarDsl : Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = Many("a" * "b")
    }

}