import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.srcgll.GLL
import org.srcgll.RecoveryMode
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import kotlin.test.assertNotNull

class TestRSMStringInputWithSPPFSuccess {
    @Test
    fun `test 'empty' hand-crafted grammar`() {
        val nonterminalS = Nonterminal("S")
        val input = ""
        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @Test
    fun `test 'a' hand-crafted grammar`() {
        val nonterminalS = Nonterminal("S")
        val input = "a"
        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        rsmState0.addEdge(
            symbol = Terminal("a"), destState = RSMState(
                nonterminal = nonterminalS,
                isFinal = true,
            )
        )

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @Test
    fun `test 'ab' hand-crafted grammar`() {
        val nonterminalS = Nonterminal("S")
        val input = "ab"
        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            nonterminal = nonterminalS,
        )
        rsmState0.addEdge(symbol = Terminal("a"), destState = rsmState1)
        rsmState1.addEdge(
            symbol = Terminal("b"), destState = RSMState(
                nonterminal = nonterminalS, isFinal = true
            )
        )

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["", "a", "aa", "aaa", "aaaa", "aaaaa", "aaaaaa", "aaaaaaa"])
    fun `test 'a-star' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        rsmState0.addEdge(symbol = Terminal("a"), destState = rsmState1)
        rsmState1.addEdge(symbol = Terminal("a"), destState = rsmState1)

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["a", "aa", "aaa", "aaaa", "aaaaa", "aaaaaa", "aaaaaaa"])
    fun `test 'a-plus' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        rsmState0.addEdge(symbol = Terminal("a"), destState = rsmState1)
        rsmState1.addEdge(symbol = Terminal("a"), destState = rsmState1)

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["", "ab", "abab", "ababab", "abababab", "ababababab"])
    fun `test '(ab)-star' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        rsmState0.addEdge(symbol = Terminal("ab"), destState = rsmState1)
        rsmState1.addEdge(symbol = Terminal("ab"), destState = rsmState1)

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

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(
        strings = ["", "()", "()()", "()()()", "(())", "(())()", "(())()()", "(())(())", "(())(())()", "(())(())()()", "(()())(()())", "((()))", "(((())))", "((((()))))", "()()((()))(()())", "(((()()())()()())()()())"]
    )
    fun `test 'dyck' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            nonterminal = nonterminalS,
        )
        val rsmState2 = RSMState(
            nonterminal = nonterminalS,
        )
        val rsmState3 = RSMState(
            nonterminal = nonterminalS,
        )
        val rsmState4 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )

        rsmState0.addEdge(symbol = Terminal("("), destState = rsmState1)
        rsmState1.addEdge(symbol = nonterminalS, destState = rsmState2)
        rsmState2.addEdge(symbol = Terminal(")"), destState = rsmState3)
        rsmState3.addEdge(symbol = nonterminalS, destState = rsmState4)

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["ab", "cd"])
    fun `test 'ab or cd' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        val rsmState1 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )

        nonterminalS.startState = rsmState0

        rsmState0.addEdge(symbol = Terminal("ab"), destState = rsmState1)
        rsmState0.addEdge(symbol = Terminal("cd"), destState = rsmState1)

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

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["", "a"])
    fun `test 'a-optional' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        val rsmState1 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )

        nonterminalS.startState = rsmState0

        rsmState0.addEdge(symbol = Terminal("a"), destState = rsmState1)

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["abc"])
    fun `test 'abc' ambiguous hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val nonterminalA = Nonterminal("A")
        val nonterminalB = Nonterminal("B")
        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            nonterminal = nonterminalS,
        )
        val rsmState2 = RSMState(
            nonterminal = nonterminalS,
        )
        val rsmState3 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState4 = RSMState(
            nonterminal = nonterminalS,
        )
        val rsmState5 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState6 = RSMState(
            nonterminal = nonterminalA,
            isStart = true,
        )
        nonterminalA.startState = rsmState6
        val rsmState7 = RSMState(
            nonterminal = nonterminalA,
        )
        val rsmState8 = RSMState(
            nonterminal = nonterminalA,
            isFinal = true,
        )
        val rsmState9 = RSMState(
            nonterminal = nonterminalB,
            isStart = true,
        )
        nonterminalB.startState = rsmState9
        val rsmState10 = RSMState(
            nonterminal = nonterminalB,
            isFinal = true,
        )

        rsmState0.addEdge(symbol = Terminal("a"), destState = rsmState1)
        rsmState1.addEdge(symbol = nonterminalB, destState = rsmState2)
        rsmState2.addEdge(symbol = Terminal("c"), destState = rsmState3)
        rsmState0.addEdge(symbol = nonterminalA, destState = rsmState4)
        rsmState4.addEdge(symbol = Terminal("c"), destState = rsmState5)

        rsmState6.addEdge(symbol = Terminal("a"), destState = rsmState7)
        rsmState7.addEdge(symbol = Terminal("b"), destState = rsmState8)

        rsmState9.addEdge(symbol = Terminal("b"), destState = rsmState10)

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["ab", "cd"])
    fun `test 'ab or cd' ambiguous hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val nonterminalA = Nonterminal("A")
        val nonterminalB = Nonterminal("B")

        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState2 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState3 = RSMState(
            nonterminal = nonterminalA,
            isStart = true,
        )
        nonterminalA.startState = rsmState3
        val rsmState4 = RSMState(
            nonterminal = nonterminalA,
            isFinal = true,
        )
        val rsmState5 = RSMState(
            nonterminal = nonterminalA,
            isFinal = true,
        )
        val rsmState6 = RSMState(
            nonterminal = nonterminalB,
            isStart = true,
        )
        nonterminalB.startState = rsmState6
        val rsmState7 = RSMState(nonterminal = nonterminalB, isFinal = true)
        val rsmState8 = RSMState(
            nonterminal = nonterminalB,
            isFinal = true,
        )

        rsmState0.addEdge(symbol = nonterminalA, destState = rsmState1)
        rsmState0.addEdge(symbol = nonterminalB, destState = rsmState2)
        rsmState3.addEdge(symbol = Terminal("ab"), destState = rsmState4)
        rsmState3.addEdge(symbol = Terminal("cd"), destState = rsmState5)
        rsmState6.addEdge(symbol = Terminal("ab"), destState = rsmState7)
        rsmState6.addEdge(symbol = Terminal("cd"), destState = rsmState8)

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

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be NotNull for {0}")
    @ValueSource(strings = ["a", "ab", "abb", "abbb", "abbbb", "abbbbb"])
    fun `test 'a(b)-star' left recursive hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val nonterminalA = Nonterminal("A")

        val rsmState0 = RSMState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0

        val rsmState1 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState2 = RSMState(
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState3 = RSMState(
            nonterminal = nonterminalA,
            isStart = true,
            isFinal = true,
        )
        nonterminalA.startState = rsmState3
        val rsmState4 = RSMState(
            nonterminal = nonterminalA,
            isFinal = true,
        )

        rsmState0.addEdge(symbol = Terminal("a"), destState = rsmState1)
        rsmState1.addEdge(symbol = nonterminalA, destState = rsmState2)
        rsmState3.addEdge(symbol = Terminal("b"), destState = rsmState4)

        rsmState4.addEdge(symbol = nonterminalA, destState = rsmState3)

        val inputGraph = LinearInput<Int, LinearInputLabel>()
        var curVertexId = 0

        inputGraph.addVertex(curVertexId)
        for (x in input) {
            inputGraph.addEdge(curVertexId, LinearInputLabel(Terminal(x.toString())), ++curVertexId)
            inputGraph.addVertex(curVertexId)
        }
        inputGraph.addStartVertex(0)

        assertNotNull(GLL(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }
}

