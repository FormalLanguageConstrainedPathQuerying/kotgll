package dynamic.parser

import dynamic.parser.IDynamicGllTest.Companion.ONE_LINE_ERRORS_INPUTS
import dynamic.parser.IDynamicGllTest.Companion.ONE_LINE_INPUTS
import dynamic.parser.IDynamicGllTest.Companion.getFile
import dynamic.parser.IDynamicGllTest.Companion.getLines
import dynamic.parser.IDynamicGllTest.Companion.getTestName
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.Gll
import org.srcgll.parser.IGll
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.readRsmFromTxt
import java.io.File
import kotlin.test.assertNotNull


class GllRsmTest : IDynamicGllTest {
    override val mainFileName: String
        get() = "grammar.rsm"


    private fun getGll(input: String, rsm: RsmState): Gll<Int, LinearInputLabel> {
        return Gll.gll(rsm, LinearInput.buildFromSeparatedString(input))
    }

    private fun getRsm(concreteGrammarFolder: File): RsmState {
        val rsmFile = getFile(mainFileName, concreteGrammarFolder)
            ?: throw Exception("Folder $concreteGrammarFolder not contains $mainFileName")
        return readRsmFromTxt(rsmFile.toPath())
    }

    override fun getTestCases(concreteGrammarFolder: File): Iterable<DynamicNode> {
        val rsm = getRsm(concreteGrammarFolder)

        val inputs = getLines(ONE_LINE_INPUTS, concreteGrammarFolder)
            .map { input -> getCorrectTestContainer(getTestName(input), getGll(input, rsm)) }
        val errorInputs = getLines(ONE_LINE_ERRORS_INPUTS, concreteGrammarFolder)
            .map { input -> getIncorrectTestContainer(getTestName(input), getGll(input, rsm)) }

        return inputs + errorInputs
    }
    /**
     * Test for any type of incorrect input
     * Gll should be parametrized by it's input!
     */
    private fun getIncorrectTestContainer(caseName: String, gll: IGll<Int, LinearInputLabel>): DynamicNode {
        return DynamicTest.dynamicTest(caseName) {
            val result = gll.parse().first
            Assertions.assertNull(result)
        }
    }

    /**
     * Test for any type of correct input
     * Gll should be parametrized by it's input!
     */
    private fun getCorrectTestContainer(caseName: String, gll: IGll<Int, LinearInputLabel>): DynamicNode {
        return DynamicTest.dynamicTest(caseName) {
            val result = gll.parse().first
            //TODO add check for parsing result quality
            assertNotNull(result)
        }
    }
}
