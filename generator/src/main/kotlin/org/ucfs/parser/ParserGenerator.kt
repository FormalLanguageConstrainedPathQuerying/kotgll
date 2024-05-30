package org.ucfs.parser

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import org.ucfs.rsm.symbol.ITerminal

/**
 * Generator for a parser that uses a third-party lexer.
 * Unlike the scannerless parser , it uses scanner enumeration objects as terminals.
 */
open class ParserGenerator(grammarClazz: Class<*>, private val terminalsEnum: Class<*>) :
    AbstractParserGenerator(grammarClazz) {
    override fun generateTerminalHandling(terminal: ITerminal): CodeBlock {

        val terminalName = "${terminalsEnum.simpleName}.$terminal"
        return CodeBlock.of(
            "%L(%L, %L, %L, %L, %L)", HANDLE_TERMINAL, terminalName, STATE_NAME, INPUT_EDGE_NAME, DESCRIPTOR, SPPF_NODE
        )
    }

    override fun getFileBuilder(pkg: String): FileSpec.Builder {
        val builder = super.getFileBuilder(pkg)
        builder.addImport(terminalsEnum.packageName, terminalsEnum.simpleName)
        return builder
    }

}
