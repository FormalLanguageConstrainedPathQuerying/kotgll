package grammars.abc

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.*
import org.ucfs.rsm.symbol.Term

class ScanerlessGrammarDsl: Grammar() {
    val A by Nt("a" * "b")
    val B by Nt(Term("b"))
    val S by Nt("a" * B * "c" or A * "c").asStart()
}