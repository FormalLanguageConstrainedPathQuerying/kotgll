import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.srcgll.GLL
import org.srcgll.RecoveryMode
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.rsm.readRSMFromTXT
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.buildStringFromSPPF
import kotlin.test.assertNotNull

const val pathToGrammars = ".src/test/resources/cli/TestRSMReadWriteTXT"

class TestRSMStringInputWIthSPPFRecovery {
    @ParameterizedTest
    @MethodSource("test_1")
    fun `test BracketStarX grammar`(input: String, weight: Int) {
        val startState = readRSMFromTXT("${pathToGrammars}/bracket_star_x.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)


        val result = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()
        val recoveredString = buildStringFromSPPF(result.first!!)

        val recoveredInputGraph = LinearInput<Int, LinearInputLabel>()

        curVertexId = 0
        recoveredInputGraph.addVertex(curVertexId)
        for (x in recoveredString) {
            recoveredInputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            recoveredInputGraph.addVertex(curVertexId)
        }
        recoveredInputGraph.addStartVertex(0)

        assert(result.first!!.weight <= weight)
        assertNotNull(GLL(startState, recoveredInputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest
    @MethodSource("test_2")
    fun `test CAStarBStar grammar`(input: String, weight: Int) {
        val startState = readRSMFromTXT("${pathToGrammars}/c_a_star_b_star.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)


        val result = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()
        val recoveredString = buildStringFromSPPF(result.first!!)

        val recoveredInputGraph = LinearInput<Int, LinearInputLabel>()

        curVertexId = 0
        recoveredInputGraph.addVertex(curVertexId)
        for (x in recoveredString) {
            recoveredInputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            recoveredInputGraph.addVertex(curVertexId)
        }
        recoveredInputGraph.addStartVertex(0)

        assert(result.first!!.weight <= weight)
        assertNotNull(GLL(startState, recoveredInputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest
    @MethodSource("test_3")
    fun `test AB grammar`(input: String, weight: Int) {
        val startState = readRSMFromTXT("${pathToGrammars}/ab.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)


        val result = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()
        val recoveredString = buildStringFromSPPF(result.first!!)

        val recoveredInputGraph = LinearInput<Int, LinearInputLabel>()

        curVertexId = 0
        recoveredInputGraph.addVertex(curVertexId)
        for (x in recoveredString) {
            recoveredInputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            recoveredInputGraph.addVertex(curVertexId)
        }
        recoveredInputGraph.addStartVertex(0)

        assert(result.first!!.weight <= weight)
        assertNotNull(GLL(startState, recoveredInputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest
    @MethodSource("test_4")
    fun `test Dyck grammar`(input: String, weight: Int) {
        val startState = readRSMFromTXT("${pathToGrammars}/dyck.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)


        val result = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()
        val recoveredString = buildStringFromSPPF(result.first!!)

        val recoveredInputGraph = LinearInput<Int, LinearInputLabel>()

        curVertexId = 0
        recoveredInputGraph.addVertex(curVertexId)
        for (x in recoveredString) {
            recoveredInputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            recoveredInputGraph.addVertex(curVertexId)
        }
        recoveredInputGraph.addStartVertex(0)

        assert(result.first!!.weight <= weight)
        assertNotNull(GLL(startState, recoveredInputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest
    @MethodSource("test_5")
    fun `test Ambiguous grammar`(input: String, weight: Int) {
        val startState = readRSMFromTXT("${pathToGrammars}/ambiguous.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        val result = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()

        val recoveredString = buildStringFromSPPF(result.first!!)

        val recoveredInputGraph = LinearInput<Int, LinearInputLabel>()

        curVertexId = 0
        recoveredInputGraph.addVertex(curVertexId)

        for (x in recoveredString) {
            recoveredInputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            recoveredInputGraph.addVertex(curVertexId)
        }
        recoveredInputGraph.addStartVertex(0)

        assert(result.first!!.weight <= weight)
        assertNotNull(GLL(startState, recoveredInputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest
    @MethodSource("test_6")
    fun `test MultiDyck grammar`(input: String, weight: Int) {
        val startState = readRSMFromTXT("${pathToGrammars}/multi_dyck.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)


        val result = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()
        val recoveredString = buildStringFromSPPF(result.first!!)

        val recoveredInputGraph = LinearInput<Int, LinearInputLabel>()

        curVertexId = 0
        recoveredInputGraph.addVertex(curVertexId)
        for (x in recoveredString) {
            recoveredInputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            recoveredInputGraph.addVertex(curVertexId)
        }
        recoveredInputGraph.addStartVertex(0)

        assert(result.first!!.weight <= weight)
        assertNotNull(GLL(startState, recoveredInputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest
    @MethodSource("test_7")
    fun `test SimpleGolang grammar`(input: String, weight: Int) {
        val startState = readRSMFromTXT("${pathToGrammars}/simple_golang.txt")
        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        val result = GLL(startState, inputGraph, recovery = RecoveryMode.ON).parse()
        val recoveredString = buildStringFromSPPF(result.first!!)

        val recoveredInputGraph = LinearInput<Int, LinearInputLabel>()

        curVertexId = 0
        recoveredInputGraph.addVertex(curVertexId)
        for (x in recoveredString) {
            recoveredInputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            recoveredInputGraph.addVertex(curVertexId)
        }
        recoveredInputGraph.addStartVertex(0)

        assert(result.first!!.weight <= weight)
        assertNotNull(GLL(startState, recoveredInputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    companion object {
        @JvmStatic
        fun test_1() = listOf(
            Arguments.of("[[", 1),
            Arguments.of("[[x", 0),
            Arguments.of("[", 1),
            Arguments.of("x", 1),
            Arguments.of("", 2),
            Arguments.of("[x[", 1)
        )

        @JvmStatic
        fun test_2() = listOf(
            Arguments.of("", 1),
            Arguments.of("cab", 0),
            Arguments.of("caabb", 0),
            Arguments.of("caaaba", 1),
            Arguments.of("ab", 1),
            Arguments.of("ccab", 1)
        )

        @JvmStatic
        fun test_3() = listOf(
            Arguments.of("", 2),
            Arguments.of("ab", 0),
            Arguments.of("abbbb", 3),
            Arguments.of("ba", 2),
            Arguments.of("a", 1),
            Arguments.of("b", 1)
        )

        @JvmStatic
        fun test_4() = listOf(
            Arguments.of("", 0),
            Arguments.of("()", 0),
            Arguments.of("()()", 0),
            Arguments.of("()(())", 0),
            Arguments.of("(()())", 0),
            Arguments.of("(", 1),
            Arguments.of(")", 1),
            Arguments.of("(()", 1),
            Arguments.of("(()()", 1)
        )

        @JvmStatic
        fun test_5() = listOf(
            Arguments.of("", 1),
            Arguments.of("a", 0),
            Arguments.of("aa", 0),
            Arguments.of("aaa", 0),
            Arguments.of("aaaa", 0)
        )

        @JvmStatic
        fun test_6() = listOf(
            Arguments.of("{{[[]]}}()", 0),
            Arguments.of("{[]}{(())()}", 0),
            Arguments.of("{]", 2),
            Arguments.of("[(}", 3),
            Arguments.of("[(])", 2)
        )

        @JvmStatic
        fun test_7() = listOf(
            Arguments.of("1+;r1;", 1),
            Arguments.of("", 0),
            Arguments.of("1+", 2),
            Arguments.of("r1+;", 1),
            Arguments.of("r;", 1),
            Arguments.of("1+1;;", 1),
            Arguments.of("rr;", 2)
        )
    }
}