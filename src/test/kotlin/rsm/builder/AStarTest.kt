package rsm.builder

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.NT
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.or
import org.srcgll.grammar.combinator.regexp.times
import rsm.RsmTest
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AStarTest : RsmTest {
    class AStar : Grammar() {
        var S by NT()
        val A = Term("a")

        init {
            setStart(S)
            S = A or A * S or S * S
        }
    }

    @Test
    fun testRsm() {
        val aStar = AStar()
        assertNotNull(aStar.S.getNonterminal())
        assertTrue { equalsByNtName(getAStar("S"), aStar.getRsm()) }
    }
}