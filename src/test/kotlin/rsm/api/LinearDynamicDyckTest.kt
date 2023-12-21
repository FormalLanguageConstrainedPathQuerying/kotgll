package rsm.api

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.symbol.Nonterminal
import rsm.RsmTest

/**
 * Compare incremental union of Grammar Rsm and linear Delta
 * Nonterminals in Delta must be the same as in Origin Rsm!
 */
class LinearDynamicStarDyckTest : RsmTest {

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
}
