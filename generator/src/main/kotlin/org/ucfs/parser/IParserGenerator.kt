package org.ucfs.parser

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.ucfs.GeneratorException
import org.ucfs.IGeneratorFromGrammar
import org.ucfs.descriptors.Descriptor
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.Nt
import org.ucfs.input.ILabel
import org.ucfs.nullable
import org.ucfs.parser.context.IContext
import org.ucfs.rsm.RsmState
import org.ucfs.rsm.symbol.ITerminal
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.sppf.node.SppfNode
import org.ucfs.suppressWarningTypes
import java.nio.file.Path
import java.util.stream.Collectors.toList

interface IParserGenerator : IGeneratorFromGrammar {
    val grammar: Grammar

    /**
     * Define a way to store relation between nonterminal
     * and their parsing method
     */
    fun mappingAsFunction(): Boolean = true

    companion object {
        private const val PARSER = "Parser"
        val vertexType = TypeVariableName("VertexType")
        val labelType = TypeVariableName("LabelType", ILabel::class.java)
        val superClass = GeneratedParser::class.asTypeName().parameterizedBy(vertexType, labelType)
        const val CTX_NAME = "ctx"
        const val GRAMMAR_NAME = "grammar"
        const val FUNCS_NAME = "ntFuncs"
        val descriptorType = Descriptor::class.asTypeName().parameterizedBy(vertexType)
        val sppfType = SppfNode::class.asTypeName().parameterizedBy(vertexType).nullable()
        const val DESCRIPTOR = "descriptor"
        const val SPPF_NODE = "curSppfNode"
        const val RSM_FIELD = "rsmState"
        const val POS_FIELD = "inputPosition"
        const val INPUT_FIELD = "input"
        const val GET_NONTERMINAL = "getNonterminal"
        const val GET_TERMINALS = "getTerminals"
        const val TERMINALS = "terminals"
        const val HANDLE_TERMINAL = "handleTerminal"
        const val STATE_NAME = "state"
        const val ID_FIELD_NAME = "id"
        const val POS_VAR_NAME = "pos"
        const val INPUT_EDGE_NAME = "inputEdge"
        fun getParseFunName(nonterminalName: String): String = "parse${nonterminalName}"
        fun getParserClassName(grammarSimpleName: String): String {
            return grammarSimpleName + PARSER
        }
    }


    /**
     * Generate all parser properties and methods
     */
    override fun generate(location: Path, pkg: String) {
        val file = getFileBuilder(pkg).build()
        file.writeTo(location)
    }

    /**
     * Build file builder
     */
    fun getFileBuilder(pkg: String): FileSpec.Builder {
        val fileName = getParserClassName(grammarClazz.simpleName)
        val parserClass = ClassName(pkg, fileName).parameterizedBy(vertexType, labelType)

        val parserClassBuilder = TypeSpec.classBuilder(parserClass.rawType.simpleName)
            .addTypeVariable(vertexType)
            .addTypeVariable(labelType)
            .superclass(superClass)
            .addProperties(generateProperties())
            .addNtMapping()
            .addFunctions(generateParseFunctions())

        val fileBuilder = FileSpec
            .builder(pkg, parserClass.rawType.simpleName)
            .addType(parserClassBuilder.build())

        // KotlinPoet set `public` modifier to class by default (wontFix)
        // https://github.com/square/kotlinpoet/issues/1098
        fileBuilder.suppressWarningTypes("RedundantVisibilityModifier")
        return fileBuilder
    }

    fun TypeSpec.Builder.addNtMapping(): TypeSpec.Builder {
        if (mappingAsFunction()) {
            addFunction(generateCallNtFuncs())
        } else {
            addProperty(generateNtFuncsProperty())
        }
        return this
    }

    /**
     * Add properties in Parser class
     */
    fun generateProperties(): Iterable<PropertySpec> {
        return listOf(
            generateCtxProperty(),
            generateGrammarProperty(grammarClazz)
        ) + generateNonterminalsSpec()
    }

    /**
     * Generate overriding of ctx property
     */
    fun generateCtxProperty(): PropertySpec {
        val ctxType = IContext::class.asTypeName().parameterizedBy(vertexType, labelType)
        return PropertySpec.builder(CTX_NAME, ctxType, KModifier.LATEINIT, KModifier.OVERRIDE)
            .mutable()
            .build()
    }

