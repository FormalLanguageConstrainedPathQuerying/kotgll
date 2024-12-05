package dyck

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.extension.StringExtension.times
import org.ucfs.grammar.combinator.regexp.Epsilon
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.or
import org.ucfs.grammar.combinator.regexp.times

class DyckGrammar : Grammar() {
    val S by Nt().asStart()
    val Round by Nt("(" * S * ")")
    val Quadrat by Nt()
    val Curly by Nt("{" * S * "}")

    init {
        S       /= S * (Round or Quadrat or Curly) or Epsilon
        Round   /= "(" * S * ")"
        Quadrat /= "[" * S * "]"
        Curly   /= "{" * S * "}"
    }
}