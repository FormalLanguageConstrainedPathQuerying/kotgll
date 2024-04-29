import grammars.JavaGrammar
import lexers.JavaLexer
import lexers.JavaToken
import org.ucfs.input.LinearInputLabel
import org.ucfs.input.RecoveryLinearInput
import org.ucfs.parser.Gll
import org.ucfs.rsm.symbol.Term
import java.io.File
import java.io.StringReader
import kotlin.properties.Delegates

const val pathToSources = "/home/hollowcoder/Programming/SRC/UCFS/benchmarks/src/jmh/resources/srcFiles/"

fun main() {
    val projects = arrayOf("junitSources", "elasticSearchSources", "guavaSources", "rxJavaSources", "openJdkSources")

    val grammar = JavaGrammar().rsm
    var token: JavaToken
    var vertexId by Delegates.notNull<Int>()

    for (project in projects) {
        File("$pathToSources$project/").walk().filter {it.isFile}.forEach { inputPath ->
            val file = File(inputPath.path)
            val newFile = File("${pathToSources}${project}Processed/${file.name}")
            val input = file.readText()
            val inputGraph = RecoveryLinearInput<Int, LinearInputLabel>()
            val gll = Gll.recoveryGll(grammar, inputGraph)
            val lexer = JavaLexer(StringReader(input))
            vertexId = 0

            inputGraph.addStartVertex(vertexId)
            inputGraph.addVertex(vertexId)

            while (true) {
                try {
                    token = lexer.yylex()
                } catch (e: java.lang.Error) {
                    return@forEach
                }
                if (token == JavaToken.EOF) break
                inputGraph.addEdge(vertexId, LinearInputLabel(Term(token)), ++vertexId)
                inputGraph.addVertex(vertexId)
            }
            try {
                val result = gll.parse()
                if (result.first != null) {
                    file.copyTo(newFile)
                }
            } catch (e : java.lang.Error) {
                return@forEach
            }
        }
    }
}
