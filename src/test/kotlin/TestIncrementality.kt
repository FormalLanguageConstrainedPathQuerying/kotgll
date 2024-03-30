import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.srcgll.input.LinearInputLabel
import org.srcgll.input.RecoveryLinearInput
import org.srcgll.parser.Gll
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.readRsmFromTxt
import org.srcgll.rsm.symbol.Term
import org.srcgll.sppf.node.*
import org.srcgll.sppf.writeSppfToDot
import java.io.IOException
import kotlin.io.path.Path
import kotlin.test.Ignore

class TestIncrementality {
    val pathToGrammars = "/textRsmGrammar/"

    fun getRsm(fileName: String): RsmState {
        val fullName = pathToGrammars + fileName
        val url = object {}.javaClass.getResource(fullName) ?: throw IOException("Not find $fullName in project resources")
        return readRsmFromTxt(Path(url.path))
    }
    @Ignore("not implemented in parser")
    @ParameterizedTest
    @MethodSource("test_1")
    fun `test BracketStarX grammar`(input: String) {
        incrementalityTest(input, "bracket_star_x.txt", Term("["))
    }

    @ParameterizedTest
    @MethodSource("test_2")
    fun `test CAStarBStar grammar`(input: String) {
        val (result, static) = incrementalityTest(input, "c_a_star_b_star.txt", Term("a"))

        if (input == "caabb") {
            writeSppfToDot(result, "debug_incr.dot")
            writeSppfToDot(static, "debug_static.dot")
        }
    }

    @Ignore("not implemented in parser")
    @ParameterizedTest
    @MethodSource("test_3")
    fun `test AB grammar`(input: String) {
        incrementalityTest(input, "ab.txt", Term("ab"))
    }

    @Ignore("not implemented in parser")
    @ParameterizedTest
    @MethodSource("test_4")
    fun `test Dyck grammar`(input: String) {
        incrementalityTest(input, "dyck.txt", Term("("))
    }

    @ParameterizedTest
    @MethodSource("test_5")
    fun `test Ambiguous grammar`(input: String) {
        incrementalityTest(input, "ambiguous.txt", Term("a"))
    }

    @Ignore("not implemented in parser")
    @ParameterizedTest
    @MethodSource("test_6")
    fun `test MultiDyck grammar`(input: String) {
        incrementalityTest(input, "multi_dyck.txt", Term("{"))
    }

    @Ignore("not implemented in parser")
    @ParameterizedTest
    @MethodSource("test_7")
    fun `test SimpleGolang grammar`(input: String) {
        incrementalityTest(input, "simple_golang.txt", Term("1"))
    }

