package org.srcgll.generator

import GllParser
import org.srcgll.RecoveryMode
import org.srcgll.descriptors.Descriptor
import org.srcgll.descriptors.ErrorRecoveringDescriptorsStack
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.or
import org.srcgll.grammar.combinator.regexp.times
import org.srcgll.gss.GssNode
import org.srcgll.input.IGraph
import org.srcgll.input.ILabel
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.writeRsmToDot
import org.srcgll.sppf.node.SppfNode


/**
 * hand-write parser for @GrammarImpl
 *
 */
class HandWriteParser<VertexType, LabelType : ILabel>(override val recovery: RecoveryMode = RecoveryMode.OFF) :
    GllParser<VertexType, LabelType> {
    val grammar = GrammarImpl()
    override val startState: RsmState = grammar.getRsm()
    override val stack = ErrorRecoveringDescriptorsStack<VertexType>()
    override val createdGssNodes: HashMap<GssNode<VertexType>, GssNode<VertexType>> = HashMap()
    override var parseResult: SppfNode<VertexType>? = null
    override val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int> = HashMap()
    private var _input: IGraph<VertexType, LabelType>? = null
    override var input: IGraph<VertexType, LabelType>
        get() = _input ?: throw ParserException("Input not initialized!")
        set(value) {
            _input = value
        }

    override fun parse(): Pair<SppfNode<VertexType>?, HashMap<Pair<VertexType, VertexType>, Int>> {
        TODO("Not yet implemented")
    }

    override fun parse(curDescriptor: Descriptor<VertexType>) {
        TODO("Not yet implemented")
    }

}

class ParserException(msg: String? = null) : Exception(msg) {}


fun main() {
    val p = HandWriteParser<Int, LinearInputLabel>()
    p.input = LinearInput()
    val q = HandWriteParser<Int, LinearInputLabel>()
    val g = GrammarImpl()
    writeRsmToDot(g.getRsm(), "gen/impl.dot")
}

/**
Grammar for
 *  S = A C a B | A B a a
 *  A = a A | a
 *  B = b B | b
 *  C = b C | b
 */
class GrammarImpl() : Grammar() {
    var S by Nt()
    var A by Nt()
    var B by Nt()
    var C by Nt()
    val a = Term("a")
    val b = Term("b")

    init {
        setStart(S)

        S = A * C * a * B or A * B * a * a
        A = a * A or a
        B = b * B or b
        C = b * C or b


    }
}