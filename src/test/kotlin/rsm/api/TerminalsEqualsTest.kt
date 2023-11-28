package rsm.api

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.GlobalState
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.NT
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.or
import org.srcgll.grammar.combinator.regexp.times
import kotlin.test.assertTrue

class TerminalsEqualsTest {
    class AStarTerms : Grammar() {
        var S by NT()
        val A = Term("a")

        init {
            setStart(S)
            S = Term("a") or Term("a") * S or S * S
        }
    }

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
        GlobalState.resetCounter()
        val expected = AStar().buildRsm()
        GlobalState.resetCounter()
        val actual = AStarTerms().buildRsm()
        assertTrue { expected.rsmEquals(actual) }
    }
}