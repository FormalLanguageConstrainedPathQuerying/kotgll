package org.ucfs.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asTypeName
import org.ucfs.rsm.symbol.ITerminal

/**
 * Scanerless parser generator
 * Store @Grammar terminals as list of @Terminal<*> type
 */
class ScanerlessParserGenerator(grammarClazz: Class<*>) : AbstractParserGenerator(grammarClazz) {
    private val terminals: List<ITerminal> = grammar.getTerminals().toList()

    override fun generateProperties(): Iterable<PropertySpec> {
        return super.generateProperties() + generateTerminalsSpec()
    }

    override fun generateTerminalHandling(
        terminal: ITerminal,
    ): CodeBlock {
        return CodeBlock.of(
            "%L(%L[%L], %L, %L, %L, %L)",
            HANDLE_TERMINAL,
            TERMINALS,
            terminals.indexOf(terminal),
            STATE_NAME,
            INPUT_EDGE_NAME,
            DESCRIPTOR,
            SPPF_NODE
        )
    }

    /**
     * Generate definition and initialization for Terminals as
     * filed in parser
     */
    private fun generateTerminalsSpec(): PropertySpec {
        val termListType = List::class.asTypeName().parameterizedBy(
            ITerminal::class.asTypeName()
        )
        val propertyBuilder =
            PropertySpec.builder(TERMINALS, termListType).addModifiers(KModifier.PRIVATE)
                .initializer(
                    "%L.%L().%L()",
                    GRAMMAR_NAME,
                    GET_TERMINALS,
                    "toList"
                )
        return propertyBuilder.build()
    }

}