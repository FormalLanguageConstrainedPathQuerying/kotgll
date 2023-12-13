package rsm.api

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.add
import org.srcgll.rsm.getAllStates
import org.srcgll.rsm.remove
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import rsm.RsmTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compare incremental union of Grammar Rsm and linear Delta
 * Nonterminals in Delta must be the same as in Origin Rsm!
 */
class UnionWithLinearTest : RsmTest {

    private fun testIncremental(
        origin: RSMState,
        delta: RSMState,
        expected: RSMState,
        expectedCommonStates: Int = 0,
        isRemoving: Boolean = false
    ) {
        writeDotInDebug(expected, "expected")
        writeDotInDebug(origin, "origin")
        val originStates = origin.getAllStates()
        if (isRemoving) {
            origin.remove(delta)
        } else {
            origin.add(delta)
        }
        writeDotInDebug(origin, "actual")
        assertTrue { equalsByNtName(expected, origin) }
        assertEquals(expectedCommonStates, originStates.intersect(origin.getAllStates()).size)
    }

    @Test
    fun `test Dyck union`() {
        /**
         * Grammar for language S = ( S )
         */
        class DyckLanguage : Grammar() {
            var S by NT()

            init {
                setStart(S)
                S = Term("(") * S * Term(")")
            }
        }

        /**
         * Rsm for [ S ]
         */
        fun getDelta(nonTerm: Nonterminal): RSMState {
            val deltaStart = RSMState(nonTerm, isStart = true)
            val st1 = RSMState(nonTerm)
            val st2 = RSMState(nonTerm)
            val st3 = RSMState(nonTerm, isFinal = true)
            deltaStart.addEdge(Terminal("["), st1)
            st1.addEdge(nonTerm, st2)
            st2.addEdge(Terminal("]"), st3)
            return deltaStart
        }

        /**
         * Grammar for language S = ( S ) | [ S ]
         */
        class ExpectedLanguage : Grammar() {
            var S by NT()

            init {
                setStart(S)
                S = Term("(") * S * Term(")") or (
                        Term("[") * S * Term("]"))
            }
        }

        val origin = DyckLanguage().getRsm()
        val s = origin.nonterminal
        testIncremental(origin, getDelta(s), ExpectedLanguage().getRsm(), 4)
    }

    @Test
    fun `test union {(ba)+} with {bra}`() {
        /**
         * Grammar for language S = (ba)+
         */
        class BaPlus : Grammar() {
            var S by NT()

            init {
                setStart(S)
                S = some(makeConcat("b", "a"))
            }
        }

        val origin = BaPlus().getRsm()
        val s = origin.nonterminal
        testIncremental(origin, getBra(s), BaPlusOrBra().getRsm(), 3)
    }

    @Test
    fun `test union {(ba)+, bar} with {bra}`() {
        val origin = BaPlusOrBra().getRsm()
        val s = origin.nonterminal
        testIncremental(origin, getBar(s), BaPlusOrBarOrBra().getRsm(), 5)
    }

    @Test
    fun `test removing {baba} from {(ba)+, bar, bra}`() {
        /**
         * Grammar for language {(ba)+, bar, bra} \ {baba}
         */
        class Expected : Grammar() {
            var S by NT()

            init {
                setStart(S)
                S = makeConcat("b", "a") or (
                        makeConcat("b", "a", "r") or (
                                makeConcat("b", "r", "a") or (
                                        makeConcat("b", "a", "b", "a") * some(makeConcat("b", "a"))
                                        )))
            }
        }

        fun getBaba(nonTerm: Nonterminal): RSMState {
            val st0 = RSMState(nonTerm, isStart = true)
            val st1 = RSMState(nonTerm)
            val st2 = RSMState(nonTerm)
            val st3 = RSMState(nonTerm)
            val st4 = RSMState(nonTerm, isFinal = true)
            st0.addEdge(Terminal("b"), st1)
            st1.addEdge(Terminal("a"), st2)
            st2.addEdge(Terminal("b"), st3)
            st3.addEdge(Terminal("a"), st4)
            return st0
        }
        fun printStates(states: Set<RSMState>, prefix: String = ""){
            println(prefix)
            for(st in states){
                println("${st.hashCode()}")
            }
        }
        val origin = BaPlusOrBarOrBra().getRsm()
        val s = origin.nonterminal
        printStates(origin.getAllStates(), "before")
        testIncremental(origin, getBaba(s), Expected().getRsm(), 6, true)
        printStates(origin.getAllStates(), "after")

    }

    /**
     * Single-string automaton accepting string "bra"
     */
    private fun getBra(nonTerm: Nonterminal): RSMState {
        val st0 = RSMState(nonTerm, isStart = true)
        val st1 = RSMState(nonTerm)
        val st2 = RSMState(nonTerm)
        val st3 = RSMState(nonTerm, isFinal = true)
        st0.addEdge(Terminal("b"), st1)
        st1.addEdge(Terminal("r"), st2)
        st2.addEdge(Terminal("a"), st3)
        return st0
    }

    /**
     * Single-string automaton accepting string "bra"
     */
    private fun getBar(nonTerm: Nonterminal): RSMState {
        val st0 = RSMState(nonTerm, isStart = true)
        val st1 = RSMState(nonTerm)
        val st2 = RSMState(nonTerm)
        val st3 = RSMState(nonTerm, isFinal = true)
        st0.addEdge(Terminal("b"), st1)
        st1.addEdge(Terminal("a"), st2)
        st2.addEdge(Terminal("r"), st3)
        return st0
    }

    /**
     * Grammar for language {(ba)+, bar, bra}
     */
    private class BaPlusOrBarOrBra : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = some(makeConcat("b", "a")) or (
                    makeConcat("b", "a", "r") or (
                            makeConcat("b", "r", "a")))
        }
    }

    /**
     *  Minimal automaton accepting the language {(ba)+, bra}
     */
    private class BaPlusOrBra : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = some(makeConcat("b", "a")) or
                    makeConcat("b", "r", "a")
        }
    }


}