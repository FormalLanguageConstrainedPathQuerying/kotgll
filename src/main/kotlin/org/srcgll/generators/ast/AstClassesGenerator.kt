package org.srcgll.generators.ast

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.srcgll.ast.Node
import org.srcgll.generators.IGeneratorFromGrammar
import org.srcgll.generators.suppressWarningTypes
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.rsm.symbol.Term
import java.nio.file.Path

/**
 * Generate Ast node class for each nonterminal in grammar.
 */
class AstClassesGenerator(override val grammarClazz: Class<*>) :
    IGeneratorFromGrammar {
    val grammar: Grammar = buildGrammar(grammarClazz)

    companion object {
        private fun getClassName(nt: Nt): String = "${nt.nonterm.name}Node"

        //TODO add extensions `TerminalType: ITerminal`
        val terminalType = TypeVariableName("TerminalType")
    }

    /**
     * Generate all parser properties and methods
     */
    override fun generate(location: Path, pkg: String) {
        for (nt in grammar.nonTerms) {
            val file = generateClassFile(nt, pkg)
            file.writeTo(location)
        }
    }

    private fun generateClassFile(nt: Nt, pkg: String): FileSpec {
        val fileName = getClassName(nt)
        val ntClass = ClassName(pkg, fileName).parameterizedBy(terminalType)
        val nodeClassBuilder = TypeSpec.classBuilder(ntClass.rawType.simpleName)
            .addTypeVariable(terminalType)
            .superclass(Node::class.java.asTypeName())
            .addProperties(generateChildrenProperties(nt))

        val fileBuilder = FileSpec
            .builder(pkg, ntClass.rawType.simpleName)
            .addType(nodeClassBuilder.build())

        fileBuilder.suppressWarningTypes("RedundantVisibilityModifier")
        return fileBuilder.build()
    }

    private fun generateChildrenProperties(nt: Nt): Iterable<PropertySpec> {
        val re = nt.rsmDescription

        TODO()
    }

    private fun resolveProperty(re: Regexp, isOptional: Boolean): Iterable<PropertySpec> {
        return when (re) {
            is Optional -> resolveProperty(re.exp, true)
            is Alternative -> resolveProperty(re.left, true) +
                    resolveProperty(re.right, true)

            is Concat -> resolveProperty(re.head, isOptional) +
                    resolveProperty(re.tail, isOptional)

            is Empty -> listOf()
            is Epsilon -> listOf()
            is Many -> TODO()
            is DerivedSymbol -> listOf(generateProperty(re, isOptional))

        }
    }
    private fun generateProperty(exp: DerivedSymbol, isOptional: Boolean): PropertySpec{
        TODO()
//        return when(exp){
//            is Term<*> -> TODO()
//            is Nt -> {
//                val propClassName = getClassName(exp)
//                return PropertySpec.builder()
//            }
//            else -> TODO()
//
//        }
    }

    private fun generateListProperty(exp: DerivedSymbol, isOptional: Boolean): PropertySpec{
        TODO()
    }
}