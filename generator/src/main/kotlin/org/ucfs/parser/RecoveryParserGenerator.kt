package org.ucfs.parser

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.ucfs.intersection.RecoveryIntersection
import org.ucfs.parser.context.RecoveryContext

/**
 * Generator for a parser with error-recovery that uses a third-party lexer.
 */
class RecoveryParserGenerator(grammarClazz: Class<*>, terminalsEnum: Class<*>) :
    ParserGenerator(grammarClazz, terminalsEnum) {
    companion object {
        val recoveryEngineType = RecoveryIntersection::class.java.asTypeName()
        const val RECOVERY_METHOD_NAME = "handleRecoveryEdges"
    }

    override fun getContextType(): ParameterizedTypeName {
            return RecoveryContext::class.asTypeName().parameterizedBy(vertexType, labelType)
    }

    override fun generateParseFunctions(): Iterable<FunSpec> {
        return super.generateParseFunctions() + generateMainLoopFunction()
    }

    private fun generateMainLoopFunction(): FunSpec {
        return FunSpec.builder(MAIN_PARSE_FUNC).addModifiers(KModifier.OVERRIDE).addParameter(
            DESCRIPTOR, descriptorType
        ).addStatement("super.%L()", MAIN_PARSE_FUNC)
            .addStatement("%L.%L(this, %L)", recoveryEngineType, RECOVERY_METHOD_NAME, DESCRIPTOR).build()
    }
    override fun getParserClassName(): String {
        return super.getParserClassName() + RECOVERY
    }

}
