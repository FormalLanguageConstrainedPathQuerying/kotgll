package grammars.c_a_star_b_star

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.many
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.times
import org.ucfs.grammar.combinator.regexp.Nt


class ScanerlessGrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "c" * many("a") * many("b")
    }
}