package grammars.a

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.rsm.symbol.Term

class ScanerlessGrammarDsl : Grammar() {
    val S by Nt(Term("a")).asStart()
}