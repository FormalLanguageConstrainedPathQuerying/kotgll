package org.ucfs.input

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.ucfs.input.utils.dot.DotLexer
import org.ucfs.input.utils.dot.DotParser
import org.ucfs.input.utils.dot.GraphFromDotVisitor
import org.ucfs.parser.ParsingException
import org.ucfs.rsm.symbol.ITerminal
import java.io.File
import java.io.IOException

class DotParser {

    fun parseDotFile(filePath: String): InputGraph<Int, TerminalInputLabel> {
        val file = File(filePath)

        if (!file.exists()) {
            throw IOException("File not found: $filePath")
        }
        return parseDot(file.readText())
    }


    fun parseDot(dotView: String): InputGraph<Int, TerminalInputLabel> {
        val realParser = DotParser(
            CommonTokenStream(
                DotLexer(
                    CharStreams.fromString(dotView)
                )
            )
        )
        return GraphFromDotVisitor().visitGraph(realParser.graph())
    }


    class StringTerminal(val value: String) : ITerminal {
        override fun getComparator(): Comparator<ITerminal> {
            return object : Comparator<ITerminal> {
                override fun compare(a: ITerminal, b: ITerminal): Int {
                    if (a !is StringTerminal || b !is StringTerminal) {
                        throw ParsingException(
                            "used comparator for $javaClass, " + "but got elements of ${a.javaClass}$ and ${b.javaClass}\$"
                        )
                    }
                    return a.value.compareTo(b.value)
                }
            }
        }

        override fun toString(): String {
            return value
        }
    }
}
