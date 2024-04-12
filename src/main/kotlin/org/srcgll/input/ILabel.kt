package org.ucfs.input

import org.ucfs.rsm.symbol.ITerminal

interface ILabel {
    val terminal: ITerminal?
    override fun equals(other: Any?): Boolean
}