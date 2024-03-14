package dynamic.parser.generator

import dynamic.parser.IDynamicGllTest
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.parser.generator.ParserGenerator
import org.srcgll.sppf.buildStringFromSppf
import java.io.File
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


open class GllGeneratedTest : IDynamicGllTest {


    companion object {
        private const val OUTPUT_DIRECTORY = "gen/tests"
    }

    private fun getGll(className: String): GeneratedParser<Int, LinearInputLabel> {
        TODO()
//        val generatedParserClass = Class.forName(className)
//        val ctor = generatedParserClass
//            //.parameterizedBy(Int.Companion::class.java, LinearInputLabel::class.java)
//            .javaClass.getConstructor()
//        val obj = ctor.newInstance()
//        if (obj.isAssignableFrom(GeneratedParser::class.java)) {
//            throw Exception("Reflection load wrong class ${obj.name} instead of $className!!!")
//        }
//        val parser = obj as GeneratedParser<Int, LinearInputLabel>
//        return parser
    }


    private fun getCorrectTestContainer(input: String, gll: GeneratedParser<Int, LinearInputLabel>): DynamicNode {
        return DynamicTest.dynamicTest(getTestName(input)) {
            gll.input = LinearInput.buildFromString(input)
            val result = gll.parse().first
            assertNotNull(result)
            assertEquals(input, buildStringFromSppf(result))
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
        val grammarName = toCamelCase(concreteGrammarFolder.name)
        val inputs = getFile(oneLineTestsFileName, concreteGrammarFolder).readLines()
        val errorInputs = getFile(oneLineErrorsTestsFileName, concreteGrammarFolder).readLines()
        val gll: GeneratedParser<Int, LinearInputLabel> =
            getGll(ParserGenerator.getParserClassName(grammarName))
        return DynamicContainer.dynamicContainer(
            grammarName, inputs
                .map { getCorrectTestContainer(it, gll) } +
                    errorInputs.map { getErrorTestContainer(it, gll) })
    }


    private fun getAbsolutePath(className: String): String {
        val split = className.split("\\.".toRegex()).filter { it.isNotEmpty() }.toMutableList()
        split[split.size - 1] += ".java"

        return Paths.get(OUTPUT_DIRECTORY, split.toString()).toAbsolutePath()
            .toString()
    }

}
