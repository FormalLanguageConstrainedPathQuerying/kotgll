import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.Timeout
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration.ofSeconds
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.name

abstract class TimeParsingBenchmark {
    val version: String = LocalDateTime.now().format(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    )
    private val timePerTestCase: Long = 10
    private val repeatCount: Int = 1
    lateinit var file: File

    private fun getFolderName() = "${getShortName()}_${getResourceFolder()}_$version.csv"

    private fun initFolder(): DynamicTest {
        val resultPath = Path.of("src", "test", "result", getShortName())
        return dynamicTest("initiation for ${getShortName()}") {
            Files.createDirectories(resultPath)
            file = File(resultPath.toString(), getFolderName())
            file.createNewFile()
            file.writeText("fileName,result(avg $repeatCount times)")
        }
    }


    abstract fun getShortName(): String

    private fun getMeanTime(text: String): Double {
        var result = 0.0
        for (i in 0..repeatCount) {
            val startTime = System.currentTimeMillis()
            parse(text)
            result += System.currentTimeMillis() - startTime
        }
        result /= repeatCount
        return result
    }

    private fun measureTimeWithTimeout(fileName: String, text: String) {
        Assertions.assertTimeoutPreemptively(ofSeconds(timePerTestCase), {
            try {
                report(fileName, getMeanTime(text).toString())
            } catch (e: Exception) {
                report(fileName, e.javaClass.name)
                assert(false) { e.toString() }
            }
        }, {
            report(fileName, "timeout")
            "$fileName failed with timeout"
        })
    }

    private fun report(fileName: String, result: String) {
        val message = "$fileName,$result"
        println(message)
        file.appendText("\n$message")
    }

    abstract fun parse(text: String)

    private fun getResourceFolder(): String = Path.of("test_for_test").toString()
    //Path.of("java", "correct", "junit-4-12").toString()


    private fun getResource(resourceFolder: String): Path {
        val res = TimeParsingBenchmark::class.java.getResource(resourceFolder)
            ?: throw RuntimeException("No resource '$resourceFolder'")
        return Path.of(res.toURI())
    }

    @TestFactory
    @Timeout(1)
    fun timeTest(): Collection<DynamicTest> {
        return getTests(getResource(getResourceFolder()), ::measureTimeWithTimeout)
    }

    private fun getTests(folder: Path, run: (String, String) -> Unit): Collection<DynamicTest> {
        return listOf(initFolder()) + Files.list(folder).map { file ->
            dynamicTest(file.fileName.toString()) {
                val source = file.toFile().readText()
                    run(file.name, source)
            }
        }.toList()
    }

}

