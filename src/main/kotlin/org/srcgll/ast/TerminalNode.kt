package org.srcgll.ast

class TerminalNode<T>(parent: Node, offset: Int, length: Int) :
    Node(emptyList(), parent, offset, length)