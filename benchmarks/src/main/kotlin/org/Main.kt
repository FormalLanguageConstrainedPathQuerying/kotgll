package org

import org.ucfs.Java8
import org.ucfs.input.RecoveryLinearInput
import org.ucfs.parser.Gll
import org.ucfs.sppf.buildStringFromSppf
import org.ucfs.sppf.writeSppfToDot

class Main {

}

fun main(){
    val startState = Java8().rsm
    val srcText: String = """
package junit;"""
    val tokens = getTokenStream(srcText)
    val gll = Gll.gll(
        startState,
        tokens
    )
    println("Tokens")
    println(tokens)
    val parseResult = gll.parse().first ?: throw Exception("File $srcText cant be parsed by online gll")
    writeSppfToDot(parseResult, "java7.dot")

    //val efileContents = buildStringFromSppf(parseResult)
}