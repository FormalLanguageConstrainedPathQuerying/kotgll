package rsm.builder

import org.junit.jupiter.api.Test
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.grammar.combinator.regexp.or
import org.ucfs.grammar.combinator.regexp.times
import org.ucfs.rsm.symbol.Term
import rsm.RsmTest
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AStarTest : RsmTest {
    class AStar : Grammar() {
        val S by Nt().asStart()

        init {
            S /= Term("a") or Term("a") * S or S * S
        }
    }

    @Test
    fun testRsm() {
        val grammar = AStar()
        assertNotNull(grammar.S.nonterm)
        assertTrue { equalsByNtName(getAStarRsm("S"), grammar.rsm) }
    }
}