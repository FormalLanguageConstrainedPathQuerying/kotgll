import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.srcgll.GLL
import org.srcgll.RecoveryMode
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.rsm.readRSMFromTXT
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.*
import org.srcgll.sppf.writeSPPFToDOT
import kotlin.test.Ignore

fun sameStructure(lhs: ISPPFNode, rhs: ISPPFNode): Boolean {
    val queue = ArrayDeque<ISPPFNode>()
    val added = HashSet<ISPPFNode>()
    val lhsTreeMetrics = IntArray(5) {0}
    val rhsTreeMetrics = IntArray(5) {0}
    var curSPPFNode: ISPPFNode

    queue.addLast(lhs)

    while (queue.isNotEmpty()) {
        curSPPFNode = queue.last()

        if (curSPPFNode.weight > 0) {
            lhsTreeMetrics[4]++
        }

        when (curSPPFNode) {
            is ParentSPPFNode<*> -> {

                if (curSPPFNode is SymbolSPPFNode<*>) {
                    lhsTreeMetrics[2]++
                } else {
                    lhsTreeMetrics[1]++
                }

                curSPPFNode.kids.forEach { kid ->
                    if (!added.contains(kid)) {
                        queue.addLast(kid)
                        added.add(kid)
                    }
                }
            }

            is PackedSPPFNode<*> -> {
                lhsTreeMetrics[3]++
                if (curSPPFNode.rightSPPFNode != null) {
                    if (!added.contains(curSPPFNode.rightSPPFNode!!)) {
                        queue.addLast(curSPPFNode.rightSPPFNode!!)
                        added.add(curSPPFNode.rightSPPFNode!!)
                    }
                }
                if (curSPPFNode.leftSPPFNode != null) {
                    if (!added.contains(curSPPFNode.leftSPPFNode!!)) {
                        queue.addLast(curSPPFNode.leftSPPFNode!!)
                        added.add(curSPPFNode.leftSPPFNode!!)
                    }
                }
            }
            is TerminalSPPFNode<*> -> {
                lhsTreeMetrics[0]++
            }
        }

        if (curSPPFNode == queue.last()) {
            queue.removeLast()
        }
    }

    added.clear()
    queue.clear()

    queue.addLast(rhs)

    while (queue.isNotEmpty()) {
        curSPPFNode = queue.last()

        if (curSPPFNode.weight > 0) {
            rhsTreeMetrics[4]++
        }

        when (curSPPFNode) {
            is ParentSPPFNode<*> -> {

                if (curSPPFNode is SymbolSPPFNode<*>) {
                    rhsTreeMetrics[2]++
                } else {
                    rhsTreeMetrics[1]++
                }

                curSPPFNode.kids.forEach { kid ->
                    if (!added.contains(kid)) {
                        queue.addLast(kid)
                        added.add(kid)
                    }
                }
            }

            is PackedSPPFNode<*> -> {
                rhsTreeMetrics[3]++
                if (curSPPFNode.rightSPPFNode != null) {
                    if (!added.contains(curSPPFNode.rightSPPFNode!!)) {
                        queue.addLast(curSPPFNode.rightSPPFNode!!)
                        added.add(curSPPFNode.rightSPPFNode!!)
                    }
                }
                if (curSPPFNode.leftSPPFNode != null) {
                    if (!added.contains(curSPPFNode.leftSPPFNode!!)) {
                        queue.addLast(curSPPFNode.leftSPPFNode!!)
                        added.add(curSPPFNode.leftSPPFNode!!)
                    }
                }
            }
            is TerminalSPPFNode<*> -> {
                rhsTreeMetrics[0]++
            }
        }

        if (curSPPFNode == queue.last()) {
            queue.removeLast()
        }
    }

    val result = lhsTreeMetrics.zip(rhsTreeMetrics) { x, y -> x == y }
    return !result.contains(false)
}

