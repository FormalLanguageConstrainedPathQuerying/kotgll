import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.srcgll.Gll
import org.srcgll.RecoveryMode
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import kotlin.test.assertNotNull

class TestSuccess {
    @Test
    fun `test 'empty' hand-crafted grammar`() {
        val nonterminalS = Nonterminal("S")
        val input = ""
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0

        val inputGraph = getTokenStream(input)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @Test
    fun `test 'a' hand-crafted grammar`() {
        val nonterminalS = Nonterminal("S")
        val input = "a"
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        rsmState0.addEdge(
            symbol = Terminal("a"), head = RsmState(
                nonterminal = nonterminalS,
                isFinal = true,
            )
        )

        val inputGraph = getTokenStream(input)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @Test
    fun `test 'ab' hand-crafted grammar`() {
        val nonterminalS = Nonterminal("S")
        val input = "ab"
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RsmState(
            nonterminal = nonterminalS,
        )
        rsmState0.addEdge(symbol = Terminal("a"), head = rsmState1)
        rsmState1.addEdge(
            symbol = Terminal("b"), head = RsmState(
                nonterminal = nonterminalS, isFinal = true
            )
        )

        val inputGraph = getTokenStream(input)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["", "a", "aa", "aaa", "aaaa", "aaaaa", "aaaaaa", "aaaaaaa"])
    fun `test 'a-star' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        rsmState0.addEdge(symbol = Terminal("a"), head = rsmState1)
        rsmState1.addEdge(symbol = Terminal("a"), head = rsmState1)

        val inputGraph = getTokenStream(input)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["a", "aa", "aaa", "aaaa", "aaaaa", "aaaaaa", "aaaaaaa"])
    fun `test 'a-plus' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        rsmState0.addEdge(symbol = Terminal("a"), head = rsmState1)
        rsmState1.addEdge(symbol = Terminal("a"), head = rsmState1)

        val inputGraph = getTokenStream(input)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["", "ab", "abab", "ababab", "abababab", "ababababab"])
    fun `test '(ab)-star' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        rsmState0.addEdge(symbol = Terminal("ab"), head = rsmState1)
        rsmState1.addEdge(symbol = Terminal("ab"), head = rsmState1)

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0
        var pos = 0

        inputGraph.addVertex(curVertexId)
        while (pos < input.length) {
            var label: String
            if (input.startsWith("ab", pos)) {
                pos += 2
                label = "ab"
            } else {
                pos += 1
                label = input[pos].toString()
            }
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(label)), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(
        strings = ["", "()", "()()", "()()()", "(())", "(())()", "(())()()", "(())(())", "(())(())()", "(())(())()()", "(()())(()())", "((()))", "(((())))", "((((()))))", "()()((()))(()())", "(((()()())()()())()()())"]
    )
    fun `test 'dyck' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RsmState(
            nonterminal = nonterminalS,
        )
        val rsmState2 = RsmState(
            nonterminal = nonterminalS,
        )
        val rsmState3 = RsmState(
            nonterminal = nonterminalS,
        )
        val rsmState4 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )

        rsmState0.addEdge(symbol = Terminal("("), head = rsmState1)
        rsmState1.addEdge(symbol = nonterminalS, head = rsmState2)
        rsmState2.addEdge(symbol = Terminal(")"), head = rsmState3)
        rsmState3.addEdge(symbol = nonterminalS, head = rsmState4)

