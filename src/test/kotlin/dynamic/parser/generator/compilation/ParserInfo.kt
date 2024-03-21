package dynamic.parser.generator.compilation

import com.tschuchort.compiletesting.SourceFile
import dynamic.parser.generator.GllGeneratedTest
import dynamic.parser.generator.ScanerlessGllGeneratedTest
import org.srcgll.parser.generator.IParserGenerator
import org.srcgll.parser.generator.ParserGenerator
import org.srcgll.parser.generator.ScanerlessParserGenerator

class ParserInfo private constructor(val grammarInfo: GrammarInfo, dslName: String) {
    lateinit var clazz: Class<*>
    private val pkg = "${RuntimeCompiler.PARSER_PKG}.${grammarInfo.name}"
    private val path = RuntimeCompiler.parserPath.resolve(grammarInfo.name)
    private val name = IParserGenerator.getParserClassName(dslName)
    private val file = path.resolve("$name.kt").toFile()
    private val fqn = "$pkg.$name"
    var sources = ArrayList<SourceFile>()

    companion object {
        fun getScanerlessParserInfo(grammarInfo: GrammarInfo): ParserInfo {
            val parserInfo = ParserInfo(grammarInfo, ScanerlessGllGeneratedTest.SCANERLESS_DSL_FILE_NAME)
            ScanerlessParserGenerator(grammarInfo.clazz).generate(RuntimeCompiler.parsersFolder, parserInfo.pkg)

            parserInfo.sources.addAll(grammarInfo.sources)
            parserInfo.sources.add(RuntimeCompiler.getSource(parserInfo.file))

            val classLoader = RuntimeCompiler.compileClasses(parserInfo.sources)
            parserInfo.clazz = classLoader.loadClass(parserInfo.fqn)
            return parserInfo
        }
        fun getParserInfo(grammarInfo: GrammarInfo): ParserInfo{
            val parserInfo = ParserInfo(grammarInfo, GllGeneratedTest.DSL_FILE_NAME)
            ParserGenerator(grammarInfo.clazz, grammarInfo.tokenClass!!).generate(RuntimeCompiler.parsersFolder, parserInfo.pkg)

            parserInfo.sources.addAll(grammarInfo.sources)
            parserInfo.sources.add(RuntimeCompiler.getSource(parserInfo.file))

            val classLoader = RuntimeCompiler.compileClasses(parserInfo.sources)
            parserInfo.clazz = classLoader.loadClass(parserInfo.fqn)
            return parserInfo
        }
    }
}