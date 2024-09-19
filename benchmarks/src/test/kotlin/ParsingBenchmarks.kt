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
import kotlin.math.max

abstract class ParsingBenchmarks {
    val version: String = LocalDateTime.now().format(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    )
    private val timePerTestCase: Long = 30
    private val repeatCount: Int = 5
    lateinit var file: File
    private val resourceFolder: Path = Path.of("java", "correct", "junit-4-12")
    private lateinit var csvFileName: String
    private val memoryMeasurement = "Mb"
    private val memoryDivider: Long = 1024 * 1024

    private fun initFolder(): DynamicTest {
        val resultPath = Path.of("src", "test", "result", getShortName())
        return dynamicTest("initiation for ${getShortName()}") {
            Files.createDirectories(resultPath)
            csvFileName = "${getShortName()}_${resourceFolder.joinToString("_")}.csv"
            file = File(resultPath.toString(), csvFileName)
            file.createNewFile()
            file.writeText("% Time benchmark for ${getShortName()} on dataset $resourceFolder at $version\n")
            file.appendText("fileName,result(avg $repeatCount times)(millis), used heap memory ($memoryMeasurement)")
        }
    }


    abstract fun getShortName(): String

    private fun getHeapSize(): Long = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    private fun getPrintableHeapSize(heapSize: Long? = null): String {
        return String.format("%.2f", (heapSize ?: getHeapSize()) * 1.0/ memoryDivider)
            .trimEnd('0').trimEnd('.')
    }

    private fun getMeanTime(text: String): Pair<Double, Long> {
        var meanTimeResult = 0.0
        var maxMemoryUsage: Long = 0
        for (i in 0..repeatCount) {
            val startTime = System.currentTimeMillis()
            parse(text)
            meanTimeResult += System.currentTimeMillis() - startTime
            maxMemoryUsage = max(maxMemoryUsage, getHeapSize())
        }

        meanTimeResult /= repeatCount
        return Pair(meanTimeResult, maxMemoryUsage)
    }

    private fun measureTimeWithTimeout(fileName: String, text: String) {
        Assertions.assertTimeoutPreemptively(ofSeconds(timePerTestCase), {
            try {
                val result = getMeanTime(text)
                report(fileName, result.first.toString(), getPrintableHeapSize(result.second))
            } catch (e: Exception) {
                report(fileName, e.javaClass.name, getPrintableHeapSize())
                assert(false) { e.toString() }
            }
        }, {
            report(fileName, "timeout", getPrintableHeapSize())
            "$fileName failed with timeout"
        })
    }

    private fun report(fileName: String, result: String, usedMemory: String = "not measured") {
        val message = "$fileName,$result,$usedMemory"
        println(message)
        file.appendText("\n$message")
    }

    abstract fun parse(text: String)

    private fun getResource(resourceFolder: String): Path {
        val res = ParsingBenchmarks::class.java.getResource(resourceFolder)
            ?: throw RuntimeException("No resource '$resourceFolder'")
        return Path.of(res.toURI())
    }

    @TestFactory
    @Timeout(1)
    fun timeTest(): Collection<DynamicTest> {
        return getTests(getResource(resourceFolder.toString()), ::measureTimeWithTimeout)
    }

    private fun getTests(folder: Path, run: (String, String) -> Unit): Collection<DynamicTest> {
        return listOf(initFolder()) + Files.list(folder).sorted().map { file ->
            dynamicTest(file.fileName.toString()) {
                val source = file.toFile().readText()
                run(file.name, source)
            }
        }.toList()
    }

}

