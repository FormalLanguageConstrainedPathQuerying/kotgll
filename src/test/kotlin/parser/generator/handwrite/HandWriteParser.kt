package parser.generator.handwrite

import org.srcgll.descriptors.Descriptor
import org.srcgll.exceptions.ParsingException
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.grammar.combinator.regexp.Term
import org.srcgll.grammar.combinator.regexp.or
import org.srcgll.grammar.combinator.regexp.times
import org.srcgll.input.IInputGraph
import org.srcgll.input.ILabel
import org.srcgll.input.LinearInput
import org.srcgll.input.LinearInputLabel
import org.srcgll.parser.GllParser
import org.srcgll.parser.context.Context
import org.srcgll.parser.context.IContext
import org.srcgll.rsm.PrintableRsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.writeRsmToDot


/**
 * hand-write parser for @GrammarImpl
 *
 */
class HandWriteParser<VertexType, LabelType : ILabel>() :
    GllParser<VertexType, LabelType> {
    override lateinit var ctx: IContext<VertexType, LabelType>
    val grammar = GrammarImpl()
    var input: IInputGraph<VertexType, LabelType>
        get() {
            return ctx.input
        }
        set(value) {
            ctx = Context(grammar.getRsm(), value)
        }

    fun parseS(descriptor: Descriptor<VertexType>) {
        val d = descriptor.rsmState
        if (d is PrintableRsmState) {
            for (path in d.pathLabels) {
                when (path) {
                    "" -> {

                    }

                    "A" -> {}
                    "AB" -> {}
                    "ABa" -> {}
                    "ABaa" -> {}
                    "AC" -> {}
                    "ACa" -> {}
                    "ACaB" -> {}
                }
            }
        }
    }

//    fun parseA(descriptor: Descriptor<VertexType>) {
//        if (descriptor.rsmState is PrintableRsmState) {
//
//            for (path in descriptor.rsmState.pathLabels) {
//                when (path) {
//                    "" -> {}
//                    "a" -> {}
//                    "aA" -> {}
//                }
//            }
//        }
//    }
//
//    fun parseB(descriptor: Descriptor<VertexType>) {
//        if (descriptor.rsmState is PrintableRsmState) {
//
//            for (path in descriptor.rsmState.pathLabels) {
//                when (path) {
//                    "" -> {}
//                    "b" -> {}
//                    "bB" -> {}
//                }
//            }
//        }
//    }
//
//    fun parseC(descriptor: Descriptor<VertexType>) {
//        if (descriptor.rsmState is PrintableRsmState) {
//
//            for (path in descriptor.rsmState.pathLabels) {
//                when (path) {
//                    "" -> {}
//                    "b" -> {}
//                    "bC" -> {}
//                }
//            }
//        }
//    }

    val NtFuncs = hashMapOf<Nonterminal, (Descriptor<VertexType>) -> Unit>(
//        grammar.A.getNonterminal()!! to ::parseA,
//        grammar.B.getNonterminal()!! to ::parseB,
//        grammar.C.getNonterminal()!! to ::parseC,
        grammar.S.getNonterminal()!! to ::parseS,
    )

    override fun parse(curDescriptor: Descriptor<VertexType>) {
        val nt = curDescriptor.rsmState.nonterminal
        val func = NtFuncs[nt] ?: throw ParsingException("Nonterminal ${nt.name} is absent from the grammar!")

        func(curDescriptor)
        ctx.descriptors.addToHandled(curDescriptor)

    }


}


fun main() {
    val p = HandWriteParser<Int, LinearInputLabel>()
    p.input = LinearInput()
    val q = HandWriteParser<Int, LinearInputLabel>()
    val g = GrammarImpl()
    writeRsmToDot(g.getRsm(), "gen/impl.dot")
}

/**
Grammar for
 *  S = A C a B | A B a a
 *  A = a A | a
 *  B = b B | b
 *  C = b C | b
 */
class GrammarImpl() : Grammar() {
    var S by Nt()
    var A by Nt()
    var B by Nt()
    var C by Nt()
    val a = Term("a")
    val b = Term("b")

    init {
        setStart(S)

        S = A * C * a * B or A * B * a * a
        A = a * A or a
        B = b * B or b
        C = b * C or b


    }
}
