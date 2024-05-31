package org.ucfs.parser

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.ucfs.GeneratorException
import org.ucfs.IGeneratorFromGrammar
import org.ucfs.descriptors.Descriptor
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.input.IInputGraph
import org.ucfs.input.ILabel
import org.ucfs.nullable
import org.ucfs.parser.context.Context
import org.ucfs.parser.context.IContext
import org.ucfs.rsm.RsmState
import org.ucfs.rsm.symbol.ITerminal
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.sppf.node.SppfNode
import org.ucfs.suppressWarningTypes
import java.nio.file.Path
import java.util.stream.Collectors.toList


abstract class AbstractParserGenerator(final override val grammarClazz: Class<*>) : IGeneratorFromGrammar {
    val grammar: Grammar = buildGrammar(grammarClazz)

    companion object {
        /**
         * Types
         */
        val vertexType = TypeVariableName("VertexType")
        val labelType = TypeVariableName("LabelType", ILabel::class.java)
        val superClass = GeneratedParser::class.asTypeName().parameterizedBy(vertexType, labelType)
        val descriptorType = Descriptor::class.asTypeName().parameterizedBy(vertexType)
        val sppfType = SppfNode::class.asTypeName().parameterizedBy(vertexType).nullable()

        /**
         * Variable identifiers
         */
        const val PARSER = "Parser"
        const val RECOVERY = "Recovery"
        const val VALUE_NAME = "value"
        const val CTX_NAME = "ctx"
        const val GRAMMAR_NAME = "grammar"
        const val DESCRIPTOR = "descriptor"
        const val SPPF_NODE = "curSppfNode"
        const val RSM_FIELD = "rsmState"
        const val RSM_GRAMMAR_FIELD = "rsm"
        const val POS_FIELD = "inputPosition"
        const val INPUT_NAME = "input"
        const val NONTERMINAL = "nonterm"
        const val GET_TERMINALS = "getTerminals"
        const val TERMINALS = "terminals"
        const val HANDLE_TERMINAL = "handleTerminal"
        const val STATE_NAME = "state"
        const val ID_FIELD_NAME = "id"
        const val POS_VAR_NAME = "pos"
        const val INPUT_EDGE_NAME = "inputEdge"
        const val MAIN_PARSE_FUNC = "parse"

        /**
         * Common methods
         */

        fun getParseFunName(nonterminalName: String): String = "parse${nonterminalName}"
    }

    /**
     * Generate all parser properties and methods
     */
    fun generate(location: Path, pkg: String) {
        val file = getFileBuilder(pkg).build()
        file.writeTo(location)
    }

    open fun getParserClassName(): String {
        return grammarClazz.simpleName + PARSER
    }

    /**
     * Build file builder
     */
    protected open fun getFileBuilder(pkg: String): FileSpec.Builder {
        val fileName = getParserClassName()
        val parserClass = ClassName(pkg, fileName).parameterizedBy(vertexType, labelType)

        val parserClassBuilder = TypeSpec.classBuilder(parserClass.rawType.simpleName)
            .addTypeVariable(vertexType)
            .addTypeVariable(labelType)
            .superclass(superClass)
            .addProperties(generateProperties())
            .addNtMapping()
            .addFunctions(generateMethods())

        val fileBuilder = FileSpec
            .builder(pkg, parserClass.rawType.simpleName)
            .addType(parserClassBuilder.build())

        // KotlinPoet set `public` modifier to class by default (wontFix)
        // https://github.com/square/kotlinpoet/issues/1098
        fileBuilder.suppressWarningTypes("RedundantVisibilityModifier")
        return fileBuilder
    }

    fun TypeSpec.Builder.addNtMapping(): TypeSpec.Builder {
        addFunction(generateCallNtFuncs())
        return this
    }

    /**
     * Add properties in Parser class
     */
    open fun generateProperties(): Iterable<PropertySpec> {
        return listOf(
            //  generateCtxProperty(),
            generateGrammarProperty(grammarClazz),
        ) + generateNonterminalsSpec()
    }

    /**
     * Generate overriding of ctx property
     */
    private fun generateCtxProperty(): PropertySpec {
        val ctxType = IContext::class.asTypeName().parameterizedBy(vertexType, labelType)
        return PropertySpec.builder(CTX_NAME, ctxType, KModifier.LATEINIT, KModifier.OVERRIDE)
            .mutable()
            .build()
    }

    /**
     * Generate overriding of grammar property
     * Anr it's initialization of corresponding @Grammar class
     */
    private fun generateGrammarProperty(grammarClazz: Class<*>): PropertySpec {
        return PropertySpec
            .builder(GRAMMAR_NAME, grammarClazz)
            .initializer(
                CodeBlock.of("${grammarClazz.simpleName}()")
            )
            .build()
    }

