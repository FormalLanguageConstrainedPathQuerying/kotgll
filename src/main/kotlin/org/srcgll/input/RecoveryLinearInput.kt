package org.ucfs.input

import org.ucfs.rsm.symbol.Term

class RecoveryLinearInput<VertexType, LabelType : ILabel>
    : LinearInput<VertexType, LabelType>(), IRecoveryInputGraph<VertexType, LabelType> {
    companion object {
        /**
         * Split CharSequence into stream of strings, separated by space symbol
         */
        fun buildFromString(input: String): IRecoveryInputGraph<Int, LinearInputLabel> {
            val inputGraph = RecoveryLinearInput<Int, LinearInputLabel>()
            var curVertexId = 0

            inputGraph.addStartVertex(curVertexId)
            inputGraph.addVertex(curVertexId)

            for (x in input.trim().split(' ')) {
                if (x.isNotEmpty()) {
                    inputGraph.addEdge(curVertexId, LinearInputLabel(Term(x)), ++curVertexId)
                    inputGraph.addVertex(curVertexId)
                }
            }
            return inputGraph
        }
    }
}