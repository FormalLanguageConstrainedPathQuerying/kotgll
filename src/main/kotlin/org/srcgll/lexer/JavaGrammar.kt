package org.srcgll.lexer

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*
import org.srcgll.rsm.symbol.Term

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
        identifier = Term(JavaToken.ID)

        Literal = Term(JavaToken.INTEGERLIT) or Term(JavaToken.FLOATINGLIT) or Term(JavaToken.BOOLEANLIT) or
                Term(JavaToken.CHARLIT) or Term(JavaToken.STRINGLIT) or Term(JavaToken.NULLLIT)

        Type = PrimitiveType or ReferenceType
        PrimitiveType = Many(Annotation) * NumericType or Many(Annotation) * Term(JavaToken.BOOLEAN)
        NumericType = integralType or floatingPointType
        integralType = Term(JavaToken.BYTE) or Term(JavaToken.SHORT) or Term(JavaToken.INT) or Term(JavaToken.LONG) or Term(JavaToken.CHAR)
        floatingPointType = Term(JavaToken.FLOAT) or Term(JavaToken.DOUBLE)
        ReferenceType = classOrInterfaceType or TypeVariable or ArrayType
        classOrInterfaceType = ClassType or interfaceType
        ClassType = Many(Annotation) * identifier * Option(typeArguments) or
                classOrInterfaceType * Term(JavaToken.DOT) * Many(Annotation) * identifier * Option(typeArguments)
        interfaceType = ClassType
        TypeVariable = Many(Annotation) * identifier
        ArrayType = PrimitiveType * Dims or classOrInterfaceType * Dims or TypeVariable * Dims
        Dims = Some(Many(Annotation) * Term(JavaToken.BRACKETLEFT) * Term(JavaToken.BRACKETRIGHT))
        TypeParameter  = Many(TypeParameterModifier) * identifier * Option(TypeBound)
        TypeParameterModifier = Annotation
        TypeBound = Term(JavaToken.EXTENDS) * TypeVariable or Term(JavaToken.EXTENDS) * classOrInterfaceType * Many(AdditionalBound)
        AdditionalBound = Term(JavaToken.ANDBIT) * interfaceType
        typeArguments = Term(JavaToken.DIAMONDLEFT) * typeArgumentList * Term(JavaToken.DIAMONDRIGHT)
        typeArgumentList = typeArgument * Many(Term(JavaToken.COMMA) * typeArgument)
        typeArgument = ReferenceType or Wildcard
        Wildcard = Many(Annotation) * Term(JavaToken.QUESTIONMARK) * Option(WildcardBounds)
        WildcardBounds = Term(JavaToken.EXTENDS) * ReferenceType or Term(JavaToken.SUPER) * ReferenceType

        TypeName = identifier or PackageOrTypeName * Term(JavaToken.DOT) * identifier
        PackageOrTypeName = identifier or PackageOrTypeName * Term(JavaToken.DOT) * identifier
        ExpressionName = identifier or AmbiguousName * Term(JavaToken.DOT) * identifier
        MethodName = identifier
        PackageName = identifier or PackageName * Term(JavaToken.DOT) * identifier
        AmbiguousName = identifier or AmbiguousName * Term(JavaToken.DOT) * identifier

        CompilationUnit = Option(PackageDeclaration) * Many(ImportDeclaration) * Many(TypeDeclaration)
        PackageDeclaration = Many(PackageModifier) * Term(JavaToken.PACKAGE) * identifier * Many(Term(JavaToken.DOT) * identifier) * Term(JavaToken.SEMICOLON)
        PackageModifier = Annotation
        ImportDeclaration = SingleTypeImportDeclaration or TypeImportOnDemandDeclaration or
                SingleStaticImportDeclaration or StaticImportOnDemandDeclaration
        SingleTypeImportDeclaration = Term(JavaToken.IMPORT) * TypeName * Term(JavaToken.SEMICOLON)
        TypeImportOnDemandDeclaration = Term(JavaToken.IMPORT) * PackageOrTypeName * Term(JavaToken.DOT) * Term(JavaToken.STAR) * Term(JavaToken.SEMICOLON)
        SingleStaticImportDeclaration = Term(JavaToken.IMPORT) * Term(JavaToken.STATIC) * TypeName * Term(JavaToken.DOT) * identifier * Term(JavaToken.SEMICOLON)
        StaticImportOnDemandDeclaration = Term(JavaToken.IMPORT) * Term(JavaToken.STATIC) * TypeName * Term(JavaToken.DOT) * Term(JavaToken.STAR) * Term(JavaToken.SEMICOLON)
        TypeDeclaration = ClassDeclaration or interfaceDeclaration or Term(JavaToken.SEMICOLON)

        ClassDeclaration = NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration = Many(ClassModifier) * Term(JavaToken.CLASS) * identifier *
                Option(TypeParameters) * Option(Superclass) * Option(superinterfaces) * ClassBody
        ClassModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or
                Term(JavaToken.ABSTRACT) or Term(JavaToken.STATIC) or Term(JavaToken.FINAL) or Term(JavaToken.STRICTFP)
        TypeParameters = Term(JavaToken.DIAMONDLEFT) * TypeParameterList * Term(JavaToken.DIAMONDRIGHT)
        TypeParameterList = TypeParameter  * Many(Term(JavaToken.COMMA) * TypeParameter)
        Superclass = Term(JavaToken.EXTENDS) * ClassType
        superinterfaces = Term(JavaToken.IMPLEMENTS) * interfaceTypeList
        interfaceTypeList = interfaceType  * Many(Term(JavaToken.COMMA) * interfaceType)
        ClassBody = Term(JavaToken.CURLYLEFT) * Many(ClassBodyDeclaration) * Term(JavaToken.CURLYRIGHT)
        ClassBodyDeclaration = ClassMemberDeclaration or InstanceInitializer or StaticInitializer or ConstructorDeclaration
        ClassMemberDeclaration = FieldDeclaration or MethodDeclaration or ClassDeclaration or interfaceDeclaration or Term(JavaToken.SEMICOLON)
        FieldDeclaration = Many(FieldModifier) * unannType * VariableDeclaratorList * Term(JavaToken.SEMICOLON)
        FieldModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or Term(JavaToken.STATIC) or
                Term(JavaToken.FINAL) or Term(JavaToken.TRANSIENT) or Term(JavaToken.VOLATILE)
        VariableDeclaratorList = VariableDeclarator * Many(Term(JavaToken.COMMA) * VariableDeclarator)
        VariableDeclarator = VariableDeclaratorId * Option(Term(JavaToken.ASSIGN) * VariableInitializer)
        VariableDeclaratorId = identifier * Option(Dims)
        VariableInitializer = Expression or ArrayInitializer
        unannType = UnannPrimitiveType or UnannReferenceType
        UnannPrimitiveType = NumericType or Term(JavaToken.BOOLEAN)
        UnannReferenceType = unannClassOrInterfaceType or unannTypeVariable or UnannArrayType
        unannClassOrInterfaceType = UnannClassType or unannInterfaceType
        UnannClassType = identifier * Option(typeArguments) or
                unannClassOrInterfaceType * Term(JavaToken.DOT) * Many(Annotation) * identifier * Option(typeArguments)
        unannInterfaceType = UnannClassType
        unannTypeVariable = identifier
        UnannArrayType = UnannPrimitiveType * Dims or unannClassOrInterfaceType * Dims or unannTypeVariable * Dims
        MethodDeclaration = Many(MethodModifier) * MethodHeader * MethodBody
        MethodModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or Term(JavaToken.ABSTRACT) or
                Term(JavaToken.STATIC) or Term(JavaToken.FINAL) or Term(JavaToken.SYNCHRONIZED) or Term(JavaToken.NATIVE) or Term(JavaToken.STRICTFP)
        MethodHeader = Result * MethodDeclarator * Option(Throws) or TypeParameters * Many(Annotation) * Result *
                MethodDeclarator * Option(Throws)
        Result = unannType or Term(JavaToken.VOID)
        MethodDeclarator = identifier * Term(JavaToken.PARENTHLEFT) * Option(FormalParameterList) * Term(JavaToken.PARENTHRIGHT) * Option(Dims)
        FormalParameterList = ReceiverParameter or FormalParameters * Term(JavaToken.COMMA) * LastFormalParameter or
                LastFormalParameter
        FormalParameters = FormalParameter * Many(Term(JavaToken.COMMA) * FormalParameter) or
                ReceiverParameter * Many(Term(JavaToken.COMMA) * FormalParameter)
        FormalParameter = Many(VariableModifier) * unannType * VariableDeclaratorId
        VariableModifier = Annotation or Term(JavaToken.FINAL)
        LastFormalParameter = Many(VariableModifier) * unannType * Many(Annotation) * Term(JavaToken.ELLIPSIS) * VariableDeclaratorId or FormalParameter
        ReceiverParameter = Many(Annotation) * unannType * Option(identifier * Term(JavaToken.DOT)) * Term(JavaToken.THIS)
        Throws = Term(JavaToken.THROWS) * exceptionTypeList
        exceptionTypeList = exceptionType * Many(Term(JavaToken.COMMA) * exceptionType)
        exceptionType = ClassType or TypeVariable
        MethodBody = Block or Term(JavaToken.SEMICOLON)
        InstanceInitializer = Block
        StaticInitializer = Term(JavaToken.STATIC) * Block
        ConstructorDeclaration = Many(ConstructorModifier) * ConstructorDeclarator * Option(Throws) * ConstructorBody
        ConstructorModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE)
        ConstructorDeclarator = Option(TypeParameters) * SimpleTypeName * Term(JavaToken.PARENTHLEFT) * Option(FormalParameterList) * Term(JavaToken.PARENTHRIGHT)
        SimpleTypeName = identifier
        ConstructorBody = Term(JavaToken.CURLYLEFT) * Option(ExplicitConstructorInvocation) * Option(blockStatements) * Term(JavaToken.CURLYRIGHT)
        ExplicitConstructorInvocation = Option(typeArguments) * Term(JavaToken.THIS) * Term(JavaToken.PARENTHLEFT) * Option(argumentList) * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON) or
                Option(typeArguments) * Term(JavaToken.SUPER) * Term(JavaToken.PARENTHLEFT) * Option(argumentList) * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON) or
                ExpressionName * Term(JavaToken.DOT) * Option(typeArguments) * Term(JavaToken.SUPER) * Term(JavaToken.PARENTHLEFT) * Option(argumentList) * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON) or
                Primary * Term(JavaToken.DOT) * Option(typeArguments) * Term(JavaToken.SUPER) * Term(JavaToken.PARENTHLEFT) * Option(argumentList) * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON)
        EnumDeclaration = Many(ClassModifier) * Term(JavaToken.ENUM) * identifier * Option(superinterfaces) * EnumBody
        EnumBody = Term(JavaToken.CURLYLEFT) * Option(enumConstantList) * Option(Term(JavaToken.COMMA)) * Option(EnumBodyDeclarations) * Term(JavaToken.CURLYRIGHT)
        enumConstantList = enumConstant * Many(Term(JavaToken.COMMA) * enumConstant)
        enumConstant = Many(enumConstantModifier) * identifier * Option(Term(JavaToken.PARENTHLEFT) * Option(argumentList) * Term(JavaToken.PARENTHRIGHT) * Option(ClassBody))
        enumConstantModifier = Annotation
        EnumBodyDeclarations = Term(JavaToken.SEMICOLON) * Many(ClassBodyDeclaration)

        interfaceDeclaration = normalInterfaceDeclaration or annotationTypeDeclaration
        normalInterfaceDeclaration =
            Many(interfaceModifier) * Term(JavaToken.INTERFACE) * identifier * Option(TypeParameters) * Option(extendsInterfaces) * interfaceBody
        interfaceModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or
                Term(JavaToken.ABSTRACT) or Term(JavaToken.STATIC) or Term(JavaToken.STRICTFP)
        extendsInterfaces = Term(JavaToken.EXTENDS) * interfaceTypeList
        interfaceBody = Term(JavaToken.CURLYLEFT) * Many(interfaceMemberDeclaration) * Term(JavaToken.CURLYRIGHT)
        interfaceMemberDeclaration = constantDeclaration or interfaceMethodDeclaration or ClassDeclaration or interfaceDeclaration or Term(JavaToken.SEMICOLON)
        interfaceMethodDeclaration = Many(interfaceMethodModifier) * MethodHeader * MethodBody
        interfaceMethodModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.ABSTRACT) or Term(JavaToken.DEFAULT) or Term(JavaToken.STATIC) or Term(JavaToken.STRICTFP)
        constantDeclaration = Many(constantModifier) * unannType * VariableDeclaratorList * Term(JavaToken.SEMICOLON)
        constantModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.ABSTRACT) or Term(JavaToken.DEFAULT) or Term(JavaToken.STATIC) or Term(JavaToken.STRICTFP)
        annotationTypeDeclaration = Many(interfaceModifier) * Term(JavaToken.AT) * Term(JavaToken.INTERFACE) * identifier * annotationTypeBody
        annotationTypeBody = Term(JavaToken.CURLYLEFT) * Many(annotationTypeMemberDeclaration) * Term(JavaToken.CURLYRIGHT)
        annotationTypeMemberDeclaration = annotationTypeElementDeclaration or constantDeclaration or ClassDeclaration or interfaceDeclaration or Term(JavaToken.SEMICOLON)
        annotationTypeElementDeclaration =
            Many(annotationTypeElementModifier) * unannType * identifier * Term(JavaToken.PARENTHLEFT) * Term(JavaToken.PARENTHRIGHT) * Option(Dims) * Option(DefaultValue) * Term(JavaToken.SEMICOLON)
        annotationTypeElementModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.ABSTRACT)
        DefaultValue = Term(JavaToken.DEFAULT) * elementValue
        Annotation = NormalAnnotation or MarkerAnnotation or singleElementAnnotation
        NormalAnnotation = Term(JavaToken.AT) * TypeName * Term(JavaToken.PARENTHLEFT) * Option(elementValuePairList) * Term(JavaToken.PARENTHRIGHT)
        elementValuePairList = elementValuePair * Many(Term(JavaToken.COMMA) * elementValuePair)
        elementValuePair = identifier * Term(JavaToken.ASSIGN) * elementValue
        elementValue = ConditionalExpression or elementValueArrayInitializer or Annotation
        elementValueArrayInitializer = Term(JavaToken.CURLYLEFT) * Option(elementValueList) * Option(Term(JavaToken.COMMA)) * Term(JavaToken.CURLYRIGHT)
        elementValueList = elementValue * Many(Term(JavaToken.COMMA) * elementValue)
        MarkerAnnotation = Term(JavaToken.AT) * TypeName
        singleElementAnnotation = Term(JavaToken.AT) * TypeName * Term(JavaToken.PARENTHLEFT) * elementValue * Term(JavaToken.PARENTHRIGHT)

        ArrayInitializer = Term(JavaToken.CURLYLEFT) * Option(VariableInitializerList) * Option(Term(JavaToken.COMMA)) * Term(JavaToken.CURLYRIGHT)
        VariableInitializerList = VariableInitializer * Many(Term(JavaToken.COMMA) * VariableInitializer)

        Block = Term(JavaToken.CURLYLEFT) * Option(blockStatements) * Term(JavaToken.CURLYRIGHT)
        blockStatements = blockStatement * Many(blockStatement)
        blockStatement = localVariableDeclarationStatement or ClassDeclaration or statement
        localVariableDeclarationStatement = LocalVariableDeclaration * Term(JavaToken.SEMICOLON)
        LocalVariableDeclaration = Many(VariableModifier) * unannType * VariableDeclaratorList
        statement = statementWithoutTrailingSubstatement or labeledStatement or ifThenStatement or ifThenElseStatement or
                whileStatement or forStatement
        statementNoShortIf = statementWithoutTrailingSubstatement or labeledStatementNoShortIf or ifThenElseStatementNoShortIf or
                whileStatementNoShortIf or forStatementNoShortIf
        statementWithoutTrailingSubstatement = Block or emptyStatement or expressionStatement or assertStatement or
                switchStatement or doStatement or breakStatement or continueStatement or returnStatement or synchronizedStatement or
                throwStatement or tryStatement
        emptyStatement = Term(JavaToken.SEMICOLON)
        labeledStatement = identifier * Term(JavaToken.COLON) * statement
        labeledStatementNoShortIf = identifier * Term(JavaToken.COLON) * statementNoShortIf
        expressionStatement = statementExpression * Term(JavaToken.SEMICOLON)
        statementExpression = assignment or preIncrementExpression or preDecrementExpression or postIncrementExpression or
                postDecrementExpression or MethodInvocation or ClassInstanceCreationExpression
        ifThenStatement = Term(JavaToken.IF) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * statement
        ifThenElseStatement = Term(JavaToken.IF) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * statementNoShortIf * Term(JavaToken.ELSE) * statement
        ifThenElseStatementNoShortIf =
            Term(JavaToken.IF) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * statementNoShortIf * Term(JavaToken.ELSE) * statementNoShortIf
        assertStatement = Term(JavaToken.ASSERT) * Expression * Term(JavaToken.SEMICOLON) or
                Term(JavaToken.ASSERT) * Expression * Term(JavaToken.COLON) * Expression * Term(JavaToken.SEMICOLON)
        switchStatement = Term(JavaToken.SWITCH) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * SwitchBlock
        SwitchBlock = Term(JavaToken.CURLYLEFT) * Many(switchBlockStatementGroup) * Many(SwitchLabel) * Term(JavaToken.CURLYRIGHT)
        switchBlockStatementGroup = SwitchLabels * blockStatements
        SwitchLabels = SwitchLabel * Many(SwitchLabel)
        SwitchLabel = Term(JavaToken.CASE) * constantExpression * Term(JavaToken.COLON) or
                Term(JavaToken.CASE) * enumConstantName * Term(JavaToken.COLON) or Term(JavaToken.DEFAULT) * Term(JavaToken.COLON)
        enumConstantName = identifier
        whileStatement = Term(JavaToken.WHILE) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * statement
        whileStatementNoShortIf = Term(JavaToken.WHILE) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * statementNoShortIf
        doStatement = Term(JavaToken.DO) * statement * Term(JavaToken.WHILE) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON)
        forStatement = basicForStatement or enhancedForStatement
        forStatementNoShortIf = basicForStatementNoShortIf or enhancedForStatementNoShortIf
        basicForStatement = Term(JavaToken.FOR) * Term(JavaToken.PARENTHLEFT) * Option(ForInit) * Term(JavaToken.SEMICOLON) * Option(Expression) * Term(JavaToken.SEMICOLON) * Option(ForUpdate) * Term(JavaToken.PARENTHRIGHT) * statement
        basicForStatementNoShortIf = Term(JavaToken.FOR) * Term(JavaToken.PARENTHLEFT) * Option(ForInit) * Term(JavaToken.SEMICOLON) * Option(Expression) * Term(JavaToken.SEMICOLON) * Option(ForUpdate) * Term(JavaToken.PARENTHRIGHT) * statementNoShortIf
        ForInit = statementExpressionList or LocalVariableDeclaration
        ForUpdate = statementExpressionList
        statementExpressionList = statementExpression * Many(Term(JavaToken.COMMA) * statementExpression)
        enhancedForStatement = Term(JavaToken.FOR) * Term(JavaToken.PARENTHLEFT) * Many(VariableModifier) * unannType * VariableDeclaratorId * Term(JavaToken.COLON) * Expression * Term(JavaToken.PARENTHRIGHT) * statement
        enhancedForStatementNoShortIf = Term(JavaToken.FOR) * Term(JavaToken.PARENTHLEFT) * Many(VariableModifier) * unannType * VariableDeclaratorId * Term(JavaToken.COLON) * Expression * Term(JavaToken.PARENTHRIGHT) * statementNoShortIf
        breakStatement = Term(JavaToken.BREAK) * Option(identifier) * Term(JavaToken.SEMICOLON)
        continueStatement = Term(JavaToken.CONTINUE) * Option(identifier) * Term(JavaToken.SEMICOLON)
        returnStatement = Term(JavaToken.RETURN) * Option(Expression) * Term(JavaToken.SEMICOLON)
        throwStatement = Term(JavaToken.THROW) * Expression * Term(JavaToken.SEMICOLON)
        synchronizedStatement = Term(JavaToken.SYNCHRONIZED) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * Block
        tryStatement = Term(JavaToken.TRY) * Block * Catches or Term(JavaToken.TRY) * Block * Option(Catches) * Finally or tryWithResourcesStatement
        Catches = CatchClause * Many(CatchClause)
        CatchClause = Term(JavaToken.CATCH) * Term(JavaToken.PARENTHLEFT) * CatchFormalParameter * Term(JavaToken.PARENTHRIGHT) * Block
        CatchFormalParameter = Many(VariableModifier) * CatchType * VariableDeclaratorId
        CatchType = UnannClassType * Many(Term(JavaToken.ORBIT) * ClassType)
        Finally = Term(JavaToken.FINALLY) * Block
        tryWithResourcesStatement = Term(JavaToken.TRY) * ResourceSpecification * Block * Option(Catches) * Option(Finally)
        ResourceSpecification = Term(JavaToken.PARENTHLEFT) * ResourceList * Option(Term(JavaToken.SEMICOLON)) * Term(JavaToken.PARENTHRIGHT)
        ResourceList = Resource * Many(Term(JavaToken.COMMA) * Resource)
        Resource = Many(VariableModifier) * unannType * VariableDeclaratorId * Term(JavaToken.ASSIGN) * Expression

        Primary = PrimaryNoNewArray or ArrayCreationExpression
        PrimaryNoNewArray = Literal or ClassLiteral or Term(JavaToken.THIS) or TypeName * Term(JavaToken.DOT) * Term(JavaToken.THIS) or
                Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) or ClassInstanceCreationExpression or FieldAccess or
                ArrayAccess or MethodInvocation or MethodReference
        ClassLiteral = TypeName * Many(Term(JavaToken.BRACKETLEFT) * Term(JavaToken.BRACKETRIGHT)) * Term(JavaToken.DOT) * Term(JavaToken.CLASS) or
                NumericType * Many(Term(JavaToken.BRACKETLEFT) * Term(JavaToken.BRACKETRIGHT)) * Term(JavaToken.DOT) * Term(JavaToken.CLASS) or
                Term(JavaToken.BOOLEAN) * Many(Term(JavaToken.BRACKETLEFT) * Term(JavaToken.BRACKETRIGHT)) * Term(JavaToken.DOT) * Term(JavaToken.CLASS) or
                Term(JavaToken.VOID) * Term(JavaToken.DOT) * Term(JavaToken.CLASS)
        ClassInstanceCreationExpression = UnqualifiedClassInstanceCreationExpression or
                ExpressionName * Term(JavaToken.DOT) * UnqualifiedClassInstanceCreationExpression or
                Primary * Term(JavaToken.DOT) * UnqualifiedClassInstanceCreationExpression
        UnqualifiedClassInstanceCreationExpression =
            Term(JavaToken.NEW) * Option(typeArguments) * classOrInterfaceTypeToInstantiate * Term(JavaToken.PARENTHLEFT) * Option(argumentList) * Term(JavaToken.PARENTHRIGHT) * Option(ClassBody)
        classOrInterfaceTypeToInstantiate = Many(Annotation) * identifier * Many(Term(JavaToken.DOT) * Many(Annotation) * identifier) * Option(typeArgumentsOrDiamond)
        typeArgumentsOrDiamond = typeArguments or Term(JavaToken.DIAMOND)
        FieldAccess = Primary * Term(JavaToken.DOT) * identifier or Term(JavaToken.SUPER) * Term(JavaToken.DOT) * identifier or
                TypeName * Term(JavaToken.DOT) * Term(JavaToken.SUPER) * Term(JavaToken.DOT) * identifier
        ArrayAccess = ExpressionName * Term(JavaToken.BRACKETLEFT) * Expression * Term(JavaToken.BRACKETRIGHT) or
                PrimaryNoNewArray * Term(JavaToken.BRACKETLEFT) * Expression * Term(JavaToken.BRACKETRIGHT)
        MethodInvocation = MethodName * Term(JavaToken.PARENTHLEFT) * Option(argumentList) * Term(JavaToken.PARENTHRIGHT) or
                TypeName * Term(JavaToken.DOT) * Option(typeArguments) * identifier * Term(JavaToken.PARENTHLEFT) * Option(argumentList) * Term(JavaToken.PARENTHRIGHT) or
                ExpressionName * Term(JavaToken.DOT) * Option(typeArguments) * identifier * Term(JavaToken.PARENTHLEFT) * Option(argumentList) * Term(JavaToken.PARENTHRIGHT) or
                Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Option(typeArguments) * identifier * Term(JavaToken.PARENTHLEFT) * Option(argumentList) * Term(JavaToken.PARENTHRIGHT) or
                TypeName * Term(JavaToken.DOT) * Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Option(typeArguments) * identifier * Term(JavaToken.PARENTHLEFT) * Option(argumentList) * Term(JavaToken.PARENTHRIGHT)
        argumentList = Expression * Many(Term(JavaToken.COMMA) * Expression)
        MethodReference = ExpressionName * Term(JavaToken.DOUBLECOLON) * Option(typeArguments) * identifier or
                ReferenceType * Term(JavaToken.DOUBLECOLON) * Option(typeArguments) * identifier or
                Primary * Term(JavaToken.DOUBLECOLON) * Option(typeArguments) * identifier or
                Term(JavaToken.SUPER) * Term(JavaToken.DOUBLECOLON) * Option(typeArguments) * identifier or
                TypeName * Term(JavaToken.DOT) * Term(JavaToken.SUPER) * Term(JavaToken.DOUBLECOLON) * Option(typeArguments) * identifier or
                ClassType * Term(JavaToken.DOUBLECOLON) * Option(typeArguments) * Term(JavaToken.NEW) or
                ArrayType * Term(JavaToken.DOUBLECOLON) * Term(JavaToken.NEW)
        ArrayCreationExpression = Term(JavaToken.NEW) * PrimitiveType * DimExprs * Option(Dims) or
                Term(JavaToken.NEW) * classOrInterfaceType * DimExprs * Option(Dims) or
                Term(JavaToken.NEW) * PrimitiveType * Dims * ArrayInitializer or
                Term(JavaToken.NEW) * classOrInterfaceType * Dims * ArrayInitializer
        DimExprs = DimExpr * Many(DimExpr)
        DimExpr = Many(Annotation) * Term(JavaToken.BRACKETLEFT) * Expression * Term(JavaToken.BRACKETRIGHT)
        Expression = LambdaExpression or assignmentExpression
        LambdaExpression = LambdaParameters * Term(JavaToken.ARROW) * LambdaBody
        LambdaParameters = identifier or Term(JavaToken.PARENTHLEFT) * Option(FormalParameterList) * Term(JavaToken.PARENTHRIGHT) or
                Term(JavaToken.PARENTHLEFT) * InferredFormalParameterList * Term(JavaToken.PARENTHRIGHT)
        InferredFormalParameterList = identifier * Many(Term(JavaToken.COMMA) * identifier)
        LambdaBody = Expression or Block
        assignmentExpression = ConditionalExpression or assignment
        assignment = LeftHandSide * assignmentOperator * Expression
        LeftHandSide = ExpressionName or FieldAccess or ArrayAccess
        assignmentOperator = Term(JavaToken.ASSIGN) or Term(JavaToken.STARASSIGN) or Term(JavaToken.SLASHASSIGN) or Term(JavaToken.PERCENTASSIGN) or Term(JavaToken.PLUSASSIGN) or Term(JavaToken.MINUSASSIGN) or
                Term(JavaToken.SHIFTLEFTASSIGN) or Term(JavaToken.SHIFTRIGHTASSIGN) or Term(JavaToken.USRIGHTSHIFTASSIGN) or Term(JavaToken.ANDASSIGN) or Term(JavaToken.XORASSIGN) or Term(JavaToken.ORASSIGN)
        ConditionalExpression = ConditionalOrExpression or
                ConditionalOrExpression * Term(JavaToken.QUESTIONMARK) * Expression * Term(JavaToken.COLON) * ConditionalExpression or
                ConditionalOrExpression * Term(JavaToken.QUESTIONMARK) * Expression * Term(JavaToken.COLON) * LambdaExpression
        ConditionalOrExpression = ConditionalAndExpression or
                ConditionalOrExpression * Term(JavaToken.OR) * ConditionalAndExpression
        ConditionalAndExpression = InclusiveOrExpression or
                ConditionalAndExpression * Term(JavaToken.AND) * InclusiveOrExpression
        InclusiveOrExpression = ExclusiveOrExpression or
                InclusiveOrExpression * Term(JavaToken.ORBIT) * ExclusiveOrExpression
        ExclusiveOrExpression = AndExpression or ExclusiveOrExpression * Term(JavaToken.XORBIT) * AndExpression
        AndExpression = EqualityExpression or AndExpression * Term(JavaToken.ANDBIT) * EqualityExpression
        EqualityExpression = RelationalExpression or EqualityExpression * Term(JavaToken.EQ) * RelationalExpression or
                EqualityExpression * Term(JavaToken.NOTEQ) * RelationalExpression
        RelationalExpression = ShiftExpression or RelationalExpression * Term(JavaToken.DIAMONDLEFT) * ShiftExpression or
                RelationalExpression * Term(JavaToken.DIAMONDRIGHT) * ShiftExpression or RelationalExpression * Term(JavaToken.LESSEQ) * ShiftExpression or
                RelationalExpression * Term(JavaToken.GREATEQ) * ShiftExpression or RelationalExpression * Term(JavaToken.INSTANCEOF) * ReferenceType
        ShiftExpression = AdditiveExpression or ShiftExpression * Term(JavaToken.LEFTSHIFT) * AdditiveExpression or
                ShiftExpression * Term(JavaToken.RIGHTSHIT) * AdditiveExpression or
                ShiftExpression * Term(JavaToken.USRIGHTSHIFT) * AdditiveExpression
        AdditiveExpression = MultiplicativeExpression or AdditiveExpression * Term(JavaToken.PLUS) * MultiplicativeExpression or
                AdditiveExpression * Term(JavaToken.MINUS) * MultiplicativeExpression
        MultiplicativeExpression = UnaryExpression or MultiplicativeExpression * Term(JavaToken.STAR) * UnaryExpression or
                MultiplicativeExpression * Term(JavaToken.SLASH) * UnaryExpression or
                MultiplicativeExpression * Term(JavaToken.PERCENT) * UnaryExpression
        UnaryExpression = preIncrementExpression or preDecrementExpression or Term(JavaToken.PLUS) * UnaryExpression or
                Term(JavaToken.MINUS) * UnaryExpression or UnaryExpressionNotPlusMinus
        preIncrementExpression = Term(JavaToken.PLUSPLUS) * UnaryExpression
        preDecrementExpression = Term(JavaToken.MINUSMINUS) * UnaryExpression
        UnaryExpressionNotPlusMinus = PostfixExpression or Term(JavaToken.TILDA) * UnaryExpression or Term(JavaToken.EXCLAMATIONMARK) * UnaryExpression or
                CastExpression
        PostfixExpression = Primary or ExpressionName or postIncrementExpression or postDecrementExpression
        postIncrementExpression = PostfixExpression * Term(JavaToken.PLUSPLUS)
        postDecrementExpression = PostfixExpression * Term(JavaToken.MINUSMINUS)
        CastExpression = Term(JavaToken.PARENTHLEFT) * PrimitiveType * Term(JavaToken.PARENTHRIGHT) * UnaryExpression or
                Term(JavaToken.PARENTHLEFT) * ReferenceType * Many(AdditionalBound) * Term(JavaToken.PARENTHRIGHT) * UnaryExpressionNotPlusMinus or
                Term(JavaToken.PARENTHLEFT) * ReferenceType * Many(AdditionalBound) * Term(JavaToken.PARENTHRIGHT) * LambdaExpression
        constantExpression = Expression

        setStart(CompilationUnit)
    }
}