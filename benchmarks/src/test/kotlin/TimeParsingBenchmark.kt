import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

abstract class TimeParsingBenchmark {


    val repeatCount: Int = 5
    abstract fun getShortName(): String

    private fun runTimeTest(fileName: String, text: String) {
        var result: Long = 0
        for(i in 0..repeatCount) {
            val startTime = System.currentTimeMillis()
            parse(text)
            result += System.currentTimeMillis() - startTime
        }
        result /= repeatCount
        val message = "$fileName,$result"
        println(message)
        File("${getShortName()}.csv").writeText(message)
    }

    abstract fun parse(text: String)

    private fun getResourceFolder(): String = Path.of("java", "correct").toString()


    private fun getResource(resourceFolder: String): Path {
        val res = TimeParsingBenchmark::class.java.getResource(resourceFolder)
            ?: throw RuntimeException("No resource '$resourceFolder'")
        return Path.of(res.toURI())
    }

    @TestFactory
    fun timeTest(): Collection<DynamicTest> {
        return getTests(getResource(getResourceFolder()), ::runTimeTest)
    }

    private fun getTests(folder: Path, run: (String, String) -> Unit): Collection<DynamicTest> {
        return Files.list(folder).map { file ->
            dynamicTest(file.fileName.toString()) {
                val source = file.toFile().readText()
                run(file.name, source)
            }
        }.toList()
    }

}

