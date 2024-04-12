package org.srcgll.generators.parser

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.generators.parser.IParserGenerator.Companion.INPUT_EDGE_NAME
import org.srcgll.rsm.symbol.ITerminal

/**
 * Scanerless parser generator
 * Store @Grammar terminals as list of @Terminal<*> type
 */
class ScanerlessParserGenerator(override val grammarClazz: Class<*>) : IParserGenerator {
    override val grammar: Grammar = buildGrammar(grammarClazz)
    private val terminals: List<ITerminal> = grammar.getTerminals().toList()

    override fun generateProperties(): Iterable<PropertySpec> {
        return super.generateProperties() + generateTerminalsSpec()
    }

    override fun generateTerminalHandling(
        terminal: ITerminal,
    ): CodeBlock {
        return CodeBlock.of(
            "%L(%L[%L], %L, %L, %L, %L)",
            IParserGenerator.HANDLE_TERMINAL,
            IParserGenerator.TERMINALS,
            terminals.indexOf(terminal),
            IParserGenerator.STATE_NAME,
            INPUT_EDGE_NAME,
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
                ITerminal::class.asTypeName()
            )
        val propertyBuilder =
            PropertySpec.builder(IParserGenerator.TERMINALS, termListType)
                .addModifiers(KModifier.PRIVATE)
                .initializer("%L.%L().%L()", IParserGenerator.GRAMMAR_NAME, IParserGenerator.GET_TERMINALS, "toList")
        return propertyBuilder.build()
    }

}