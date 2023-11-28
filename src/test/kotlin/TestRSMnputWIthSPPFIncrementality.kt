import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.srcgll.GLL
import org.srcgll.RecoveryMode
import org.srcgll.rsm.readRSMFromTXT
import org.srcgll.rsm.symbol.*
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.sppf.node.*

fun sameStructure(lhs : ISPPFNode, rhs : ISPPFNode) : Boolean
{
    val stack = ArrayDeque<Pair<ISPPFNode, ISPPFNode>>()
    val cycle = HashSet<Pair<ISPPFNode, ISPPFNode>>()
    val added = HashSet<Pair<ISPPFNode, ISPPFNode>>()
    var curPair : Pair<ISPPFNode, ISPPFNode>

    stack.addLast(Pair(lhs, rhs))

    while (stack.isNotEmpty()) {
        curPair = stack.last()
        added.add(curPair)

        val x = curPair.first
        val y = curPair.second

        when (x) {
            is SymbolSPPFNode<*> -> {
                when (y) {
                    is SymbolSPPFNode<*> -> {
                        if (!cycle.contains(curPair)) {
                            cycle.add(curPair)

                            if (x != y) return false
                            if (x.kids.count() != y.kids.count()) return false

                            for (i in x.kids.indices) {
                                val pair = Pair(x.kids.elementAt(i), y.kids.elementAt(i))

                                if (!added.contains(pair)) {
                                    stack.addLast(pair)
                                }
                            }

                            if (stack.last() == curPair) {
                                cycle.remove(curPair)
                            }
                        }
                    }
                    else -> return false
                }
            }
            is ItemSPPFNode<*> -> {
                when (y) {
                    is ItemSPPFNode<*> -> {
                        if (!cycle.contains(curPair)) {
                            cycle.add(curPair)

                            if (x != y) return false
                            if (x.kids.count() != y.kids.count()) return false

                            for (i in x.kids.indices) {
                                val pair = Pair(x.kids.elementAt(i), y.kids.elementAt(i))
                                if (!added.contains(pair)) {
                                    stack.addLast(pair)
                                }
                            }

                            if (stack.last() == curPair) {
                                cycle.remove(curPair)
                            }
                        }
                    }
                    else -> return false
                }
            }
            is PackedSPPFNode<*> -> {
                when (y) {
                    is PackedSPPFNode<*> -> {
                        if (x.rsmState != y.rsmState) return false
                        if (x.pivot != y.pivot)       return false

                        if (x.leftSPPFNode != null && y.leftSPPFNode != null) {
                            val pair = Pair(x.leftSPPFNode!!, y.leftSPPFNode!!)

                            if (!added.contains(pair)) {
                                stack.addLast(pair)
                            }
                        } else if (x.leftSPPFNode != null || y.leftSPPFNode != null) {
                            return false
                        }
                        if (x.rightSPPFNode != null && y.rightSPPFNode != null) {
                            val pair = Pair(x.rightSPPFNode!!, y.rightSPPFNode!!)

                            if (!added.contains(pair)) {
                                stack.addLast(pair)
                            }
                        } else if (x.rightSPPFNode != null || y.rightSPPFNode != null) {
                            return false
                        }
                    }
                    else -> return false
                }
            }
            is TerminalSPPFNode<*> -> {
                when (y) {
                    is TerminalSPPFNode<*> -> {
                        if (x != y) return false
                    }
                    else -> return false
                }
            }
        }

        if (stack.last() == curPair) stack.removeLast()
    }

    return true
}

class TestRSMStringInputWIthSPPFIncrementality
{
    @ParameterizedTest
    @MethodSource("test_1")
    fun `test BracketStarX grammar`(input : String)
    {
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
    fun `test CAStarBStar grammar`(input : String)
    {
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

        assert(sameStructure(result.first!!, static.first!!))
    }

    @ParameterizedTest
    @MethodSource("test_3")
    fun `test AB grammar`(input : String)
    {
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
        inputGraph.addEdge(addFrom, LinearInputLabel(Terminal("b")), ++curVertexId)
        inputGraph.edges[curVertexId] = initEdges

        inputGraph.addVertex(curVertexId)

        result = gll.parse(addFrom)
        val static = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()

        assert(sameStructure(result.first!!, static.first!!))
    }

    @ParameterizedTest
    @MethodSource("test_4")
    fun `test Dyck grammar`(input : String)
    {
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
    fun `test Ambiguous grammar`(input : String)
    {
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

    @ParameterizedTest
    @MethodSource("test_6")
    fun `test MultiDyck grammar`(input : String)
    {
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

    @ParameterizedTest
    @MethodSource("test_7")
    fun `test SimpleGolang grammar`(input : String)
    {
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