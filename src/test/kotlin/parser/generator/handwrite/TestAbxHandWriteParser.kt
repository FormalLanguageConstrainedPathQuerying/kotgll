package parser.generator.handwrite

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.buildStringFromSppf
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TestHandWriteParsers {
    private fun buildInputGraph(input: String): LinearInput<Int, LinearInputLabel> {
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0
        var pos = 0

        inputGraph.addVertex(curVertexId)
        while (pos < input.length) {
            val label = input[pos].toString()
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(label)), ++curVertexId)
            inputGraph.addVertex(curVertexId)
            pos += 1
        }
        inputGraph.addStartVertex(0)
        return inputGraph
    }

    private fun testSuccess(input: String, parser: GeneratedParser<Int, LinearInputLabel>) {
        parser.input = buildInputGraph(input)
        val res = parser.parse().first
        assertNotNull(res)
        assertEquals(input, buildStringFromSppf(res))
    }

    private fun testFailure(input: String, parser: GeneratedParser<Int, LinearInputLabel>) {
        parser.input = buildInputGraph(input)
        val res = parser.parse().first
        assertNull(res)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "x", "ab", "abab", "ababab", "abababab", "ababababab"])
    fun `test-success 'Many(ab) or x' hand-crafted parser`(input: String) {
        val parser = ManyAbHandWriteParser<Int, LinearInputLabel>()
        testSuccess(input, parser)
    }

    @ParameterizedTest
    @ValueSource(strings = ["aaa", "bbb", "b"])
    fun `test-failure 'Many(ab) or x' hand-crafted parser `(input: String) {
        val parser = ManyAbHandWriteParser<Int, LinearInputLabel>()
        testFailure(input, parser)
    }

    @ParameterizedTest
    @ValueSource(strings = ["x", "ab", "abab", "ababab", "abababab", "ababababab"])
    fun `test-success 'Some(ab) or x' hand-crafted parser`(input: String) {
        val parser = SomeAbHandWriteParser<Int, LinearInputLabel>()
        testSuccess(input, parser)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "aaa", "bbb", "b"])
    fun `test-failure 'Some(ab) or x' hand-crafted parser `(input: String) {
        val parser = SomeAbHandWriteParser<Int, LinearInputLabel>()
        testFailure(input, parser)
    }
}