package grammars.aStar

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.many
import org.ucfs.grammar.combinator.regexp.Nt

class ScanerlessGrammarDsl : Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = many("a")
    }
}