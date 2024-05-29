package org.ucfs.ast

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.ucfs.IGeneratorFromGrammar
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.*
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.suppressWarningTypes
import java.nio.file.Path

/**
 * Generate Ast node class for each nonterminal in grammar.
 */
class NodeClassesGenerator(override val grammarClazz: Class<*>) :
    IGeneratorFromGrammar {
    val grammar: Grammar = buildGrammar(grammarClazz)

    private val superClass: Class<*> = Node::class.java

    companion object {
        fun getClassName(nt: Nt): String = getClassName(nt.nonterm)
        fun getClassName(nt: Nonterminal): String = "${nt.name}Node"

        //TODO add extensions `TerminalType: ITerminal`
        val terminalType = TypeVariableName("TerminalType")
        const val FUN_GET_CHILDREN = "getChildren"
        const val OFFSET = "offset"
        const val PARENT = "parent"
        const val LENGTH = "length"

    }

    /**
     * Generate class for each nonterminal in grammar
     */
    fun generate(location: Path, pkg: String) {
        for (nt in grammar.nonTerms) {
            val file = generateClassFile(nt, pkg)
            file.writeTo(location)
        }
    }

    /**
     * Generate class for concrete nonterminal
     */
    private fun generateClassFile(nt: Nt, pkg: String): FileSpec {
        val fileName = getClassName(nt)
        val ntClass = ClassName(pkg, fileName).parameterizedBy(terminalType)
        val nodeClassBuilder = TypeSpec.classBuilder(ntClass.rawType.simpleName)
            .addTypeVariable(terminalType)
            .superclass(superClass.asTypeName())
            .addFunction(generateConstructor())

        val fileBuilder = FileSpec
            .builder(pkg, ntClass.rawType.simpleName)
            .addType(nodeClassBuilder.build())

        fileBuilder.suppressWarningTypes("RedundantVisibilityModifier")
        return fileBuilder.build()
    }

    /**
     * Generate constructor
     */
    private fun generateConstructor(): FunSpec {
        return FunSpec.constructorBuilder()
            .addParameter(PARENT, superClass)
            .addParameter(OFFSET, Int::class)
            .callSuperConstructor(PARENT, OFFSET)
            .build()
    }

    private fun extractChildren(re: Regexp, isOptional: Boolean): List<PropertySpec> {
        return when (re) {
            is Alternative -> extractChildren(re.left, true) +
                    extractChildren(re.right, true)

            is Concat -> extractChildren(re.head, isOptional) +
                    extractChildren(re.tail, isOptional)

            is Empty -> listOf()
            is Epsilon -> listOf()
            is Many -> extractChildren(re.exp, true)
            is DerivedSymbol -> listOf(generateProperty(re, isOptional))
        }
    }


    private fun <T> generateProperty(value: T, isOptional: Boolean): PropertySpec {
        TODO()
    }

}