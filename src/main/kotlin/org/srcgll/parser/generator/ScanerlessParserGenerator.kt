package org.srcgll.parser.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Terminal

class ScanerlessParserGenerator(override val grammarClazz: Class<*>) : IParserGenerator {
    override val grammar: Grammar = buildGrammar(grammarClazz)
    private val terminals: List<Terminal<*>> = grammar.getTerminals().toList()

    @Override
    override fun generateProperties(): Iterable<PropertySpec> {
        return super.generateProperties() + generateTerminalsSpec()
    }

    override fun generateTerminalParsing(state: RsmState, funSpec: FunSpec.Builder) {
        if (state.terminalEdges.isNotEmpty()) {
            funSpec.addComment("handle terminal edges")
            val inputEdge = "inputEdge"
            funSpec.beginControlFlow(
                "for (%L in %L.%L.getEdges(%L))",
                inputEdge,
                IParserGenerator.CTX_NAME,
                IParserGenerator.INPUT_FIELD,
                IParserGenerator.POS_VAR_NAME
            )
            for (term in state.terminalEdges.keys) {
                generateTerminalHandling(funSpec, term, inputEdge)
            }
            funSpec.endControlFlow()
        }
    }

    private fun generateTerminalHandling(
        funSpec: FunSpec.Builder,
        terminal: Terminal<*>,
        edgeName: String
    ) {
        funSpec.addStatement(
            "%L(%L[%L], %L, %L, %L, %L)",
            IParserGenerator.HANDLE_TERMINAL,
            IParserGenerator.TERMINALS,
            terminals.indexOf(terminal),
            IParserGenerator.STATE_NAME,
            edgeName,
            IParserGenerator.DESCRIPTOR,
            IParserGenerator.SPPF_NODE
        )
    }

    /**
     * Generate definition and initialization for Terminals as
     * filed in parser
     */
    private fun generateTerminalsSpec(): PropertySpec {
        val termListType = List::class.asTypeName()
            .parameterizedBy(
                Terminal::class.asTypeName().parameterizedBy(STAR)
            )
        val propertyBuilder =
            PropertySpec.builder(IParserGenerator.TERMINALS, termListType)
                .addModifiers(KModifier.PRIVATE)
                .initializer("%L.%L().%L()", IParserGenerator.GRAMMAR_NAME, IParserGenerator.GET_TERMINALS, "toList")
        return propertyBuilder.build()
    }

}