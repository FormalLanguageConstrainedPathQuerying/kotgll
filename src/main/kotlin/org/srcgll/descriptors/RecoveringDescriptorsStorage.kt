package org.ucfs.descriptors

/**
 * Collection of error recovery descriptors
 * @param VertexType - type of vertex in input graph
 */
class RecoveringDescriptorsStorage<VertexType> : DescriptorsStorage<VertexType>() {
    /**
     * Collection of descriptors with nonzero weight
     */
    private val errorRecoveringDescriptorsStorage = LinkedHashMap<Int, ArrayDeque<Descriptor<VertexType>>>()

    /**
     * Part of error recovery mechanism.
     * Calculate weight of descriptor. If weight is 0 => add descriptor to default descriptors storage, otherwise
     * add descriptor to error recovery descriptors storage
     * @param descriptor - descriptor to add
     */
    override fun addToHandling(descriptor: Descriptor<VertexType>) {
        if (!isAlreadyHandled(descriptor)) {
            val pathWeight = descriptor.weight

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


    /**
     * If default descriptor storage is not empty - retrieve descriptors from it, otherwise retrieve
     * error recovery descriptor
     * @return next descriptor to handle
     */
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