        val inputGraph = getTokenStream(input)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["ab", "cd"])
    fun `test 'ab or cd' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        val rsmState1 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )

        nonterminalS.startState = rsmState0

        rsmState0.addEdge(symbol = Terminal("ab"), head = rsmState1)
        rsmState0.addEdge(symbol = Terminal("cd"), head = rsmState1)

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0
        var pos = 0

        inputGraph.addVertex(curVertexId)
        while (pos < input.length) {
            var label: String
            if (input.startsWith("ab", pos)) {
                pos += 2
                label = "ab"
            } else if (input.startsWith("cd", pos)) {
                pos += 2
                label = "cd"
            } else {
                pos += 1
                label = input[pos].toString()
            }
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(label)), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["", "a"])
    fun `test 'a-optional' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        val rsmState1 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )

        nonterminalS.startState = rsmState0

        rsmState0.addEdge(symbol = Terminal("a"), head = rsmState1)

        val inputGraph = getTokenStream(input)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["abc"])
    fun `test 'abc' ambiguous hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val nonterminalA = Nonterminal("A")
        val nonterminalB = Nonterminal("B")
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RsmState(
            nonterminal = nonterminalS,
        )
        val rsmState2 = RsmState(
            nonterminal = nonterminalS,
        )
        val rsmState3 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState4 = RsmState(
            nonterminal = nonterminalS,
        )
        val rsmState5 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState6 = RsmState(
            nonterminal = nonterminalA,
            isStart = true,
        )
        nonterminalA.startState = rsmState6
        val rsmState7 = RsmState(
            nonterminal = nonterminalA,
        )
        val rsmState8 = RsmState(
            nonterminal = nonterminalA,
            isFinal = true,
        )
        val rsmState9 = RsmState(
            nonterminal = nonterminalB,
            isStart = true,
        )
        nonterminalB.startState = rsmState9
        val rsmState10 = RsmState(
            nonterminal = nonterminalB,
            isFinal = true,
        )

        rsmState0.addEdge(symbol = Terminal("a"), head = rsmState1)
        rsmState1.addEdge(symbol = nonterminalB, head = rsmState2)
        rsmState2.addEdge(symbol = Terminal("c"), head = rsmState3)
        rsmState0.addEdge(symbol = nonterminalA, head = rsmState4)
        rsmState4.addEdge(symbol = Terminal("c"), head = rsmState5)

        rsmState6.addEdge(symbol = Terminal("a"), head = rsmState7)
        rsmState7.addEdge(symbol = Terminal("b"), head = rsmState8)

        rsmState9.addEdge(symbol = Terminal("b"), head = rsmState10)

        val inputGraph = getTokenStream(input)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["ab", "cd"])
    fun `test 'ab or cd' ambiguous hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val nonterminalA = Nonterminal("A")
        val nonterminalB = Nonterminal("B")

        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState2 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState3 = RsmState(
            nonterminal = nonterminalA,
            isStart = true,
        )
        nonterminalA.startState = rsmState3
        val rsmState4 = RsmState(
            nonterminal = nonterminalA,
            isFinal = true,
        )
        val rsmState5 = RsmState(
            nonterminal = nonterminalA,
            isFinal = true,
        )
        val rsmState6 = RsmState(
            nonterminal = nonterminalB,
            isStart = true,
        )
        nonterminalB.startState = rsmState6
        val rsmState7 = RsmState(nonterminal = nonterminalB, isFinal = true)
        val rsmState8 = RsmState(
            nonterminal = nonterminalB,
            isFinal = true,
        )

        rsmState0.addEdge(symbol = nonterminalA, head = rsmState1)
        rsmState0.addEdge(symbol = nonterminalB, head = rsmState2)
        rsmState3.addEdge(symbol = Terminal("ab"), head = rsmState4)
        rsmState3.addEdge(symbol = Terminal("cd"), head = rsmState5)
        rsmState6.addEdge(symbol = Terminal("ab"), head = rsmState7)
        rsmState6.addEdge(symbol = Terminal("cd"), head = rsmState8)

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0
        var pos = 0

        inputGraph.addVertex(curVertexId)

        while (pos < input.length) {
            var label: String
            if (input.startsWith("ab", pos)) {
                pos += 2
                label = "ab"
            } else if (input.startsWith("cd", pos)) {
                pos += 2
                label = "cd"
            } else {
                pos += 1
                label = input[pos].toString()
            }
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(label)), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }

        inputGraph.addStartVertex(0)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["a", "ab", "abb", "abbb", "abbbb", "abbbbb"])
    fun `test 'a(b)-star' left recursive hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val nonterminalA = Nonterminal("A")

        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0

        val rsmState1 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState2 = RsmState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState3 = RsmState(
            nonterminal = nonterminalA,
            isStart = true,
            isFinal = true,
        )
        nonterminalA.startState = rsmState3
        val rsmState4 = RsmState(
            nonterminal = nonterminalA,
            isFinal = true,
        )

        rsmState0.addEdge(symbol = Terminal("a"), head = rsmState1)
        rsmState1.addEdge(symbol = nonterminalA, head = rsmState2)
        rsmState3.addEdge(symbol = Terminal("b"), head = rsmState4)

        rsmState4.addEdge(symbol = nonterminalA, head = rsmState3)

        val inputGraph = getTokenStream(input)

        assertNotNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }
}

