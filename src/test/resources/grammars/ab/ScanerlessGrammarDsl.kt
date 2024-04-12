package grammars.ab

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Nt

class ScanerlessGrammarDsl: Grammar() {
    var S by Nt()

    init {
        setStart(S)
        S = "a" * "b"
    }
}