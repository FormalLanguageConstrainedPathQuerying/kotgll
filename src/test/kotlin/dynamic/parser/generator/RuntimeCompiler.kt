package dynamic.parser.generator

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import dynamic.parser.IDynamicGllTest
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.parser.generator.ParserGenerator
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path

object RuntimeCompiler {

    private const val PARSER_PKG = "gen.parser"
    private val parsersFolder = Path.of("src", "test", "resources")
    private val parserPath = Path.of("src", "test", "resources", "gen", "parser")

    data class GrammarInfo(val folderFile: File, val name: String) {
        val source: SourceFile
        val clazz: Class<*>
        private val file = folderFile.toPath().resolve("${GllGeneratedTest.DSL_FILE_NAME}.kt").toFile()
        private val fqn: String = "${IDynamicGllTest.GRAMMAR_FOLDER}.$name.${GllGeneratedTest.DSL_FILE_NAME}"

        init {
            assert(file.exists()) { "Grammar file $file does not exist" }
            source = getSource(file)
            val grammarClassLoader = compileClasses(listOf(source))
            clazz = grammarClassLoader.loadClass(fqn)
        }
    }

    data class ParserInfo(val grammarInfo: GrammarInfo) {
        val clazz: Class<*>
        private val pkg = "$PARSER_PKG.${grammarInfo.name}"
        private val path = parserPath.resolve(grammarInfo.name)
        private val name = ParserGenerator.getParserClassName(GllGeneratedTest.DSL_FILE_NAME)
        private val file = path.resolve("$name.kt").toFile()
        private val fqn = "$pkg.$name"

        init {
            ParserGenerator(grammarInfo.clazz).generate(parsersFolder, pkg)
            assert(file.exists()) {
                "Parser file $file does not exist"
            }
            val source = getSource(file)
            val classLoader = compileClasses(listOf(grammarInfo.source, source))
            clazz = classLoader.loadClass(fqn)
        }
    }


    @Suppress("UNCHECKED_CAST")
    fun generateParser(grammarFolderFile: File, grammarName: String): GeneratedParser<Int, LinearInputLabel> {
        val grammarInfo = GrammarInfo(grammarFolderFile, grammarName)
        val parserInfo = ParserInfo(grammarInfo)
        val parser = parserInfo.clazz.getDeclaredConstructor().newInstance()
        if (parser !is (GeneratedParser<*, *>)) {
            throw Exception("Loader exception: the generated parser is not inherited from the ${GeneratedParser::class} ")
        }
        return parser as (GeneratedParser<Int, LinearInputLabel>)
    }

    private fun getSource(sourcePath: File): SourceFile {
        assert(sourcePath.exists())
        val sourceFile = SourceFile.fromPath(sourcePath)
        return sourceFile
    }

    private fun compileClasses(sourceFiles: List<SourceFile>): URLClassLoader {
        val compileResult = KotlinCompilation().apply {
            sources = sourceFiles
            inheritClassPath = true
            verbose = false
        }.compile()
        if (compileResult.exitCode != KotlinCompilation.ExitCode.OK) {
            throw Exception(compileResult.messages)
        }
        return compileResult.classLoader
    }
}