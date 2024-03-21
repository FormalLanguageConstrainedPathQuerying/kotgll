package dynamic.parser.generator.compilation

import com.tschuchort.compiletesting.SourceFile
import dynamic.parser.IDynamicGllTest.Companion.GRAMMARS_FOLDER
import dynamic.parser.generator.GllGeneratedTest
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path

class LexerInfo private constructor(grammarName: String){
    private val name = GllGeneratedTest.LEXER_NAME
    lateinit var clazz: Class<*>
    private val pkg = "${GRAMMARS_FOLDER}.$grammarName"
    val path = RuntimeCompiler.parsersFolder.resolve(grammarName)
    private val file = path.resolve("$name.java").toFile()
    private val fqn = "$pkg.$name"
    private val outputPath = Path.of("test", "resources", "gen", "parser", name)

    init {
       // assert(file.exists()) { "Lexer file $file does not exist" }
    }

    companion object {
        fun getLexerInfo(name: String): LexerInfo {
            val lexerInfo = LexerInfo(name)
            RuntimeCompiler.compileJavaSource(listOf(lexerInfo.file), lexerInfo.outputPath.toString())
            lexerInfo.clazz = getJavaClass(lexerInfo.fqn, lexerInfo.outputPath.toFile())
            return lexerInfo
        }


        fun getJavaClass(className: String, path: File): Class<*> {
            // Assuming the .class files are in the 'classes' directory
            val classesDir = path.toURI().toURL()
            val classLoader = URLClassLoader(arrayOf(classesDir))
            return classLoader.loadClass(className)
        }
    }
}