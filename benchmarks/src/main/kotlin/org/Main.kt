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
package junit.runner;

/**
 * This class defines the current version of JUnit
 */
public class Version {
	private Version() {
		// don't instantiate
	}

	public static String id() {
		return "4.12-SNAPSHOT";
	}
	
	public static void main(String[] args) {
		System.out.println(id());
	}
}
"""
    val tokens = getTokenStream(srcText)
    val gll = Gll.gll(
        startState,
        tokens
    )
    println("Tokens")
    println(tokens)
    val parseResult = gll.parse().first ?: throw Exception("File $srcText cant be parsed by online gll")
    writeSppfToDot(parseResult, "java7.dot")

}