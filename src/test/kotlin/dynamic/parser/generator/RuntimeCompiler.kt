package dynamic.parser.generator

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.parser.generator.ParserGenerator
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths

object RuntimeCompiler {

    private const val PARSER_PKG = "gen.parser"
    private val parsersFolder = Path.of("src", "test", "resources")
    private val parserPath = Path.of("src", "test", "resources", "gen", "parser")

    data class GrammarInfo(val clazz: Class<*>, val source: SourceFile)

    private fun compileGrammarFile(grammarFolderFile: File, grammarName: String): GrammarInfo {
        val grammarFile = grammarFolderFile.toPath().resolve("$grammarName.kt").toFile()
        assert(grammarFile.exists()) { "Grammar file $grammarFile does not exist" }
        val sourceFile = getSource(grammarFile)
        val grammarClassLoader = compileClasses(listOf(sourceFile))
        val fqn = sequenceOf("grammars", grammarName, grammarName).joinToString(separator = ".")
        return GrammarInfo(grammarClassLoader.loadClass(fqn), sourceFile)
    }

    fun generateParser(grammarFolderFile: File, grammarName: String): GeneratedParser<Int, LinearInputLabel> {
        val grammar: GrammarInfo = compileGrammarFile(grammarFolderFile, grammarName)
        ParserGenerator(grammar.clazz).generate(parsersFolder, PARSER_PKG)
        val parserName = ParserGenerator.getParserClassName(grammarName)
        val parserFile = parserPath.resolve("$parserName.kt").toFile()
        assert(parserFile.exists()) {
            "Parser file $parserFile does not exist"
        }
        val parserSource = getSource(parserFile)
        val parserClassLoader = compileClasses(listOf(grammar.source, parserSource))
        val parserClass: Class<*> = parserClassLoader.loadClass("${PARSER_PKG}.$parserName")
        val parser = parserClass.getDeclaredConstructor().newInstance()
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