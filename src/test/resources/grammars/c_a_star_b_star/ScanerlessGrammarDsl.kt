package grammars.c_a_star_b_star

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.extension.StringExtension.many
import org.srcgll.grammar.combinator.extension.StringExtension.times
import org.srcgll.grammar.combinator.regexp.times
import org.srcgll.grammar.combinator.regexp.Nt


class ScanerlessGrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "c" * many("a") * many("b")
    }
}