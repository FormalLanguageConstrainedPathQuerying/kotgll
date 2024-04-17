package org.ucfs.descriptors

import org.ucfs.parser.ParsingException

/**
 * Collection of default descriptors
 * @param VertexType - type of vertex in input graph
 */
open class DescriptorsStorage<VertexType>{
    /**
     * Collection of already handled descriptors, accessible via descriptor's hashcode
     */
    protected val handledDescriptors = HashMap<VertexType, HashSet<Descriptor<VertexType>>>()

    /**
     * Collection of descriptors with zero weight
     */
    protected val defaultDescriptorsStorage = ArrayDeque<Descriptor<VertexType>>()

    /**
     * @return true if we have default descriptors to handle, false otherwise
     */
    fun defaultDescriptorsStorageIsEmpty() = defaultDescriptorsStorage.isEmpty()

    open fun addToHandling(descriptor: Descriptor<VertexType>) {
        if (!isAlreadyHandled(descriptor)) {
            defaultDescriptorsStorage.addLast(descriptor)
        }
    }

    /**
     * @return next default descriptor to handle
     */
    open fun next(): Descriptor<VertexType> {
        if (defaultDescriptorsStorageIsEmpty()) {
            throw ParsingException("Descriptor storage is empty")
        }
        return defaultDescriptorsStorage.removeLast()
    }

    /**
     * @param descriptor - descriptor to check
     * @return true if the descriptor was already processed, false otherwise
     */
    fun isAlreadyHandled(descriptor: Descriptor<VertexType>): Boolean {
        val handledDescriptor = descriptor.gssNode.handledDescriptors.find { descriptor.hashCode() == it.hashCode() }

        return handledDescriptor != null && handledDescriptor.weight <= descriptor.weight
    }

    fun addToHandled(descriptor: Descriptor<VertexType>) {
        descriptor.gssNode.handledDescriptors.add(descriptor)

        if (!handledDescriptors.containsKey(descriptor.inputPosition)) {
            handledDescriptors[descriptor.inputPosition] = HashSet()
        }

        handledDescriptors.getValue(descriptor.inputPosition).add(descriptor)
    }

    /**
     * Part of incrementality mechanism.
     * Remove descriptor from already handled to process them again
     * @param descriptor - descriptor to remove from handled
     */
    fun removeFromHandled(descriptor: Descriptor<VertexType>) {
        descriptor.gssNode.handledDescriptors.remove(descriptor)

        if (handledDescriptors.containsKey(descriptor.inputPosition)) {
            handledDescriptors.getValue(descriptor.inputPosition).remove(descriptor)
        }
    }

    /**
     * Part of incrementality mechanism.
     * Restore all descriptors which contain passed vertex as value of the corresponding field.
     * Add all such descriptors to process again
     * @param vertex - vertex in input graph
     */
    fun restoreDescriptors(vertex: VertexType) {
        handledDescriptors.getOrDefault(vertex, HashSet()).forEach { descriptor ->
            descriptor.gssNode.handledDescriptors.remove(descriptor)
            addToHandling(descriptor)
        }
        handledDescriptors.remove(vertex)
    }
}

