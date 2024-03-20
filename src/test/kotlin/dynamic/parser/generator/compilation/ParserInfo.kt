package dynamic.parser.generator.compilation

import com.tschuchort.compiletesting.SourceFile
import dynamic.parser.generator.GllGeneratedTest
import org.srcgll.parser.generator.IParserGenerator
import org.srcgll.parser.generator.ScanerlessParserGenerator

class ParserInfo private constructor(val grammarInfo: GrammarInfo) {
    lateinit var clazz: Class<*>
    private val pkg = "${RuntimeCompiler.PARSER_PKG}.${grammarInfo.name}"
    private val path = RuntimeCompiler.parserPath.resolve(grammarInfo.name)
    private val name = IParserGenerator.getParserClassName(GllGeneratedTest.DSL_FILE_NAME)
    private val file = path.resolve("$name.kt").toFile()
    private val fqn = "$pkg.$name"
    private var source: SourceFile
    init{
        ScanerlessParserGenerator(grammarInfo.clazz).generate(RuntimeCompiler.parsersFolder, pkg)
        source = RuntimeCompiler.getSource(file)
    }

    companion object {
        fun getScanerlessParserInfo(grammarInfo: GrammarInfo): ParserInfo {
            val parserInfo = ParserInfo(grammarInfo)

            assert(parserInfo.file.exists()) {
                "Parser file ${parserInfo.file} does not exist"
            }
            val classLoader = RuntimeCompiler.compileClasses(listOf(grammarInfo.source, parserInfo.source))
            parserInfo.clazz = classLoader.loadClass(parserInfo.fqn)
            return parserInfo
        }
    }
}