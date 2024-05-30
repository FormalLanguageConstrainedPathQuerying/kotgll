package rsm.api

import org.junit.jupiter.api.Test
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.or
import org.ucfs.grammar.combinator.regexp.times
import org.ucfs.rsm.symbol.Term
import rsm.RsmTest
import kotlin.test.assertTrue

class TerminalsEqualsTest : RsmTest {
    class AStarTerms : Grammar() {
        val S by Nt().asStart()

        init {
            S /= Term("a") or Term("a") * S or S * S
        }
    }

    class AStar : Grammar() {
        val S by Nt().asStart()
        val A = Term("a")

        init {
            S /= A or A * S or S * S
        }
    }

    @Test
    fun testRsm() {
        assertTrue { equalsByNtName(AStar().rsm, AStarTerms().rsm) }
    }
}