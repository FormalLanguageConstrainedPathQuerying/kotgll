package org.ucfs
import org.ucfs.JavaToken.*
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.*

class Java8 : Grammar() {
    val compilationUnit by Nt().asStart()
    val identifier by Nt()
    val literal by Nt()
    val type by Nt()
    val primitiveType by Nt()
    val referenceType by Nt()
    val annotation by Nt()
    val numericType by Nt()
    val integralType by Nt()
    val floatingPointType by Nt()
    val classOrInterfaceType by Nt()
    val typeVariable by Nt()
    val arrayType by Nt()
    val classType by Nt()
    val interfaceType by Nt()
    val typeArguments by Nt()
    val dims by Nt()
    val typeParameter by Nt()
    val typeParameterModifier by Nt()
    val typeBound by Nt()
    val additionalBound by Nt()
    val typeArgumentList by Nt()
    val typeArgument by Nt()
    val wildcard by Nt()
    val wildcardBounds by Nt()
    val typeName by Nt()
    val packageOrTypeName by Nt()
    val expressionName by Nt()
    val ambiguousName by Nt()
    val methodName by Nt()
    val packageName by Nt()
    val result by Nt()
    val packageDeclaration by Nt()
    val importDeclaration by Nt()
    val typeDeclaration by Nt()
    val packageModifier by Nt()
    val singleTypeImportDeclaration by Nt()
    val typeImportOnDemandDeclaration by Nt()
    val singleStaticImportDeclaration by Nt()
    val staticImportOnDemandDeclaration by Nt()
    val classDeclaration by Nt()
    val interfaceDeclaration by Nt()
    val throws by Nt()
    val normalClassDeclaration by Nt()
    val enumDeclaration by Nt()
    val classModifier by Nt()
    val typeParameters by Nt()
    val superclass by Nt()
    val superinterfaces by Nt()
    val classBody by Nt()
    val typeParameterList by Nt()
    val interfaceTypeList by Nt()
    val classBodyDeclaration by Nt()
    val classMemberDeclaration by Nt()
    val instanceInitializer by Nt()
    val staticInitializer by Nt()
    val constructorDeclaration by Nt()
    val fieldDeclaration by Nt()
    val methodDeclaration by Nt()
    val fieldModifier by Nt()
    val unannType by Nt()
    val variableDeclaratorList by Nt()
    val variableDeclarator
                by Nt()
    val variableDeclaratorId by Nt()
    val variableInitializer by Nt()
    val expression by Nt()
    val arrayInitializer by Nt()
    val unannPrimitiveType by Nt()
    val unannReferenceType by Nt()
    val unannClassOrInterfaceType by Nt()
    val unannTypeVariable by Nt()
    val unannArrayType by Nt()
    val unannClassType by Nt()
    val unannInterfaceType by Nt()
    val methodModifier by Nt()
    val methodHeader by Nt()
    val methodBody by Nt()
    val methodDeclarator
                by Nt()
    val formalParameterList by Nt()
    val receiverParameter by Nt()
    val formalParameters by Nt()
    val lastFormalParameter by Nt()
    val formalParameter by Nt()
    val variableModifier by Nt()
    val exceptionTypeList by Nt()
    val exceptionType by Nt()
    val block by Nt()
    val constructorModifier by Nt()
    val constructorDeclarator
                by Nt()
    val constructorBody by Nt()
    val simpleTypeName by Nt()
    val explicitConstructorInvocation by Nt()
    val enumBody by Nt()
    val enumConstantList by Nt()
    val enumConstant by Nt()
    val enumConstantModifier by Nt()
    val enumBodyDeclarations by Nt()
    val blockStatements by Nt()
    val argumentList by Nt()
    val primary by Nt()
    val normalInterfaceDeclaration by Nt()
    val interfaceModifier by Nt()
    val extendsInterfaces by Nt()
    val interfaceBody by Nt()
    val interfaceMemberDeclaration by Nt()
    val constantDeclaration by Nt()
    val constantModifier by Nt()
    val annotationTypeDeclaration by Nt()
    val annotationTypeBody by Nt()
    val annotationTypeMemberDeclaration by Nt()
    val annotationTypeElementDeclaration by Nt()
    val defaultValue by Nt()
    val normalAnnotation by Nt()
    val elementValuePairList by Nt()
    val elementValuePair by Nt()
    val elementValue by Nt()
    val elementValueArrayInitializer by Nt()
    val elementValueList by Nt()
    val markerAnnotation by Nt()
    val singleElementAnnotation by Nt()
    val interfaceMethodDeclaration by Nt()
    val annotationTypeElementModifier by Nt()
    val conditionalExpression by Nt()
    val variableInitializerList by Nt()
    val blockStatement by Nt()
    val localVariableDeclarationStatement by Nt()
    val localVariableDeclaration by Nt()
    val statement by Nt()
    val statementNoShortIf by Nt()
    val statementWithoutTrailingSubstatement by Nt()
    val emptyStatement by Nt()
    val labeledStatement by Nt()
    val labeledStatementNoShortIf by Nt()
    val expressionStatement by Nt()
    val statementExpression by Nt()
    val ifThenStatement by Nt()
    val ifThenElseStatement by Nt()
    val ifThenElseStatementNoShortIf by Nt()
    val assertStatement by Nt()
    val switchStatement by Nt()
    val switchBlock by Nt()
    val switchBlockStatementGroup by Nt()
    val switchLabels by Nt()
    val switchLabel by Nt()
    val enumConstantName by Nt()
    val whileStatement by Nt()
    val whileStatementNoShortIf by Nt()
    val doStatement by Nt()
    val interfaceMethodModifier by Nt()
    val forStatement by Nt()
    val forStatementNoShortIf by Nt()
    val basicForStatement by Nt()
    val basicForStatementNoShortIf by Nt()
    val forInit by Nt()
    val forUpdate by Nt()
    val statementExpressionList by Nt()
    val enhancedForStatement by Nt()
    val enhancedForStatementNoShortIf by Nt()
    val breakStatement by Nt()
    val continueStatement by Nt()
    val returnStatement by Nt()
    val throwStatement by Nt()
    val synchronizedStatement by Nt()
    val tryStatement by Nt()
    val catches by Nt()
    val catchClause by Nt()
    val catchFormalParameter by Nt()
    val catchType by Nt()
    val finally by Nt()
    val tryWithResourcesStatement by Nt()
    val resourceSpecification by Nt()
    val resourceList by Nt()
    val resource by Nt()
    val primaryNoNewArray by Nt()
    val classLiteral by Nt()
    val classOrInterfaceTypeToInstantiate by Nt()
    val unqualifiedClassInstanceCreationExpression by Nt()
    val classInstanceCreationExpression by Nt()
    val fieldAccess by Nt()
    val typeArgumentsOrDiamond by Nt()
    val arrayAccess by Nt()
    val methodInvocation by Nt()
    val methodReference by Nt()
    val arrayCreationExpression by Nt()
    val dimExprs by Nt()
    val dimExpr by Nt()
    val lambdaExpression by Nt()
    val lambdaParameters by Nt()
    val inferredFormalParameterList by Nt()
    val lambdaBody by Nt()
    val assignmentExpression by Nt()
    val assignment by Nt()
    val leftHandSide by Nt()
    val assignmentOperator
                by Nt()
    val conditionalOrExpression by Nt()
    val conditionalAndExpression by Nt()
    val inclusiveOrExpression by Nt()
    val exclusiveOrExpression by Nt()
    val andExpression by Nt()
    val equalityExpression by Nt()
    val relationalExpression by Nt()
    val shiftExpression by Nt()
    val additiveExpression by Nt()
    val multiplicativeExpression by Nt()
    val preIncrementExpression by Nt()
    val preDecrementExpression by Nt()
    val unaryExpressionNotPlusMinus by Nt()
    val unaryExpression by Nt()
    val postfixExpression by Nt()
    val postIncrementExpression by Nt()
    val postDecrementExpression by Nt()
    val castExpression by Nt()
    val constantExpression by Nt()

