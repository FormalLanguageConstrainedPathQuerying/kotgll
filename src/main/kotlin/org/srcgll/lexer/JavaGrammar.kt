package org.srcgll.lexer

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class JavaGrammar : Grammar() {
    var CompilationUnit by Nt()
    var identifier by Nt()
    var Literal by Nt()
    var Type by Nt()
    var PrimitiveType by Nt()
    var ReferenceType by Nt()
    var Annotation by Nt()
    var NumericType by Nt()
    var integralType by Nt()
    var floatingPointType by Nt()
    var classOrInterfaceType by Nt()
    var TypeVariable by Nt()
    var ArrayType by Nt()
    var ClassType by Nt()
    var interfaceType by Nt()
    var typeArguments by Nt()
    var Dims by Nt()
    var TypeParameter by Nt()
    var TypeParameterModifier by Nt()
    var TypeBound by Nt()
    var AdditionalBound by Nt()
    var typeArgumentList by Nt()
    var typeArgument by Nt()
    var Wildcard by Nt()
    var WildcardBounds by Nt()
    var TypeName by Nt()
    var PackageOrTypeName by Nt()
    var ExpressionName by Nt()
    var AmbiguousName by Nt()
    var MethodName by Nt()
    var PackageName by Nt()
    var Result by Nt()
    var PackageDeclaration by Nt()
    var ImportDeclaration by Nt()
    var TypeDeclaration by Nt()
    var PackageModifier by Nt()
    var SingleTypeImportDeclaration by Nt()
    var TypeImportOnDemandDeclaration by Nt()
    var SingleStaticImportDeclaration by Nt()
    var StaticImportOnDemandDeclaration by Nt()
    var ClassDeclaration by Nt()
    var interfaceDeclaration by Nt()
    var Throws by Nt()
    var NormalClassDeclaration by Nt()
    var EnumDeclaration by Nt()
    var ClassModifier by Nt()
    var TypeParameters by Nt()
    var Superclass by Nt()
    var superinterfaces by Nt()
    var ClassBody by Nt()
    var TypeParameterList by Nt()
    var interfaceTypeList by Nt()
    var ClassBodyDeclaration by Nt()
    var ClassMemberDeclaration by Nt()
    var InstanceInitializer by Nt()
    var StaticInitializer by Nt()
    var ConstructorDeclaration by Nt()
    var FieldDeclaration by Nt()
    var MethodDeclaration by Nt()
    var FieldModifier by Nt()
    var unannType by Nt()
    var VariableDeclaratorList by Nt()
    var VariableDeclarator by Nt()
    var VariableDeclaratorId by Nt()
    var VariableInitializer by Nt()
    var Expression by Nt()
    var ArrayInitializer by Nt()
    var UnannPrimitiveType by Nt()
    var UnannReferenceType by Nt()
    var unannClassOrInterfaceType by Nt()
    var unannTypeVariable by Nt()
    var UnannArrayType by Nt()
    var UnannClassType by Nt()
    var unannInterfaceType by Nt()
    var MethodModifier by Nt()
    var MethodHeader by Nt()
    var MethodBody by Nt()
    var MethodDeclarator by Nt()
    var FormalParameterList by Nt()
    var ReceiverParameter by Nt()
    var FormalParameters by Nt()
    var LastFormalParameter by Nt()
    var FormalParameter by Nt()
    var VariableModifier by Nt()
    var exceptionTypeList by Nt()
    var exceptionType by Nt()
    var Block by Nt()
    var ConstructorModifier by Nt()
    var ConstructorDeclarator by Nt()
    var ConstructorBody by Nt()
    var SimpleTypeName by Nt()
    var ExplicitConstructorInvocation by Nt()
    var EnumBody by Nt()
    var enumConstantList by Nt()
    var enumConstant by Nt()
    var enumConstantModifier by Nt()
    var EnumBodyDeclarations by Nt()
    var blockStatements by Nt()
    var argumentList by Nt()
    var Primary by Nt()
    var normalInterfaceDeclaration by Nt()
    var interfaceModifier by Nt()
    var extendsInterfaces by Nt()
    var interfaceBody by Nt()
    var interfaceMemberDeclaration by Nt()
    var constantDeclaration by Nt()
    var constantModifier by Nt()
    var annotationTypeDeclaration by Nt()
    var annotationTypeBody by Nt()
    var annotationTypeMemberDeclaration by Nt()
    var annotationTypeElementDeclaration by Nt()
    var DefaultValue by Nt()
    var NormalAnnotation by Nt()
    var elementValuePairList by Nt()
    var elementValuePair by Nt()
    var elementValue by Nt()
    var elementValueArrayInitializer by Nt()
    var elementValueList by Nt()
    var MarkerAnnotation by Nt()
    var singleElementAnnotation by Nt()
    var interfaceMethodDeclaration by Nt()
    var annotationTypeElementModifier by Nt()
    var ConditionalExpression by Nt()
    var VariableInitializerList by Nt()
    var blockStatement by Nt()
    var localVariableDeclarationStatement by Nt()
    var LocalVariableDeclaration by Nt()
    var statement by Nt()
    var statementNoShortIf by Nt()
    var statementWithoutTrailingSubstatement by Nt()
    var emptyStatement by Nt()
    var labeledStatement by Nt()
    var labeledStatementNoShortIf by Nt()
    var expressionStatement by Nt()
    var statementExpression by Nt()
    var ifThenStatement by Nt()
    var ifThenElseStatement by Nt()
    var ifThenElseStatementNoShortIf by Nt()
    var assertStatement by Nt()
    var switchStatement by Nt()
    var SwitchBlock by Nt()
    var switchBlockStatementGroup by Nt()
    var SwitchLabels by Nt()
    var SwitchLabel by Nt()
    var enumConstantName by Nt()
    var whileStatement by Nt()
    var whileStatementNoShortIf by Nt()
    var doStatement by Nt()
    var interfaceMethodModifier by Nt()
    var forStatement by Nt()
    var forStatementNoShortIf by Nt()
    var basicForStatement by Nt()
    var basicForStatementNoShortIf by Nt()
    var ForInit by Nt()
    var ForUpdate by Nt()
    var statementExpressionList by Nt()
    var enhancedForStatement by Nt()
    var enhancedForStatementNoShortIf by Nt()
    var breakStatement by Nt()
    var continueStatement by Nt()
    var returnStatement by Nt()
    var throwStatement by Nt()
    var synchronizedStatement by Nt()
    var tryStatement by Nt()
    var Catches by Nt()
    var CatchClause by Nt()
    var CatchFormalParameter by Nt()
    var CatchType by Nt()
    var Finally by Nt()
    var tryWithResourcesStatement by Nt()
    var ResourceSpecification by Nt()
    var ResourceList by Nt()
    var Resource by Nt()
    var PrimaryNoNewArray by Nt()
    var ClassLiteral by Nt()
    var classOrInterfaceTypeToInstantiate by Nt()
    var UnqualifiedClassInstanceCreationExpression by Nt()
    var ClassInstanceCreationExpression by Nt()
    var FieldAccess by Nt()
    var typeArgumentsOrDiamond by Nt()
    var ArrayAccess by Nt()
    var MethodInvocation by Nt()
    var MethodReference by Nt()
    var ArrayCreationExpression by Nt()
    var DimExprs by Nt()
    var DimExpr by Nt()
    var LambdaExpression by Nt()
    var LambdaParameters by Nt()
    var InferredFormalParameterList by Nt()
    var LambdaBody by Nt()
    var assignmentExpression by Nt()
    var assignment by Nt()
    var LeftHandSide by Nt()
    var assignmentOperator by Nt()
    var ConditionalOrExpression by Nt()
    var ConditionalAndExpression by Nt()
    var InclusiveOrExpression by Nt()
    var ExclusiveOrExpression by Nt()
    var AndExpression by Nt()
    var EqualityExpression by Nt()
    var RelationalExpression by Nt()
    var ShiftExpression by Nt()
    var AdditiveExpression by Nt()
    var MultiplicativeExpression by Nt()
    var preIncrementExpression by Nt()
    var preDecrementExpression by Nt()
    var UnaryExpressionNotPlusMinus by Nt()
    var UnaryExpression by Nt()
    var PostfixExpression by Nt()
    var postIncrementExpression by Nt()
    var postDecrementExpression by Nt()
    var CastExpression by Nt()
    var constantExpression by Nt()

    init {
        identifier = Token.ID

        Literal = Token.INTEGERLIT or Token.FLOATINGLIT or Token.BOOLEANLIT or
                Token.CHARLIT or Token.STRINGLIT or Token.NULLLIT

        Type = PrimitiveType or ReferenceType
        PrimitiveType = Many(Annotation) * NumericType or Many(Annotation) * Token.BOOLEAN
        NumericType = integralType or floatingPointType
        integralType = Token.BYTE or Token.SHORT or Token.INT or Token.LONG or Token.CHAR
        floatingPointType = Token.FLOAT or Token.DOUBLE
        ReferenceType = classOrInterfaceType or TypeVariable or ArrayType
        classOrInterfaceType = ClassType or interfaceType
        ClassType = Many(Annotation) * identifier * Option(typeArguments) or
                classOrInterfaceType * Token.DOT * Many(Annotation) * identifier * Option(typeArguments)
        interfaceType = ClassType
        TypeVariable = Many(Annotation) * identifier
        ArrayType = PrimitiveType * Dims or classOrInterfaceType * Dims or TypeVariable * Dims
        Dims = Some(Many(Annotation) * Token.BRACKETLEFT * Token.BRACKETRIGHT)
        TypeParameter  = Many(TypeParameterModifier) * identifier * Option(TypeBound)
        TypeParameterModifier = Annotation
        TypeBound = Token.EXTENDS * TypeVariable or Token.EXTENDS * classOrInterfaceType * Many(AdditionalBound)
        AdditionalBound = Token.ANDBIT * interfaceType
        typeArguments = Token.DIAMONDLEFT * typeArgumentList * Token.DIAMONDRIGHT
        typeArgumentList = typeArgument * Many(Token.COMMA * typeArgument)
        typeArgument = ReferenceType or Wildcard
        Wildcard = Many(Annotation) * Token.QUESTIONMARK * Option(WildcardBounds)
        WildcardBounds = Token.EXTENDS * ReferenceType or Token.SUPER * ReferenceType

        TypeName = identifier or PackageOrTypeName * Token.DOT * identifier
        PackageOrTypeName = identifier or PackageOrTypeName * Token.DOT * identifier
        ExpressionName = identifier or AmbiguousName * Token.DOT * identifier
        MethodName = identifier
        PackageName = identifier or PackageName * Token.DOT * identifier
        AmbiguousName = identifier or AmbiguousName * Token.DOT * identifier

        CompilationUnit = Option(PackageDeclaration) * Many(ImportDeclaration) * Many(TypeDeclaration)
        PackageDeclaration = Many(PackageModifier) * Token.PACKAGE * identifier * Many(Token.DOT * identifier) * Token.SEMICOLON
        PackageModifier = Annotation
        ImportDeclaration = SingleTypeImportDeclaration or TypeImportOnDemandDeclaration or
                SingleStaticImportDeclaration or StaticImportOnDemandDeclaration
        SingleTypeImportDeclaration = Token.IMPORT * TypeName * Token.SEMICOLON
        TypeImportOnDemandDeclaration = Token.IMPORT * PackageOrTypeName * Token.DOT * Token.STAR * Token.SEMICOLON
        SingleStaticImportDeclaration = Token.IMPORT * Token.STATIC * TypeName * Token.DOT * identifier * Token.SEMICOLON
        StaticImportOnDemandDeclaration = Token.IMPORT * Token.STATIC * TypeName * Token.DOT * Token.STAR * Token.SEMICOLON
        TypeDeclaration = ClassDeclaration or interfaceDeclaration or Token.SEMICOLON

        ClassDeclaration = NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration = Many(ClassModifier) * Token.CLASS * identifier *
                Option(TypeParameters) * Option(Superclass) * Option(superinterfaces) * ClassBody
        ClassModifier = Annotation or Token.PUBLIC or Token.PROTECTED or Token.PRIVATE or
                Token.ABSTRACT or Token.STATIC or Token.FINAL or Token.STRICTFP
        TypeParameters = Token.DIAMONDLEFT * TypeParameterList * Token.DIAMONDRIGHT
        TypeParameterList = TypeParameter  * Many(Token.COMMA * TypeParameter)
        Superclass = Token.EXTENDS * ClassType
        superinterfaces = Token.IMPLEMENTS * interfaceTypeList
        interfaceTypeList = interfaceType  * Many(Token.COMMA * interfaceType)
        ClassBody = Token.CURLYLEFT * Many(ClassBodyDeclaration) * Token.CURLYRIGHT
        ClassBodyDeclaration = ClassMemberDeclaration or InstanceInitializer or StaticInitializer or ConstructorDeclaration
        ClassMemberDeclaration = FieldDeclaration or MethodDeclaration or ClassDeclaration or interfaceDeclaration or Token.SEMICOLON
        FieldDeclaration = Many(FieldModifier) * unannType * VariableDeclaratorList * Token.SEMICOLON
        FieldModifier = Annotation or Token.PUBLIC or Token.PROTECTED or Token.PRIVATE or Token.STATIC or
                Token.FINAL or Token.TRANSIENT or Token.VOLATILE
        VariableDeclaratorList = VariableDeclarator * Many(Token.COMMA * VariableDeclarator)
        VariableDeclarator = VariableDeclaratorId * Option(Token.ASSIGN * VariableInitializer)
        VariableDeclaratorId = identifier * Option(Dims)
        VariableInitializer = Expression or ArrayInitializer
        unannType = UnannPrimitiveType or UnannReferenceType
        UnannPrimitiveType = NumericType or Token.BOOLEAN
        UnannReferenceType = unannClassOrInterfaceType or unannTypeVariable or UnannArrayType
        unannClassOrInterfaceType = UnannClassType or unannInterfaceType
        UnannClassType = identifier * Option(typeArguments) or
                unannClassOrInterfaceType * Token.DOT * Many(Annotation) * identifier * Option(typeArguments)
        unannInterfaceType = UnannClassType
        unannTypeVariable = identifier
        UnannArrayType = UnannPrimitiveType * Dims or unannClassOrInterfaceType * Dims or unannTypeVariable * Dims
        MethodDeclaration = Many(MethodModifier) * MethodHeader * MethodBody
        MethodModifier = Annotation or Token.PUBLIC or Token.PROTECTED or Token.PRIVATE or Token.ABSTRACT or
                Token.STATIC or Token.FINAL or Token.SYNCHRONIZED or Token.NATIVE or Token.STRICTFP
        MethodHeader = Result * MethodDeclarator * Option(Throws) or TypeParameters * Many(Annotation) * Result *
                MethodDeclarator * Option(Throws)
        Result = unannType or Token.VOID
        MethodDeclarator = identifier * Token.PARENTHLEFT * Option(FormalParameterList) * Token.PARENTHRIGHT * Option(Dims)
        FormalParameterList = ReceiverParameter or FormalParameters * Token.COMMA * LastFormalParameter or
                LastFormalParameter
        FormalParameters = FormalParameter * Many(Token.COMMA * FormalParameter) or
                ReceiverParameter * Many(Token.COMMA * FormalParameter)
        FormalParameter = Many(VariableModifier) * unannType * VariableDeclaratorId
        VariableModifier = Annotation or Token.FINAL
        LastFormalParameter = Many(VariableModifier) * unannType * Many(Annotation) * Token.ELLIPSIS * VariableDeclaratorId or FormalParameter
        ReceiverParameter = Many(Annotation) * unannType * Option(identifier * Token.DOT) * Token.THIS
        Throws = Token.THROWS * exceptionTypeList
        exceptionTypeList = exceptionType * Many(Token.COMMA * exceptionType)
        exceptionType = ClassType or TypeVariable
        MethodBody = Block or Token.SEMICOLON
        InstanceInitializer = Block
        StaticInitializer = Token.STATIC * Block
        ConstructorDeclaration = Many(ConstructorModifier) * ConstructorDeclarator * Option(Throws) * ConstructorBody
        ConstructorModifier = Annotation or Token.PUBLIC or Token.PROTECTED or Token.PRIVATE
        ConstructorDeclarator = Option(TypeParameters) * SimpleTypeName * Token.PARENTHLEFT * Option(FormalParameterList) * Token.PARENTHRIGHT
        SimpleTypeName = identifier
        ConstructorBody = Token.CURLYLEFT * Option(ExplicitConstructorInvocation) * Option(blockStatements) * Token.CURLYRIGHT
        ExplicitConstructorInvocation = Option(typeArguments) * Token.THIS * Token.PARENTHLEFT * Option(argumentList) * Token.PARENTHRIGHT * Token.SEMICOLON or
                Option(typeArguments) * Token.SUPER * Token.PARENTHLEFT * Option(argumentList) * Token.PARENTHRIGHT * Token.SEMICOLON or
                ExpressionName * Token.DOT * Option(typeArguments) * Token.SUPER * Token.PARENTHLEFT * Option(argumentList) * Token.PARENTHRIGHT * Token.SEMICOLON or
                Primary * Token.DOT * Option(typeArguments) * Token.SUPER * Token.PARENTHLEFT * Option(argumentList) * Token.PARENTHRIGHT * Token.SEMICOLON
        EnumDeclaration = Many(ClassModifier) * Token.ENUM * identifier * Option(superinterfaces) * EnumBody
        EnumBody = Token.CURLYLEFT * Option(enumConstantList) * Option(Token.COMMA) * Option(EnumBodyDeclarations) * Token.CURLYRIGHT
        enumConstantList = enumConstant * Many(Token.COMMA * enumConstant)
        enumConstant = Many(enumConstantModifier) * identifier * Option(Token.PARENTHLEFT * Option(argumentList) * Token.PARENTHRIGHT * Option(ClassBody))
        enumConstantModifier = Annotation
        EnumBodyDeclarations = Token.SEMICOLON * Many(ClassBodyDeclaration)

        interfaceDeclaration = normalInterfaceDeclaration or annotationTypeDeclaration
        normalInterfaceDeclaration =
            Many(interfaceModifier) * Token.INTERFACE * identifier * Option(TypeParameters) * Option(extendsInterfaces) * interfaceBody
        interfaceModifier = Annotation or Token.PUBLIC or Token.PROTECTED or Token.PRIVATE or
                Token.ABSTRACT or Token.STATIC or Token.STRICTFP
        extendsInterfaces = Token.EXTENDS * interfaceTypeList
        interfaceBody = Token.CURLYLEFT * Many(interfaceMemberDeclaration) * Token.CURLYRIGHT
        interfaceMemberDeclaration = constantDeclaration or interfaceMethodDeclaration or ClassDeclaration or interfaceDeclaration or Token.SEMICOLON
        interfaceMethodDeclaration = Many(interfaceMethodModifier) * MethodHeader * MethodBody
        interfaceMethodModifier = Annotation or Token.PUBLIC or Token.ABSTRACT or Token.DEFAULT or Token.STATIC or Token.STRICTFP
        constantDeclaration = Many(constantModifier) * unannType * VariableDeclaratorList * Token.SEMICOLON
        constantModifier = Annotation or Token.PUBLIC or Token.ABSTRACT or Token.DEFAULT or Token.STATIC or Token.STRICTFP
        annotationTypeDeclaration = Many(interfaceModifier) * Token.AT * Token.INTERFACE * identifier * annotationTypeBody
        annotationTypeBody = Token.CURLYLEFT * Many(annotationTypeMemberDeclaration) * Token.CURLYRIGHT
        annotationTypeMemberDeclaration = annotationTypeElementDeclaration or constantDeclaration or ClassDeclaration or interfaceDeclaration or Token.SEMICOLON
        annotationTypeElementDeclaration =
            Many(annotationTypeElementModifier) * unannType * identifier * Token.PARENTHLEFT * Token.PARENTHRIGHT * Option(Dims) * Option(DefaultValue) * Token.SEMICOLON
        annotationTypeElementModifier = Annotation or Token.PUBLIC or Token.ABSTRACT
        DefaultValue = Token.DEFAULT * elementValue
        Annotation = NormalAnnotation or MarkerAnnotation or singleElementAnnotation
        NormalAnnotation = Token.AT * TypeName * Token.PARENTHLEFT * Option(elementValuePairList) * Token.PARENTHRIGHT
        elementValuePairList = elementValuePair * Many(Token.COMMA * elementValuePair)
        elementValuePair = identifier * Token.ASSIGN * elementValue
        elementValue = ConditionalExpression or elementValueArrayInitializer or Annotation
        elementValueArrayInitializer = Token.CURLYLEFT * Option(elementValueList) * Option(Token.COMMA) * Token.CURLYRIGHT
        elementValueList = elementValue * Many(Token.COMMA * elementValue)
        MarkerAnnotation = Token.AT * TypeName
        singleElementAnnotation = Token.AT * TypeName * Token.PARENTHLEFT * elementValue * Token.PARENTHRIGHT

        ArrayInitializer = Token.CURLYLEFT * Option(VariableInitializerList) * Option(Token.COMMA) * Token.CURLYRIGHT
        VariableInitializerList = VariableInitializer * Many(Token.COMMA * VariableInitializer)

        Block = Token.CURLYLEFT * Option(blockStatements) * Token.CURLYRIGHT
        blockStatements = blockStatement * Many(blockStatement)
        blockStatement = localVariableDeclarationStatement or ClassDeclaration or statement
        localVariableDeclarationStatement = LocalVariableDeclaration * Token.SEMICOLON
        LocalVariableDeclaration = Many(VariableModifier) * unannType * VariableDeclaratorList
        statement = statementWithoutTrailingSubstatement or labeledStatement or ifThenStatement or ifThenElseStatement or
                whileStatement or forStatement
        statementNoShortIf = statementWithoutTrailingSubstatement or labeledStatementNoShortIf or ifThenElseStatementNoShortIf or
                whileStatementNoShortIf or forStatementNoShortIf
        statementWithoutTrailingSubstatement = Block or emptyStatement or expressionStatement or assertStatement or
                switchStatement or doStatement or breakStatement or continueStatement or returnStatement or synchronizedStatement or
                throwStatement or tryStatement
        emptyStatement = Token.SEMICOLON
        labeledStatement = identifier * Token.COLON * statement
        labeledStatementNoShortIf = identifier * Token.COLON * statementNoShortIf
        expressionStatement = statementExpression * Token.SEMICOLON
        statementExpression = assignment or preIncrementExpression or preDecrementExpression or postIncrementExpression or
                postDecrementExpression or MethodInvocation or ClassInstanceCreationExpression
        ifThenStatement = Token.IF * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * statement
        ifThenElseStatement = Token.IF * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * statementNoShortIf * Token.ELSE * statement
        ifThenElseStatementNoShortIf =
            Token.IF * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * statementNoShortIf * Token.ELSE * statementNoShortIf
        assertStatement = Token.ASSERT * Expression * Token.SEMICOLON or
                Token.ASSERT * Expression * Token.COLON * Expression * Token.SEMICOLON
        switchStatement = Token.SWITCH * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * SwitchBlock
        SwitchBlock = Token.CURLYLEFT * Many(switchBlockStatementGroup) * Many(SwitchLabel) * Token.CURLYRIGHT
        switchBlockStatementGroup = SwitchLabels * blockStatements
        SwitchLabels = SwitchLabel * Many(SwitchLabel)
        SwitchLabel = Token.CASE * constantExpression * Token.COLON or
                Token.CASE * enumConstantName * Token.COLON or Token.DEFAULT * Token.COLON
        enumConstantName = identifier
        whileStatement = Token.WHILE * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * statement
        whileStatementNoShortIf = Token.WHILE * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * statementNoShortIf
        doStatement = Token.DO * statement * Token.WHILE * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * Token.SEMICOLON
        forStatement = basicForStatement or enhancedForStatement
        forStatementNoShortIf = basicForStatementNoShortIf or enhancedForStatementNoShortIf
        basicForStatement = Token.FOR * Token.PARENTHLEFT * Option(ForInit) * Token.SEMICOLON * Option(Expression) * Token.SEMICOLON * Option(ForUpdate) * Token.PARENTHRIGHT * statement
        basicForStatementNoShortIf = Token.FOR * Token.PARENTHLEFT * Option(ForInit) * Token.SEMICOLON * Option(Expression) * Token.SEMICOLON * Option(ForUpdate) * Token.PARENTHRIGHT * statementNoShortIf
        ForInit = statementExpressionList or LocalVariableDeclaration
        ForUpdate = statementExpressionList
        statementExpressionList = statementExpression * Many(Token.COMMA * statementExpression)
        enhancedForStatement = Token.FOR * Token.PARENTHLEFT * Many(VariableModifier) * unannType * VariableDeclaratorId * Token.COLON * Expression * Token.PARENTHRIGHT * statement
        enhancedForStatementNoShortIf = Token.FOR * Token.PARENTHLEFT * Many(VariableModifier) * unannType * VariableDeclaratorId * Token.COLON * Expression * Token.PARENTHRIGHT * statementNoShortIf
        breakStatement = Token.BREAK * Option(identifier) * Token.SEMICOLON
        continueStatement = Token.CONTINUE * Option(identifier) * Token.SEMICOLON
        returnStatement = Token.RETURN * Option(Expression) * Token.SEMICOLON
        throwStatement = Token.THROW * Expression * Token.SEMICOLON
        synchronizedStatement = Token.SYNCHRONIZED * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * Block
        tryStatement = Token.TRY * Block * Catches or Token.TRY * Block * Option(Catches) * Finally or tryWithResourcesStatement
        Catches = CatchClause * Many(CatchClause)
        CatchClause = Token.CATCH * Token.PARENTHLEFT * CatchFormalParameter * Token.PARENTHRIGHT * Block
        CatchFormalParameter = Many(VariableModifier) * CatchType * VariableDeclaratorId
        CatchType = UnannClassType * Many(Token.ORBIT * ClassType)
        Finally = Token.FINALLY * Block
        tryWithResourcesStatement = Token.TRY * ResourceSpecification * Block * Option(Catches) * Option(Finally)
        ResourceSpecification = Token.PARENTHLEFT * ResourceList * Option(Token.SEMICOLON) * Token.PARENTHRIGHT
        ResourceList = Resource * Many(Token.COMMA * Resource)
        Resource = Many(VariableModifier) * unannType * VariableDeclaratorId * Token.ASSIGN * Expression

        Primary = PrimaryNoNewArray or ArrayCreationExpression
        PrimaryNoNewArray = Literal or ClassLiteral or Token.THIS or TypeName * Token.DOT * Token.THIS or
                Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT or ClassInstanceCreationExpression or FieldAccess or
                ArrayAccess or MethodInvocation or MethodReference
        ClassLiteral = TypeName * Many(Token.BRACKETLEFT * Token.BRACKETRIGHT) * Token.DOT * Token.CLASS or
                NumericType * Many(Token.BRACKETLEFT * Token.BRACKETRIGHT) * Token.DOT * Token.CLASS or
                Token.BOOLEAN * Many(Token.BRACKETLEFT * Token.BRACKETRIGHT) * Token.DOT * Token.CLASS or
                Token.VOID * Token.DOT * Token.CLASS
        ClassInstanceCreationExpression = UnqualifiedClassInstanceCreationExpression or
                ExpressionName * Token.DOT * UnqualifiedClassInstanceCreationExpression or
                Primary * Token.DOT * UnqualifiedClassInstanceCreationExpression
        UnqualifiedClassInstanceCreationExpression =
            Token.NEW * Option(typeArguments) * classOrInterfaceTypeToInstantiate * Token.PARENTHLEFT * Option(argumentList) * Token.PARENTHRIGHT * Option(ClassBody)
        classOrInterfaceTypeToInstantiate = Many(Annotation) * identifier * Many(Token.DOT * Many(Annotation) * identifier) * Option(typeArgumentsOrDiamond)
        typeArgumentsOrDiamond = typeArguments or Token.DIAMOND
        FieldAccess = Primary * Token.DOT * identifier or Token.SUPER * Token.DOT * identifier or
                TypeName * Token.DOT * Token.SUPER * Token.DOT * identifier
        ArrayAccess = ExpressionName * Token.BRACKETLEFT * Expression * Token.BRACKETRIGHT or
                PrimaryNoNewArray * Token.BRACKETLEFT * Expression * Token.BRACKETRIGHT
        MethodInvocation = MethodName * Token.PARENTHLEFT * Option(argumentList) * Token.PARENTHRIGHT or
                TypeName * Token.DOT * Option(typeArguments) * identifier * Token.PARENTHLEFT * Option(argumentList) * Token.PARENTHRIGHT or
                ExpressionName * Token.DOT * Option(typeArguments) * identifier * Token.PARENTHLEFT * Option(argumentList) * Token.PARENTHRIGHT or
                Token.SUPER * Token.DOT * Option(typeArguments) * identifier * Token.PARENTHLEFT * Option(argumentList) * Token.PARENTHRIGHT or
                TypeName * Token.DOT * Token.SUPER * Token.DOT * Option(typeArguments) * identifier * Token.PARENTHLEFT * Option(argumentList) * Token.PARENTHRIGHT
        argumentList = Expression * Many(Token.COMMA * Expression)
        MethodReference = ExpressionName * Token.DOUBLECOLON * Option(typeArguments) * identifier or
                ReferenceType * Token.DOUBLECOLON * Option(typeArguments) * identifier or
                Primary * Token.DOUBLECOLON * Option(typeArguments) * identifier or
                Token.SUPER * Token.DOUBLECOLON * Option(typeArguments) * identifier or
                TypeName * Token.DOT * Token.SUPER * Token.DOUBLECOLON * Option(typeArguments) * identifier or
                ClassType * Token.DOUBLECOLON * Option(typeArguments) * Token.NEW or
                ArrayType * Token.DOUBLECOLON * Token.NEW
        ArrayCreationExpression = Token.NEW * PrimitiveType * DimExprs * Option(Dims) or
                Token.NEW * classOrInterfaceType * DimExprs * Option(Dims) or
                Token.NEW * PrimitiveType * Dims * ArrayInitializer or
                Token.NEW * classOrInterfaceType * Dims * ArrayInitializer
        DimExprs = DimExpr * Many(DimExpr)
        DimExpr = Many(Annotation) * Token.BRACKETLEFT * Expression * Token.BRACKETRIGHT
        Expression = LambdaExpression or assignmentExpression
        LambdaExpression = LambdaParameters * Token.ARROW * LambdaBody
        LambdaParameters = identifier or Token.PARENTHLEFT * Option(FormalParameterList) * Token.PARENTHRIGHT or
                Token.PARENTHLEFT * InferredFormalParameterList * Token.PARENTHRIGHT
        InferredFormalParameterList = identifier * Many(Token.COMMA * identifier)
        LambdaBody = Expression or Block
        assignmentExpression = ConditionalExpression or assignment
        assignment = LeftHandSide * assignmentOperator * Expression
        LeftHandSide = ExpressionName or FieldAccess or ArrayAccess
        assignmentOperator = Token.ASSIGN or Token.STARASSIGN or Token.SLASHASSIGN or Token.PERCENTASSIGN or Token.PLUSASSIGN or Token.MINUSASSIGN or
                Token.SHIFTLEFTASSIGN or Token.SHIFTRIGHTASSIGN or Token.USRIGHTSHIFTASSIGN or Token.ANDASSIGN or Token.XORASSIGN or Token.ORASSIGN
        ConditionalExpression = ConditionalOrExpression or
                ConditionalOrExpression * Token.QUESTIONMARK * Expression * Token.COLON * ConditionalExpression or
                ConditionalOrExpression * Token.QUESTIONMARK * Expression * Token.COLON * LambdaExpression
        ConditionalOrExpression = ConditionalAndExpression or
                ConditionalOrExpression * Token.OR * ConditionalAndExpression
        ConditionalAndExpression = InclusiveOrExpression or
                ConditionalAndExpression * Token.AND * InclusiveOrExpression
        InclusiveOrExpression = ExclusiveOrExpression or
                InclusiveOrExpression * Token.ORBIT * ExclusiveOrExpression
        ExclusiveOrExpression = AndExpression or ExclusiveOrExpression * Token.XORBIT * AndExpression
        AndExpression = EqualityExpression or AndExpression * Token.ANDBIT * EqualityExpression
        EqualityExpression = RelationalExpression or EqualityExpression * Token.EQ * RelationalExpression or
                EqualityExpression * Token.NOTEQ * RelationalExpression
        RelationalExpression = ShiftExpression or RelationalExpression * Token.DIAMONDLEFT * ShiftExpression or
                RelationalExpression * Token.DIAMONDRIGHT * ShiftExpression or RelationalExpression * Token.LESSEQ * ShiftExpression or
                RelationalExpression * Token.GREATEQ * ShiftExpression or RelationalExpression * Token.INSTANCEOF * ReferenceType
        ShiftExpression = AdditiveExpression or ShiftExpression * Token.LEFTSHIFT * AdditiveExpression or
                ShiftExpression * Token.RIGHTSHIT * AdditiveExpression or
                ShiftExpression * Token.USRIGHTSHIFT * AdditiveExpression
        AdditiveExpression = MultiplicativeExpression or AdditiveExpression * Token.PLUS * MultiplicativeExpression or
                AdditiveExpression * Token.MINUS * MultiplicativeExpression
        MultiplicativeExpression = UnaryExpression or MultiplicativeExpression * Token.STAR * UnaryExpression or
                MultiplicativeExpression * Token.SLASH * UnaryExpression or
                MultiplicativeExpression * Token.PERCENT * UnaryExpression
        UnaryExpression = preIncrementExpression or preDecrementExpression or Token.PLUS * UnaryExpression or
                Token.MINUS * UnaryExpression or UnaryExpressionNotPlusMinus
        preIncrementExpression = Token.PLUSPLUS * UnaryExpression
        preDecrementExpression = Token.MINUSMINUS * UnaryExpression
        UnaryExpressionNotPlusMinus = PostfixExpression or Token.TILDA * UnaryExpression or Token.EXCLAMATIONMARK * UnaryExpression or
                CastExpression
        PostfixExpression = Primary or ExpressionName or postIncrementExpression or postDecrementExpression
        postIncrementExpression = PostfixExpression * Token.PLUSPLUS
        postDecrementExpression = PostfixExpression * Token.MINUSMINUS
        CastExpression = Token.PARENTHLEFT * PrimitiveType * Token.PARENTHRIGHT * UnaryExpression or
                Token.PARENTHLEFT * ReferenceType * Many(AdditionalBound) * Token.PARENTHRIGHT * UnaryExpressionNotPlusMinus or
                Token.PARENTHLEFT * ReferenceType * Many(AdditionalBound) * Token.PARENTHRIGHT * LambdaExpression
        constantExpression = Expression

        setStart(CompilationUnit)
    }
}