    private fun generateCallNtFuncs(): FunSpec {
        val funSpec = FunSpec.builder("callNtFuncs")
        funSpec.addModifiers(KModifier.OVERRIDE)
            .addParameter("nt", Nonterminal::class.asTypeName())
            .addParameter(DESCRIPTOR, descriptorType)
            .addParameter(SPPF_NODE, sppfType)
            .beginControlFlow("when(nt.name)", STATE_NAME, ID_FIELD_NAME)
        for (nt in grammar.nonTerms) {
            val ntName = nt.nonterm.name
                ?: throw GeneratorException("Unnamed nonterminal in grammar ${grammarClazz.simpleName}")
            funSpec.addStatement("%S -> %L($DESCRIPTOR, $SPPF_NODE)", ntName, getParseFunName(ntName))
        }
        funSpec.endControlFlow()
        return funSpec.build()
    }

    private fun generateMethods(): Iterable<FunSpec> {
        return generateParseFunctions() + generateInputSetter()
    }

    /**
     * Generate Parse methods for all nonterminals
     */
    protected open fun generateParseFunctions(): Iterable<FunSpec> {
        return grammar.nonTerms.map { generateParseFunction(it) }
    }

    /**
     * Generate Parse method for concrete nonterminal
     */
    private fun generateParseFunction(nt: Nt): FunSpec {
        val states = nt.nonterm.getStates()
        val funSpec = FunSpec.builder(getParseFunName(nt.nonterm.name!!))
        funSpec.addModifiers(KModifier.PRIVATE)
            .addParameter(DESCRIPTOR, descriptorType)
            .addParameter(SPPF_NODE, sppfType)
            .addStatement("val %L = %L.%L", STATE_NAME, DESCRIPTOR, RSM_FIELD)

        if (states.any { state -> state.terminalEdges.isNotEmpty() }) {
            funSpec.addStatement("val %L = %L.%L", POS_VAR_NAME, DESCRIPTOR, POS_FIELD)
        }

        funSpec.beginControlFlow("when(%L.%L)", STATE_NAME, "numId")

        for (state in states.sortedBy { it.numId }) {
            generateParseForState(state, funSpec)
        }

        funSpec.endControlFlow()
        return funSpec.build()
    }

    /**
     * Generate code for concrete switch block by nonterminal RSM states
     * (handle parsing for concrete state)
     */
    fun generateParseForState(state: RsmState, funSpec: FunSpec.Builder) {
        funSpec.addStatement("%L -> ", state.numId)
        funSpec.beginControlFlow("")
        generateTerminalParsing(state, funSpec)
        generateNonterminalParsing(state, funSpec)
        funSpec.endControlFlow()
    }

    /**
     * Generate and add to funSpec method that parse all terminals edge from current state
     */
    private fun generateTerminalParsing(state: RsmState, funSpec: FunSpec.Builder) {
        if (state.terminalEdges.isNotEmpty()) {
            funSpec.addComment("handle terminal edges")
            funSpec.beginControlFlow(
                "for (%L in %L.%L.getEdges(%L))",
                INPUT_EDGE_NAME,
                CTX_NAME,
                INPUT_NAME,
                POS_VAR_NAME
            )

            funSpec.beginControlFlow("when(%L.label.terminal)", INPUT_EDGE_NAME)
            for (term in state.terminalEdges.keys) {
                val terminalName =  getTerminalName(term)
                funSpec.addStatement("%L -> ", terminalName)
                funSpec.addStatement("%L(%L, %L, %L, %L, %L)", HANDLE_TERMINAL, terminalName,
                    STATE_NAME, INPUT_EDGE_NAME, DESCRIPTOR, SPPF_NODE)
            }
            funSpec.addStatement("else -> {}")
            funSpec.endControlFlow()

            funSpec.endControlFlow()
        }
    }

    abstract fun getTerminalName(terminal: ITerminal): String

    /**
     * Generate code for parsing all edges with Nonterminal label
     * from given @RsmState state
     */
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

    /**
     * Generate definition and initialization for all Nonterminals
     * as parser fields (with correspond nonterminal names)
     */
    private fun generateNonterminalsSpec(): Iterable<PropertySpec> {
        return grammar.nonTerms.stream().map { generateNonterminalSpec(it) }.collect(toList())
    }

    /**
     * Generate definition and initialization for concrete Nonterminal
     * as parser field (with correspond nonterminal name)
     */
    private fun generateNonterminalSpec(nt: Nt): PropertySpec {
        val ntName = nt.nonterm.name!!
        val propertyBuilder =
            PropertySpec.builder(ntName, Nonterminal::class.asTypeName())
                .addModifiers(KModifier.PRIVATE)
                .initializer("%L.%L.%L", GRAMMAR_NAME, ntName, NONTERMINAL)
        return propertyBuilder.build()
    }

    protected open fun getContextType(): ParameterizedTypeName {
        return Context::class.asTypeName().parameterizedBy(vertexType, labelType)
    }

    private fun generateInputSetter(): FunSpec {
        val ctxType = getContextType()
        val inputType = IInputGraph::class.asTypeName().parameterizedBy(vertexType, labelType)
        return FunSpec.builder("setInput")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(VALUE_NAME, inputType)
            .addStatement(
                "%L = %L(%L.%L, %L)",
                CTX_NAME,
                ctxType.rawType,
                GRAMMAR_NAME,
                RSM_GRAMMAR_FIELD,
                VALUE_NAME
            )
            .build()
    }
}

