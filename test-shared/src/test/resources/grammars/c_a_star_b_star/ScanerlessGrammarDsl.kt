package grammars.c_a_star_b_star

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.many
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.times


class ScanerlessGrammarDsl : Grammar() {
    val S by Nt("c" * many("a") * many("b")).asStart()
}