    /**
     * Generate overriding of grammar property
     * Anr it's initialization of corresponding @Grammar class
     */
    fun generateGrammarProperty(grammarClazz: Class<*>): PropertySpec {
        return PropertySpec
            .builder(GRAMMAR_NAME, grammarClazz, KModifier.OVERRIDE)
            .initializer(
                CodeBlock.of("${grammarClazz.simpleName}()")
            )
            .build()
    }

    fun generateCallNtFuncs(): FunSpec {
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

    /**
     * Generate overriding of property that map nonterminal to it's handling function.
     * And initialize it.
     */
    fun generateNtFuncsProperty(): PropertySpec {
        val funcType = LambdaTypeName.get(
            parameters = arrayOf(
                ParameterSpec("descriptor", descriptorType),
                ParameterSpec("sppf", sppfType.copy(nullable = true))
            ),
            returnType = Unit::class.asTypeName()
        )
        val mapType = HashMap::class
            .asTypeName()
            .parameterizedBy(String::class.asTypeName(), funcType)
        val mapInitializer = CodeBlock.builder()
            .addStatement("hashMapOf(")
        for (nt in grammar.nonTerms) {
            val ntName = nt.nonterm.name
                ?: throw GeneratorException("Unnamed nonterminal in grammar ${grammarClazz.simpleName}")
            mapInitializer.addStatement("%S to ::%L,", ntName, getParseFunName(ntName))
        }
        mapInitializer.addStatement(")")

        return PropertySpec
            .builder(FUNCS_NAME, mapType, KModifier.OVERRIDE)
            .initializer(mapInitializer.build())
            .build()
    }

    /**
     * Generate Parse methods for all nonterminals
     */
    fun generateParseFunctions(): Iterable<FunSpec> {
        return grammar.nonTerms.map { generateParseFunction(it) }
    }

    /**
     * Generate Parse method for concrete nonterminal
     */
    fun generateParseFunction(nt: Nt): FunSpec {
        val funSpec = FunSpec.builder(getParseFunName(nt.nonterm.name!!))
        funSpec.addModifiers(KModifier.PRIVATE)
            .addParameter(DESCRIPTOR, descriptorType)
            .addParameter(SPPF_NODE, sppfType)
            .addStatement("val %L = %L.%L", STATE_NAME, DESCRIPTOR, RSM_FIELD)
            .addStatement("val %L = %L.%L", POS_VAR_NAME, DESCRIPTOR, POS_FIELD)
            .beginControlFlow("when(%L.%L)", STATE_NAME, "numId")

        for (state in nt.nonterm.getStates()) {
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
    fun generateTerminalParsing(state: RsmState, funSpec: FunSpec.Builder) {
        if (state.terminalEdges.isNotEmpty()) {
            funSpec.addComment("handle terminal edges")
            funSpec.beginControlFlow(
                "for (%L in %L.%L.getEdges(%L))",
                INPUT_EDGE_NAME,
                CTX_NAME,
                INPUT_FIELD,
                POS_VAR_NAME
            )
            for (term in state.terminalEdges.keys) {
                funSpec.addStatement(generateTerminalHandling(term).toString())
            }
            funSpec.endControlFlow()
        }
    }

    /**
     * Generate code for handle one Edge with Terminal<*> label
     */
    fun generateTerminalHandling(terminal: ITerminal): CodeBlock


    /**
     * Generate code for parsing all edges with Nonterminal label
     * from given @RsmState state
     */
    fun generateNonterminalParsing(state: RsmState, funSpec: FunSpec.Builder) {
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
    fun generateNonterminalsSpec(): Iterable<PropertySpec> {
        return grammar.nonTerms.stream().map { generateNonterminalSpec(it) }.collect(toList())
    }

    /**
     * Generate definition and initialization for concrete Nonterminal
     * as parser field (with correspond nonterminal name)
     */
    fun generateNonterminalSpec(nt: Nt): PropertySpec {
        val ntName = nt.nonterm.name!!
        val propertyBuilder =
            PropertySpec.builder(ntName, Nonterminal::class.asTypeName())
                .addModifiers(KModifier.PRIVATE)
                .initializer("%L.%L.%L()!!", GRAMMAR_NAME, ntName, GET_NONTERMINAL)
        return propertyBuilder.build()
    }
}

