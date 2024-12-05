package org.ucfs.simple

import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.*
import org.ucfs.rsm.symbol.Term

class SimpleGrammar: Grammar() {
    val a = Term("a")
    val b = Term("b")
    val c = Term("c")
    val S by Nt().asStart()
    init{
        //compilationUnit /= Option(packageDeclaration) * Many(importDeclaration) * Many(typeDeclaration)
        //S /= Option(b) * (a * b or a * S * b)
        //S /= a * b
        S /= c
        //S /= a * b
      //  S /= a * S * b or a * S or c
    }
}