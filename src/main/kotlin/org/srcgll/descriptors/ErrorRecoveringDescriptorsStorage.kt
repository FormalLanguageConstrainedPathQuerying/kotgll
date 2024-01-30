package org.srcgll.descriptors


class ErrorRecoveringDescriptorsStorage<VertexType> : IDescriptorsStorage<VertexType> {
    private val handledDescriptors = HashMap<VertexType, HashSet<Descriptor<VertexType>>>()
    private val defaultDescriptorsStorage = ArrayDeque<Descriptor<VertexType>>()
    private val errorRecoveringDescriptorsStorage = LinkedHashMap<Int, ArrayDeque<Descriptor<VertexType>>>()

    override fun defaultDescriptorsStorageIsEmpty() = defaultDescriptorsStorage.isEmpty()

    override fun addToHandling(descriptor: Descriptor<VertexType>) {
        if (!isAlreadyHandled(descriptor)) {
            val pathWeight = descriptor.weight()

            if (pathWeight == 0) {
                defaultDescriptorsStorage.addLast(descriptor)
            } else {
                if (!errorRecoveringDescriptorsStorage.containsKey(pathWeight)) {
                    errorRecoveringDescriptorsStorage[pathWeight] = ArrayDeque()
                }
                errorRecoveringDescriptorsStorage.getValue(pathWeight).addLast(descriptor)
            }
        }
    }

    override fun recoverDescriptors(vertex: VertexType) {
        handledDescriptors.getOrDefault(vertex, HashSet()).forEach { descriptor ->
            descriptor.gssNode.handledDescriptors.remove(descriptor)
            addToHandling(descriptor)
        }
        handledDescriptors.remove(vertex)
    }

    override fun next(): Descriptor<VertexType> {
        if (defaultDescriptorsStorageIsEmpty()) {
            val iterator = errorRecoveringDescriptorsStorage.keys.iterator()
            val currentMin = iterator.next()
            val result = errorRecoveringDescriptorsStorage.getValue(currentMin).removeLast()

            if (errorRecoveringDescriptorsStorage.getValue(currentMin).isEmpty()) {
                errorRecoveringDescriptorsStorage.remove(currentMin)
            }

            return result
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

