package dynamic.parser.generator.compilation

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import dynamic.parser.generator.compilation.GrammarInfo.Companion.getGrammarInfo
import dynamic.parser.generator.compilation.GrammarInfo.Companion.getScanerlessGrammarInfo
import dynamic.parser.generator.compilation.ParserInfo.Companion.getParserInfo
import dynamic.parser.generator.compilation.ParserInfo.Companion.getScanerlessParserInfo
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.generator.GeneratedParser
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path

object RuntimeCompiler {

    const val PARSER_PKG = "gen.parser"
    val parsersFolder = Path.of("src", "test", "resources")
    val parserPath = Path.of("src", "test", "resources", "gen", "parser")


    @Suppress("UNCHECKED_CAST")
    fun generateScanerlessParser(grammarFolderFile: File, grammarName: String): GeneratedParser<Int, LinearInputLabel> {
        val grammarInfo = getScanerlessGrammarInfo(grammarFolderFile, grammarName)
        val parserInfo = getScanerlessParserInfo(grammarInfo)
        val parser = parserInfo.clazz.getDeclaredConstructor().newInstance()
        if (parser !is (GeneratedParser<*, *>)) {
            throw Exception("Loader exception: the generated parser is not inherited from the ${GeneratedParser::class} ")
        }
        return parser as (GeneratedParser<Int, LinearInputLabel>)
    }

    @Suppress("UNCHECKED_CAST")
    fun generateParser(grammarFolderFile: File, grammarName: String): GeneratedParser<Int, LinearInputLabel> {
        val grammarInfo = getGrammarInfo(grammarFolderFile, grammarName)
        val parserInfo = getParserInfo(grammarInfo)
        val parser = parserInfo.clazz.getDeclaredConstructor().newInstance()
        if (parser !is (GeneratedParser<*, *>)) {
            throw Exception("Loader exception: the generated parser is not inherited from the ${GeneratedParser::class} ")
        }
        return parser as (GeneratedParser<Int, LinearInputLabel>)
    }

    /**
     * Get generation source from file
     */
    fun getSource(sourcePath: File): SourceFile {
        assert(sourcePath.exists()) { "Source file ${sourcePath.path} doesn't exists" }
        val sourceFile = SourceFile.fromPath(sourcePath)
        return sourceFile
    }

    /**
     * Compile all files for given sources
     */
    fun compileClasses(sourceFiles: List<SourceFile>): URLClassLoader {
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