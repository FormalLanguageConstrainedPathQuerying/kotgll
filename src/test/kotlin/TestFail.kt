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
import kotlin.test.assertNull

class TestFail {
    @Test
    fun `test 'empty' hand-crafted grammar`() {
        val nonterminalS = Nonterminal("S")
        val input = "a"
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
            isFinal = true,
        )
        nonterminalS.startState = rsmState0

        val inputGraph = getTokenStream(input)

        assertNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be Null for {0}")
    @ValueSource(strings = ["", "b", "bb", "ab", "aa"])
    fun `test 'a' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
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

        assertNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be Null for {0}")
    @ValueSource(strings = ["", "a", "b", "aba", "ababa", "aa", "b", "bb", "c", "cc"])
    fun `test 'ab' hand-crafted grammar`(input: String) {
        val nonterminalS = Nonterminal("S")
        val rsmState0 = RsmState(
            nonterminal = nonterminalS,
            isStart = true,
        )
        nonterminalS.startState = rsmState0
        val rsmState1 = RsmState(
            nonterminal = nonterminalS,
        )
        rsmState0.addEdge(
            symbol = Terminal("a"),
            head = rsmState1,
        )
        rsmState1.addEdge(
            symbol = Terminal("b"), head = RsmState(
                nonterminal = nonterminalS,
                isFinal = true,
            )
        )

        val inputGraph = getTokenStream(input)

        assertNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be Null for {0}")
    @ValueSource(strings = ["b", "bb", "c", "cc", "ab", "ac"])
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
        rsmState0.addEdge(
            symbol = Terminal("a"),
            head = rsmState1,
        )
        rsmState1.addEdge(
            symbol = Terminal("a"),
            head = rsmState1,
        )

        val inputGraph = getTokenStream(input)

        assertNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be Null for {0}")
    @ValueSource(strings = ["", "b", "bb", "c", "cc", "ab", "ac"])
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
        rsmState0.addEdge(
            symbol = Terminal("a"),
            head = rsmState1,
        )
        rsmState1.addEdge(
            symbol = Terminal("a"),
            head = rsmState1,
        )

        val inputGraph = getTokenStream(input)

        assertNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be Null for {0}")
    @ValueSource(
        strings = [
            "abaa",
            "abba",
            "abca",
            "ababaa",
            "ababba",
            "ababca",
            "abbb",
            "abcb",
            "ababbb",
            "ababcb",
            "abac",
            "abbc",
            "abcc",
            "ababac",
            "ababbc",
            "ababcc",
        ]
    )
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
        rsmState0.addEdge(
            symbol = Terminal("ab"),
            head = rsmState1,
        )
        rsmState1.addEdge(
            symbol = Terminal("ab"),
            head = rsmState1,
        )

        val inputGraph = getTokenStream(input)

        assertNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be Null for {0}")
    @ValueSource(
        strings = [
            "(",
            ")",
            "((",
            "))",
            "()(",
            "()()(",
            "()()()(",
            "())",
            "()())",
            "()()())",
            "(())(",
            "(())()(",
            "(())()()(",
            "(()))",
            "(())())",
            "(())()())",
            "(())(())(",
            "(())(())()(",
            "(())(())()()(",
            "(())(()))",
            "(())(())())",
            "(())(())()())",
            "(()())(()())(",
            "(()())(()()))",
        ]
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

        rsmState0.addEdge(
            symbol = Terminal("("),
            head = rsmState1,
        )
        rsmState1.addEdge(
            symbol = nonterminalS,
            head = rsmState2,
        )
        rsmState2.addEdge(
            symbol = Terminal(")"),
            head = rsmState3,
        )
        rsmState3.addEdge(
            symbol = nonterminalS,
            head = rsmState4,
        )

        val inputGraph = getTokenStream(input)

        assertNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be Null for {0}")
    @ValueSource(
        strings = [
            "",
            "a",
            "b",
            "c",
            "d",
            "aa",
            "ac",
            "ad",
            "ba",
            "bb",
            "bc",
            "bd",
            "ca",
            "cb",
            "cc",
            "da",
            "db",
            "dc",
            "dd",
        ]
    )
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

        val inputGraph = getTokenStream(input)

        assertNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be Null for {0}")
    @ValueSource(strings = ["b", "bb", "ab"])
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

        assertNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be Null for {0}")
    @ValueSource(strings = ["", "a", "b", "c", "ab", "ac", "abb", "bc", "abcd"])
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

        rsmState0.addEdge(
            symbol = Terminal("a"),
            head = rsmState1,
        )
        rsmState1.addEdge(
            symbol = nonterminalB,
            head = rsmState2,
        )
        rsmState2.addEdge(
            symbol = Terminal("c"),
            head = rsmState3,
        )
        rsmState0.addEdge(
            symbol = nonterminalA,
            head = rsmState4,
        )
        rsmState4.addEdge(
            symbol = Terminal("c"),
            head = rsmState5,
        )

        rsmState6.addEdge(
            symbol = Terminal("a"),
            head = rsmState7,
        )
        rsmState7.addEdge(
            symbol = Terminal("b"),
            head = rsmState8,
        )

        rsmState9.addEdge(
            symbol = Terminal("b"),
            head = rsmState10,
        )

        val inputGraph = getTokenStream(input)

        assertNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }

    @ParameterizedTest(name = "Should be Null for {0}")
    @ValueSource(
        strings = [
            "",
            "a",
            "b",
            "c",
            "d",
            "aa",
            "ac",
            "ad",
            "ba",
            "bb",
            "bc",
            "bd",
            "ca",
            "cb",
            "cc",
            "da",
            "db",
            "dc",
            "dd",
        ]
    )
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

        rsmState0.addEdge(
            symbol = nonterminalA,
            head = rsmState1,
        )
        rsmState0.addEdge(
            symbol = nonterminalB,
            head = rsmState2,
        )
        rsmState3.addEdge(
            symbol = Terminal("ab"),
            head = rsmState4,
        )
        rsmState3.addEdge(
            symbol = Terminal("cd"),
            head = rsmState5,
        )
        rsmState6.addEdge(
            symbol = Terminal("ab"),
            head = rsmState7,
        )
        rsmState6.addEdge(
            symbol = Terminal("cd"),
            head = rsmState8,
        )

        val inputGraph = getTokenStream(input)

        assertNull(Gll(rsmState0, inputGraph, recovery = RecoveryMode.OFF).parse().first)
    }
}
