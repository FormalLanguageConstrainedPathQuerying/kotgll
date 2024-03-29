package dynamic.parser.generator

import dynamic.parser.IDynamicGllTest
import dynamic.parser.IDynamicGllTest.Companion.getLines
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInput.Companion.SPACE
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.sppf.buildStringFromSppf
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class ScanerlessGllGeneratedTest : IOfflineGllTest {
    companion object {
        const val SCANERLESS_DSL_FILE_NAME = "ScanerlessGrammarDsl"
    }

    override val mainFileName: String
        get() = "$SCANERLESS_DSL_FILE_NAME.kt"


    override fun getTestCases(concreteGrammarFolder: File): Iterable<DynamicNode> {
        val gll: GeneratedParser<Int, LinearInputLabel> = getGll(concreteGrammarFolder)

        val correctOneLineInputs = getLines(IDynamicGllTest.ONE_LINE_INPUTS, concreteGrammarFolder)
            .map { input -> getCorrectTestContainer(input, IDynamicGllTest.getTestName(input), gll) }

        val incorrectOneLineInputs = getLines(IDynamicGllTest.ONE_LINE_ERRORS_INPUTS, concreteGrammarFolder)
            .map { input ->
                getIncorrectTestContainer(IDynamicGllTest.getTestName(input), gll, LinearInput.buildFromString(input))
            }

        return correctOneLineInputs + incorrectOneLineInputs
    }

    private fun getGll(concreteGrammarFolder: File): GeneratedParser<Int, LinearInputLabel> {
        val parserClass = RuntimeCompiler.loadScanerlessParser(concreteGrammarFolder)
        return RuntimeCompiler.instantiateParser(parserClass)
    }

    /**
     * Test case for String input without escape symbols
     * Contains additional check for parsing result
     */
    private fun getCorrectTestContainer(
        input: String,
        caseName: String,
        gll: GeneratedParser<Int, LinearInputLabel>
    ): DynamicNode {
        return DynamicTest.dynamicTest(caseName) {
            gll.input = LinearInput.buildFromString(input)
            val result = gll.parse().first
            assertNotNull(result)
            assertEquals(input.replace(SPACE, ""), buildStringFromSppf(result))
        }
    }

}
