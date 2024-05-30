package grammars.cAPlusBStar

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.many
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.some
import org.ucfs.grammar.combinator.regexp.times
import org.ucfs.rsm.symbol.Term

class ScanerlessGrammarDsl : Grammar() {
    val S by Nt(Term("c") * some(Term("a")) * many(Term("b"))).asStart()
}