import org.antlr.fast.JavaLexer
import org.antlr.fast.JavaParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class AntlrFastBenchmark: ParsingBenchmarks() {

    override fun getShortName(): String = "AntlrFast"

    @Override
    override fun parse(text: String) {
        val antlrParser =
            JavaParser(
                CommonTokenStream(
                    JavaLexer(
                        CharStreams.fromString(text)
                    )
                )
            )
        try {
            var compilationUnit = antlrParser.compilationUnit()
        }
        catch (e: Exception){
            print(e)
        }
    }
}