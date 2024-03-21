package dynamic.parser.generator.compilation

import com.tschuchort.compiletesting.SourceFile
import dynamic.parser.IDynamicGllTest
import dynamic.parser.generator.GllGeneratedTest
import java.io.File

class TokensInfo private constructor(val folderFile: File, val name: String) {
    var sources = ArrayList<SourceFile>()
    lateinit var clazz: Class<*>
    val file: File = folderFile.toPath().resolve("${GllGeneratedTest.TOKENS_FILE_NAME}.kt").toFile()
    val fqn: String = "${IDynamicGllTest.GRAMMARS_FOLDER}.$name.${GllGeneratedTest.TOKENS_FILE_NAME}"

    init {
        assert(file.exists()) { "Grammar file $file does not exist" }
        sources.add(RuntimeCompiler.getSource(file))
    }

    companion object {
        fun getTokenInfo(folderFile: File, name: String): TokensInfo {
            val tokensInfo = TokensInfo(folderFile, name)
            val grammarClassLoader = RuntimeCompiler.compileClasses(tokensInfo.sources)
            tokensInfo.clazz = grammarClassLoader.loadClass(tokensInfo.fqn)
            return tokensInfo
        }
    }
}