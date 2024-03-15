package dynamic.parser

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicContainer
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


open class GllRsmTest : IDynamicGllTest {
    override val mainFileName: String
        get() = "grammar.rsm"

    private fun getGll(input: String, rsm: RsmState): Gll<Int, LinearInputLabel> {
        return Gll.gll(rsm, LinearInput.buildFromString(input))
    }

    private fun getRsm(concreteGrammarFolder: File): RsmState {
        val rsmFile = getFile(mainFileName, concreteGrammarFolder) ?:
        throw Exception("Folder $concreteGrammarFolder not contains $mainFileName")
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


    override fun handleFolder(concreteGrammarFolder: File): DynamicContainer {
        val inputs = getLines(oneLineTestsFileName, concreteGrammarFolder)
        val errorInputs = getLines(oneLineErrorsTestsFileName, concreteGrammarFolder)
        val rsm = getRsm(concreteGrammarFolder)
        return DynamicContainer.dynamicContainer(
            concreteGrammarFolder.name,
            inputs.map { getCorrectTestContainer(it, rsm) }
                    + (errorInputs.map { getErrorTestContainer(it, rsm) })
        )
    }
}
