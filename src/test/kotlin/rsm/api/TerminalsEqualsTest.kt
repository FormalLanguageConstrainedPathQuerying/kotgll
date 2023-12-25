package rsm.api

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.NT
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.or
import org.srcgll.grammar.combinator.regexp.times
import org.srcgll.rsm.writeRSMToDOT
import rsm.RsmTest
import kotlin.test.assertFalse

class TerminalsEqualsTest : RsmTest {
    class AStarTerms : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = Term("a") or Term("a") * S or S * S
        }
    }

    class AStar : Grammar() {
        var S by NT()
        var A by NT()

        init {
            setStart(S)
            S = A or A * S or S * S
            A = Term("a")

        }
    }

    @Test
    fun testRsm() {
        writeRSMToDOT(AStar().getRsm(), "actual.dot")
        assertFalse { equalsByNtName(AStar().getRsm(), AStarTerms().getRsm()) }
    }
}