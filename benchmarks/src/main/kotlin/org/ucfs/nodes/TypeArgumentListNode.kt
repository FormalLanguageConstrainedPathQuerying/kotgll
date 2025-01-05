@file:Suppress("RedundantVisibilityModifier")

package org.ucfs.nodes

import kotlin.Int
import org.ucfs.ast.Node

public class TypeArgumentListNode<TerminalType> : Node {
  public constructor(parent: Node, offset: Int) : super(parent, offset)
}
