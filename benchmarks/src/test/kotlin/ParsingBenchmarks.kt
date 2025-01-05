import org.junit.jupiter.api.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
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
    private val timePerTestCase: Long = 400
    private val repeatCount: Int = 10
    lateinit var file: File

    val datasetName = "junit-4-12"
    //val datasetName = "rxjava-2-2-2"

    val resourceFolder: Path = Path.of("java", "correct", datasetName)
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
            file.writeText("fileName,processing_tim_avg_${repeatCount}_times_millis,max_heap_size_mb$memoryMeasurement")
        }
    }


    abstract fun getShortName(): String

    private fun getHeapSize(): Long = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    private fun getPrintableHeapSize(heapSize: Long? = null): String {
        return String.format("%.2f", (heapSize ?: getHeapSize()) * 1.0 / memoryDivider)
            .trimEnd('0').trimEnd('.')
    }

    private fun getMeanTime(text: String): Pair<Double, Long> {
        var meanTimeResult = 0.0
        var maxMemoryUsage: Long = 0

        for (i in 0..repeatCount) {
            System.gc()
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
            } catch (e: OutOfMemoryError) {
                System.gc()
                report(fileName, e.javaClass.name, "OOM")
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

    fun getResource(resourceFolder: String): Path {
        val res = ParsingBenchmarks::class.java.getResource(resourceFolder)
            ?: throw RuntimeException("No resource '$resourceFolder'")
        return Path.of(res.toURI())
    }

    @Disabled("Disable for running on CI")
    @TestFactory
    @Timeout(100)
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

