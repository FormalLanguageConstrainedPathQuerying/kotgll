package org.srcgll.parser.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.parser.generator.IParserGenerator.Companion.INPUT_EDGE_NAME
import org.srcgll.rsm.symbol.Terminal

/**
 * Scanerless parser generator
 * Store @Grammar terminals as list of @Terminal<*> type
 */
class ScanerlessParserGenerator(override val grammarClazz: Class<*>) : IParserGenerator {
    override val grammar: Grammar = buildGrammar(grammarClazz)
    private val terminals: List<Terminal<*>> = grammar.getTerminals().toList()

    override fun generateProperties(): Iterable<PropertySpec> {
        return super.generateProperties() + generateTerminalsSpec()
    }

    override fun generateTerminalHandling(
        terminal: Terminal<*>,
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
                Terminal::class.asTypeName().parameterizedBy(STAR)
            )
        val propertyBuilder =
            PropertySpec.builder(IParserGenerator.TERMINALS, termListType)
                .addModifiers(KModifier.PRIVATE)
                .initializer("%L.%L().%L()", IParserGenerator.GRAMMAR_NAME, IParserGenerator.GET_TERMINALS, "toList")
        return propertyBuilder.build()
    }

}