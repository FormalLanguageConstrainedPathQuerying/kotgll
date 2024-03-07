package org.srcgll.parser.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.srcgll.descriptors.Descriptor
import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.Nt
import org.srcgll.input.ILabel
import org.srcgll.parser.context.IContext
import org.srcgll.rsm.RsmState
import org.srcgll.rsm.symbol.Nonterminal
import org.srcgll.rsm.symbol.Terminal
import org.srcgll.sppf.node.SppfNode
import java.nio.file.Path
import java.util.stream.Collectors.toList

object ParserGenerator {
    private const val PARSER = "Parser"
    private val vertexType = TypeVariableName("VertexType")
    private val labelType = TypeVariableName("LabelType", ILabel::class.java)
    private val superClass = GeneratedParser::class.asTypeName().parameterizedBy(vertexType, labelType)
    private const val CTX_NAME = "ctx"
    private const val GRAMMAR_NAME = "grammar"
    private const val FUNCS_NAME = "ntFuncs"
    private val descriptorType = Descriptor::class.asTypeName().parameterizedBy(vertexType)
    private val sppfType = SppfNode::class.asTypeName().parameterizedBy(vertexType)
    private const val DESCRIPTOR = "descriptor"
    private const val SPPF_NODE = "curSppfNode"
    private const val RSM_FIELD = "rsmState"
    private const val POS_FIELD = "inputPosition"
    private const val INPUT_FIELD = "input"
    private const val GET_NONTERMINAL = "getNonterminal"


    fun getRsm(grammarClazz: Class<*>): Grammar {
        if (!Grammar::class.java.isAssignableFrom(grammarClazz)) {
            throw ParserGeneratorException(ParserGeneratorException.grammarExpectedMsg)
        }
        val grammar = grammarClazz.getConstructor().newInstance()
        if (grammar is Grammar) {
            return grammar
        }
        throw ParserGeneratorException(ParserGeneratorException.grammarExpectedMsg)
    }


    fun generate(grammarClazz: Class<*>, location: Path, pkg: String) {
        val fileName = grammarClazz.simpleName + PARSER
        val parserClass = ClassName(pkg, fileName).parameterizedBy(vertexType, labelType)
        val grammar = getRsm(grammarClazz)


        val fileBuilder = FileSpec.builder(pkg, parserClass.rawType.simpleName)
        fileBuilder.addType(
            TypeSpec.classBuilder(parserClass.rawType.simpleName)
                .addTypeVariable(vertexType)
                .addTypeVariable(labelType)
                .superclass(superClass)
                .addProperty(generateCtxProperty())
                .addProperty(generateGrammarProperty(grammarClazz))
                .addProperties(generateNonterminals(grammar))
                .addProperties(generateTerminals(grammar))
                .addProperty(generateNtFuncsProperty())
                .build()
        )

        // KotlinPoet set `public` modifier to class by default (wont_fix)
        // https://github.com/square/kotlinpoet/issues/1098
        fileBuilder.suppressWarningTypes("RedundantVisibilityModifier")
        val file = fileBuilder.build()
        file.writeTo(location)
    }

    private fun generateCtxProperty(): PropertySpec {
        val ctxType = IContext::class.asTypeName().parameterizedBy(vertexType, labelType)
        return PropertySpec.builder(CTX_NAME, ctxType, KModifier.LATEINIT, KModifier.OVERRIDE)
            .mutable()
            .build()
    }

    private fun generateGrammarProperty(grammarClazz: Class<*>): PropertySpec {
        return PropertySpec
            .builder(GRAMMAR_NAME, grammarClazz, KModifier.OVERRIDE)
            .initializer(CodeBlock.of("${grammarClazz.simpleName}()"))
            .build()
    }

    private fun generateNtFuncsProperty(): PropertySpec {
        val funcType = LambdaTypeName.get(
            parameters = arrayOf(
                ParameterSpec("descriptor", descriptorType),
                ParameterSpec("sppf", sppfType.copy(nullable = true))
            ),
            returnType = Unit::class.asTypeName()
        )
        val mapType = HashMap::class
            .asTypeName()
            .parameterizedBy(Nonterminal::class.asTypeName(), funcType)
        return PropertySpec
            .builder(FUNCS_NAME, mapType, KModifier.OVERRIDE)
            .initializer(CodeBlock.of("hashMapOf()"))
            .build()
    }

