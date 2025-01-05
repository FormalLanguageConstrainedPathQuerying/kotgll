import org.antlr.Java8Lexer
import org.antlr.Java8Parser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File

fun main(args: Array<String>) {
    try {
        AntlrBenchmark().parse(File(args[0]).readText())
    } catch (e: Throwable) {
        println(e)
        System.exit(1)
    }
}

class AntlrBenchmark : ParsingBenchmarks() {

    override fun getShortName(): String = "Antlr"

    @Override
    override fun parse(text: String) {
        var x = 30
        while (x-- > 0) {
            val antlrParser = Java8Parser(
                CommonTokenStream(
                    Java8Lexer(
                        CharStreams.fromString(text)
                    )
                )
            )
            try {
                antlrParser.compilationUnit()
            } catch (e: Exception) {
                print(e)
            }
        }
    }
}

