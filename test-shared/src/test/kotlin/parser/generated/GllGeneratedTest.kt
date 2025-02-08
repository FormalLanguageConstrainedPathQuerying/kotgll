package parser.generated

import org.junit.jupiter.api.DynamicNode
import org.ucfs.GeneratorException
import org.ucfs.IDynamicGllTest
import org.ucfs.IDynamicGllTest.Companion.getFiles
import org.ucfs.IDynamicGllTest.Companion.getLines
import org.ucfs.IDynamicGllTest.Companion.readFile
import org.ucfs.input.LinearInput
import org.ucfs.input.TerminalInputLabel
import org.ucfs.parser.ParsingException
import org.ucfs.rsm.symbol.ITerminal
import java.io.File
import java.io.Reader


open class GllGeneratedTest : IOfflineGllTest {
    companion object {
        const val DSL_FILE_NAME = "GrammarDsl"
        const val TOKENS = "Token"
        const val LEXER_NAME = "Lexer"
    }

    override val mainFileName: String
        get() = "$DSL_FILE_NAME.kt"

    private fun tokenizeInput(input: String, lexerClass: Class<*>): LinearInput<Int, TerminalInputLabel> {
        val inputGraph = LinearInput<Int, TerminalInputLabel>()

        //equals to `val lexer = Lexer(input.reader())`
        val lexer = lexerClass.getConstructor(Reader::class.java).newInstance(input.reader())
        val yylex = lexerClass.methods.firstOrNull { it.name == "yylex" }
            ?: throw GeneratorException("cant find jflex class $lexerClass")

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
            inputGraph.addEdge(vertexId, TerminalInputLabel(token), ++vertexId)
            inputGraph.addVertex(vertexId)
        }
        return inputGraph
    }

    override fun getTestCases(concreteGrammarFolder: File): Iterable<DynamicNode> {
        val classes = RuntimeCompiler.loadParser(concreteGrammarFolder)
        val gll = RuntimeCompiler.instantiateParser(classes.parser)

        val correctOneLineInputs = getLines(IDynamicGllTest.ONE_LINE_INPUTS, concreteGrammarFolder)
            .map {
                getCorrectTestContainer(IDynamicGllTest.getTestName(it), gll, tokenizeInput(it, classes.lexer))
            }

        val incorrectOneLineInputs = getLines(IDynamicGllTest.ONE_LINE_ERRORS_INPUTS, concreteGrammarFolder)
            .map {
                getIncorrectTestContainer(
                    IDynamicGllTest.getTestName(it),
                    gll,
                    tokenizeInput(it, classes.lexer)
                )
            }

        val correctInputs =
            getFiles(IDynamicGllTest.INPUTS, concreteGrammarFolder)?.map { file ->
                getCorrectTestContainer(file.name, gll, tokenizeInput(readFile(file), classes.lexer))
            } ?: emptyList()

        val incorrectInputs =
            getFiles(IDynamicGllTest.INCORRECT_INPUTS, concreteGrammarFolder)?.map { file ->
                getIncorrectTestContainer(file.name, gll, tokenizeInput(readFile(file), classes.lexer))
            } ?: emptyList()

        return correctOneLineInputs + incorrectOneLineInputs + correctInputs + incorrectInputs
    }

}