    private fun generateParseFunctions(grammar: Grammar): Iterable<FunSpec> {
        return grammar.nonTerms.map { generateParseFunction(it) }
    }

    private fun generateParseFunction(nt: Nt): FunSpec {
        val funSpec = FunSpec.builder("parse${nt.nonterm.name}")
        val stateName = "state"
        val pos = "pos"
        funSpec.addModifiers(KModifier.PRIVATE)
            .addParameter(DESCRIPTOR, descriptorType)
            .addParameter(SPPF_NODE, sppfType)
            .addStatement("val %L = %L.%L", stateName, DESCRIPTOR, RSM_FIELD)
            .addStatement("val %L = %L.%L", pos, DESCRIPTOR, POS_FIELD)
            .beginControlFlow("when")

        for (state in nt.nonterm.getStates()) {
            generateParseForState(state, funSpec)
        }

        funSpec.endControlFlow()


        return funSpec.build()
    }

    private fun generateParseForState(state: RsmState, funSpec: FunSpec.Builder) {
        funSpec.addStatement("%L -> ", state.id)

        funSpec.beginControlFlow("")
        generateTerminalParsing(state, funSpec)
        generateNonterminalParsing(state, funSpec)
        funSpec.endControlFlow()

    }

    private fun generateTerminalParsing(state: RsmState, funSpec: FunSpec.Builder) {
        if (state.terminalEdges.isNotEmpty()) {
            val inputEdge = "inputEdge"
            funSpec.addComment("handle terminal edges")
            funSpec.beginControlFlow("for %L in %L.%L.getEdges(%L)", inputEdge, CTX_NAME, INPUT_FIELD, POS_FIELD)
        }
    }

    private fun generateNonterminalParsing(state: RsmState, funSpec: FunSpec.Builder) {
        if (state.nonterminalEdges.isNotEmpty()) {
            funSpec.addComment("handle nonterminal edges")
            for (edge in state.nonterminalEdges) {
                val ntName = edge.key.name!!
                funSpec.addStatement("val %L = %L.%L.getNonterminal()!!", ntName, GRAMMAR_NAME, ntName)
                funSpec.addStatement(
                    "handleNonterminalEdge(%L, %L, state.nonterminalEdges[%L]!!, %L)",
                    DESCRIPTOR,
                    ntName,
                    ntName,
                    SPPF_NODE
                )
            }
        }

    }

    private fun generateNonterminals(grammar: Grammar): Iterable<PropertySpec> {
        return grammar.nonTerms.stream().map { generateNonterminal(it) }.collect(toList())
    }

    private fun generateNonterminal(nt: Nt): PropertySpec {
        val ntName = nt.nonterm.name!!
        val propertyBuilder =
            PropertySpec.builder(ntName, Nonterminal::class.asTypeName())
                .addModifiers(KModifier.PRIVATE)
                .initializer("%L.%L.%L()!!", GRAMMAR_NAME, ntName, GET_NONTERMINAL)
        return propertyBuilder.build()
    }


    private fun generateTerminals(grammar: Grammar): Iterable<PropertySpec> {
        return grammar.nonTerms.stream().map { generateTerminal(it) }.collect(toList())
    }

    private fun generateTerminal(nt: Nt): PropertySpec {
        val ntName = nt.nonterm.name!!
        val termListType = List::class.asTypeName().parameterizedBy(Terminal::class.asTypeName())
        val propertyBuilder =
            PropertySpec.builder(ntName, termListType)
                .addModifiers(KModifier.PRIVATE)
                .initializer("%L.%L.%L()!!", GRAMMAR_NAME, ntName, GET_NONTERMINAL)
        return propertyBuilder.build()
    }

}

internal fun FileSpec.Builder.suppressWarningTypes(vararg types: String) {
    if (types.isEmpty()) {
        return
    }

    val format = "%L,".repeat(types.count()).trimEnd(',')
    addAnnotation(
        AnnotationSpec.builder(ClassName("", "Suppress"))
            .addMember(format, *types)
            .build()
    )
}


