package org.srcgll.parser.generator

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.input.IInputGraph
import org.srcgll.input.ILabel
import org.srcgll.parser.IGll
import org.srcgll.parser.context.Context

abstract class GeneratedParser<VertexType, LabelType : ILabel> :
    IGll<VertexType, LabelType> {
    abstract val grammar: Grammar


    var input: IInputGraph<VertexType, LabelType>
        get() {
            return ctx.input
        }
        set(value) {
            ctx = Context(grammar.buildRsm(), value)
        }


}