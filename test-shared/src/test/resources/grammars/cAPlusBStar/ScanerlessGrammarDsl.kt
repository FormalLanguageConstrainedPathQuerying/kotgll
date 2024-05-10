package grammars.cAPlusBStar

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.Many
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.Some
import org.ucfs.grammar.combinator.regexp.times
import org.ucfs.rsm.symbol.Term

class ScanerlessGrammarDsl : Grammar() {
    val S by Nt(Term("c") * Some(Term("a")) * Many(Term("b"))).asStart()
}