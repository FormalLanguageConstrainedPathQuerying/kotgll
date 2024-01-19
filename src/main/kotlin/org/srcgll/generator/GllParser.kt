import org.srcgll.RecoveryMode
import org.srcgll.descriptors.Descriptor
import org.srcgll.descriptors.IDescriptorsStack
import org.srcgll.gss.GssNode
import org.srcgll.input.IGraph
import org.srcgll.input.ILabel
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.sppf.node.SppfNode
import org.srcgll.sppf.node.SymbolSppfNode

/**
 * Interface for Gll parser with helper functions and main parsing loop
 */
interface GllParser<VertexType, LabelType : ILabel> {
    val startState: RsmState
    var input: IGraph<VertexType, LabelType>
    val stack: IDescriptorsStack<VertexType>
    val recovery: RecoveryMode
    val createdGssNodes: HashMap<GssNode<VertexType>, GssNode<VertexType>>
    var parseResult: SppfNode<VertexType>?
    val reachabilityPairs: HashMap<Pair<VertexType, VertexType>, Int>

    fun parse(): Pair<SppfNode<VertexType>?, HashMap<Pair<VertexType, VertexType>, Int>> {
        initDescriptors(input)

        // Continue parsing until all default descriptors processed
        while (!stack.defaultDescriptorsStackIsEmpty()) {
            parse(stack.next())
        }

        // If string was not parsed - process recovery descriptors until first valid parse tree is found
        // Due to the Error Recovery algorithm used it will be parse tree of the string with min editing cost
        if (recovery == RecoveryMode.ON) {
            while (parseResult == null) {
                parse(stack.next())
            }
        }
        return Pair(parseResult, reachabilityPairs)
    }

    fun parse(curDescriptor: Descriptor<VertexType>)

    fun initDescriptors(input: IGraph<VertexType, LabelType>) {
        for (startVertex in input.getInputStartVertices()) {
            val descriptor = Descriptor(
                startState,
                getOrCreateGssNode(startState.nonterminal, startVertex, weight = 0),
                sppfNode = null,
                startVertex
            )
            addDescriptor(descriptor)
        }
    }

    fun getOrCreateGssNode(
        nonterminal: Nonterminal,
        inputPosition: VertexType,
        weight: Int,
    ): GssNode<VertexType> {
        val gssNode = GssNode(nonterminal, inputPosition, weight)

        if (createdGssNodes.containsKey(gssNode)) {
            if (createdGssNodes.getValue(gssNode).minWeightOfLeftPart > weight) {
                createdGssNodes.getValue(gssNode).minWeightOfLeftPart = weight
            }
        } else createdGssNodes[gssNode] = gssNode

        return createdGssNodes.getValue(gssNode)
    }

    private fun addDescriptor(descriptor: Descriptor<VertexType>) {
        val sppfNode = descriptor.sppfNode
        val state = descriptor.rsmState
        val leftExtent = sppfNode?.leftExtent
        val rightExtent = sppfNode?.rightExtent

        if (parseResult == null && sppfNode is SymbolSppfNode<*> && state.nonterminal == startState.nonterminal && input.isStart(
                leftExtent!!
            ) && input.isFinal(rightExtent!!)
        ) {
            stack.removeFromHandled(descriptor)
        }
        stack.add(descriptor)
    }
}
