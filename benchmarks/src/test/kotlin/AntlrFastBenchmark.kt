import org.antlr.Java8Lexer
import org.antlr.fast.JavaLexer
import org.antlr.fast.JavaParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.Timeout
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    try {
        AntlrFastBenchmark().parse(File(args[0]).readText())
    } catch (e: Throwable) {
        println(e)
        System.exit(1)
    }
}

class AntlrFastBenchmark : ParsingBenchmarks() {

    override fun getShortName(): String = "AntlrFast"

    fun main(args: Array<String>) {
        parse(File(args[0]).readText())
    }

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
        } catch (e: Exception) {
            print(e)
        }
    }

    fun getTokenCount(text: String): Int {
        val tokenStream = CommonTokenStream(
            Java8Lexer(
                CharStreams.fromString(text)
            )
        )
        tokenStream.fill()
        return tokenStream.getTokens().size
    }

    var sum_count: Int = 0
    val fileName = "tokens_count.csv"

    @Disabled
    @TestFactory
    @Timeout(100)
    fun getTokensCount(): Collection<DynamicTest> {
        File(fileName).writeText("filename,tokens\n")
        return getTests(getResource(resourceFolder.toString()))
    }

    private fun getTests(folder: Path): Collection<DynamicTest> {
        return Files.list(folder).sorted().map { file ->
            dynamicTest(file.fileName.toString()) {
                val source = file.toFile().readText()
                val count = getTokenCount(source)
                sum_count += count
                File(fileName).appendText("${file.fileName},${count}\n")
            }
        }.toList()
    }
}