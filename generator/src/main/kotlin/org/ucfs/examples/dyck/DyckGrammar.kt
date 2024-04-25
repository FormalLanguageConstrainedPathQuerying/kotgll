package org.ucfs.examples.dyck

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.many
import org.ucfs.grammar.combinator.regexp.or

class DyckGrammar : Grammar() {
    var S by Nt()
    var Round by Nt()
    var Quadrat by Nt()
    var Curly by Nt()

    init {
        setStart(S)
        S = many(Round or Quadrat or Curly)
        Round = "(" * S * ")"
        Quadrat = "[" * S * "]"
        Curly = "{" * S * "}"
    }
}
