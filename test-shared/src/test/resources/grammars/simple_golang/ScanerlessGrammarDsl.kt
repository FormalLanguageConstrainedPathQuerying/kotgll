package grammars.simple_golang

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.or
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Many
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.or

class ScanerlessGrammarDsl : Grammar() {
    val IntExpr by Nt("1" or "1" * "+" * "1")
    val Statement by Nt(IntExpr * ";" or "r" * IntExpr * ";")
    val Block by Nt(Many(Statement))
    val Program by Nt(Block).asStart()
}