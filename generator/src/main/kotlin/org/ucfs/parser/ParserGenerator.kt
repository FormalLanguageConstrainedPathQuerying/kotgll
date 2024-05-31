package org.ucfs.parser

import com.squareup.kotlinpoet.FileSpec
import org.ucfs.rsm.symbol.ITerminal

/**
 * Generator for a parser that uses a third-party lexer.
 * Unlike the scannerless parser , it uses scanner enumeration objects as terminals.
 */
open class ParserGenerator(grammarClazz: Class<*>, private val terminalsEnum: Class<*>) :
    AbstractParserGenerator(grammarClazz) {


    override fun getTerminalName(terminal: ITerminal): String {
        return "${terminalsEnum.simpleName}.$terminal"
    }

    override fun getFileBuilder(pkg: String): FileSpec.Builder {
        val builder = super.getFileBuilder(pkg)
        builder.addImport(terminalsEnum.packageName, terminalsEnum.simpleName)
        return builder
    }

}
