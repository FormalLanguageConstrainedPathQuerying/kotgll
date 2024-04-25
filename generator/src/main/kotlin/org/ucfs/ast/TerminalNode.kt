package org.ucfs.ast

import org.ucfs.rsm.symbol.ITerminal

class TerminalNode<T : ITerminal>(parent: Node, offset: Int, val terminal: T?, override var left: Node?) :
    Node(parent, offset) {
    init {
        length = terminal.toString().length
    }
}