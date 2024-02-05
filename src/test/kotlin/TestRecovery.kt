import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.srcgll.Gll
import org.srcgll.RecoveryMode
import org.srcgll.rsm.RsmState

import org.srcgll.rsm.readRsmFromTxt
import org.srcgll.sppf.buildStringFromSppf
import java.io.IOException
import kotlin.io.path.Path
import kotlin.test.assertNotNull

const val pathToGrammars = "/cli/TestRSMReadWriteTXT/"

fun getRsm(fileName: String): RsmState {
    val fullName = pathToGrammars + fileName
    val url = object {}.javaClass.getResource(fullName) ?: throw IOException("Cannot find $fullName in project resources")
    return readRsmFromTxt(Path(url.path))
}

fun performTest(startState: RsmState, input: String, expectedWeight: Int) {
    val inputGraph = getTokenStream(input)
    val result = Gll(startState, inputGraph, recovery = RecoveryMode.ON).parse()
    val recoveredString = buildStringFromSppf(result.first!!)
    val recoveredInputGraph = getTokenStream(recoveredString)

    assert(result.first!!.weight == expectedWeight)
    assertNotNull(Gll(startState, recoveredInputGraph, recovery = RecoveryMode.OFF).parse().first)
}

class TestRecovery {
    @ParameterizedTest
    @MethodSource("test_1")
    fun `test BracketStarX grammar`(input: String, weight: Int) {
        val startState = getRsm("bracket_star_x.txt")
        performTest(startState, input, weight)
    }
    @ParameterizedTest
    @MethodSource("test_2")
    fun `test CAStarBStar grammar`(input: String, weight: Int) {
        val startState = getRsm("c_a_star_b_star.txt")
        performTest(startState, input, weight)
    }
    @ParameterizedTest
    @MethodSource("test_3")
    fun `test AB grammar`(input: String, weight: Int) {
        val startState = getRsm("ab.txt")
        performTest(startState, input, weight)
    }

    @ParameterizedTest
    @MethodSource("test_4")
    fun `test Dyck grammar`(input: String, weight: Int) {
        val startState = getRsm("dyck.txt")
        performTest(startState, input, weight)
    }

    @ParameterizedTest
    @MethodSource("test_5")
    fun `test Ambiguous grammar`(input: String, weight: Int) {
        val startState = getRsm("ambiguous.txt")
        performTest(startState, input, weight)
    }

    @ParameterizedTest
    @MethodSource("test_6")
    fun `test MultiDyck grammar`(input: String, weight: Int) {
        val startState = getRsm("multi_dyck.txt")
        performTest(startState, input, weight)
    }

    @ParameterizedTest
    @MethodSource("test_7")
    fun `test SimpleGolang grammar`(input: String, weight: Int) {
        val startState = getRsm("simple_golang.txt")
        performTest(startState, input, weight)
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