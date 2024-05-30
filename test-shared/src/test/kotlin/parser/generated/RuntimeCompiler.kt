package parser.generated

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.ucfs.GeneratorException
import org.ucfs.input.LinearInputLabel
import org.ucfs.parser.GeneratedParser
import org.ucfs.parser.ParserGenerator
import org.ucfs.parser.ScanerlessParserGenerator
import parser.generated.GllGeneratedTest.Companion.DSL_FILE_NAME
import parser.generated.GllGeneratedTest.Companion.LEXER_NAME
import parser.generated.GllGeneratedTest.Companion.TOKENS
import parser.generated.ScanerlessGllGeneratedTest.Companion.SCANERLESS_DSL_FILE_NAME
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object RuntimeCompiler {

    private fun resourceOf(name: String): Path {
        return Path.of(
            this.javaClass.getResource(name)?.path
                ?: throw GeneratorException("Can't find $name file in test resources")
        )
    }

    private fun getParserPkg(grammarName: String): String = "gen.parser.$grammarName"
    private val parsersFolder = resourceOf("/")
    private val generationPath: Path = Files
        .createDirectories(Paths.get("${parsersFolder.toAbsolutePath()}/gen/parser"))

    /**
     * Compile ScanerlessGrammarDsl and generate ScanerlessParser,
     * compile it and return loaded class
     */
    @Suppress("UNCHECKED_CAST")
    fun loadScanerlessParser(grammarFolderFile: File): Class<*> {
        val grammarName = grammarFolderFile.name
        //val parserName = ScanerlessParserGenerator().getParserClassName(SCANERLESS_DSL_FILE_NAME)

        fun generateParserCode(): Pair<KotlinCompilation.Result, String> {
            val grammar = getKtSource(grammarFolderFile, SCANERLESS_DSL_FILE_NAME)
            val compilationResult = compileClasses(listOf(grammar))
            val classLoader = compilationResult.classLoader

            val grammarClass = classLoader.loadFromGrammar(SCANERLESS_DSL_FILE_NAME, grammarName)

            val generator = ScanerlessParserGenerator(grammarClass)
            generator.generate(parsersFolder, getParserPkg(grammarName))
            return Pair(compilationResult, generator.getParserClassName())
        }

        var (compilationResult, parserName) = generateParserCode()
        val parser = getKtSource(generationPath.resolve(grammarName).toFile(), parserName)

        compilationResult = compileClasses(
            listOf(parser),
            listOf(compilationResult.outputDirectory)
        )
        return compilationResult.classLoader.loadClass(
            "${getParserPkg(grammarName)}.$parserName"
        )
    }

    data class ParsingClasses(
        val parser: Class<*>, val tokens: Class<*>,
        val lexer: Class<*>
    )

    private fun ClassLoader.loadFromGrammar(fileName: String, grammarName: String): Class<*> {
        return loadClass("grammars.$grammarName.$fileName")
    }


    @Suppress("UNCHECKED_CAST")
    fun loadParser(grammarFolderFile: File): ParsingClasses {
        val grammarName = grammarFolderFile.name

        fun generateParserCode(): Pair<KotlinCompilation.Result, String> {
            val token = getKtSource(grammarFolderFile, TOKENS)
            val grammar = getKtSource(grammarFolderFile, DSL_FILE_NAME)
            val compilationResult = compileClasses(listOf(token, grammar))
            val classLoader = compilationResult.classLoader

            val grammarClass = classLoader.loadFromGrammar(DSL_FILE_NAME, grammarName)
            val tokenClass = classLoader.loadFromGrammar(TOKENS, grammarName)

            val generator = ParserGenerator(grammarClass, tokenClass)
            generator.generate(parsersFolder, getParserPkg(grammarName))
            return Pair(compilationResult, generator.getParserClassName())
        }

        var (compilationResult, parserName) = generateParserCode()
        val lexer = getKtSource(grammarFolderFile, LEXER_NAME)
        val parser = getKtSource(generationPath.resolve(grammarName).toFile(), parserName)

        compilationResult = compileClasses(
            listOf(parser, lexer),
            listOf(compilationResult.outputDirectory)
        )
        val loader = compilationResult.classLoader
        return ParsingClasses(
            loader.loadClass("${getParserPkg(grammarName)}.$parserName"),
            loader.loadFromGrammar(TOKENS, grammarName),
            loader.loadFromGrammar(LEXER_NAME, grammarName)
        )
    }

    fun instantiateParser(parserClass: Class<*>): GeneratedParser<Int, LinearInputLabel> {
        val parser = parserClass.getConstructor().newInstance()
        if (parser !is (GeneratedParser<*, *>)) {
            throw Exception("Loader exception: the generated parser is not inherited from the ${GeneratedParser::class} ")
        }
        return parser as (GeneratedParser<Int, LinearInputLabel>)
    }


    /**
     * Get generation source from file
     */
    private fun getSource(sourcePath: File): SourceFile {
        assert(sourcePath.exists()) { "Source file ${sourcePath.path} doesn't exists" }
        return SourceFile.fromPath(sourcePath)
    }

    private fun getKtSource(grammarFolderFile: File, fileName: String): SourceFile {
        return getSource(grammarFolderFile.resolve("$fileName.kt"))
    }

    /**
     * Compile all files for given sources
     */
    private fun compileClasses(sourceFiles: List<SourceFile>, classpath: List<File> = emptyList()): KotlinCompilation.Result {
        val compileResult = KotlinCompilation().apply {
            sources = sourceFiles
            //use application classpath
            inheritClassPath = true
            verbose = false
            classpaths += classpath
        }.compile()
        if (compileResult.exitCode != KotlinCompilation.ExitCode.OK) {
            throw Exception(compileResult.messages)
        }
        return compileResult
    }

}