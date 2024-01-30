package org.srcgll.descriptors


class ErrorRecoveringDescriptorsStorage<VertexType> : DescriptorsStorage<VertexType>() {
    private val errorRecoveringDescriptorsStorage = LinkedHashMap<Int, ArrayDeque<Descriptor<VertexType>>>()

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

    fun recoverDescriptors(vertex: VertexType) {
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
}

