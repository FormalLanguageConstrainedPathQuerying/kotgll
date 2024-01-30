package org.srcgll.descriptors

interface IDescriptorsStorage<VertexType> {
    fun defaultDescriptorsStorageIsEmpty(): Boolean
    fun addToHandling(descriptor: Descriptor<VertexType>)
    fun next(): Descriptor<VertexType>
    fun isAlreadyHandled(descriptor: Descriptor<VertexType>): Boolean
    fun addToHandled(descriptor: Descriptor<VertexType>)
    fun removeFromHandled(descriptor: Descriptor<VertexType>)
}