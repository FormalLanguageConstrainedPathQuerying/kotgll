package rsm.api

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.or
import org.srcgll.grammar.combinator.regexp.times
import rsm.RsmTest
import kotlin.test.assertTrue

class TerminalsEqualsTest : RsmTest {
    class AStarTerms : Grammar() {
        var S by Nt()

        init {
            setStart(S)
            S = Term("a") or Term("a") * S or S * S
        }
    }

    class AStar : Grammar() {
        var S by Nt()
        val A = Term("a")

        init {
            setStart(S)
            S = A or A * S or S * S
        }
    }

    @Test
    fun testRsm() {
        assertTrue { equalsByNtName(AStar().getRsm(), AStarTerms().getRsm()) }
    }
}