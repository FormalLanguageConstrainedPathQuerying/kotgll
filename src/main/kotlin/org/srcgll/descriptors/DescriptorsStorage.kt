package org.srcgll.descriptors

import org.srcgll.exceptions.ParsingException


open class DescriptorsStorage<VertexType>{
    protected val handledDescriptors = HashMap<VertexType, HashSet<Descriptor<VertexType>>>()
    protected val defaultDescriptorsStorage = ArrayDeque<Descriptor<VertexType>>()

    fun defaultDescriptorsStorageIsEmpty() = defaultDescriptorsStorage.isEmpty()

    open fun addToHandling(descriptor: Descriptor<VertexType>) {
        if (!isAlreadyHandled(descriptor)) {
            defaultDescriptorsStorage.addLast(descriptor)
        }
    }

    open fun next(): Descriptor<VertexType> {
        if (defaultDescriptorsStorageIsEmpty()) {
            throw ParsingException("Access to empty descriptor storage")
        }
        return defaultDescriptorsStorage.removeLast()
    }

    fun isAlreadyHandled(descriptor: Descriptor<VertexType>): Boolean {
        val handledDescriptor = descriptor.gssNode.handledDescriptors.find { descriptor.hashCode() == it.hashCode() }

        return handledDescriptor != null && handledDescriptor.weight() <= descriptor.weight()
    }

    fun addToHandled(descriptor: Descriptor<VertexType>) {
        descriptor.gssNode.handledDescriptors.add(descriptor)

        if (!handledDescriptors.containsKey(descriptor.inputPosition)) {
            handledDescriptors[descriptor.inputPosition] = HashSet()
        }

        handledDescriptors.getValue(descriptor.inputPosition).add(descriptor)
    }

    fun removeFromHandled(descriptor: Descriptor<VertexType>) {
        descriptor.gssNode.handledDescriptors.remove(descriptor)

        if (handledDescriptors.containsKey(descriptor.inputPosition)) {
            handledDescriptors.getValue(descriptor.inputPosition).remove(descriptor)
        }
    }

    /**
     * Process already handled descriptors again, since after changes in input descriptor configuration has changed
     */
    fun restoreDescriptors(vertex: VertexType) {
        handledDescriptors.getOrDefault(vertex, HashSet()).forEach { descriptor ->
            descriptor.gssNode.handledDescriptors.remove(descriptor)
            addToHandling(descriptor)
        }
        handledDescriptors.remove(vertex)
    }
}

