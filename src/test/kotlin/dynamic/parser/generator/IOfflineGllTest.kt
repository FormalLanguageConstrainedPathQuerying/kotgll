package dynamic.parser.generator

import dynamic.parser.IDynamicGllTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.srcgll.generators.parser.GeneratedParser
import org.srcgll.input.IInputGraph
import org.srcgll.input.ILabel
import kotlin.test.assertNotNull

interface IOfflineGllTest : IDynamicGllTest {
    /**
     * Test for any type of incorrect input
     * Parametrize parser with it's input before parsing
     */
    fun <VertexType, LabelType : ILabel> getIncorrectTestContainer(
        caseName: String,
        gll: GeneratedParser<VertexType, LabelType>,
        input: IInputGraph<VertexType, LabelType>
    ): DynamicNode {
        return DynamicTest.dynamicTest("[fail] $caseName") {
            testError(gll, input)
        }
    }

    fun <VertexType, LabelType : ILabel> testError(
        gll: GeneratedParser<VertexType, LabelType>,
        input: IInputGraph<VertexType, LabelType>
    ) {
        gll.input = input
        val result = gll.parse().first
        Assertions.assertNull(result)
    }

    /**
     * Test for any type of correct input
     * Parametrize parser with it's input before parsing
     */
    fun <VertexType, LabelType : ILabel> getCorrectTestContainer(
        caseName: String,
        gll: GeneratedParser<VertexType, LabelType>,
        input: IInputGraph<VertexType, LabelType>
    ): DynamicNode {
        return DynamicTest.dynamicTest("[ok] $caseName") {
            testSuccess(gll, input)
        }
    }

    fun <VertexType, LabelType : ILabel> testSuccess(
        gll: GeneratedParser<VertexType, LabelType>,
        input: IInputGraph<VertexType, LabelType>
    ) {
        gll.input = input
        val result = gll.parse()
        if (result.first == null) {
            System.err.println("input: $input")
        }
        //TODO add check for parsing result quality
        assertNotNull(result.first)
    }

}