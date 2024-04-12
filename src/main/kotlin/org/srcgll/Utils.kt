package org.ucfs

import java.util.*

fun <ElementType> incrementalDfs(
    startElementL: ElementType,
    next: (ElementType) -> Iterable<ElementType>
): HashSet<ElementType> {
    val used = HashSet<ElementType>()
    val queue = LinkedList<ElementType>()
    queue.add(startElementL)
    while (queue.isNotEmpty()) {
        val element = queue.remove()
        used.add(element)
        for (nextElement in next(element)) {
            if (!used.contains(nextElement)) {
                queue.add(nextElement)
            }
        }
    }
    return used
}

fun <CollectionType, ElementType> incrementalDfs(
    startElementL: ElementType,
    next: (ElementType) -> Iterable<ElementType>,
    collection: CollectionType,
    addToCollection: (ElementType, CollectionType) -> Unit
): CollectionType {
    val used = HashSet<ElementType>()
    val queue = LinkedList<ElementType>()
    queue.add(startElementL)
    while (queue.isNotEmpty()) {
        val element = queue.remove()
        used.add(element)
        addToCollection(element, collection)
        for (nextElement in next(element)) {
            if (!used.contains(nextElement)) {
                queue.add(nextElement)
            }
        }
    }
    return collection
}



