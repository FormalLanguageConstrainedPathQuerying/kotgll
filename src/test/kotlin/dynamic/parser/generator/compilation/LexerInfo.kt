package dynamic.parser.generator.compilation

import com.tschuchort.compiletesting.SourceFile
import dynamic.parser.IDynamicGllTest.Companion.GRAMMARS_FOLDER
import dynamic.parser.generator.GllGeneratedTest
import java.io.File
import java.nio.file.Path

class LexerInfo private constructor(grammarName: String){
    private val name = GllGeneratedTest.LEXER_NAME
    lateinit var clazz: Class<*>
    private val pkg = "${GRAMMARS_FOLDER}.$grammarName"
    val path = RuntimeCompiler.parsersFolder.resolve(GRAMMARS_FOLDER).resolve(grammarName)
    private val file = path.resolve("$name.kt").toFile()
    private val fqn = "$pkg.$name"
    private lateinit var tokenClass: Class<*>
    var sources = ArrayList<SourceFile>()

    companion object {
        fun getLexerInfo(name: String): LexerInfo {
            val lexerInfo = LexerInfo(name)
            val tokensInfo = TokensInfo.getTokenInfo(lexerInfo.path.toFile(), name)
            lexerInfo.tokenClass = tokensInfo.clazz
            lexerInfo.sources.addAll(tokensInfo.sources)
            lexerInfo.sources.add(RuntimeCompiler.getSource(lexerInfo.file))
            val grammarClassLoader = RuntimeCompiler.compileClasses(lexerInfo.sources)
            lexerInfo.clazz = grammarClassLoader.loadClass(lexerInfo.fqn)
            return lexerInfo
        }
    }
}