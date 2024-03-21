package dynamic.parser

import dynamic.parser.IDynamicGllTest.Companion.ONE_LINE_ERRORS_INPUTS
import dynamic.parser.IDynamicGllTest.Companion.ONE_LINE_INPUTS
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.Gll
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.readRsmFromTxt
import org.srcgll.sppf.buildStringFromSppf
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class GllRsmTest : IDynamicGllTest {
    override val mainFileName: String
        get() = "grammar.rsm"


    private fun getGll(input: String, rsm: RsmState): Gll<Int, LinearInputLabel> {
        return Gll.gll(rsm, LinearInput.buildFromString(input))
    }

    private fun getRsm(concreteGrammarFolder: File): RsmState {
        val rsmFile = getFile(mainFileName, concreteGrammarFolder)
            ?: throw Exception("Folder $concreteGrammarFolder not contains $mainFileName")
        return readRsmFromTxt(rsmFile.toPath())
    }

    private fun getCorrectTestContainer(input: String, rsm: RsmState): DynamicNode {
        return DynamicTest.dynamicTest(getTestName(input)) {
            val result = getGll(input, rsm).parse().first
            assertNotNull(result)
            assertEquals(input, buildStringFromSppf(result))
        }
    }

    private fun getErrorTestContainer(input: String, rsm: RsmState): DynamicNode {
        return DynamicTest.dynamicTest(getTestName(input)) {
            val result = getGll(input, rsm).parse().first
            assertNull(result)
        }
    }


    override fun getTestCases(concreteGrammarFolder: File): Iterable<DynamicNode> {
        val inputs = getLines(ONE_LINE_INPUTS, concreteGrammarFolder)
        val errorInputs = getLines(ONE_LINE_ERRORS_INPUTS, concreteGrammarFolder)
        val rsm = getRsm(concreteGrammarFolder)
        return inputs.map { getCorrectTestContainer(it, rsm) } + (errorInputs.map { getErrorTestContainer(it, rsm) })
    }
}
