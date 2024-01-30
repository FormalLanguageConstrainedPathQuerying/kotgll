package org.srcgll.descriptors

import org.srcgll.exceptions.ParsingException


open class DescriptorsStorage<VertexType> : IDescriptorsStorage<VertexType> {
    protected val handledDescriptors = HashMap<VertexType, HashSet<Descriptor<VertexType>>>()
    protected val defaultDescriptorsStorage = ArrayDeque<Descriptor<VertexType>>()

    override fun defaultDescriptorsStorageIsEmpty() = defaultDescriptorsStorage.isEmpty()

    override fun addToHandling(descriptor: Descriptor<VertexType>) {
        if (!isAlreadyHandled(descriptor)) {
            defaultDescriptorsStorage.addLast(descriptor)
        }
    }

    override fun next(): Descriptor<VertexType> {
        if (defaultDescriptorsStorageIsEmpty()) {
            throw ParsingException("Access to empty descriptor storage")
        }
        return defaultDescriptorsStorage.removeLast()
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