    private fun incrementalityTest(
        input: String,
        pathToRsm: String,
        additionalLabel: Term<*>
    ): Pair<SppfNode<Int>, SppfNode<Int>> {
        val startState = getRsm(pathToRsm)

        val inputGraph = RecoveryLinearInput<Int, LinearInputLabel>()
        val gll = Gll.recoveryGll(startState, inputGraph)
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Term(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        gll.parse()

        val addFrom = if (curVertexId > 1) curVertexId - 1 else 0
        val initEdges = inputGraph.getEdges(addFrom)

        inputGraph.edges.remove(addFrom)
        inputGraph.addEdge(addFrom, LinearInputLabel(additionalLabel), ++curVertexId)
        inputGraph.edges[curVertexId] = initEdges

        inputGraph.addVertex(curVertexId)

        val result = gll.parse(addFrom)
        val static = Gll.recoveryGll(startState, inputGraph).parse()

        assert(sameStructure(result.first!!, static.first!!))

        return Pair(result.first!!, static.first!!)

    }

    companion object {
        @JvmStatic
        fun test_1() = listOf(
            Arguments.of("[["),
            Arguments.of("[[x"),
            Arguments.of("["),
            Arguments.of("x"),
            Arguments.of(""),
            Arguments.of("[x[")
        )

        @JvmStatic
        fun test_2() = listOf(
            Arguments.of(""),
            Arguments.of("cab"),
            Arguments.of("caabb"),
            Arguments.of("caaaba"),
            Arguments.of("ab"),
            Arguments.of("ccab")
        )

        @JvmStatic
        fun test_3() = listOf(
            Arguments.of(""),
            Arguments.of("ab"),
            Arguments.of("abbbb"),
            Arguments.of("ba"),
            Arguments.of("a"),
            Arguments.of("b")
        )

        @JvmStatic
        fun test_4() = listOf(
            Arguments.of(""),
            Arguments.of("()"),
            Arguments.of("()()"),
            Arguments.of("()(())"),
            Arguments.of("(()())"),
            Arguments.of("("),
            Arguments.of(")"),
            Arguments.of("(()"),
            Arguments.of("(()()")
        )

        @JvmStatic
        fun test_5() = listOf(
            Arguments.of(""),
            Arguments.of("a"),
            Arguments.of("aa"),
            Arguments.of("aaa"),
            Arguments.of("aaaa")
        )

        @JvmStatic
        fun test_6() = listOf(
            Arguments.of("{{[[]]}}()"),
            Arguments.of("{[]}{(())()}"),
            Arguments.of("{]"),
            Arguments.of("[(}"),
            Arguments.of("[(])")
        )

        @JvmStatic
        fun test_7() = listOf(
            Arguments.of("1+;r1;"),
            Arguments.of(""),
            Arguments.of("1+"),
            Arguments.of("r1+;"),
            Arguments.of("r;"),
            Arguments.of("1+1;;"),
            Arguments.of("rr;")
        )

        fun sameStructure(lhs: ISppfNode, rhs: ISppfNode): Boolean {
            val queue = ArrayDeque<ISppfNode>()
            val added = HashSet<ISppfNode>()
            val lhsTreeMetrics = IntArray(5) { 0 }
            val rhsTreeMetrics = IntArray(5) { 0 }
            var curSppfNode: ISppfNode

            queue.addLast(lhs)

            while (queue.isNotEmpty()) {
                curSppfNode = queue.last()

                if (curSppfNode.weight > 0) {
                    lhsTreeMetrics[4]++
                }

                when (curSppfNode) {
                    is NonterminalSppfNode<*> -> {

                        if (curSppfNode is SymbolSppfNode<*>) {
                            lhsTreeMetrics[2]++
                        } else {
                            lhsTreeMetrics[1]++
                        }

                        curSppfNode.children.forEach { kid ->
                            if (!added.contains(kid)) {
                                queue.addLast(kid)
                                added.add(kid)
                            }
                        }
                    }

                    is PackedSppfNode<*> -> {
                        lhsTreeMetrics[3]++
                        if (curSppfNode.rightSppfNode != null) {
                            if (!added.contains(curSppfNode.rightSppfNode!!)) {
                                queue.addLast(curSppfNode.rightSppfNode!!)
                                added.add(curSppfNode.rightSppfNode!!)
                            }
                        }
                        if (curSppfNode.leftSppfNode != null) {
                            if (!added.contains(curSppfNode.leftSppfNode!!)) {
                                queue.addLast(curSppfNode.leftSppfNode!!)
                                added.add(curSppfNode.leftSppfNode!!)
                            }
                        }
                    }

                    is TerminalSppfNode<*> -> {
                        lhsTreeMetrics[0]++
                    }
                }

                if (curSppfNode == queue.last()) {
                    queue.removeLast()
                }
            }

            added.clear()
            queue.clear()

            queue.addLast(rhs)

            while (queue.isNotEmpty()) {
                curSppfNode = queue.last()

                if (curSppfNode.weight > 0) {
                    rhsTreeMetrics[4]++
                }

                when (curSppfNode) {
                    is NonterminalSppfNode<*> -> {

                        if (curSppfNode is SymbolSppfNode<*>) {
                            rhsTreeMetrics[2]++
                        } else {
                            rhsTreeMetrics[1]++
                        }

                        curSppfNode.children.forEach { kid ->
                            if (!added.contains(kid)) {
                                queue.addLast(kid)
                                added.add(kid)
                            }
                        }
                    }

                    is PackedSppfNode<*> -> {
                        rhsTreeMetrics[3]++
                        if (curSppfNode.rightSppfNode != null) {
                            if (!added.contains(curSppfNode.rightSppfNode!!)) {
                                queue.addLast(curSppfNode.rightSppfNode!!)
                                added.add(curSppfNode.rightSppfNode!!)
                            }
                        }
                        if (curSppfNode.leftSppfNode != null) {
                            if (!added.contains(curSppfNode.leftSppfNode!!)) {
                                queue.addLast(curSppfNode.leftSppfNode!!)
                                added.add(curSppfNode.leftSppfNode!!)
                            }
                        }
                    }

                    is TerminalSppfNode<*> -> {
                        rhsTreeMetrics[0]++
                    }
                }

                if (curSppfNode == queue.last()) {
                    queue.removeLast()
                }
            }

            val result = lhsTreeMetrics.zip(rhsTreeMetrics) { x, y -> x == y }
            return !result.contains(false)
        }
    }

}