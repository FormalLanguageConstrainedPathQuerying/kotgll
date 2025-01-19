package solver

import org.junit.jupiter.api.*
import org.ucfs.IDynamicGllTest
import org.ucfs.IDynamicGllTest.Companion.GRAMMAR_FOLDER
import org.ucfs.IDynamicGllTest.Companion.ONE_LINE_ERRORS_INPUTS
import org.ucfs.IDynamicGllTest.Companion.ONE_LINE_INPUTS
import org.ucfs.IDynamicGllTest.Companion.getFile
import org.ucfs.IDynamicGllTest.Companion.getLines
import org.ucfs.IDynamicGllTest.Companion.getTestName
import org.ucfs.input.LinearInput
import org.ucfs.input.LinearInputLabel
import org.ucfs.parser.Gll
import org.ucfs.parser.IGll
import org.ucfs.rsm.RsmState
import org.ucfs.rsm.readRsmFromTxt
import org.ucfs.rsm.writeRsmToDot
import org.ucfs.sppf.writeSppfToDot
import parser.generated.ScanerlessGrammarDsl
import java.io.File
import kotlin.test.assertNotNull


class GllRsmTest : IDynamicGllTest {
    override val mainFileName: String
        get() = "grammar.rsm"


    @Test
//    @Disabled
    fun testSingle() {
//        val folderName = "ab"
//        val input = "a b"

        val folderName = "ambiguous"
        val input = "a a a"
        val folder = File(GRAMMAR_FOLDER).toPath().resolve(folderName).toFile()
        val rsm = ScanerlessGrammarDsl().rsm

        val gll = getGll(input, rsm)
        val result = gll.parse().first
        assertNotNull(result)
        writeSppfToDot(result, "boob.dot")
        writeRsmToDot(rsm, "rsm.dot")
    }

    private fun getGll(input: String, rsm: RsmState): Gll<Int, LinearInputLabel> {
        return Gll.gll(rsm, LinearInput.buildFromString(input))
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
