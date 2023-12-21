package rsm.api

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.getAllStates
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import rsm.RsmTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compare incremental union of Grammar Rsm and linear Delta
 * Nonterminals in Delta must be the same as in Origin Rsm!
 */
class LinearDynamicSimpleTest : RsmTest {

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
    fun `test union {(ba)+, bra} with {bar}`() {
        val origin = BaPlusOrBra().getRsm()
        val s = origin.nonterminal
        testIncremental(origin, getBar(s), BaPlusOrBarOrBra().getRsm(), 6)
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

        val origin = BaPlusOrBarOrBra().getRsm()
        val s = origin.nonterminal
        testIncremental(origin, getBaba(s), Expected().getRsm(), 7, true)

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