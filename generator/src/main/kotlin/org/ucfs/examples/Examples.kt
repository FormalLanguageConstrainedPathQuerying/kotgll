package org.ucfs.examples

import org.ucfs.ast.AstExtractor
import org.ucfs.ast.DotWriter
import org.ucfs.ast.NodeClassesGenerator
import org.ucfs.examples.dyck.DyckGrammar
import org.ucfs.examples.golang.SimpleGolang
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.input.LinearInput
import org.ucfs.parser.Gll
import org.ucfs.rsm.writeRsmToDot
import org.ucfs.sppf.writeSppfToDot
import java.nio.file.Path


object Examples {
    fun generateAst(grammar: Grammar, pkg: String, input: String, name: String) {
        val grammarClass = grammar::class.java
        NodeClassesGenerator(grammarClass).generate(Path.of("generator", "src", "main", "kotlin"), pkg)
        val gll = Gll.gll(grammar.rsm, LinearInput.buildFromString(input))
        val sppf = gll.parse().first
        writeSppfToDot(sppf!!, Path.of("${name}.dot").toString(), "${grammarClass.simpleName} SPPF for $input")
        val ast = AstExtractor(pkg).extract(sppf)
        val label = "${grammarClass.simpleName} AST for $input"
        DotWriter().writeToFile(
            ast,
            name,
            label,
            false
        )
        DotWriter().writeToFile(
            ast,
            "$name with siblings",
            label,
            true
        )

    }
}


fun main() {
    writeRsmToDot(DyckGrammar().rsm, "rsm.dot")
    Examples.generateAst(SimpleGolang(), "org.ucfs.examples.golang", "r 1 + 1 ;", "simple golang")
    Examples.generateAst(SimpleGolang(), "org.ucfs.examples.golang", "r 1 + 1 ; 1 ; r 1 ;", "simple golang")
    Examples.generateAst(DyckGrammar(), "org.ucfs.examples.dyck", "[ ( ) ] ", "1_dyck")
    Examples.generateAst(DyckGrammar(), "org.ucfs.examples.dyck", "[ ( ) ] { }", "2_dyck")
    Examples.generateAst(DyckGrammar(), "org.ucfs.examples.dyck", "[ ] { } [ ( ) ]", "3_dyck")
    Examples.generateAst(DyckGrammar(), "org.ucfs.examples.dyck", " [ { } ( ) ] ", "3_dyck")
    Examples.generateAst(DyckGrammar(), "org.ucfs.examples.dyck", "[ ] { { } ( ) } [ ( ) ]", "3_dyck")
}