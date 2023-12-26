package rsm.builder

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.or
import org.srcgll.grammar.combinator.regexp.times
import rsm.RsmTest
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AStarTest : RsmTest {
    class AStar : Grammar() {
        var S by Nt()

        init {
            setStart(S)
            S = Term("a") or Term("a") * S or S * S
        }
    }

    @Test
    fun testRsm() {
        val grammar = AStar()
        assertNotNull(grammar.S.getNonterminal())
        assertTrue { equalsByNtName(getAStarRsm("S"), grammar.getRsm()) }
    }
}