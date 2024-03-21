package dynamic.parser.generator.compilation

import com.tschuchort.compiletesting.SourceFile
import dynamic.parser.IDynamicGllTest
import dynamic.parser.generator.GllGeneratedTest
import dynamic.parser.generator.ScanerlessGllGeneratedTest
import java.io.File

class GrammarInfo private constructor(folderFile: File, val name: String, dslName: String) {

    var sources = ArrayList<SourceFile>()
    lateinit var clazz: Class<*>
    val file: File = folderFile.toPath().resolve("$dslName.kt").toFile()
    val fqn: String = "${IDynamicGllTest.GRAMMARS_FOLDER}.$name.$dslName"
    var tokenClass: Class<*>? = null

    init {
        assert(file.exists()) { "Grammar file $file does not exist" }
    }

    companion object {
        fun getScanerlessGrammarInfo(folderFile: File, name: String): GrammarInfo {
            val grammarInfo = GrammarInfo(folderFile, name, ScanerlessGllGeneratedTest.SCANERLESS_DSL_FILE_NAME)
            grammarInfo.sources.add(RuntimeCompiler.getSource(grammarInfo.file))
            val grammarClassLoader = RuntimeCompiler.compileClasses(grammarInfo.sources)
            grammarInfo.clazz = grammarClassLoader.loadClass(grammarInfo.fqn)
            return grammarInfo
        }

        fun getGrammarInfo(folderFile: File, name: String): GrammarInfo {
            val tokensInfo = TokensInfo.getTokenInfo(folderFile, name)
            val grammarInfo = GrammarInfo(folderFile, name, GllGeneratedTest.DSL_FILE_NAME)
            grammarInfo.tokenClass = tokensInfo.clazz
            grammarInfo.sources.addAll(tokensInfo.sources)
            grammarInfo.sources.add(RuntimeCompiler.getSource(grammarInfo.file))
            val grammarClassLoader = RuntimeCompiler.compileClasses(grammarInfo.sources)
            grammarInfo.clazz = grammarClassLoader.loadClass(grammarInfo.fqn)
            return grammarInfo
        }
    }
}