class TestRSMStringInputWIthSPPFIncrementality {
    @Ignore("not implemented in parser")
    @ParameterizedTest
    @MethodSource("test_1")
    fun `test BracketStarX grammar`(input: String) {
        val startState = readRSMFromTXT("${pathToGrammars}/bracket_star_x.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        val gll = GLL(startState, inputGraph, recovery = RecoveryMode.ON)
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        var result = gll.parse()

        var addFrom = if (curVertexId > 1) curVertexId - 1 else 0
        val initEdges = inputGraph.getEdges(addFrom)

        inputGraph.edges.remove(addFrom)
        inputGraph.addEdge(addFrom, LinearInputLabel(Terminal("[")), ++curVertexId)
        inputGraph.edges[curVertexId] = initEdges

        inputGraph.addVertex(curVertexId)

        result = gll.parse(addFrom)
        val static = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()

        assert(sameStructure(result.first!!, static.first!!))
    }

    @ParameterizedTest
    @MethodSource("test_2")
    fun `test CAStarBStar grammar`(input: String) {
        val startState = readRSMFromTXT("${pathToGrammars}/c_a_star_b_star.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        val gll = GLL(startState, inputGraph, recovery = RecoveryMode.ON)
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        var result = gll.parse()

        var addFrom = if (curVertexId > 1) curVertexId - 1 else 0
        val initEdges = inputGraph.getEdges(addFrom)

        inputGraph.edges.remove(addFrom)
        inputGraph.addEdge(addFrom, LinearInputLabel(Terminal("a")), ++curVertexId)
        inputGraph.edges[curVertexId] = initEdges

        inputGraph.addVertex(curVertexId)

        result = gll.parse(addFrom)
        val static = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()

        if (input == "caabb") {
            writeSPPFToDOT(result.first!!, "./debug_incr.dot")
            writeSPPFToDOT(static.first!!, "./debug_static.dot")
        }

        assert(sameStructure(result.first!!, static.first!!))
    }

    @Ignore("not implemented in parser")
    @ParameterizedTest
    @MethodSource("test_3")
    fun `test AB grammar`(input: String) {
        val startState = readRSMFromTXT("${pathToGrammars}/ab.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        val gll = GLL(startState, inputGraph, recovery = RecoveryMode.ON)
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)


        var result = gll.parse()

        var addFrom = if (curVertexId > 1) curVertexId - 1 else 0
        val initEdges = inputGraph.getEdges(addFrom)

        inputGraph.edges.remove(addFrom)
        inputGraph.addEdge(addFrom, LinearInputLabel(Terminal("ab")), ++curVertexId)
        inputGraph.edges[curVertexId] = initEdges

        inputGraph.addVertex(curVertexId)

        result = gll.parse(addFrom)
        val static = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()

        assert(sameStructure(result.first!!, static.first!!))
    }

    @Ignore("not implemented in parser")
    @ParameterizedTest
    @MethodSource("test_4")
    fun `test Dyck grammar`(input: String) {
        val startState = readRSMFromTXT("${pathToGrammars}/dyck.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        val gll = GLL(startState, inputGraph, recovery = RecoveryMode.ON)
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)


        var result = gll.parse()

        var addFrom = if (curVertexId > 1) curVertexId - 1 else 0
        val initEdges = inputGraph.getEdges(addFrom)

        inputGraph.edges.remove(addFrom)
        inputGraph.addEdge(addFrom, LinearInputLabel(Terminal("(")), ++curVertexId)
        inputGraph.edges[curVertexId] = initEdges

        inputGraph.addVertex(curVertexId)

        result = gll.parse(addFrom)
        val static = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()

        assert(sameStructure(result.first!!, static.first!!))
    }

    @ParameterizedTest
    @MethodSource("test_5")
    fun `test Ambiguous grammar`(input: String) {
        val startState = readRSMFromTXT("${pathToGrammars}/ambiguous.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        val gll = GLL(startState, inputGraph, recovery = RecoveryMode.ON)
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        var result = gll.parse()

        var addFrom = if (curVertexId > 1) curVertexId - 1 else 0
        val initEdges = inputGraph.getEdges(addFrom)

        inputGraph.edges.remove(addFrom)
        inputGraph.addEdge(addFrom, LinearInputLabel(Terminal("a")), ++curVertexId)
        inputGraph.edges[curVertexId] = initEdges

        inputGraph.addVertex(curVertexId)

        result = gll.parse(addFrom)
        val static = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()

        assert(sameStructure(result.first!!, static.first!!))
    }

    @Ignore("not implemented in parser")
    @ParameterizedTest
    @MethodSource("test_6")
    fun `test MultiDyck grammar`(input: String) {
        val startState = readRSMFromTXT("${pathToGrammars}/multi_dyck.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        val gll = GLL(startState, inputGraph, recovery = RecoveryMode.ON)
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)


        var result = gll.parse()

        var addFrom = if (curVertexId > 1) curVertexId - 1 else 0
        val initEdges = inputGraph.getEdges(addFrom)

        inputGraph.edges.remove(addFrom)
        inputGraph.addEdge(addFrom, LinearInputLabel(Terminal("{")), ++curVertexId)
        inputGraph.edges[curVertexId] = initEdges

        inputGraph.addVertex(curVertexId)

        result = gll.parse(addFrom)
        val static = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()

        assert(sameStructure(result.first!!, static.first!!))
    }
    @Ignore("not implemented in parser")
    @ParameterizedTest
    @MethodSource("test_7")
    fun `test SimpleGolang grammar`(input: String) {
        val startState = readRSMFromTXT("${pathToGrammars}/simple_golang.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        val gll = GLL(startState, inputGraph, recovery = RecoveryMode.ON)
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        var result = gll.parse()

        var addFrom = if (curVertexId > 1) curVertexId - 1 else 0
        val initEdges = inputGraph.getEdges(addFrom)

        inputGraph.edges.remove(addFrom)
        inputGraph.addEdge(addFrom, LinearInputLabel(Terminal("1")), ++curVertexId)
        inputGraph.edges[curVertexId] = initEdges

        inputGraph.addVertex(curVertexId)

        result = gll.parse(addFrom)
        val static = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()

        assert(sameStructure(result.first!!, static.first!!))
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
    }
}