@file:Suppress("RedundantVisibilityModifier")

package parser.generator.generated

import java.util.HashMap
import kotlin.Unit
import org.srcgll.descriptors.Descriptor
import org.srcgll.input.ILabel
import org.srcgll.parser.context.IContext
import org.srcgll.parser.generator.GeneratedParser
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.sppf.node.SppfNode
import parser.generator.handwrite.SomeAbX

public class SomeAbXParser<VertexType, LabelType : ILabel> :
    GeneratedParser<VertexType, LabelType>() {
  override lateinit var ctx: IContext<VertexType, LabelType>

  override val grammar: SomeAbX = SomeAbX()

  override val ntFuncs: HashMap<Nonterminal, (descriptor: Descriptor<VertexType>,
      sppf: SppfNode<VertexType>?) -> Unit> = hashMapOf()
}
