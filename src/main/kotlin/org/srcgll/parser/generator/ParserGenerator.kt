package org.srcgll.parser.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmWildcard
import org.srcgll.input.ILabel
import org.srcgll.parser.context.IContext
import java.nio.file.Path

object ParserGenerator {
    val parser = "Parser"
    private val vertexType = TypeVariableName("VertexType")
    private val labelType =  TypeVariableName("LabelType", ILabel::class.java)
    fun generate(clazz: Class<*>, outputPath: String){
        val parserClass = ClassName("", clazz.simpleName + parser)
        val file = FileSpec.builder("", parserClass.simpleName)
            .addType(
                TypeSpec.classBuilder(parserClass.simpleName)
                    .addTypeVariable(vertexType)
                    .addTypeVariable(labelType)
                    .primaryConstructor(FunSpec.constructorBuilder().build())
                    .addSuperinterface(GeneratedParser::class.parameterizedBy())
                    .addProperty(generateCtxProperty())
                    .build()
            )

            .build()
        file.writeTo(Path.of(outputPath, parserClass.simpleName))
    }

    fun generateCtxProperty(): PropertySpec{
        return PropertySpec.builder("_ctx", IContext::class, KModifier.LATEINIT, KModifier.OVERRIDE)
            .mutable()
            .build()
    }
}

