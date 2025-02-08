import org.junit.jupiter.api.Test
import org.ucfs.input.DotParser
import org.ucfs.input.InputGraph
import org.ucfs.input.TerminalInputLabel
import org.ucfs.input.utils.DotWriter
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals

class TestDotParser {
    @Test
    fun testParser(){
        val testCasesFolder = File(Path.of("src", "test", "resources", "dotParserTest").toUri())
        if (!testCasesFolder.exists()) {
            println("Can't find test case for dotParserTest")
        }
        for (file in testCasesFolder.listFiles()) {
            val originalDot = file.readText()
            val graph: InputGraph<Int, TerminalInputLabel> = DotParser().parseDot(originalDot)
            assertEquals(originalDot, DotWriter().getDotView(graph))
        }
    }


}