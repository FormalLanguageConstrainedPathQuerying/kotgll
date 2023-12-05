package rsm.api

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.NT
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.times
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.add
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.rsm.writeRSMToDOT
import rsm.RsmTest
import kotlin.test.assertEquals

class UnionWithRsmTest : RsmTest {
    class DyckLanguage : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = Term("(") * S * Term(")")
        }
    }

    @Test
    fun testSimpleAddition() {
        val grammar = DyckLanguage()
        val startOrigin = grammar.getRsm()
        val oldOrigin = startOrigin
        val deltaStart = RSMState(grammar.S.getNonterminal()!!, isStart = true, isFinal = false)
        val st1 = RSMState(grammar.S.getNonterminal()!!, isStart = false, isFinal = false)
        val st2 = RSMState(grammar.S.getNonterminal()!!, isStart = false, isFinal = false)
        val st3 = RSMState(grammar.S.getNonterminal()!!, isStart = false, isFinal = true)
        deltaStart.addEdge(Terminal("["), st1)
        st1.addEdge(grammar.S.getNonterminal()!!, st2)
        st2.addEdge(Terminal("]"), st3)
        writeRSMToDOT(deltaStart, "delta.dot")
        writeRSMToDOT(startOrigin, "source.dot")
        startOrigin.add(deltaStart)
        writeRSMToDOT(startOrigin, "result.dot")
        assertEquals(oldOrigin, startOrigin)
    }
}