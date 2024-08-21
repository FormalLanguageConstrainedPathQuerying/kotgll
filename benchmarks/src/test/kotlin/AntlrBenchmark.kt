import org.antlr.Java8Lexer
import org.antlr.Java8Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class AntlrBenchmark: TimeParsingBenchmark() {

    override fun getShortName(): String = "Antlr"

    @Override
    override fun parse(text: String) {
        val antlrParser =
            Java8Parser(
                CommonTokenStream(
                    Java8Lexer(
                        CharStreams.fromString(text)
                    )
                )
            )
        antlrParser.compilationUnit()
    }

}