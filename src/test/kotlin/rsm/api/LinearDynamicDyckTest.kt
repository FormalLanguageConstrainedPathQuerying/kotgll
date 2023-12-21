package rsm.api

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import rsm.RsmTest

/**
 * Compare incremental union of Grammar Rsm and linear Delta
 * Nonterminals in Delta must be the same as in Origin Rsm!
 */
class LinearDynamicDyckTest : RsmTest {

    @Test
    fun `test Dyck addition`() {
        val origin = DyckLanguage().getRsm()
        val delta = getDyckDelta(origin.nonterminal, "[", "]")
        testIncremental(origin, delta, Dyck2().getRsm(), 4)
    }

    @Test
    fun `test removing brace from Dyck language`() {
        val origin = Dyck2().getRsm()
        val delta = getDyckDelta(origin.nonterminal, "[", "]")
        testIncremental(origin, delta, DyckLanguage().getRsm(), 4, true)
    }

    @Test
    fun `test ExtDyck2 removing`() {
        val origin = ExtDyck2().getRsm()
        val delta = getExtDyckDelta(origin.nonterminal, "[", "]")
        testIncremental(origin, delta, ExtDyck1().getRsm(), 5, true)
    }

    @Test
    fun `test ExtDyck2 addition`() {
        val origin = ExtDyck2().getRsm()
        val delta = getExtDyckDelta(origin.nonterminal, "{", "}")
        testIncremental(origin, delta, ExtDyck3().getRsm(), 7)
    }

    @Test
    fun `test DyckStar addition`() {
        val origin = DyckStar1().getRsm()
        val delta = getStarDyckDelta(origin.nonterminal, "[", "]")
        testIncremental(origin, delta, DyckStar2().getRsm(), 3)
    }


    /**
     * Rsm for <'openBrace' nonTerm 'closeBrace'>
     */
    private fun getDyckDelta(nonTerm: Nonterminal, openBrace: String, closeBrace: String): RSMState {
        val nt = StandAloneNt(nonTerm)
        return nt.buildRsm(Term(openBrace) * nt * Term(closeBrace))
    }

    /**
     * Rsm for <'openBrace' nonTerm 'closeBrace' nonTerm>
     */
    private fun getExtDyckDelta(nonTerm: Nonterminal, openBrace: String, closeBrace: String): RSMState {
        val nt = StandAloneNt(nonTerm)
        return nt.buildRsm(Term(openBrace) * nt * Term(closeBrace) * nt)
    }

    /**
     * Rsm for <'openBrace' nonTerm 'closeBrace' nonTerm>
     */
    private fun getStarDyckDelta(nonTerm: Nonterminal, openBrace: String, closeBrace: String): RSMState {
        val nt = StandAloneNt(nonTerm)
        return nt.buildRsm(Many(Term(openBrace) * nt * Term(closeBrace)))
    }

    /**
     * Grammar for language S = '(' S ')' | '[[' S ']]'
     */
    private class Dyck2 : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = Term("[") * S * Term("]") or Term("(") * S * Term(")")
        }
    }

    /**
     * Grammar for language S = eps | '(' S ')' S
     */
    private class ExtDyck1 : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = Epsilon or Term("(") * S * Term(")") * S
        }
    }

    /**
     * Grammar for language S = eps | '(' S ')' S | '[[' S ']]' S
     */
    private class ExtDyck2 : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = Epsilon or Term("[") * S * Term("]") * S or Term("(") * S * Term(")") * S
        }
    }

    /**
     * Grammar for language S = eps | '(' S ')' S | '[[' S ']]' S
     */
    private class ExtDyck3 : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = Epsilon or Term("[") * S * Term("]") * S or (
                    Term("(") * S * Term(")") * S) or (
                    Term("{") * S * Term("}") * S
                    )
        }
    }


    /**
     * Grammar for language S = ( '(' S ')' )*
     */
    private class DyckStar1 : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = Many(Term("(") * S * Term(")"))
        }
    }

    /**
     * Grammar for language S = ( '(' S ')' )*
     */
    private class DyckStar2 : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = Many(Term("(") * S * Term(")") or (
                    Term("[") * S * Term("]"))
            )
        }
    }

    /**
     * Get RSM for S = Many('{' S '}') with  nonTerminal S
     */
    private fun getDyckStar1Delta(nonTerm: Nonterminal): RSMState {
        val deltaStart = RSMState(nonTerm, isStart = true, isFinal = true)
        val st1 = RSMState(nonTerm)
        val st2 = RSMState(nonTerm)
        deltaStart.addEdge(Terminal("{"), st1)
        st1.addEdge(nonTerm, st2)
        st2.addEdge(Terminal("}"), deltaStart)
        return deltaStart
    }

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
}