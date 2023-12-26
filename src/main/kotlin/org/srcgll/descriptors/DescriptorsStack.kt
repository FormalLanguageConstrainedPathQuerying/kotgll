package org.srcgll.descriptors

import org.srcgll.gss.GssNode
import org.srcgll.rsm.RsmState
import org.srcgll.sppf.node.SppfNode

class Descriptor<VertexType>(
    val rsmState: RsmState,
    val gssNode: GssNode<VertexType>,
    val sppfNode: SppfNode<VertexType>?,
    val inputPosition: VertexType,
) {
    val hashCode = 23 * (23 * (23 * 17 + rsmState.hashCode()) + inputPosition.hashCode()) + gssNode.hashCode()

    fun weight(): Int = (sppfNode?.weight ?: 0) + gssNode.minWeightOfLeftPart

    override fun hashCode() = hashCode

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Descriptor<*>) return false
        if (other.rsmState != rsmState) return false
        if (other.gssNode != gssNode) return false
        if (other.sppfNode != sppfNode) return false
        if (other.inputPosition != inputPosition) return false

        return true
    }
}

interface IDescriptorsStack<VertexType> {
    fun defaultDescriptorsStackIsEmpty(): Boolean
    fun add(descriptor: Descriptor<VertexType>)
    fun recoverDescriptors(vertex: VertexType)
    fun next(): Descriptor<VertexType>
    fun isAlreadyHandled(descriptor: Descriptor<VertexType>): Boolean
    fun addToHandled(descriptor: Descriptor<VertexType>)
    fun removeFromHandled(descriptor: Descriptor<VertexType>)
}

class ErrorRecoveringDescriptorsStack<VertexType> : IDescriptorsStack<VertexType> {
    private val handledDescriptors = HashMap<VertexType, HashSet<Descriptor<VertexType>>>()
    private val defaultDescriptorsStack = ArrayDeque<Descriptor<VertexType>>()
    private val errorRecoveringDescriptorsStacks = LinkedHashMap<Int, ArrayDeque<Descriptor<VertexType>>>()

    override fun defaultDescriptorsStackIsEmpty() = defaultDescriptorsStack.isEmpty()

    override fun add(descriptor: Descriptor<VertexType>) {
        if (!isAlreadyHandled(descriptor)) {
            val pathWeight = descriptor.weight()

            if (pathWeight == 0) {
                defaultDescriptorsStack.addLast(descriptor)
            } else {
                if (!errorRecoveringDescriptorsStacks.containsKey(pathWeight)) {
                    errorRecoveringDescriptorsStacks[pathWeight] = ArrayDeque()
                }

                errorRecoveringDescriptorsStacks.getValue(pathWeight).addLast(descriptor)
            }
        }
    }

    override fun recoverDescriptors(vertex: VertexType) {
        handledDescriptors.getOrDefault(vertex, HashSet()).forEach { descriptor ->
            descriptor.gssNode.handledDescriptors.remove(descriptor)
            add(descriptor)
        }
        handledDescriptors.remove(vertex)
    }

    override fun next(): Descriptor<VertexType> {
        if (defaultDescriptorsStackIsEmpty()) {
            val iterator = errorRecoveringDescriptorsStacks.keys.iterator()
            val currentMin = iterator.next()
            val result = errorRecoveringDescriptorsStacks.getValue(currentMin).removeLast()

            if (errorRecoveringDescriptorsStacks.getValue(currentMin).isEmpty()) {
                errorRecoveringDescriptorsStacks.remove(currentMin)
            }

            return result
        }
        return defaultDescriptorsStack.removeLast()
    }

    override fun isAlreadyHandled(descriptor: Descriptor<VertexType>): Boolean {
        val handledDescriptor = descriptor.gssNode.handledDescriptors.find { descriptor.hashCode() == it.hashCode() }

        return handledDescriptor != null && handledDescriptor.weight() <= descriptor.weight()
    }

    override fun addToHandled(descriptor: Descriptor<VertexType>) {
        descriptor.gssNode.handledDescriptors.add(descriptor)

        if (!handledDescriptors.containsKey(descriptor.inputPosition)) {
            handledDescriptors[descriptor.inputPosition] = HashSet()
        }

        handledDescriptors.getValue(descriptor.inputPosition).add(descriptor)
    }

    override fun removeFromHandled(descriptor: Descriptor<VertexType>) {
        descriptor.gssNode.handledDescriptors.remove(descriptor)

        if (handledDescriptors.containsKey(descriptor.inputPosition)) {
            handledDescriptors.getValue(descriptor.inputPosition).remove(descriptor)
        }
    }
}

