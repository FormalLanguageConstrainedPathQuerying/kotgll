package dynamic.parser

import dynamic.parser.IDynamicGllTest.Companion.INCORRECT_INPUTS
import dynamic.parser.IDynamicGllTest.Companion.INPUTS
import dynamic.parser.IDynamicGllTest.Companion.ONE_LINE_ERRORS_INPUTS
import dynamic.parser.IDynamicGllTest.Companion.ONE_LINE_INPUTS
import org.junit.jupiter.api.*
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.sppf.buildStringFromSppf
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


interface IOfflineGllTest: IDynamicGllTest {

    fun getGll(concreteGrammarFolder: File): GeneratedParser<Int, LinearInputLabel>
    override fun getTestCases(concreteGrammarFolder: File): Iterable<DynamicNode>{
        val gll: GeneratedParser<Int, LinearInputLabel> = getGll(concreteGrammarFolder)

        val correctOneLineInputs = getLines(ONE_LINE_INPUTS, concreteGrammarFolder)
            .map { getCorrectTestContainer(it, gll, getTestName(it)) }

        val incorrectOneLineInputs = getLines(ONE_LINE_ERRORS_INPUTS, concreteGrammarFolder)
            .map { getIncorrectTestContainer(it, gll, getTestName(it)) }

        val correctInputs =
            getFiles(INPUTS, concreteGrammarFolder)?.
            map{ file -> getCorrectTestContainer(readFile(file), gll, file.name)} ?: emptyList()

        val incorrectInputs =
            getFiles(INCORRECT_INPUTS, concreteGrammarFolder)?.
            map{ file -> getIncorrectTestContainer(readFile(file), gll, file.name)} ?: emptyList()

        return correctOneLineInputs + incorrectOneLineInputs + correctInputs + incorrectInputs
    }

    fun getIncorrectTestContainer(input: String, gll: GeneratedParser<Int, LinearInputLabel>, caseName: String): DynamicNode {
        return DynamicTest.dynamicTest(caseName) {
            gll.input = LinearInput.buildFromString(input)
            val result = gll.parse().first
            Assertions.assertNull(result)
        }
    }
    fun getCorrectTestContainer(input: String, gll: GeneratedParser<Int, LinearInputLabel>, caseName: String): DynamicNode {
        return DynamicTest.dynamicTest(caseName) {
            gll.input = LinearInput.buildFromString(input)
            val result = gll.parse().first
            assertNotNull(result)
            assertEquals(input, buildStringFromSppf(result))
        }
    }

}
