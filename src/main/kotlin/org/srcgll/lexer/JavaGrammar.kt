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
        identifier = JavaToken.ID

        Literal = JavaToken.INTEGERLIT or JavaToken.FLOATINGLIT or JavaToken.BOOLEANLIT or
                JavaToken.CHARLIT or JavaToken.STRINGLIT or JavaToken.NULLLIT

        Type = PrimitiveType or ReferenceType
        PrimitiveType = Many(Annotation) * NumericType or Many(Annotation) * JavaToken.BOOLEAN
        NumericType = integralType or floatingPointType
        integralType = JavaToken.BYTE or JavaToken.SHORT or JavaToken.INT or JavaToken.LONG or JavaToken.CHAR
        floatingPointType = JavaToken.FLOAT or JavaToken.DOUBLE
        ReferenceType = classOrInterfaceType or TypeVariable or ArrayType
        classOrInterfaceType = ClassType or interfaceType
        ClassType = Many(Annotation) * identifier * opt(typeArguments) or
                classOrInterfaceType * JavaToken.DOT * Many(Annotation) * identifier * opt(typeArguments)
        interfaceType = ClassType
        TypeVariable = Many(Annotation) * identifier
        ArrayType = PrimitiveType * Dims or classOrInterfaceType * Dims or TypeVariable * Dims
        Dims = Some(Many(Annotation) * JavaToken.BRACKETLEFT * JavaToken.BRACKETRIGHT)
        TypeParameter  = Many(TypeParameterModifier) * identifier * opt(TypeBound)
        TypeParameterModifier = Annotation
        TypeBound = JavaToken.EXTENDS * TypeVariable or JavaToken.EXTENDS * classOrInterfaceType * Many(AdditionalBound)
        AdditionalBound = JavaToken.ANDBIT * interfaceType
        typeArguments = JavaToken.DIAMONDLEFT * typeArgumentList * JavaToken.DIAMONDRIGHT
        typeArgumentList = typeArgument * Many(JavaToken.COMMA * typeArgument)
        typeArgument = ReferenceType or Wildcard
        Wildcard = Many(Annotation) * JavaToken.QUESTIONMARK * opt(WildcardBounds)
        WildcardBounds = JavaToken.EXTENDS * ReferenceType or JavaToken.SUPER * ReferenceType

        TypeName = identifier or PackageOrTypeName * JavaToken.DOT * identifier
        PackageOrTypeName = identifier or PackageOrTypeName * JavaToken.DOT * identifier
        ExpressionName = identifier or AmbiguousName * JavaToken.DOT * identifier
        MethodName = identifier
        PackageName = identifier or PackageName * JavaToken.DOT * identifier
        AmbiguousName = identifier or AmbiguousName * JavaToken.DOT * identifier

        CompilationUnit = opt(PackageDeclaration) * Many(ImportDeclaration) * Many(TypeDeclaration)
        PackageDeclaration = Many(PackageModifier) * JavaToken.PACKAGE * identifier * Many(JavaToken.DOT * identifier) * JavaToken.SEMICOLON
        PackageModifier = Annotation
        ImportDeclaration = SingleTypeImportDeclaration or TypeImportOnDemandDeclaration or
                SingleStaticImportDeclaration or StaticImportOnDemandDeclaration
        SingleTypeImportDeclaration = JavaToken.IMPORT * TypeName * JavaToken.SEMICOLON
        TypeImportOnDemandDeclaration = JavaToken.IMPORT * PackageOrTypeName * JavaToken.DOT * JavaToken.STAR * JavaToken.SEMICOLON
        SingleStaticImportDeclaration = JavaToken.IMPORT * JavaToken.STATIC * TypeName * JavaToken.DOT * identifier * JavaToken.SEMICOLON
        StaticImportOnDemandDeclaration = JavaToken.IMPORT * JavaToken.STATIC * TypeName * JavaToken.DOT * JavaToken.STAR * JavaToken.SEMICOLON
        TypeDeclaration = ClassDeclaration or interfaceDeclaration or JavaToken.SEMICOLON

        ClassDeclaration = NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration = Many(ClassModifier) * JavaToken.CLASS * identifier *
                opt(TypeParameters) * opt(Superclass) * opt(superinterfaces) * ClassBody
        ClassModifier = Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or
                JavaToken.ABSTRACT or JavaToken.STATIC or JavaToken.FINAL or JavaToken.STRICTFP
        TypeParameters = JavaToken.DIAMONDLEFT * TypeParameterList * JavaToken.DIAMONDRIGHT
        TypeParameterList = TypeParameter  * Many(JavaToken.COMMA * TypeParameter)
        Superclass = JavaToken.EXTENDS * ClassType
        superinterfaces = JavaToken.IMPLEMENTS * interfaceTypeList
        interfaceTypeList = interfaceType  * Many(JavaToken.COMMA * interfaceType)
        ClassBody = JavaToken.CURLYLEFT * Many(ClassBodyDeclaration) * JavaToken.CURLYRIGHT
        ClassBodyDeclaration = ClassMemberDeclaration or InstanceInitializer or StaticInitializer or ConstructorDeclaration
        ClassMemberDeclaration = FieldDeclaration or MethodDeclaration or ClassDeclaration or interfaceDeclaration or JavaToken.SEMICOLON
        FieldDeclaration = Many(FieldModifier) * unannType * VariableDeclaratorList * JavaToken.SEMICOLON
        FieldModifier = Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or JavaToken.STATIC or
                JavaToken.FINAL or JavaToken.TRANSIENT or JavaToken.VOLATILE
        VariableDeclaratorList = VariableDeclarator * Many(JavaToken.COMMA * VariableDeclarator)
        VariableDeclarator = VariableDeclaratorId * opt(JavaToken.ASSIGN * VariableInitializer)
        VariableDeclaratorId = identifier * opt(Dims)
        VariableInitializer = Expression or ArrayInitializer
        unannType = UnannPrimitiveType or UnannReferenceType
        UnannPrimitiveType = NumericType or JavaToken.BOOLEAN
        UnannReferenceType = unannClassOrInterfaceType or unannTypeVariable or UnannArrayType
        unannClassOrInterfaceType = UnannClassType or unannInterfaceType
        UnannClassType = identifier * opt(typeArguments) or
                unannClassOrInterfaceType * JavaToken.DOT * Many(Annotation) * identifier * opt(typeArguments)
        unannInterfaceType = UnannClassType
        unannTypeVariable = identifier
        UnannArrayType = UnannPrimitiveType * Dims or unannClassOrInterfaceType * Dims or unannTypeVariable * Dims
        MethodDeclaration = Many(MethodModifier) * MethodHeader * MethodBody
        MethodModifier = Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or JavaToken.ABSTRACT or
                JavaToken.STATIC or JavaToken.FINAL or JavaToken.SYNCHRONIZED or JavaToken.NATIVE or JavaToken.STRICTFP
        MethodHeader = Result * MethodDeclarator * opt(Throws) or TypeParameters * Many(Annotation) * Result *
                MethodDeclarator * opt(Throws)
        Result = unannType or JavaToken.VOID
        MethodDeclarator = identifier * JavaToken.PARENTHLEFT * opt(FormalParameterList) * JavaToken.PARENTHRIGHT * opt(Dims)
        FormalParameterList = ReceiverParameter or FormalParameters * JavaToken.COMMA * LastFormalParameter or
                LastFormalParameter
        FormalParameters = FormalParameter * Many(JavaToken.COMMA * FormalParameter) or
                ReceiverParameter * Many(JavaToken.COMMA * FormalParameter)
        FormalParameter = Many(VariableModifier) * unannType * VariableDeclaratorId
        VariableModifier = Annotation or JavaToken.FINAL
        LastFormalParameter = Many(VariableModifier) * unannType * Many(Annotation) * JavaToken.ELLIPSIS * VariableDeclaratorId or FormalParameter
        ReceiverParameter = Many(Annotation) * unannType * opt(identifier * JavaToken.DOT) * JavaToken.THIS
        Throws = JavaToken.THROWS * exceptionTypeList
        exceptionTypeList = exceptionType * Many(JavaToken.COMMA * exceptionType)
        exceptionType = ClassType or TypeVariable
        MethodBody = Block or JavaToken.SEMICOLON
        InstanceInitializer = Block
        StaticInitializer = JavaToken.STATIC * Block
        ConstructorDeclaration = Many(ConstructorModifier) * ConstructorDeclarator * opt(Throws) * ConstructorBody
        ConstructorModifier = Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE
        ConstructorDeclarator = opt(TypeParameters) * SimpleTypeName * JavaToken.PARENTHLEFT * opt(FormalParameterList) * JavaToken.PARENTHRIGHT
        SimpleTypeName = identifier
        ConstructorBody = JavaToken.CURLYLEFT * opt(ExplicitConstructorInvocation) * opt(blockStatements) * JavaToken.CURLYRIGHT
        ExplicitConstructorInvocation = opt(typeArguments) * JavaToken.THIS * JavaToken.PARENTHLEFT * opt(argumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON or
                opt(typeArguments) * JavaToken.SUPER * JavaToken.PARENTHLEFT * opt(argumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON or
                ExpressionName * JavaToken.DOT * opt(typeArguments) * JavaToken.SUPER * JavaToken.PARENTHLEFT * opt(argumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON or
                Primary * JavaToken.DOT * opt(typeArguments) * JavaToken.SUPER * JavaToken.PARENTHLEFT * opt(argumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON
        EnumDeclaration = Many(ClassModifier) * JavaToken.ENUM * identifier * opt(superinterfaces) * EnumBody
        EnumBody = JavaToken.CURLYLEFT * opt(enumConstantList) * opt(JavaToken.COMMA) * opt(EnumBodyDeclarations) * JavaToken.CURLYRIGHT
        enumConstantList = enumConstant * Many(JavaToken.COMMA * enumConstant)
        enumConstant = Many(enumConstantModifier) * identifier * opt(JavaToken.PARENTHLEFT * opt(argumentList) * JavaToken.PARENTHRIGHT * opt(ClassBody))
        enumConstantModifier = Annotation
        EnumBodyDeclarations = JavaToken.SEMICOLON * Many(ClassBodyDeclaration)

        interfaceDeclaration = normalInterfaceDeclaration or annotationTypeDeclaration
        normalInterfaceDeclaration =
            Many(interfaceModifier) * JavaToken.INTERFACE * identifier * opt(TypeParameters) * opt(extendsInterfaces) * interfaceBody
        interfaceModifier = Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or
                JavaToken.ABSTRACT or JavaToken.STATIC or JavaToken.STRICTFP
        extendsInterfaces = JavaToken.EXTENDS * interfaceTypeList
        interfaceBody = JavaToken.CURLYLEFT * Many(interfaceMemberDeclaration) * JavaToken.CURLYRIGHT
        interfaceMemberDeclaration = constantDeclaration or interfaceMethodDeclaration or ClassDeclaration or interfaceDeclaration or JavaToken.SEMICOLON
        interfaceMethodDeclaration = Many(interfaceMethodModifier) * MethodHeader * MethodBody
        interfaceMethodModifier = Annotation or JavaToken.PUBLIC or JavaToken.ABSTRACT or JavaToken.DEFAULT or JavaToken.STATIC or JavaToken.STRICTFP
        constantDeclaration = Many(constantModifier) * unannType * VariableDeclaratorList * JavaToken.SEMICOLON
        constantModifier = Annotation or JavaToken.PUBLIC or JavaToken.ABSTRACT or JavaToken.DEFAULT or JavaToken.STATIC or JavaToken.STRICTFP
        annotationTypeDeclaration = Many(interfaceModifier) * JavaToken.AT * JavaToken.INTERFACE * identifier * annotationTypeBody
        annotationTypeBody = JavaToken.CURLYLEFT * Many(annotationTypeMemberDeclaration) * JavaToken.CURLYRIGHT
        annotationTypeMemberDeclaration = annotationTypeElementDeclaration or constantDeclaration or ClassDeclaration or interfaceDeclaration or JavaToken.SEMICOLON
        annotationTypeElementDeclaration =
            Many(annotationTypeElementModifier) * unannType * identifier * JavaToken.PARENTHLEFT * JavaToken.PARENTHRIGHT * opt(Dims) * opt(DefaultValue) * JavaToken.SEMICOLON
        annotationTypeElementModifier = Annotation or JavaToken.PUBLIC or JavaToken.ABSTRACT
        DefaultValue = JavaToken.DEFAULT * elementValue
        Annotation = NormalAnnotation or MarkerAnnotation or singleElementAnnotation
        NormalAnnotation = JavaToken.AT * TypeName * JavaToken.PARENTHLEFT * opt(elementValuePairList) * JavaToken.PARENTHRIGHT
        elementValuePairList = elementValuePair * Many(JavaToken.COMMA * elementValuePair)
        elementValuePair = identifier * JavaToken.ASSIGN * elementValue
        elementValue = ConditionalExpression or elementValueArrayInitializer or Annotation
        elementValueArrayInitializer = JavaToken.CURLYLEFT * opt(elementValueList) * opt(JavaToken.COMMA) * JavaToken.CURLYRIGHT
        elementValueList = elementValue * Many(JavaToken.COMMA * elementValue)
        MarkerAnnotation = JavaToken.AT * TypeName
        singleElementAnnotation = JavaToken.AT * TypeName * JavaToken.PARENTHLEFT * elementValue * JavaToken.PARENTHRIGHT

        ArrayInitializer = JavaToken.CURLYLEFT * opt(VariableInitializerList) * opt(JavaToken.COMMA) * JavaToken.CURLYRIGHT
        VariableInitializerList = VariableInitializer * Many(JavaToken.COMMA * VariableInitializer)

        Block = JavaToken.CURLYLEFT * opt(blockStatements) * JavaToken.CURLYRIGHT
        blockStatements = blockStatement * Many(blockStatement)
        blockStatement = localVariableDeclarationStatement or ClassDeclaration or statement
        localVariableDeclarationStatement = LocalVariableDeclaration * JavaToken.SEMICOLON
        LocalVariableDeclaration = Many(VariableModifier) * unannType * VariableDeclaratorList
        statement = statementWithoutTrailingSubstatement or labeledStatement or ifThenStatement or ifThenElseStatement or
                whileStatement or forStatement
        statementNoShortIf = statementWithoutTrailingSubstatement or labeledStatementNoShortIf or ifThenElseStatementNoShortIf or
                whileStatementNoShortIf or forStatementNoShortIf
        statementWithoutTrailingSubstatement = Block or emptyStatement or expressionStatement or assertStatement or
                switchStatement or doStatement or breakStatement or continueStatement or returnStatement or synchronizedStatement or
                throwStatement or tryStatement
        emptyStatement = JavaToken.SEMICOLON
        labeledStatement = identifier * JavaToken.COLON * statement
        labeledStatementNoShortIf = identifier * JavaToken.COLON * statementNoShortIf
        expressionStatement = statementExpression * JavaToken.SEMICOLON
        statementExpression = assignment or preIncrementExpression or preDecrementExpression or postIncrementExpression or
                postDecrementExpression or MethodInvocation or ClassInstanceCreationExpression
        ifThenStatement = JavaToken.IF * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * statement
        ifThenElseStatement = JavaToken.IF * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * statementNoShortIf * JavaToken.ELSE * statement
        ifThenElseStatementNoShortIf =
            JavaToken.IF * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * statementNoShortIf * JavaToken.ELSE * statementNoShortIf
        assertStatement = JavaToken.ASSERT * Expression * JavaToken.SEMICOLON or
                JavaToken.ASSERT * Expression * JavaToken.COLON * Expression * JavaToken.SEMICOLON
        switchStatement = JavaToken.SWITCH * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * SwitchBlock
        SwitchBlock = JavaToken.CURLYLEFT * Many(switchBlockStatementGroup) * Many(SwitchLabel) * JavaToken.CURLYRIGHT
        switchBlockStatementGroup = SwitchLabels * blockStatements
        SwitchLabels = SwitchLabel * Many(SwitchLabel)
        SwitchLabel = JavaToken.CASE * constantExpression * JavaToken.COLON or
                JavaToken.CASE * enumConstantName * JavaToken.COLON or JavaToken.DEFAULT * JavaToken.COLON
        enumConstantName = identifier
        whileStatement = JavaToken.WHILE * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * statement
        whileStatementNoShortIf = JavaToken.WHILE * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * statementNoShortIf
        doStatement = JavaToken.DO * statement * JavaToken.WHILE * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON
        forStatement = basicForStatement or enhancedForStatement
        forStatementNoShortIf = basicForStatementNoShortIf or enhancedForStatementNoShortIf
        basicForStatement = JavaToken.FOR * JavaToken.PARENTHLEFT * opt(ForInit) * JavaToken.SEMICOLON * opt(Expression) * JavaToken.SEMICOLON * opt(ForUpdate) * JavaToken.PARENTHRIGHT * statement
        basicForStatementNoShortIf = JavaToken.FOR * JavaToken.PARENTHLEFT * opt(ForInit) * JavaToken.SEMICOLON * opt(Expression) * JavaToken.SEMICOLON * opt(ForUpdate) * JavaToken.PARENTHRIGHT * statementNoShortIf
        ForInit = statementExpressionList or LocalVariableDeclaration
        ForUpdate = statementExpressionList
        statementExpressionList = statementExpression * Many(JavaToken.COMMA * statementExpression)
        enhancedForStatement = JavaToken.FOR * JavaToken.PARENTHLEFT * Many(VariableModifier) * unannType * VariableDeclaratorId * JavaToken.COLON * Expression * JavaToken.PARENTHRIGHT * statement
        enhancedForStatementNoShortIf = JavaToken.FOR * JavaToken.PARENTHLEFT * Many(VariableModifier) * unannType * VariableDeclaratorId * JavaToken.COLON * Expression * JavaToken.PARENTHRIGHT * statementNoShortIf
        breakStatement = JavaToken.BREAK * opt(identifier) * JavaToken.SEMICOLON
        continueStatement = JavaToken.CONTINUE * opt(identifier) * JavaToken.SEMICOLON
        returnStatement = JavaToken.RETURN * opt(Expression) * JavaToken.SEMICOLON
        throwStatement = JavaToken.THROW * Expression * JavaToken.SEMICOLON
        synchronizedStatement = JavaToken.SYNCHRONIZED * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * Block
        tryStatement = JavaToken.TRY * Block * Catches or JavaToken.TRY * Block * opt(Catches) * Finally or tryWithResourcesStatement
        Catches = CatchClause * Many(CatchClause)
        CatchClause = JavaToken.CATCH * JavaToken.PARENTHLEFT * CatchFormalParameter * JavaToken.PARENTHRIGHT * Block
        CatchFormalParameter = Many(VariableModifier) * CatchType * VariableDeclaratorId
        CatchType = UnannClassType * Many(JavaToken.ORBIT * ClassType)
        Finally = JavaToken.FINALLY * Block
        tryWithResourcesStatement = JavaToken.TRY * ResourceSpecification * Block * opt(Catches) * opt(Finally)
        ResourceSpecification = JavaToken.PARENTHLEFT * ResourceList * opt(JavaToken.SEMICOLON) * JavaToken.PARENTHRIGHT
        ResourceList = Resource * Many(JavaToken.COMMA * Resource)
        Resource = Many(VariableModifier) * unannType * VariableDeclaratorId * JavaToken.ASSIGN * Expression

        Primary = PrimaryNoNewArray or ArrayCreationExpression
        PrimaryNoNewArray = Literal or ClassLiteral or JavaToken.THIS or TypeName * JavaToken.DOT * JavaToken.THIS or
                JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT or ClassInstanceCreationExpression or FieldAccess or
                ArrayAccess or MethodInvocation or MethodReference
        ClassLiteral = TypeName * Many(JavaToken.BRACKETLEFT * JavaToken.BRACKETRIGHT) * JavaToken.DOT * JavaToken.CLASS or
                NumericType * Many(JavaToken.BRACKETLEFT * JavaToken.BRACKETRIGHT) * JavaToken.DOT * JavaToken.CLASS or
                JavaToken.BOOLEAN * Many(JavaToken.BRACKETLEFT * JavaToken.BRACKETRIGHT) * JavaToken.DOT * JavaToken.CLASS or
                JavaToken.VOID * JavaToken.DOT * JavaToken.CLASS
        ClassInstanceCreationExpression = UnqualifiedClassInstanceCreationExpression or
                ExpressionName * JavaToken.DOT * UnqualifiedClassInstanceCreationExpression or
                Primary * JavaToken.DOT * UnqualifiedClassInstanceCreationExpression
        UnqualifiedClassInstanceCreationExpression =
            JavaToken.NEW * opt(typeArguments) * classOrInterfaceTypeToInstantiate * JavaToken.PARENTHLEFT * opt(argumentList) * JavaToken.PARENTHRIGHT * opt(ClassBody)
        classOrInterfaceTypeToInstantiate = Many(Annotation) * identifier * Many(JavaToken.DOT * Many(Annotation) * identifier) * opt(typeArgumentsOrDiamond)
        typeArgumentsOrDiamond = typeArguments or JavaToken.DIAMOND
        FieldAccess = Primary * JavaToken.DOT * identifier or JavaToken.SUPER * JavaToken.DOT * identifier or
                TypeName * JavaToken.DOT * JavaToken.SUPER * JavaToken.DOT * identifier
        ArrayAccess = ExpressionName * JavaToken.BRACKETLEFT * Expression * JavaToken.BRACKETRIGHT or
                PrimaryNoNewArray * JavaToken.BRACKETLEFT * Expression * JavaToken.BRACKETRIGHT
        MethodInvocation = MethodName * JavaToken.PARENTHLEFT * opt(argumentList) * JavaToken.PARENTHRIGHT or
                TypeName * JavaToken.DOT * opt(typeArguments) * identifier * JavaToken.PARENTHLEFT * opt(argumentList) * JavaToken.PARENTHRIGHT or
                ExpressionName * JavaToken.DOT * opt(typeArguments) * identifier * JavaToken.PARENTHLEFT * opt(argumentList) * JavaToken.PARENTHRIGHT or
                JavaToken.SUPER * JavaToken.DOT * opt(typeArguments) * identifier * JavaToken.PARENTHLEFT * opt(argumentList) * JavaToken.PARENTHRIGHT or
                TypeName * JavaToken.DOT * JavaToken.SUPER * JavaToken.DOT * opt(typeArguments) * identifier * JavaToken.PARENTHLEFT * opt(argumentList) * JavaToken.PARENTHRIGHT
        argumentList = Expression * Many(JavaToken.COMMA * Expression)
        MethodReference = ExpressionName * JavaToken.DOUBLECOLON * opt(typeArguments) * identifier or
                ReferenceType * JavaToken.DOUBLECOLON * opt(typeArguments) * identifier or
                Primary * JavaToken.DOUBLECOLON * opt(typeArguments) * identifier or
                JavaToken.SUPER * JavaToken.DOUBLECOLON * opt(typeArguments) * identifier or
                TypeName * JavaToken.DOT * JavaToken.SUPER * JavaToken.DOUBLECOLON * opt(typeArguments) * identifier or
                ClassType * JavaToken.DOUBLECOLON * opt(typeArguments) * JavaToken.NEW or
                ArrayType * JavaToken.DOUBLECOLON * JavaToken.NEW
        ArrayCreationExpression = JavaToken.NEW * PrimitiveType * DimExprs * opt(Dims) or
                JavaToken.NEW * classOrInterfaceType * DimExprs * opt(Dims) or
                JavaToken.NEW * PrimitiveType * Dims * ArrayInitializer or
                JavaToken.NEW * classOrInterfaceType * Dims * ArrayInitializer
        DimExprs = DimExpr * Many(DimExpr)
        DimExpr = Many(Annotation) * JavaToken.BRACKETLEFT * Expression * JavaToken.BRACKETRIGHT
        Expression = LambdaExpression or assignmentExpression
        LambdaExpression = LambdaParameters * JavaToken.ARROW * LambdaBody
        LambdaParameters = identifier or JavaToken.PARENTHLEFT * opt(FormalParameterList) * JavaToken.PARENTHRIGHT or
                JavaToken.PARENTHLEFT * InferredFormalParameterList * JavaToken.PARENTHRIGHT
        InferredFormalParameterList = identifier * Many(JavaToken.COMMA * identifier)
        LambdaBody = Expression or Block
        assignmentExpression = ConditionalExpression or assignment
        assignment = LeftHandSide * assignmentOperator * Expression
        LeftHandSide = ExpressionName or FieldAccess or ArrayAccess
        assignmentOperator = JavaToken.ASSIGN or JavaToken.STARASSIGN or JavaToken.SLASHASSIGN or JavaToken.PERCENTASSIGN or JavaToken.PLUSASSIGN or JavaToken.MINUSASSIGN or
                JavaToken.SHIFTLEFTASSIGN or JavaToken.SHIFTRIGHTASSIGN or JavaToken.USRIGHTSHIFTASSIGN or JavaToken.ANDASSIGN or JavaToken.XORASSIGN or JavaToken.ORASSIGN
        ConditionalExpression = ConditionalOrExpression or
                ConditionalOrExpression * JavaToken.QUESTIONMARK * Expression * JavaToken.COLON * ConditionalExpression or
                ConditionalOrExpression * JavaToken.QUESTIONMARK * Expression * JavaToken.COLON * LambdaExpression
        ConditionalOrExpression = ConditionalAndExpression or
                ConditionalOrExpression * JavaToken.OR * ConditionalAndExpression
        ConditionalAndExpression = InclusiveOrExpression or
                ConditionalAndExpression * JavaToken.AND * InclusiveOrExpression
        InclusiveOrExpression = ExclusiveOrExpression or
                InclusiveOrExpression * JavaToken.ORBIT * ExclusiveOrExpression
        ExclusiveOrExpression = AndExpression or ExclusiveOrExpression * JavaToken.XORBIT * AndExpression
        AndExpression = EqualityExpression or AndExpression * JavaToken.ANDBIT * EqualityExpression
        EqualityExpression = RelationalExpression or EqualityExpression * JavaToken.EQ * RelationalExpression or
                EqualityExpression * JavaToken.NOTEQ * RelationalExpression
        RelationalExpression = ShiftExpression or RelationalExpression * JavaToken.DIAMONDLEFT * ShiftExpression or
                RelationalExpression * JavaToken.DIAMONDRIGHT * ShiftExpression or RelationalExpression * JavaToken.LESSEQ * ShiftExpression or
                RelationalExpression * JavaToken.GREATEQ * ShiftExpression or RelationalExpression * JavaToken.INSTANCEOF * ReferenceType
        ShiftExpression = AdditiveExpression or ShiftExpression * JavaToken.LEFTSHIFT * AdditiveExpression or
                ShiftExpression * JavaToken.RIGHTSHIT * AdditiveExpression or
                ShiftExpression * JavaToken.USRIGHTSHIFT * AdditiveExpression
        AdditiveExpression = MultiplicativeExpression or AdditiveExpression * JavaToken.PLUS * MultiplicativeExpression or
                AdditiveExpression * JavaToken.MINUS * MultiplicativeExpression
        MultiplicativeExpression = UnaryExpression or MultiplicativeExpression * JavaToken.STAR * UnaryExpression or
                MultiplicativeExpression * JavaToken.SLASH * UnaryExpression or
                MultiplicativeExpression * JavaToken.PERCENT * UnaryExpression
        UnaryExpression = preIncrementExpression or preDecrementExpression or JavaToken.PLUS * UnaryExpression or
                JavaToken.MINUS * UnaryExpression or UnaryExpressionNotPlusMinus
        preIncrementExpression = JavaToken.PLUSPLUS * UnaryExpression
        preDecrementExpression = JavaToken.MINUSMINUS * UnaryExpression
        UnaryExpressionNotPlusMinus = PostfixExpression or JavaToken.TILDA * UnaryExpression or JavaToken.EXCLAMATIONMARK * UnaryExpression or
                CastExpression
        PostfixExpression = Primary or ExpressionName or postIncrementExpression or postDecrementExpression
        postIncrementExpression = PostfixExpression * JavaToken.PLUSPLUS
        postDecrementExpression = PostfixExpression * JavaToken.MINUSMINUS
        CastExpression = JavaToken.PARENTHLEFT * PrimitiveType * JavaToken.PARENTHRIGHT * UnaryExpression or
                JavaToken.PARENTHLEFT * ReferenceType * Many(AdditionalBound) * JavaToken.PARENTHRIGHT * UnaryExpressionNotPlusMinus or
                JavaToken.PARENTHLEFT * ReferenceType * Many(AdditionalBound) * JavaToken.PARENTHRIGHT * LambdaExpression
        constantExpression = Expression

        setStart(CompilationUnit)
    }
}