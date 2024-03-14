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

class ParserGenerator(private val grammarClazz: Class<*>) {
    companion object {
        private const val PARSER = "Parser"
        private val vertexType = TypeVariableName("VertexType")
        private val labelType = TypeVariableName("LabelType", ILabel::class.java)
        private val superClass = GeneratedParser::class.asTypeName().parameterizedBy(vertexType, labelType)
        private const val CTX_NAME = "ctx"
        private const val GRAMMAR_NAME = "grammar"
        private const val FUNCS_NAME = "ntFuncs"
        private val descriptorType = Descriptor::class.asTypeName().parameterizedBy(vertexType)
        private val sppfType = SppfNode::class.asTypeName().parameterizedBy(vertexType).copy(true)
        private const val DESCRIPTOR = "descriptor"
        private const val SPPF_NODE = "curSppfNode"
        private const val RSM_FIELD = "rsmState"
        private const val POS_FIELD = "inputPosition"
        private const val INPUT_FIELD = "input"
        private const val GET_NONTERMINAL = "getNonterminal"
        private const val GET_TERMINALS = "getTerminals"
        private const val TERMINALS = "terminals"
        private const val HANDLE_TERMINAL = "handleTerminal"
        private const val STATE_NAME = "state"
        private const val ID_FIELD_NAME = "id"
        private const val POS_VAR_NAME = "pos"
        private fun getParseFunName(nonterminalName: String): String = "parse${nonterminalName}"
        fun getParserClassName(grammarSimpleName: String): String {
            return grammarSimpleName + PARSER
        }
    }

    private val grammar: Grammar = buildGrammar(grammarClazz)
    private val terminals = grammar.getTerminals().toList()


    private fun buildGrammar(grammarClazz: Class<*>): Grammar {
        if (!Grammar::class.java.isAssignableFrom(grammarClazz)) {
            throw ParserGeneratorException(ParserGeneratorException.grammarExpectedMsg)
        }
        val grammar = grammarClazz.getConstructor().newInstance()
        if (grammar is Grammar) {
            return grammar
        }
        throw ParserGeneratorException(ParserGeneratorException.grammarExpectedMsg)
    }


    fun generate(location: Path, pkg: String) {
        val fileName = getParserClassName(grammarClazz.simpleName)
        val parserClass = ClassName(pkg, fileName).parameterizedBy(vertexType, labelType)

        val fileBuilder = FileSpec.builder(pkg, parserClass.rawType.simpleName)
        fileBuilder.addType(
            TypeSpec.classBuilder(parserClass.rawType.simpleName)
                .addTypeVariable(vertexType)
                .addTypeVariable(labelType)
                .superclass(superClass)
                .addProperty(generateCtxProperty())
                .addProperty(generateGrammarProperty(grammarClazz))
                .addProperties(generateNonterminalsSpec())
                .addProperty(generateTerminalsSpec())
                .addProperty(generateNtFuncsProperty())
                .addFunctions(generateParseFunctions())

                .build()
        )

        // KotlinPoet set `public` modifier to class by default (wontFix)
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
        val mapInitializer = CodeBlock.builder()
            .addStatement("hashMapOf(")
        for (nt in grammar.nonTerms) {
            val ntName = nt.nonterm.name
                ?: throw ParserGeneratorException("Unnamed nonterminal in grammar ${grammarClazz.simpleName}")
            mapInitializer.addStatement("%L to ::%L,", ntName, getParseFunName(ntName))
        }
        mapInitializer.addStatement(")")

        return PropertySpec
            .builder(FUNCS_NAME, mapType, KModifier.OVERRIDE)
            .initializer(mapInitializer.build())
            .build()
    }

    private fun generateParseFunctions(): Iterable<FunSpec> {
        return grammar.nonTerms.map { generateParseFunction(it) }
    }


    private fun generateParseFunction(nt: Nt): FunSpec {
        val funSpec = FunSpec.builder(getParseFunName(nt.nonterm.name!!))
        funSpec.addModifiers(KModifier.PRIVATE)
            .addParameter(DESCRIPTOR, descriptorType)
            .addParameter(SPPF_NODE, sppfType)
            .addStatement("val %L = %L.%L", STATE_NAME, DESCRIPTOR, RSM_FIELD)
            .addStatement("val %L = %L.%L", POS_VAR_NAME, DESCRIPTOR, POS_FIELD)
            .beginControlFlow("when(%L.%L)", STATE_NAME, ID_FIELD_NAME)

        for (state in nt.nonterm.getStates()) {
            generateParseForState(state, funSpec)
        }

        funSpec.endControlFlow()
        return funSpec.build()
    }

    private fun generateParseForState(state: RsmState, funSpec: FunSpec.Builder) {
        funSpec.addStatement("%S -> ", state.id)
        funSpec.beginControlFlow("")
        generateTerminalParsing(state, funSpec)
        generateNonterminalParsing(state, funSpec)
        funSpec.endControlFlow()

    }

    private fun generateTerminalParsing(state: RsmState, funSpec: FunSpec.Builder) {
        if (state.terminalEdges.isNotEmpty()) {
            funSpec.addComment("handle terminal edges")
            val inputEdge = "inputEdge"
            funSpec.beginControlFlow(
                "for (%L in %L.%L.getEdges(%L))",
                inputEdge,
                CTX_NAME,
                INPUT_FIELD,
                POS_VAR_NAME
            )
            for (term in state.terminalEdges.keys) {
                generateTerminalHandling(funSpec, term, inputEdge)
            }
            funSpec.endControlFlow()
        }
    }

    private fun generateTerminalHandling(
        funSpec: FunSpec.Builder,
        terminal: Terminal<*>,
        edgeName: String
    ) {
        funSpec.addStatement(
            "%L(%L[%L], %L, %L, %L, %L)",
            HANDLE_TERMINAL,
            TERMINALS,
            terminals.indexOf(terminal),
            STATE_NAME,
            edgeName,
            DESCRIPTOR,
            SPPF_NODE
        )
    }

    private fun generateNonterminalParsing(state: RsmState, funSpec: FunSpec.Builder) {
        if (state.nonterminalEdges.isNotEmpty()) {
            funSpec.addComment("handle nonterminal edges")
            for (edge in state.nonterminalEdges) {
                val ntName = edge.key.name!!
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

    private fun generateNonterminalsSpec(): Iterable<PropertySpec> {
        return grammar.nonTerms.stream().map { generateNonterminalSpec(it) }.collect(toList())
    }

    private fun generateNonterminalSpec(nt: Nt): PropertySpec {
        val ntName = nt.nonterm.name!!
        val propertyBuilder =
            PropertySpec.builder(ntName, Nonterminal::class.asTypeName())
                .addModifiers(KModifier.PRIVATE)
                .initializer("%L.%L.%L()!!", GRAMMAR_NAME, ntName, GET_NONTERMINAL)
        return propertyBuilder.build()
    }


    private fun generateTerminalsSpec(): PropertySpec {
        val termListType = List::class.asTypeName()
            .parameterizedBy(
                Terminal::class.asTypeName().parameterizedBy(STAR)
            )
        val propertyBuilder =
            PropertySpec.builder(TERMINALS, termListType)
                .addModifiers(KModifier.PRIVATE)
                .initializer("%L.%L().%L()", GRAMMAR_NAME, GET_TERMINALS, "toList")
        return propertyBuilder.build()
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


