package dynamic.parser.generator

import dynamic.parser.IDynamicGllTest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.sppf.buildStringFromSppf
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


open class GllGeneratedTest : IDynamicGllTest {
    companion object {
        const val DSL_FILE_NAME = "GrammarDsl"
    }

    override val mainFileName: String
        get() = "$DSL_FILE_NAME.kt"

    private fun getCorrectTestContainer(input: String, gll: GeneratedParser<Int, LinearInputLabel>): DynamicNode {
        return DynamicTest.dynamicTest(getTestName(input)) {
            gll.input = LinearInput.buildFromString(input)
            val result = gll.parse().first
            assertNotNull(result)
            assertEquals(input.replace(" ", ""), buildStringFromSppf(result))
        }
    }

    private fun getErrorTestContainer(input: String, gll: GeneratedParser<Int, LinearInputLabel>): DynamicNode {
        return DynamicTest.dynamicTest(getTestName(input)) {
            gll.input = LinearInput.buildFromString(input)
            val result = gll.parse().first
            assertNull(result)
        }
    }


    override fun handleFolder(concreteGrammarFolder: File): DynamicContainer {
        val grammarName = concreteGrammarFolder.name
        val inputs = getLines(oneLineTestsFileName, concreteGrammarFolder)
        val errorInputs = getLines(oneLineErrorsTestsFileName, concreteGrammarFolder)
        val gll: GeneratedParser<Int, LinearInputLabel> =
            RuntimeCompiler.generateParser(concreteGrammarFolder, grammarName)
        return DynamicContainer.dynamicContainer(
            grammarName, inputs
                .map { getCorrectTestContainer(it, gll) } +
                    errorInputs.map { getErrorTestContainer(it, gll) })
    }
}
