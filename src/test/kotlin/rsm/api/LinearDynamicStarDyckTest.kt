package rsm.api

import org.junit.jupiter.api.Test
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.symbol.Nonterminal
import rsm.RsmTest
import kotlin.test.Ignore

/**
 * Compare incremental union of Grammar Rsm and linear Delta
 * Nonterminals in Delta must be the same as in Origin Rsm!
 */
class LinearDynamicDyckTest : RsmTest {

    @Test
    @Ignore("not implemented yet")
    fun `test DyckStar addition`() {
        val origin = DyckStar1().getRsm()
        val delta = getStarDyckDelta(origin.nonterminal, "[", "]")
        testIncremental(origin, delta, DyckStar2().getRsm(), 3)
    }

    /**
     * Rsm for <'openBrace' nonTerm 'closeBrace' nonTerm>
     */
    private fun getStarDyckDelta(nonTerm: Nonterminal, openBrace: String, closeBrace: String): RSMState {
        val nt = StandAloneNt(nonTerm)
        return nt.buildRsm(Many(Term(openBrace) * nt * Term(closeBrace)))
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
     * Grammar for language S = ( '(' S ')' | '[[' S ']]' )*
     */
    private class DyckStar2 : Grammar() {
        var S by NT()

        init {
            setStart(S)
            S = Many(
                Term("(") * S * Term(")") or (
                        Term("[") * S * Term("]"))
            )
        }
    }

}