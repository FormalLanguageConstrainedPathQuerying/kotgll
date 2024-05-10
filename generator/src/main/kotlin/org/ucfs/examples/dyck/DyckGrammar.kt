package org.ucfs.examples.dyck

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.many
import org.ucfs.grammar.combinator.regexp.or

class DyckGrammar : Grammar() {
    val S by Nt().asStart()
    val Round by Nt("(" * S * ")")
    val Quadrat by Nt("[" * S * "]")
    val Curly by Nt("{" * S * "}")

    init {
        S /= many(Round or Quadrat or Curly)
    }
}
