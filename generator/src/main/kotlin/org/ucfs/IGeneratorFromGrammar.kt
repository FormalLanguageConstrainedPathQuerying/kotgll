package org.ucfs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import org.ucfs.grammar.combinator.Grammar

/**
 * Common logic for generators that use a Grammar class
 */
interface IGeneratorFromGrammar {
    val grammarClazz: Class<*>

    /**
     * Build a grammar object from Class<*>
     */
    fun buildGrammar(grammarClazz: Class<*>): Grammar {
        if (!Grammar::class.java.isAssignableFrom(grammarClazz)) {
            throw GeneratorException(GeneratorException.GRAMMAR_EXPECTED)
        }
        val grammar = grammarClazz.getConstructor().newInstance()
        if (grammar is Grammar) {
            grammar.rsm
            return grammar
        }
        throw GeneratorException(GeneratorException.GRAMMAR_EXPECTED)
    }
}

internal fun FileSpec.Builder.suppressWarningTypes(vararg types: String) {
    if (types.isEmpty()) {
        return
    }

    val format = "%S,".repeat(types.count()).trimEnd(',')
    addAnnotation(
        AnnotationSpec.builder(ClassName("", "Suppress"))
            .addMember(format, *types)
            .build()
    )
}

fun TypeName.nullable(): TypeName = this.copy(nullable = true)