    init {
        identifier /= IDENTIFIER

        /**
         * productions from §4 (Lexical structure)
         */
        literal /= INTEGER_LITERAL or
                FLOATING_POINT_LITERAL or
                BOOLEAN_LITERAL or
                CHARACTER_LITERAL or
                STRING_LITERAL or
                NULL_LITERAL

        /**
         * productions from §4 (types, values, and variables)
         */
        type /= primitiveType or
                referenceType

        primitiveType /= Many(annotation) * numericType or
                Many(annotation) * BOOLEAN

        numericType /= integralType or
                floatingPointType

        integralType /= BYTE or
                SHORT or
                INT or
                LONG or
                CHAR

        floatingPointType /= FLOAT or
                DOUBLE

        referenceType /= classOrInterfaceType or
                typeVariable or
                arrayType

        classOrInterfaceType /= classType or
                interfaceType

        classType /= Many(annotation) * identifier * Option(typeArguments) or
                classOrInterfaceType * DOT * Many(annotation) * identifier * Option(typeArguments)

        interfaceType /= classType

        typeVariable /= Many(annotation) * identifier

        arrayType /= primitiveType * dims or
                classOrInterfaceType * dims or
                typeVariable * dims

        dims /= some(Many(annotation) * LBRACK * RBRACK)

        typeParameter  /= Many(typeParameterModifier) * identifier * Option(typeBound)

        typeParameterModifier /= annotation

        typeBound /= EXTENDS * typeVariable or
                EXTENDS * classOrInterfaceType * Many(additionalBound)

        additionalBound /= AND * interfaceType

        typeArguments /= LT * typeArgumentList * GT

        typeArgumentList /= typeArgument * Many(COMMA * typeArgument)

        typeArgument /= referenceType or
                wildcard

        wildcard /= Many(annotation) * QUESTION * Option(wildcardBounds)

        wildcardBounds /= EXTENDS * referenceType or
                SUPER * referenceType

        /**
         * productions from §6 (Names)
         */
        packageName /= identifier or
                packageName * DOT * identifier

        typeName /= identifier or
                packageOrTypeName * DOT * identifier

        packageOrTypeName /= identifier or
                packageOrTypeName * DOT * identifier

        expressionName /= identifier or
                ambiguousName * DOT * identifier

        methodName /= identifier

        ambiguousName /= identifier or
                ambiguousName * DOT * identifier

        /**
         * productions from §7 (packages)
         */

        compilationUnit /= Option(packageDeclaration) * Many(importDeclaration) * Many(typeDeclaration)

        packageDeclaration /= Many(packageModifier) * PACKAGE * identifier * Many(DOT * identifier ) * SEMICOLON

        packageModifier /= annotation

        importDeclaration /= singleTypeImportDeclaration or
                typeImportOnDemandDeclaration or
                singleStaticImportDeclaration or
                staticImportOnDemandDeclaration

        singleTypeImportDeclaration /= IMPORT * typeName * SEMICOLON

        typeImportOnDemandDeclaration /= IMPORT * packageOrTypeName * DOT * MULT * SEMICOLON

        singleStaticImportDeclaration /= IMPORT * STATIC * typeName * DOT * identifier * SEMICOLON

        staticImportOnDemandDeclaration /= IMPORT * STATIC * typeName * DOT * MULT * SEMICOLON

        typeDeclaration /= classDeclaration or
                interfaceDeclaration or
                SEMICOLON

        /**
         * productions from §8 (classes)
         */

        classDeclaration /= normalClassDeclaration or
                enumDeclaration

        normalClassDeclaration /= Many(classModifier) * CLASS * identifier *
                Option(typeParameters) * Option(superclass) * Option(superinterfaces) * classBody

        classModifier /= annotation or
                PUBLIC or
                PROTECTED or
                PRIVATE or
                ABSTRACT or
                STATIC or
                FINAL or
                STRICTFP

        typeParameters /= LT * typeParameterList * GT

        typeParameterList /= typeParameter  * Many(COMMA * typeParameter)

        superclass /= EXTENDS * classType

        superinterfaces /= IMPLEMENTS * interfaceTypeList

        interfaceTypeList /= interfaceType  * Many(COMMA * interfaceType)

        classBody /= LBRACE * Many(classBodyDeclaration) * RBRACE

        classBodyDeclaration /= classMemberDeclaration or
                instanceInitializer or
                staticInitializer or
                constructorDeclaration

        classMemberDeclaration /= fieldDeclaration or
                methodDeclaration or
                classDeclaration or
                interfaceDeclaration or
                SEMICOLON

        fieldDeclaration /= Many(fieldModifier) * unannType * variableDeclaratorList * SEMICOLON

        fieldModifier /= annotation or
                PUBLIC or
                PROTECTED or
                PRIVATE or
                STATIC or
                FINAL or
                TRANSIENT or
                VOLATILE

        variableDeclaratorList /= variableDeclarator * Many(COMMA * variableDeclarator)

        variableDeclarator /= variableDeclaratorId * Option(EQ * variableInitializer)

        variableDeclaratorId /= identifier * Option(dims)

        variableInitializer /= expression or
                arrayInitializer

        unannType /= unannPrimitiveType or
                unannReferenceType

        unannPrimitiveType /= numericType or
                BOOLEAN

        unannReferenceType /= unannClassOrInterfaceType or
                unannTypeVariable or
                unannArrayType

        unannClassOrInterfaceType /= unannClassType or
                unannInterfaceType

        unannClassType /= identifier * Option(typeArguments) or
                unannClassOrInterfaceType * DOT * Many(annotation) * identifier * Option(typeArguments)

        unannInterfaceType /= unannClassType

        unannTypeVariable /= identifier

        unannArrayType /= unannPrimitiveType * dims or
                unannClassOrInterfaceType * dims or
                unannTypeVariable * dims

        methodDeclaration /= Many(methodModifier) * methodHeader * methodBody

        methodModifier /= annotation or
                PUBLIC or
                PROTECTED or
                PRIVATE or
                ABSTRACT or
                STATIC or
                FINAL or
                SYNCHRONIZED or
                NATIVE or
                STRICTFP

        methodHeader /= result * methodDeclarator * Option(throws) or
                typeParameters * Many(annotation) * result * methodDeclarator * Option(throws)

        result /= unannType or
                VOID

        methodDeclarator /= identifier * LPAREN * Option(formalParameterList) * RPAREN * Option(dims)

        formalParameterList /= receiverParameter or
                formalParameters * COMMA * lastFormalParameter or
                lastFormalParameter

        formalParameters /= formalParameter * Many(COMMA * formalParameter) or
                receiverParameter * Many(COMMA * formalParameter)

        formalParameter /= Many(variableModifier) * unannType * variableDeclaratorId

        variableModifier /= annotation or
                FINAL

        lastFormalParameter /= Many(variableModifier) * unannType * Many(annotation) * ELLIPSIS * variableDeclaratorId or
                formalParameter

        receiverParameter /= Many(annotation) * unannType * Option(identifier * DOT) * THIS

        throws /= THROWS * exceptionTypeList

        exceptionTypeList /= exceptionType * Many(COMMA * exceptionType)

        exceptionType /= classType or
                typeVariable

        methodBody /= block or
                SEMICOLON

        instanceInitializer /= block

        staticInitializer /= STATIC * block

        constructorDeclaration /= Many(constructorModifier) * constructorDeclarator * Option(throws) * constructorBody

        constructorModifier /= annotation or
                PUBLIC or
                PROTECTED or
                PRIVATE

        constructorDeclarator /= Option(typeParameters) * simpleTypeName * LPAREN * Option(formalParameterList) * RPAREN

        simpleTypeName /= identifier

        constructorBody /= LBRACE * Option(explicitConstructorInvocation) * Option(blockStatements) * RBRACE

        explicitConstructorInvocation /= Option(typeArguments) * THIS * LPAREN * Option(argumentList) * RPAREN * SEMICOLON or
                Option(typeArguments) * SUPER * LPAREN * Option(argumentList) * RPAREN * SEMICOLON or
                expressionName * DOT * Option(typeArguments) * SUPER * LPAREN * Option(argumentList) * RPAREN * SEMICOLON or
                primary * DOT * Option(typeArguments) * SUPER * LPAREN * Option(argumentList) * RPAREN * SEMICOLON

        enumDeclaration /= Many(classModifier) * ENUM * identifier * Option(superinterfaces) * enumBody

        enumBody /= LBRACE * Option(enumConstantList) * Option(COMMA) * Option(enumBodyDeclarations) * RBRACE

        enumConstantList /= enumConstant * Many(COMMA * enumConstant)

        enumConstant /= Many(enumConstantModifier) * identifier * Option(LPAREN * Option(argumentList) * RPAREN * Option(classBody))

        enumConstantModifier /= annotation

        enumBodyDeclarations /= SEMICOLON * Many(classBodyDeclaration)

        /**
         * productions from §9 (interfaces)
         */

        interfaceDeclaration /= normalInterfaceDeclaration or
                annotationTypeDeclaration

        normalInterfaceDeclaration /=
            Many(interfaceModifier) * INTERFACE * identifier * Option(typeParameters) *
                    Option(extendsInterfaces) * interfaceBody

        interfaceModifier /= annotation or
                PUBLIC or
                PROTECTED or
                PRIVATE or
                ABSTRACT or
                STATIC or
                STRICTFP

        extendsInterfaces /= EXTENDS * interfaceTypeList

        interfaceBody /= LBRACE * Many(interfaceMemberDeclaration) * RBRACE

        interfaceMemberDeclaration /= constantDeclaration or
                interfaceMethodDeclaration or
                classDeclaration or
                interfaceDeclaration or
                SEMICOLON

        constantDeclaration /= Many(constantModifier) * unannType * variableDeclaratorList * SEMICOLON

        constantModifier /= annotation or
                PUBLIC or
                STATIC or
                FINAL

        interfaceMethodDeclaration /= Many(interfaceMethodModifier) * methodHeader * methodBody

        interfaceMethodModifier /= annotation or
                PUBLIC or
                ABSTRACT or
                DEFAULT or
                STATIC or
                STRICTFP

        annotationTypeDeclaration /= Many(interfaceModifier) * AT * INTERFACE * identifier * annotationTypeBody

        annotationTypeBody /= LBRACE * Many(annotationTypeMemberDeclaration) * RBRACE

        annotationTypeMemberDeclaration /= annotationTypeElementDeclaration or
                constantDeclaration or
                classDeclaration or
                interfaceDeclaration or
                SEMICOLON

        annotationTypeElementDeclaration /=
            Many(annotationTypeElementModifier) * unannType * identifier * LPAREN * RPAREN *
                    Option(dims) * Option(defaultValue) * SEMICOLON

        annotationTypeElementModifier /= annotation or
                PUBLIC or
                ABSTRACT

        defaultValue /= DEFAULT * elementValue

        annotation /= normalAnnotation or
                markerAnnotation or
                singleElementAnnotation

        normalAnnotation /= AT * typeName * LPAREN * Option(elementValuePairList) * RPAREN

        elementValuePairList /= elementValuePair * Many(COMMA * elementValuePair)

        elementValuePair /= identifier * EQ * elementValue

        elementValue /= conditionalExpression or
                elementValueArrayInitializer or
                annotation

        elementValueArrayInitializer /= LBRACE * Option(elementValueList) * Option(COMMA) * RBRACE

        elementValueList /= elementValue * Many(COMMA * elementValue)

        markerAnnotation /= AT * typeName

        singleElementAnnotation /= AT * typeName * LPAREN * elementValue * RPAREN

        /**
         * productions from §10 (arrays)
         */

        arrayInitializer /= LBRACE * Option(variableInitializerList) * Option(COMMA) * RBRACE

        variableInitializerList /= variableInitializer * Many(COMMA * variableInitializer)

        /**
         * productions from §14 (Blocks and statements)
         */

        block /= LBRACE * Option(blockStatements) * RBRACE

        blockStatements /= blockStatement * Many(blockStatement)

        blockStatement /= localVariableDeclarationStatement or
                classDeclaration or
                statement

        localVariableDeclarationStatement /= localVariableDeclaration * SEMICOLON

        localVariableDeclaration /= Many(variableModifier) * unannType * variableDeclaratorList

        statement /= statementWithoutTrailingSubstatement or
                labeledStatement or
                ifThenStatement or
                ifThenElseStatement or
                whileStatement or
                forStatement

        statementNoShortIf /= statementWithoutTrailingSubstatement or
                labeledStatementNoShortIf or
                ifThenElseStatementNoShortIf or
                whileStatementNoShortIf or
                forStatementNoShortIf

        statementWithoutTrailingSubstatement /= block or
                emptyStatement or
                expressionStatement or
                assertStatement or
                switchStatement or
                doStatement or
                breakStatement or
                continueStatement or
                returnStatement or
                synchronizedStatement or
                throwStatement or
                tryStatement

        emptyStatement /= SEMICOLON

        labeledStatement /= identifier * COLON * statement

        labeledStatementNoShortIf /= identifier * COLON * statementNoShortIf

        expressionStatement /= statementExpression * SEMICOLON

        statementExpression /= assignment or
                preIncrementExpression or
                preDecrementExpression or
                postIncrementExpression or
                postDecrementExpression or
                methodInvocation or
                classInstanceCreationExpression

        ifThenStatement /= IF * LPAREN * expression * RPAREN * statement

        ifThenElseStatement /= IF * LPAREN * expression * RPAREN * statementNoShortIf * ELSE * statement

        ifThenElseStatementNoShortIf /=
            IF * LPAREN * expression * RPAREN * statementNoShortIf * ELSE * statementNoShortIf

        assertStatement /= ASSERT * expression * SEMICOLON or
                ASSERT * expression * COLON * expression * SEMICOLON

        switchStatement /= SWITCH * LPAREN * expression * RPAREN * switchBlock

        switchBlock /= LBRACE * Many(switchBlockStatementGroup) * Many(switchLabel) * RBRACE

        switchBlockStatementGroup /= switchLabels * blockStatements

        switchLabels /= some(switchLabel)

        switchLabel /= CASE * constantExpression * COLON or
                CASE * enumConstantName * COLON or
                DEFAULT * COLON

        enumConstantName /= identifier

        whileStatement /= WHILE * LPAREN * expression * RPAREN * statement

        whileStatementNoShortIf /= WHILE * LPAREN * expression * RPAREN * statementNoShortIf

        doStatement /= DO * statement * WHILE * LPAREN * expression * RPAREN * SEMICOLON

        forStatement /= basicForStatement or
                enhancedForStatement

        forStatementNoShortIf /= basicForStatementNoShortIf or
                enhancedForStatementNoShortIf

        basicForStatement /= FOR * LPAREN * Option(forInit) * SEMICOLON * Option(expression) * SEMICOLON *
                Option(forUpdate) * RPAREN * statement

        basicForStatementNoShortIf /= FOR * LPAREN * Option(forInit) * SEMICOLON * Option(expression) * SEMICOLON *
                Option(forUpdate) * RPAREN * statementNoShortIf

        forInit /= statementExpressionList or
                localVariableDeclaration

        forUpdate /= statementExpressionList

        statementExpressionList /= statementExpression * Many(COMMA * statementExpression)

        enhancedForStatement /= FOR * LPAREN * Many(variableModifier) * unannType * variableDeclaratorId * COLON *
                expression * RPAREN * statement
        enhancedForStatementNoShortIf /= FOR * LPAREN * Many(variableModifier) * unannType * variableDeclaratorId *
                COLON * expression * RPAREN * statementNoShortIf

        breakStatement /= BREAK * Option(identifier) * SEMICOLON

        continueStatement /= CONTINUE * Option(identifier) * SEMICOLON

        returnStatement /= RETURN * Option(expression) * SEMICOLON

        throwStatement /= THROW * expression * SEMICOLON

        synchronizedStatement /= SYNCHRONIZED * LPAREN * expression * RPAREN * block

        tryStatement /= TRY * block * catches or
                TRY * block * Option(catches) * finally or
                tryWithResourcesStatement

        catches /= some(catchClause)

        catchClause /= CATCH * LPAREN * catchFormalParameter * RPAREN * block

        catchFormalParameter /= Many(variableModifier) * catchType * variableDeclaratorId

        catchType /= unannClassType * Many(OR * classType)

        finally /= FINALLY * block

        tryWithResourcesStatement /= TRY * resourceSpecification * block * Option(catches) * Option(finally)

        resourceSpecification /= LPAREN * resourceList * Option(SEMICOLON) * RPAREN

        resourceList /= resource * Many(COMMA * resource)

        resource /= Many(variableModifier) * unannType * variableDeclaratorId * EQ * expression

        /**
         * productions from §15 (expressions)
         */

        primary /= primaryNoNewArray or
                arrayCreationExpression

        primaryNoNewArray /= literal or
                classLiteral or
                THIS or
                typeName * DOT * THIS or
                LPAREN * expression * RPAREN or
                classInstanceCreationExpression or
                fieldAccess or
                arrayAccess or
                methodInvocation or
                methodReference

        classLiteral /= typeName * Many(LBRACK * RBRACK) * DOT * CLASS or
                numericType * Many(LBRACK * RBRACK) * DOT * CLASS or
                BOOLEAN * Many(LBRACK * RBRACK) * DOT * CLASS or
                VOID * DOT * CLASS

        classInstanceCreationExpression /= unqualifiedClassInstanceCreationExpression or
                expressionName * DOT * unqualifiedClassInstanceCreationExpression or
                primary * DOT * unqualifiedClassInstanceCreationExpression

        unqualifiedClassInstanceCreationExpression /= NEW * Option(typeArguments) * classOrInterfaceTypeToInstantiate *
                    LPAREN * Option(argumentList) * RPAREN * Option(classBody)

        classOrInterfaceTypeToInstantiate /= Many(annotation) *
                identifier * Many(DOT * Many(annotation) * identifier ) * Option(typeArgumentsOrDiamond)

        typeArgumentsOrDiamond /= typeArguments or
                LT * GT

        fieldAccess /= primary * DOT * identifier or
                SUPER * DOT * identifier or
                typeName * DOT * SUPER * DOT * identifier

        arrayAccess /= expressionName * LBRACK * expression * RBRACK or
                primaryNoNewArray * LBRACK * expression * RBRACK

        methodInvocation /= methodName * LPAREN * Option(argumentList) * RPAREN or
                typeName * DOT * Option(typeArguments) * identifier * LPAREN * Option(argumentList) * RPAREN or
                expressionName * DOT * Option(typeArguments) * identifier * LPAREN * Option(argumentList) * RPAREN or
                primary * DOT * Option(typeArguments) * identifier * LPAREN * Option(argumentList) * RPAREN or
                SUPER * DOT * Option(typeArguments) * identifier * LPAREN * Option(argumentList) * RPAREN or
                typeName * DOT * SUPER * DOT * Option(typeArguments) * identifier * LPAREN * Option(argumentList) * RPAREN

        argumentList /= expression * Many(COMMA * expression)

        methodReference /= expressionName * COLONCOLON * Option(typeArguments) * identifier or
                referenceType * COLONCOLON * Option(typeArguments) * identifier or
                primary * COLONCOLON * Option(typeArguments) * identifier or
                SUPER * COLONCOLON * Option(typeArguments) * identifier or
                typeName * DOT * SUPER * COLONCOLON * Option(typeArguments) * identifier or
                classType * COLONCOLON * Option(typeArguments) * NEW or
                arrayType * COLONCOLON * NEW

        arrayCreationExpression /= NEW * primitiveType * dimExprs * Option(dims) or
                NEW * classOrInterfaceType * dimExprs * Option(dims) or
                NEW * primitiveType * dims * arrayInitializer or
                NEW * classOrInterfaceType * dims * arrayInitializer

        dimExprs /= some(dimExpr)

        dimExpr /= Many(annotation) * LBRACK * expression * RBRACK

        expression /= lambdaExpression or
                assignmentExpression

        lambdaExpression /= lambdaParameters * ARROW * lambdaBody

        lambdaParameters /= identifier or
                LPAREN * Option(formalParameterList) * RPAREN or
                LPAREN * inferredFormalParameterList * RPAREN

        inferredFormalParameterList /= identifier * Many(COMMA * identifier )

        lambdaBody /= expression or
                block

        assignmentExpression /= conditionalExpression or
                assignment

        assignment /= leftHandSide * assignmentOperator * expression

        leftHandSide /= expressionName or
                fieldAccess or
                arrayAccess

        assignmentOperator /= EQ or
                MULTEQ or
                DIVEQ or
                MODEQ or
                PLUSEQ or
                MINUSEQ or
                LSHIFTEQ or
                RSHIFTEQ or
                URSHIFTEQ or
                ANDEQ or
                XOREQ or
                OREQ

        conditionalExpression /= conditionalOrExpression or
                conditionalOrExpression * QUESTION * expression * COLON * conditionalExpression or
                conditionalOrExpression * QUESTION * expression * COLON * lambdaExpression

        conditionalOrExpression /= conditionalAndExpression or
                conditionalOrExpression * OROR * conditionalAndExpression

        conditionalAndExpression /= inclusiveOrExpression or
                conditionalAndExpression * ANDAND * inclusiveOrExpression

        inclusiveOrExpression /= exclusiveOrExpression or
                inclusiveOrExpression * OR * exclusiveOrExpression

        exclusiveOrExpression /= andExpression or
                exclusiveOrExpression * XOR * andExpression

        andExpression /= equalityExpression or
                andExpression * AND * equalityExpression

        equalityExpression /= relationalExpression or
                equalityExpression * EQEQ * relationalExpression or
                equalityExpression * NOTEQ * relationalExpression

        relationalExpression /= shiftExpression or
                relationalExpression * LT * shiftExpression or
                relationalExpression * GT * shiftExpression or
                relationalExpression * LTEQ * shiftExpression or
                relationalExpression * GTEQ * shiftExpression or
                relationalExpression * INSTANCEOF * referenceType

        shiftExpression /= additiveExpression or
                shiftExpression * LT * LT * additiveExpression or
                shiftExpression * GT * GT * additiveExpression or
                shiftExpression * GT * GT * GT * additiveExpression

        additiveExpression /= multiplicativeExpression or
                additiveExpression * PLUS * multiplicativeExpression or
                additiveExpression * MINUS * multiplicativeExpression

        multiplicativeExpression /= unaryExpression or
                multiplicativeExpression * MULT * unaryExpression or
                multiplicativeExpression * DIV * unaryExpression or
                multiplicativeExpression * MOD * unaryExpression

        unaryExpression /= preIncrementExpression or
                preDecrementExpression or
                PLUS * unaryExpression or
                MINUS * unaryExpression or
                unaryExpressionNotPlusMinus

        preIncrementExpression /= PLUSPLUS * unaryExpression

        preDecrementExpression /= MINUSMINUS * unaryExpression

        unaryExpressionNotPlusMinus /= postfixExpression or
                COMP * unaryExpression or
                NOT * unaryExpression or
                castExpression

        postfixExpression /= primary or
                expressionName or
                postIncrementExpression or
                postDecrementExpression

        postIncrementExpression /= postfixExpression * PLUSPLUS

        postDecrementExpression /= postfixExpression * MINUSMINUS

        castExpression /= LPAREN * primitiveType * RPAREN * unaryExpression or
                LPAREN * referenceType * Many(additionalBound) * RPAREN * unaryExpressionNotPlusMinus or
                LPAREN * referenceType * Many(additionalBound) * RPAREN * lambdaExpression
        
        constantExpression /= expression
    }
}