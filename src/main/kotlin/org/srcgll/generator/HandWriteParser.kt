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
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.writeRsmToDot
import org.srcgll.sppf.Sppf
import org.srcgll.sppf.node.SppfNode
import org.srcgll.sppf.node.SymbolSppfNode


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
    override val sppf: Sppf<VertexType> = Sppf()

    private var _input: IGraph<VertexType, LabelType>? = null
    override var input: IGraph<VertexType, LabelType>
        get() = _input ?: throw ParserException("Input not initialized!")
        set(value) {
            _input = value
        }

    fun parseS(descriptor: Descriptor<VertexType>) {
        for(path in descriptor.rsmState.pathLabels){
            when(path){
                "" -> {}
                "A" -> {}
                "AB" -> {}
                "ABa" -> {}
                "ABaa" -> {}
                "AC" -> {}
                "ACa" -> {}
                "ACaB" -> {}
            }
        }
    }
    fun parseA(descriptor: Descriptor<VertexType>) {
        for(path in descriptor.rsmState.pathLabels){
            when(path){
                "" -> {}
                "a" -> {}
                "aA" -> {}
            }
        }
    }
    fun parseB(descriptor: Descriptor<VertexType>) {
        for(path in descriptor.rsmState.pathLabels){
            when(path){
                "" -> {}
                "b" -> {}
                "bB" -> {}
            }
        }
    }
    fun parseC(descriptor: Descriptor<VertexType>) {
        for(path in descriptor.rsmState.pathLabels){
            when(path){
                "" -> {}
                "b" -> {}
                "bC" -> {}
            }
        }
    }

    val NtFuncs = hashMapOf<Nonterminal, (Descriptor<VertexType>) -> Unit>(
        grammar.A.getNonterminal()!! to ::parseA,
        grammar.B.getNonterminal()!! to ::parseB,
        grammar.C.getNonterminal()!! to ::parseC,
        grammar.S.getNonterminal()!! to ::parseS,
    )

    override fun parse(curDescriptor: Descriptor<VertexType>) {
        val nt = curDescriptor.rsmState.nonterminal
        val func = NtFuncs[nt] ?: throw ParserException("Nonterminal ${nt.name} is absent from the grammar!")

        func(curDescriptor)
        stack.addToHandled(curDescriptor)

    }

    fun updateSppf(curDescriptor: Descriptor<VertexType>){
        val state = curDescriptor.rsmState
        val pos = curDescriptor.inputPosition
//        val gssNode = curDescriptor.gssNode
        var curSppfNode = curDescriptor.sppfNode
        var leftExtent = curSppfNode?.leftExtent
        var rightExtent = curSppfNode?.rightExtent
//        val terminalEdges = state.getTerminalEdges()
//        val nonterminalEdges = state.getNonterminalEdges()
//
        if (state.isStart && state.isFinal) {
            curSppfNode = sppf.getNodeP(state, curSppfNode, sppf.getOrCreateItemSppfNode(state, pos, pos, weight = 0))
            leftExtent = curSppfNode.leftExtent
            rightExtent = curSppfNode.rightExtent
        }

        if (curSppfNode is SymbolSppfNode<VertexType> && state.nonterminal == startState.nonterminal
            && input.isStart(leftExtent!!) && input.isFinal(rightExtent!!)
        ) {
            if (parseResult == null || parseResult!!.weight > curSppfNode.weight) {
                parseResult = curSppfNode
            }

            val pair = Pair(leftExtent, rightExtent)
            val distance = sppf.minDistance(curSppfNode)

            reachabilityPairs[pair] =
                if (reachabilityPairs.containsKey(pair)) {
                    minOf(distance, reachabilityPairs[pair]!!)
                } else {
                    distance
                }
        }

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