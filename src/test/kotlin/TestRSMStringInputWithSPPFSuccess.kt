import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.srcgll.GLL
import org.srcgll.RecoveryMode
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.rsm.RSMNonterminalEdge
import org.srcgll.rsm.RSMState
import org.srcgll.rsm.RSMTerminalEdge
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import kotlin.test.assertNotNull

class TestRSMStringInputWithSPPFSuccess {
    @Test
    fun `test 'empty' hand-crafted grammar`() {
        val nonterminalS = Nonterminal("S")
        val input = ""
        val rsmState0 = RSMState(
            id = 0,
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
            id = 0,
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        rsmState0.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("a"), head = RSMState(
                    id = 1,
                    nonterminal = nonterminalS,
                    isFinal = true,
                )
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
            id = 0,
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            id = 1,
            nonterminal = nonterminalS,
        )
        rsmState0.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("a"),
                head = rsmState1,
            )
        )
        rsmState1.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("b"), head = RSMState(
                    id = 2,
                    nonterminal = nonterminalS,
                    isFinal = true,
                )
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
            id = 0,
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            id = 1,
            nonterminal = nonterminalS,
            isFinal = true,
        )
        rsmState0.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("a"),
                head = rsmState1,
            )
        )
        rsmState1.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("a"),
                head = rsmState1,
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
    @ValueSource(strings = ["a", "aa", "aaa", "aaaa", "aaaaa", "aaaaaa", "aaaaaaa"])
    fun `test 'a-plus' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RSMState(
            id = 0,
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            id = 1,
            nonterminal = nonterminalS,
            isFinal = true,
        )
        rsmState0.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("a"),
                head = rsmState1,
            )
        )
        rsmState1.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("a"),
                head = rsmState1,
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
    @ValueSource(strings = ["", "ab", "abab", "ababab", "abababab", "ababababab"])
    fun `test '(ab)-star' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RSMState(
            id = 0,
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            id = 1,
            nonterminal = nonterminalS,
            isFinal = true,
        )
        rsmState0.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("ab"),
                head = rsmState1,
            )
        )
        rsmState1.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("ab"),
                head = rsmState1,
            )
        )

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
            id = 0,
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            id = 1,
            nonterminal = nonterminalS,
        )
        val rsmState2 = RSMState(
            id = 2,
            nonterminal = nonterminalS,
        )
        val rsmState3 = RSMState(
            id = 3,
            nonterminal = nonterminalS,
        )
        val rsmState4 = RSMState(
            id = 4,
            nonterminal = nonterminalS,
            isFinal = true,
        )

        rsmState0.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("("),
                head = rsmState1,
            )
        )
        rsmState1.addNonterminalEdge(
            RSMNonterminalEdge(
                nonterminal = nonterminalS,
                head = rsmState2,
            )
        )
        rsmState2.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal(")"),
                head = rsmState3,
            )
        )
        rsmState3.addNonterminalEdge(
            RSMNonterminalEdge(
                nonterminal = nonterminalS,
                head = rsmState4,
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
    @ValueSource(strings = ["ab", "cd"])
    fun `test 'ab or cd' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RSMState(
            id = 0,
            nonterminal = nonterminalS,
            isStart = true,
        )
        val rsmState1 = RSMState(
            id = 1,
            nonterminal = nonterminalS,
            isFinal = true,
        )

        nonterminalS.startState = rsmState0

        rsmState0.addTerminalEdge(RSMTerminalEdge(terminal = Terminal("ab"), head = rsmState1))
        rsmState0.addTerminalEdge(RSMTerminalEdge(terminal = Terminal("cd"), head = rsmState1))

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
            id = 0,
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        val rsmState1 = RSMState(
            id = 1,
            nonterminal = nonterminalS,
            isFinal = true,
        )

        nonterminalS.startState = rsmState0

        rsmState0.addTerminalEdge(RSMTerminalEdge(terminal = Terminal("a"), head = rsmState1))

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
            id = 0,
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            id = 1,
            nonterminal = nonterminalS,
        )
        val rsmState2 = RSMState(
            id = 2,
            nonterminal = nonterminalS,
        )
        val rsmState3 = RSMState(
            id = 3,
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState4 = RSMState(
            id = 4,
            nonterminal = nonterminalS,
        )
        val rsmState5 = RSMState(
            id = 5,
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState6 = RSMState(
            id = 6,
            nonterminal = nonterminalA,
            isStart = true,
        )
        nonterminalA.startState = rsmState6
        val rsmState7 = RSMState(
            id = 7,
            nonterminal = nonterminalA,
        )
        val rsmState8 = RSMState(
            id = 8,
            nonterminal = nonterminalA,
            isFinal = true,
        )
        val rsmState9 = RSMState(
            id = 9,
            nonterminal = nonterminalB,
            isStart = true,
        )
        nonterminalB.startState = rsmState9
        val rsmState10 = RSMState(
            id = 10,
            nonterminal = nonterminalB,
            isFinal = true,
        )

        rsmState0.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("a"),
                head = rsmState1,
            )
        )
        rsmState1.addNonterminalEdge(
            RSMNonterminalEdge(
                nonterminal = nonterminalB,
                head = rsmState2,
            )
        )
        rsmState2.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("c"),
                head = rsmState3,
            )
        )
        rsmState0.addNonterminalEdge(
            RSMNonterminalEdge(
                nonterminal = nonterminalA,
                head = rsmState4,
            )
        )
        rsmState4.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("c"),
                head = rsmState5,
            )
        )

        rsmState6.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("a"),
                head = rsmState7,
            )
        )
        rsmState7.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("b"),
                head = rsmState8,
            )
        )

        rsmState9.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("b"),
                head = rsmState10,
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
    @ValueSource(strings = ["ab", "cd"])
    fun `test 'ab or cd' ambiguous hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val nonterminalA = Nonterminal("A")
        val nonterminalB = Nonterminal("B")

        val rsmState0 = RSMState(
            id = 0,
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RSMState(
            id = 1,
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState2 = RSMState(
            id = 2,
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState3 = RSMState(
            id = 3,
            nonterminal = nonterminalA,
            isStart = true,
        )
        nonterminalA.startState = rsmState3
        val rsmState4 = RSMState(
            id = 4,
            nonterminal = nonterminalA,
            isFinal = true,
        )
        val rsmState5 = RSMState(
            id = 5,
            nonterminal = nonterminalA,
            isFinal = true,
        )
        val rsmState6 = RSMState(
            id = 6,
            nonterminal = nonterminalB,
            isStart = true,
        )
        nonterminalB.startState = rsmState6
        val rsmState7 = RSMState(id = 7, nonterminal = nonterminalB, isFinal = true)
        val rsmState8 = RSMState(
            id = 8,
            nonterminal = nonterminalB,
            isFinal = true,
        )

        rsmState0.addNonterminalEdge(
            RSMNonterminalEdge(
                nonterminal = nonterminalA,
                head = rsmState1,
            )
        )
        rsmState0.addNonterminalEdge(
            RSMNonterminalEdge(
                nonterminal = nonterminalB,
                head = rsmState2,
            )
        )
        rsmState3.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("ab"),
                head = rsmState4,
            )
        )
        rsmState3.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("cd"),
                head = rsmState5,
            )
        )
        rsmState6.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("ab"),
                head = rsmState7,
            )
        )
        rsmState6.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("cd"),
                head = rsmState8,
            )
        )

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
            id = 0,
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0

        val rsmState1 = RSMState(
            id = 1,
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState2 = RSMState(
            id = 2,
            nonterminal = nonterminalS,
            isFinal = true,
        )
        val rsmState3 = RSMState(
            id = 3,
            nonterminal = nonterminalA,
            isStart = true,
            isFinal = true,
        )
        nonterminalA.startState = rsmState3
        val rsmState4 = RSMState(
            id = 4,
            nonterminal = nonterminalA,
            isFinal = true,
        )

        rsmState0.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("a"),
                head = rsmState1,
            )
        )
        rsmState1.addNonterminalEdge(
            RSMNonterminalEdge(
                nonterminal = nonterminalA,
                head = rsmState2,
            )
        )
        rsmState3.addTerminalEdge(
            RSMTerminalEdge(
                terminal = Terminal("b"), head = rsmState4
            )
        )

        rsmState4.addNonterminalEdge(
            RSMNonterminalEdge(
                nonterminal = nonterminalA,
                head = rsmState3,
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
}

