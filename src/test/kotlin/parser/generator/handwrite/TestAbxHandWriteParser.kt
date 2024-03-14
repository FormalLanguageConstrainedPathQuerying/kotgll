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

    private fun testSuccess(input: String, parser: GeneratedParser<Int, LinearInputLabel>) {
        parser.input = LinearInput.buildFromString(input)
        val res = parser.parse().first
        assertNotNull(res)
        assertEquals(input, buildStringFromSppf(res))
    }

    private fun testFailure(input: String, parser: GeneratedParser<Int, LinearInputLabel>) {
        parser.input = LinearInput.buildFromString(input)
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