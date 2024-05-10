package grammars.aStar

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.many
import org.ucfs.grammar.combinator.regexp.Nt

class ScanerlessGrammarDsl : Grammar() {
    val S by Nt(many("a")).asStart()
}