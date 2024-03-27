package dynamic.parser.generator

import dynamic.parser.IDynamicGllTest
import dynamic.parser.IDynamicGllTest.Companion.getFiles
import dynamic.parser.IDynamicGllTest.Companion.getLines
import dynamic.parser.IDynamicGllTest.Companion.readFile
import dynamic.parser.generator.compilation.LexerInfo
import dynamic.parser.generator.compilation.RuntimeCompiler
import org.junit.jupiter.api.DynamicNode
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.ParsingException
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.parser.generator.ParserGeneratorException
import org.srcgll.rsm.symbol.ITerminal
import java.io.File
import java.io.Reader


open class GllGeneratedTest : IOfflineGllTest {
    companion object {
        const val DSL_FILE_NAME = "GrammarDsl"
        const val TOKENS_FILE_NAME = "Token"
        const val LEXER_NAME = "Lexer"
    }

    override val mainFileName: String
        get() = "$DSL_FILE_NAME.kt"

    private fun getGll(concreteGrammarFolder: File): GeneratedParser<Int, LinearInputLabel> {
        val grammarName = concreteGrammarFolder.name
        return RuntimeCompiler.generateParser(concreteGrammarFolder, grammarName)
    }

    private fun tokenizeInput(input: String, lexerInfo: LexerInfo): LinearInput<Int, LinearInputLabel> {
        val inputGraph = LinearInput<Int, LinearInputLabel>()

        //equals to val lexer = Lexer(input.reader())
        val lexer = lexerInfo.clazz.getConstructor(Reader::class.java).newInstance(input.reader())
        val yylex = lexerInfo.clazz.methods.firstOrNull { it.name == "yylex" }
            ?: throw ParserGeneratorException("cant find jflex class ${lexerInfo.path}")

        var token: ITerminal
        var vertexId = 0
        inputGraph.addStartVertex(vertexId)
        inputGraph.addVertex(vertexId)

        while (true) {
            try {
                val tkn = yylex.invoke(lexer)
                if (tkn !is ITerminal) {
                    throw ParsingException("Lexer must return ITerminal instance, but got ${tkn.javaClass}")
                }
                token = tkn
            } catch (e: java.lang.Error) {
                throw ParsingException("Lexer exception: $e")
            }
            if (token.toString() == "EOF") break
            inputGraph.addEdge(vertexId, LinearInputLabel(token), ++vertexId)
            inputGraph.addVertex(vertexId)
        }
        return inputGraph
    }

    override fun getTestCases(concreteGrammarFolder: File): Iterable<DynamicNode> {
        val lexerInfo = LexerInfo.getLexerInfo(concreteGrammarFolder.name)
        val gll: GeneratedParser<Int, LinearInputLabel> = getGll(concreteGrammarFolder)

        val correctOneLineInputs = getLines(IDynamicGllTest.ONE_LINE_INPUTS, concreteGrammarFolder)
            .map {
                getCorrectTestContainer("[ok]${IDynamicGllTest.getTestName(it)}", gll, tokenizeInput(it, lexerInfo))
            }

        val incorrectOneLineInputs = getLines(IDynamicGllTest.ONE_LINE_ERRORS_INPUTS, concreteGrammarFolder)
            .map {
                getIncorrectTestContainer("[fail]${IDynamicGllTest.getTestName(it)}", gll, tokenizeInput(it, lexerInfo))
            }

        val correctInputs =
            getFiles(IDynamicGllTest.INPUTS, concreteGrammarFolder)?.map { file ->
                getCorrectTestContainer("[ok]${file.name}", gll, tokenizeInput(readFile(file), lexerInfo))
            } ?: emptyList()

        val incorrectInputs =
            getFiles(IDynamicGllTest.INPUTS, concreteGrammarFolder)?.map { file ->
                getIncorrectTestContainer("[fail]${file.name}", gll, tokenizeInput(readFile(file), lexerInfo))
            } ?: emptyList()

        return correctOneLineInputs + incorrectOneLineInputs + correctInputs + incorrectInputs
    }

}
