package dynamic.parser.generator

import dynamic.parser.IOfflineGllTest
import dynamic.parser.generator.compilation.RuntimeCompiler
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.generator.GeneratedParser
import java.io.File


class ScanerlessGllGeneratedTest : IOfflineGllTest {
    companion object {
        const val SCANERLESS_DSL_FILE_NAME = "ScanerlessGrammarDsl"
    }

    override val mainFileName: String
        get() = "$SCANERLESS_DSL_FILE_NAME.kt"

    override fun getGll(concreteGrammarFolder: File): GeneratedParser<Int, LinearInputLabel> {
        val grammarName = concreteGrammarFolder.name
        return RuntimeCompiler.generateScanerlessParser(concreteGrammarFolder, grammarName)
    }
}
