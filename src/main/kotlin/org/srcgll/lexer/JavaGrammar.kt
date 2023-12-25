package org.srcgll.lexer

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class JavaGrammar : Grammar() {
    var CompilationUnit by NT()
    var Identifier by NT()
    var Literal by NT()
    var Type by NT()
    var PrimitiveType by NT()
    var ReferenceType by NT()
    var Annotation by NT()
    var NumericType by NT()
    var IntegralType by NT()
    var FloatingPointType by NT()
    var ClassOrInterfaceType by NT()
    var TypeVariable by NT()
    var ArrayType by NT()
    var ClassType by NT()
    var InterfaceType by NT()
    var TypeArguments by NT()
    var Dims by NT()
    var TypeParameter by NT()
    var TypeParameterModifier by NT()
    var TypeBound by NT()
    var AdditionalBound by NT()
    var TypeArgumentList by NT()
    var TypeArgument by NT()
    var Wildcard by NT()
    var WildcardBounds by NT()
    var TypeName by NT()
    var PackageOrTypeName by NT()
    var ExpressionName by NT()
    var AmbiguousName by NT()
    var MethodName by NT()
    var PackageName by NT()
    var Result by NT()
    var PackageDeclaration by NT()
    var ImportDeclaration by NT()
    var TypeDeclaration by NT()
    var PackageModifier by NT()
    var SingleTypeImportDeclaration by NT()
    var TypeImportOnDemandDeclaration by NT()
    var SingleStaticImportDeclaration by NT()
    var StaticImportOnDemandDeclaration by NT()
    var ClassDeclaration by NT()
    var InterfaceDeclaration by NT()
    var Throws by NT()
    var NormalClassDeclaration by NT()
    var EnumDeclaration by NT()
    var ClassModifier by NT()
    var TypeParameters by NT()
    var Superclass by NT()
    var Superinterfaces by NT()
    var ClassBody by NT()
    var TypeParameterList by NT()
    var InterfaceTypeList by NT()
    var ClassBodyDeclaration by NT()
    var ClassMemberDeclaration by NT()
    var InstanceInitializer by NT()
    var StaticInitializer by NT()
    var ConstructorDeclaration by NT()
    var FieldDeclaration by NT()
    var MethodDeclaration by NT()
    var FieldModifier by NT()
    var UnannType by NT()
    var VariableDeclaratorList by NT()
    var VariableDeclarator by NT()
    var VariableDeclaratorId by NT()
    var VariableInitializer by NT()
    var Expression by NT()
    var ArrayInitializer by NT()
    var UnannPrimitiveType by NT()
    var UnannReferenceType by NT()
    var UnannClassOrInterfaceType by NT()
    var UnannTypeVariable by NT()
    var UnannArrayType by NT()
    var UnannClassType by NT()
    var UnannInterfaceType by NT()
    var MethodModifier by NT()
    var MethodHeader by NT()
    var MethodBody by NT()
    var MethodDeclarator by NT()
    var FormalParameterList by NT()
    var ReceiverParameter by NT()
    var FormalParameters by NT()
    var LastFormalParameter by NT()
    var FormalParameter by NT()
    var VariableModifier by NT()
    var ExceptionTypeList by NT()
    var ExceptionType by NT()
    var Block by NT()
    var ConstructorModifier by NT()
    var ConstructorDeclarator by NT()
    var ConstructorBody by NT()
    var SimpleTypeName by NT()
    var ExplicitConstructorInvocation by NT()
    var EnumBody by NT()
    var EnumConstantList by NT()
    var EnumConstant by NT()
    var EnumConstantModifier by NT()
    var EnumBodyDeclarations by NT()
    var BlockStatements by NT()
    var ArgumentList by NT()
    var Primary by NT()
    var NormalInterfaceDeclaration by NT()
    var InterfaceModifier by NT()
    var ExtendsInterfaces by NT()
    var InterfaceBody by NT()
    var InterfaceMemberDeclaration by NT()
    var ConstantDeclaration by NT()
    var ConstantModifier by NT()
    var AnnotationTypeDeclaration by NT()
    var AnnotationTypeBody by NT()
    var AnnotationTypeMemberDeclaration by NT()
    var AnnotationTypeElementDeclaration by NT()
    var DefaultValue by NT()
    var NormalAnnotation by NT()
    var ElementValuePairList by NT()
    var ElementValuePair by NT()
    var ElementValue by NT()
    var ElementValueArrayInitializer by NT()
    var ElementValueList by NT()
    var MarkerAnnotation by NT()
    var SingleElementAnnotation by NT()
    var InterfaceMethodDeclaration by NT()
    var AnnotationTypeElementModifier by NT()
    var ConditionalExpression by NT()
    var VariableInitializerList by NT()
    var BlockStatement by NT()
    var LocalVariableDeclarationStatement by NT()
    var LocalVariableDeclaration by NT()
    var Statement by NT()
    var StatementNoShortIf by NT()
    var StatementWithoutTrailingSubstatement by NT()
    var EmptyStatement by NT()
    var LabeledStatement by NT()
    var LabeledStatementNoShortIf by NT()
    var ExpressionStatement by NT()
    var StatementExpression by NT()
    var IfThenStatement by NT()
    var IfThenElseStatement by NT()
    var IfThenElseStatementNoShortIf by NT()
    var AssertStatement by NT()
    var SwitchStatement by NT()
    var SwitchBlock by NT()
    var SwitchBlockStatementGroup by NT()
    var SwitchLabels by NT()
    var SwitchLabel by NT()
    var EnumConstantName by NT()
    var WhileStatement by NT()
    var WhileStatementNoShortIf by NT()
    var DoStatement by NT()
    var InterfaceMethodModifier by NT()
    var ForStatement by NT()
    var ForStatementNoShortIf by NT()
    var BasicForStatement by NT()
    var BasicForStatementNoShortIf by NT()
    var ForInit by NT()
    var ForUpdate by NT()
    var StatementExpressionList by NT()
    var EnhancedForStatement by NT()
    var EnhancedForStatementNoShortIf by NT()
    var BreakStatement by NT()
    var ContinueStatement by NT()
    var ReturnStatement by NT()
    var ThrowStatement by NT()
    var SynchronizedStatement by NT()
    var TryStatement by NT()
    var Catches by NT()
    var CatchClause by NT()
    var CatchFormalParameter by NT()
    var CatchType by NT()
    var Finally by NT()
    var TryWithResourcesStatement by NT()
    var ResourceSpecification by NT()
    var ResourceList by NT()
    var Resource by NT()
    var PrimaryNoNewArray by NT()
    var ClassLiteral by NT()
    var ClassOrInterfaceTypeToInstantiate by NT()
    var UnqualifiedClassInstanceCreationExpression by NT()
    var ClassInstanceCreationExpression by NT()
    var FieldAccess by NT()
    var TypeArgumentsOrDiamond by NT()
    var ArrayAccess by NT()
    var MethodInvocation by NT()
    var MethodReference by NT()
    var ArrayCreationExpression by NT()
    var DimExprs by NT()
    var DimExpr by NT()
    var LambdaExpression by NT()
    var LambdaParameters by NT()
    var InferredFormalParameterList by NT()
    var LambdaBody by NT()
    var AssignmentExpression by NT()
    var Assignment by NT()
    var LeftHandSide by NT()
    var AssignmentOperator by NT()
    var ConditionalOrExpression by NT()
    var ConditionalAndExpression by NT()
    var InclusiveOrExpression by NT()
    var ExclusiveOrExpression by NT()
    var AndExpression by NT()
    var EqualityExpression by NT()
    var RelationalExpression by NT()
    var ShiftExpression by NT()
    var AdditiveExpression by NT()
    var MultiplicativeExpression by NT()
    var PreIncrementExpression by NT()
    var PreDecrementExpression by NT()
    var UnaryExpressionNotPlusMinus by NT()
    var UnaryExpression by NT()
    var PostfixExpression by NT()
    var PostIncrementExpression by NT()
    var PostDecrementExpression by NT()
    var CastExpression by NT()
    var ConstantExpression by NT()

    init {
        Identifier = Term(JavaToken.ID)

        Literal = Term(JavaToken.INTEGERLIT) or Term(JavaToken.FLOATINGLIT) or Term(JavaToken.BOOLEANLIT) or
                Term(JavaToken.CHARLIT) or Term(JavaToken.STRINGLIT) or Term(JavaToken.NULLLIT)

        Type = PrimitiveType or ReferenceType
        PrimitiveType = Many(Annotation) * NumericType or Many(Annotation) * Term(JavaToken.BOOLEAN)
        NumericType = IntegralType or FloatingPointType
        IntegralType = Term(JavaToken.BYTE) or Term(JavaToken.SHORT) or Term(JavaToken.INT) or Term(JavaToken.LONG) or Term(JavaToken.CHAR)
        FloatingPointType = Term(JavaToken.FLOAT) or Term(JavaToken.DOUBLE)
        ReferenceType = ClassOrInterfaceType or TypeVariable or ArrayType
        ClassOrInterfaceType = ClassType or InterfaceType
        ClassType = Many(Annotation) * Identifier * Option(TypeArguments) or
                ClassOrInterfaceType * Term(JavaToken.DOT) * Many(Annotation) * Identifier * Option(TypeArguments)
        InterfaceType = ClassType
        TypeVariable = Many(Annotation) * Identifier
        ArrayType = PrimitiveType * Dims or ClassOrInterfaceType * Dims or TypeVariable * Dims
        Dims = Some(Many(Annotation) * Term(JavaToken.BRACKETLEFT) * Term(JavaToken.BRACKETRIGHT))
        TypeParameter  = Many(TypeParameterModifier) * Identifier * Option(TypeBound)
        TypeParameterModifier = Annotation
        TypeBound = Term(JavaToken.EXTENDS) * TypeVariable or Term(JavaToken.EXTENDS) * ClassOrInterfaceType * Many(AdditionalBound)
        AdditionalBound = Term(JavaToken.ANDBIT) * InterfaceType
        TypeArguments = Term(JavaToken.DIAMONDLEFT) * TypeArgumentList * Term(JavaToken.DIAMONDRIGHT)
        TypeArgumentList = TypeArgument * Many(Term(JavaToken.COMMA) * TypeArgument)
        TypeArgument = ReferenceType or Wildcard
        Wildcard = Many(Annotation) * Term(JavaToken.QUESTIONMARK) * Option(WildcardBounds)
        WildcardBounds = Term(JavaToken.EXTENDS) * ReferenceType or Term(JavaToken.SUPER) * ReferenceType

        TypeName = Identifier or PackageOrTypeName * Term(JavaToken.DOT) * Identifier
        PackageOrTypeName = Identifier or PackageOrTypeName * Term(JavaToken.DOT) * Identifier
        ExpressionName = Identifier or AmbiguousName * Term(JavaToken.DOT) * Identifier
        MethodName = Identifier
        PackageName = Identifier or PackageName * Term(JavaToken.DOT) * Identifier
        AmbiguousName = Identifier or AmbiguousName * Term(JavaToken.DOT) * Identifier

        CompilationUnit = Option(PackageDeclaration) * Many(ImportDeclaration) * Many(TypeDeclaration)
        PackageDeclaration = Many(PackageModifier) * Term(JavaToken.PACKAGE) * Identifier * Many(Term(JavaToken.DOT) * Identifier) * Term(JavaToken.SEMICOLON)
        PackageModifier = Annotation
        ImportDeclaration = SingleTypeImportDeclaration or TypeImportOnDemandDeclaration or
                SingleStaticImportDeclaration or StaticImportOnDemandDeclaration
        SingleTypeImportDeclaration = Term(JavaToken.IMPORT) * TypeName * Term(JavaToken.SEMICOLON)
        TypeImportOnDemandDeclaration = Term(JavaToken.IMPORT) * PackageOrTypeName * Term(JavaToken.DOT) * Term(JavaToken.STAR) * Term(JavaToken.SEMICOLON)
        SingleStaticImportDeclaration = Term(JavaToken.IMPORT) * Term(JavaToken.STATIC) * TypeName * Term(JavaToken.DOT) * Identifier * Term(JavaToken.SEMICOLON)
        StaticImportOnDemandDeclaration = Term(JavaToken.IMPORT) * Term(JavaToken.STATIC) * TypeName * Term(JavaToken.DOT) * Term(JavaToken.STAR) * Term(JavaToken.SEMICOLON)
        TypeDeclaration = ClassDeclaration or InterfaceDeclaration or Term(JavaToken.SEMICOLON)

        ClassDeclaration = NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration = Many(ClassModifier) * Term(JavaToken.CLASS) * Identifier *
                Option(TypeParameters) * Option(Superclass) * Option(Superinterfaces) * ClassBody
        ClassModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or
                Term(JavaToken.ABSTRACT) or Term(JavaToken.STATIC) or Term(JavaToken.FINAL) or Term(JavaToken.STRICTFP)
        TypeParameters = Term(JavaToken.DIAMONDLEFT) * TypeParameterList * Term(JavaToken.DIAMONDRIGHT)
        TypeParameterList = TypeParameter  * Many(Term(JavaToken.COMMA) * TypeParameter)
        Superclass = Term(JavaToken.EXTENDS) * ClassType
        Superinterfaces = Term(JavaToken.IMPLEMENTS) * InterfaceTypeList
        InterfaceTypeList = InterfaceType  * Many(Term(JavaToken.COMMA) * InterfaceType)
        ClassBody = Term(JavaToken.CURLYLEFT) * Many(ClassBodyDeclaration) * Term(JavaToken.CURLYRIGHT)
        ClassBodyDeclaration = ClassMemberDeclaration or InstanceInitializer or StaticInitializer or ConstructorDeclaration
        ClassMemberDeclaration = FieldDeclaration or MethodDeclaration or ClassDeclaration or InterfaceDeclaration or Term(JavaToken.SEMICOLON)
        FieldDeclaration = Many(FieldModifier) * UnannType * VariableDeclaratorList * Term(JavaToken.SEMICOLON)
        FieldModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or Term(JavaToken.STATIC) or
                Term(JavaToken.FINAL) or Term(JavaToken.TRANSIENT) or Term(JavaToken.VOLATILE)
        VariableDeclaratorList = VariableDeclarator * Many(Term(JavaToken.COMMA) * VariableDeclarator)
        VariableDeclarator = VariableDeclaratorId * Option(Term(JavaToken.ASSIGN) * VariableInitializer)
        VariableDeclaratorId = Identifier * Option(Dims)
        VariableInitializer = Expression or ArrayInitializer
        UnannType = UnannPrimitiveType or UnannReferenceType
        UnannPrimitiveType = NumericType or Term(JavaToken.BOOLEAN)
        UnannReferenceType = UnannClassOrInterfaceType or UnannTypeVariable or UnannArrayType
        UnannClassOrInterfaceType = UnannClassType or UnannInterfaceType
        UnannClassType = Identifier * Option(TypeArguments) or
                UnannClassOrInterfaceType * Term(JavaToken.DOT) * Many(Annotation) * Identifier * Option(TypeArguments)
        UnannInterfaceType = UnannClassType
        UnannTypeVariable = Identifier
        UnannArrayType = UnannPrimitiveType * Dims or UnannClassOrInterfaceType * Dims or UnannTypeVariable * Dims
        MethodDeclaration = Many(MethodModifier) * MethodHeader * MethodBody
        MethodModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or Term(JavaToken.ABSTRACT) or
                Term(JavaToken.STATIC) or Term(JavaToken.FINAL) or Term(JavaToken.SYNCHRONIZED) or Term(JavaToken.NATIVE) or Term(JavaToken.STRICTFP)
        MethodHeader = Result * MethodDeclarator * Option(Throws) or TypeParameters * Many(Annotation) * Result *
                MethodDeclarator * Option(Throws)
        Result = UnannType or Term(JavaToken.VOID)
        MethodDeclarator = Identifier * Term(JavaToken.PARENTHLEFT) * Option(FormalParameterList) * Term(JavaToken.PARENTHRIGHT) * Option(Dims)
        FormalParameterList = ReceiverParameter or FormalParameters * Term(JavaToken.COMMA) * LastFormalParameter or
                LastFormalParameter
        FormalParameters = FormalParameter * Many(Term(JavaToken.COMMA) * FormalParameter) or
                ReceiverParameter * Many(Term(JavaToken.COMMA) * FormalParameter)
        FormalParameter = Many(VariableModifier) * UnannType * VariableDeclaratorId
        VariableModifier = Annotation or Term(JavaToken.FINAL)
        LastFormalParameter = Many(VariableModifier) * UnannType * Many(Annotation) * Term(JavaToken.ELLIPSIS) * VariableDeclaratorId or FormalParameter
        ReceiverParameter = Many(Annotation) * UnannType * Option(Identifier * Term(JavaToken.DOT)) * Term(JavaToken.THIS)
        Throws = Term(JavaToken.THROWS) * ExceptionTypeList
        ExceptionTypeList = ExceptionType * Many(Term(JavaToken.COMMA) * ExceptionType)
        ExceptionType = ClassType or TypeVariable
        MethodBody = Block or Term(JavaToken.SEMICOLON)
        InstanceInitializer = Block
        StaticInitializer = Term(JavaToken.STATIC) * Block
        ConstructorDeclaration = Many(ConstructorModifier) * ConstructorDeclarator * Option(Throws) * ConstructorBody
        ConstructorModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE)
        ConstructorDeclarator = Option(TypeParameters) * SimpleTypeName * Term(JavaToken.PARENTHLEFT) * Option(FormalParameterList) * Term(JavaToken.PARENTHRIGHT)
        SimpleTypeName = Identifier
        ConstructorBody = Term(JavaToken.CURLYLEFT) * Option(ExplicitConstructorInvocation) * Option(BlockStatements) * Term(JavaToken.CURLYRIGHT)
        ExplicitConstructorInvocation = Option(TypeArguments) * Term(JavaToken.THIS) * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON) or
                Option(TypeArguments) * Term(JavaToken.SUPER) * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON) or
                ExpressionName * Term(JavaToken.DOT) * Option(TypeArguments) * Term(JavaToken.SUPER) * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON) or
                Primary * Term(JavaToken.DOT) * Option(TypeArguments) * Term(JavaToken.SUPER) * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON)
        EnumDeclaration = Many(ClassModifier) * Term(JavaToken.ENUM) * Identifier * Option(Superinterfaces) * EnumBody
        EnumBody = Term(JavaToken.CURLYLEFT) * Option(EnumConstantList) * Option(Term(JavaToken.COMMA)) * Option(EnumBodyDeclarations) * Term(JavaToken.CURLYRIGHT)
        EnumConstantList = EnumConstant * Many(Term(JavaToken.COMMA) * EnumConstant)
        EnumConstant = Many(EnumConstantModifier) * Identifier * Option(Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) * Option(ClassBody))
        EnumConstantModifier = Annotation
        EnumBodyDeclarations = Term(JavaToken.SEMICOLON) * Many(ClassBodyDeclaration)

        InterfaceDeclaration = NormalInterfaceDeclaration or AnnotationTypeDeclaration
        NormalInterfaceDeclaration =
            Many(InterfaceModifier) * Term(JavaToken.INTERFACE) * Identifier * Option(TypeParameters) * Option(ExtendsInterfaces) * InterfaceBody
        InterfaceModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or
                Term(JavaToken.ABSTRACT) or Term(JavaToken.STATIC) or Term(JavaToken.STRICTFP)
        ExtendsInterfaces = Term(JavaToken.EXTENDS) * InterfaceTypeList
        InterfaceBody = Term(JavaToken.CURLYLEFT) * Many(InterfaceMemberDeclaration) * Term(JavaToken.CURLYRIGHT)
        InterfaceMemberDeclaration = ConstantDeclaration or InterfaceMethodDeclaration or ClassDeclaration or InterfaceDeclaration or Term(JavaToken.SEMICOLON)
        InterfaceMethodDeclaration = Many(InterfaceMethodModifier) * MethodHeader * MethodBody
        InterfaceMethodModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.ABSTRACT) or Term(JavaToken.DEFAULT) or Term(JavaToken.STATIC) or Term(JavaToken.STRICTFP)
        ConstantDeclaration = Many(ConstantModifier) * UnannType * VariableDeclaratorList * Term(JavaToken.SEMICOLON)
        ConstantModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.ABSTRACT) or Term(JavaToken.DEFAULT) or Term(JavaToken.STATIC) or Term(JavaToken.STRICTFP)
        AnnotationTypeDeclaration = Many(InterfaceModifier) * Term(JavaToken.AT) * Term(JavaToken.INTERFACE) * Identifier * AnnotationTypeBody
        AnnotationTypeBody = Term(JavaToken.CURLYLEFT) * Many(AnnotationTypeMemberDeclaration) * Term(JavaToken.CURLYRIGHT)
        AnnotationTypeMemberDeclaration = AnnotationTypeElementDeclaration or ConstantDeclaration or ClassDeclaration or InterfaceDeclaration or Term(JavaToken.SEMICOLON)
        AnnotationTypeElementDeclaration =
            Many(AnnotationTypeElementModifier) * UnannType * Identifier * Term(JavaToken.PARENTHLEFT) * Term(JavaToken.PARENTHRIGHT) * Option(Dims) * Option(DefaultValue) * Term(JavaToken.SEMICOLON)
        AnnotationTypeElementModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.ABSTRACT)
        DefaultValue = Term(JavaToken.DEFAULT) * ElementValue
        Annotation = NormalAnnotation or MarkerAnnotation or SingleElementAnnotation
        NormalAnnotation = Term(JavaToken.AT) * TypeName * Term(JavaToken.PARENTHLEFT) * Option(ElementValuePairList) * Term(JavaToken.PARENTHRIGHT)
        ElementValuePairList = ElementValuePair * Many(Term(JavaToken.COMMA) * ElementValuePair)
        ElementValuePair = Identifier * Term(JavaToken.ASSIGN) * ElementValue
        ElementValue = ConditionalExpression or ElementValueArrayInitializer or Annotation
        ElementValueArrayInitializer = Term(JavaToken.CURLYLEFT) * Option(ElementValueList) * Option(Term(JavaToken.COMMA)) * Term(JavaToken.CURLYRIGHT)
        ElementValueList = ElementValue * Many(Term(JavaToken.COMMA) * ElementValue)
        MarkerAnnotation = Term(JavaToken.AT) * TypeName
        SingleElementAnnotation = Term(JavaToken.AT) * TypeName * Term(JavaToken.PARENTHLEFT) * ElementValue * Term(JavaToken.PARENTHRIGHT)

        ArrayInitializer = Term(JavaToken.CURLYLEFT) * Option(VariableInitializerList) * Option(Term(JavaToken.COMMA)) * Term(JavaToken.CURLYRIGHT)
        VariableInitializerList = VariableInitializer * Many(Term(JavaToken.COMMA) * VariableInitializer)

        Block = Term(JavaToken.CURLYLEFT) * Option(BlockStatements) * Term(JavaToken.CURLYRIGHT)
        BlockStatements = BlockStatement * Many(BlockStatement)
        BlockStatement = LocalVariableDeclarationStatement or ClassDeclaration or Statement
        LocalVariableDeclarationStatement = LocalVariableDeclaration * Term(JavaToken.SEMICOLON)
        LocalVariableDeclaration = Many(VariableModifier) * UnannType * VariableDeclaratorList
        Statement = StatementWithoutTrailingSubstatement or LabeledStatement or IfThenStatement or IfThenElseStatement or
                WhileStatement or ForStatement
        StatementNoShortIf = StatementWithoutTrailingSubstatement or LabeledStatementNoShortIf or IfThenElseStatementNoShortIf or
                WhileStatementNoShortIf or ForStatementNoShortIf
        StatementWithoutTrailingSubstatement = Block or EmptyStatement or ExpressionStatement or AssertStatement or
                SwitchStatement or DoStatement or BreakStatement or ContinueStatement or ReturnStatement or SynchronizedStatement or
                ThrowStatement or TryStatement
        EmptyStatement = Term(JavaToken.SEMICOLON)
        LabeledStatement = Identifier * Term(JavaToken.COLON) * Statement
        LabeledStatementNoShortIf = Identifier * Term(JavaToken.COLON) * StatementNoShortIf
        ExpressionStatement = StatementExpression * Term(JavaToken.SEMICOLON)
        StatementExpression = Assignment or PreIncrementExpression or PreDecrementExpression or PostIncrementExpression or
                PostDecrementExpression or MethodInvocation or ClassInstanceCreationExpression
        IfThenStatement = Term(JavaToken.IF) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * Statement
        IfThenElseStatement = Term(JavaToken.IF) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * StatementNoShortIf * Term(JavaToken.ELSE) * Statement
        IfThenElseStatementNoShortIf =
            Term(JavaToken.IF) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * StatementNoShortIf * Term(JavaToken.ELSE) * StatementNoShortIf
        AssertStatement = Term(JavaToken.ASSERT) * Expression * Term(JavaToken.SEMICOLON) or
                Term(JavaToken.ASSERT) * Expression * Term(JavaToken.COLON) * Expression * Term(JavaToken.SEMICOLON)
        SwitchStatement = Term(JavaToken.SWITCH) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * SwitchBlock
        SwitchBlock = Term(JavaToken.CURLYLEFT) * Many(SwitchBlockStatementGroup) * Many(SwitchLabel) * Term(JavaToken.CURLYRIGHT)
        SwitchBlockStatementGroup = SwitchLabels * BlockStatements
        SwitchLabels = SwitchLabel * Many(SwitchLabel)
        SwitchLabel = Term(JavaToken.CASE) * ConstantExpression * Term(JavaToken.COLON) or
                Term(JavaToken.CASE) * EnumConstantName * Term(JavaToken.COLON) or Term(JavaToken.DEFAULT) * Term(JavaToken.COLON)
        EnumConstantName = Identifier
        WhileStatement = Term(JavaToken.WHILE) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * Statement
        WhileStatementNoShortIf = Term(JavaToken.WHILE) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * StatementNoShortIf
        DoStatement = Term(JavaToken.DO) * Statement * Term(JavaToken.WHILE) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON)
        ForStatement = BasicForStatement or EnhancedForStatement
        ForStatementNoShortIf = BasicForStatementNoShortIf or EnhancedForStatementNoShortIf
        BasicForStatement = Term(JavaToken.FOR) * Term(JavaToken.PARENTHLEFT) * Option(ForInit) * Term(JavaToken.SEMICOLON) * Option(Expression) * Term(JavaToken.SEMICOLON) * Option(ForUpdate) * Term(JavaToken.PARENTHRIGHT) * Statement
        BasicForStatementNoShortIf = Term(JavaToken.FOR) * Term(JavaToken.PARENTHLEFT) * Option(ForInit) * Term(JavaToken.SEMICOLON) * Option(Expression) * Term(JavaToken.SEMICOLON) * Option(ForUpdate) * Term(JavaToken.PARENTHRIGHT) * StatementNoShortIf
        ForInit = StatementExpressionList or LocalVariableDeclaration
        ForUpdate = StatementExpressionList
        StatementExpressionList = StatementExpression * Many(Term(JavaToken.COMMA) * StatementExpression)
        EnhancedForStatement = Term(JavaToken.FOR) * Term(JavaToken.PARENTHLEFT) * Many(VariableModifier) * UnannType * VariableDeclaratorId * Term(JavaToken.COLON) * Expression * Term(JavaToken.PARENTHRIGHT) * Statement
        EnhancedForStatementNoShortIf = Term(JavaToken.FOR) * Term(JavaToken.PARENTHLEFT) * Many(VariableModifier) * UnannType * VariableDeclaratorId * Term(JavaToken.COLON) * Expression * Term(JavaToken.PARENTHRIGHT) * StatementNoShortIf
        BreakStatement = Term(JavaToken.BREAK) * Option(Identifier) * Term(JavaToken.SEMICOLON)
        ContinueStatement = Term(JavaToken.CONTINUE) * Option(Identifier) * Term(JavaToken.SEMICOLON)
        ReturnStatement = Term(JavaToken.RETURN) * Option(Expression) * Term(JavaToken.SEMICOLON)
        ThrowStatement = Term(JavaToken.THROW) * Expression * Term(JavaToken.SEMICOLON)
        SynchronizedStatement = Term(JavaToken.SYNCHRONIZED) * Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) * Block
        TryStatement = Term(JavaToken.TRY) * Block * Catches or Term(JavaToken.TRY) * Block * Option(Catches) * Finally or TryWithResourcesStatement
        Catches = CatchClause * Many(CatchClause)
        CatchClause = Term(JavaToken.CATCH) * Term(JavaToken.PARENTHLEFT) * CatchFormalParameter * Term(JavaToken.PARENTHRIGHT) * Block
        CatchFormalParameter = Many(VariableModifier) * CatchType * VariableDeclaratorId
        CatchType = UnannClassType * Many(Term(JavaToken.ORBIT) * ClassType)
        Finally = Term(JavaToken.FINALLY) * Block
        TryWithResourcesStatement = Term(JavaToken.TRY) * ResourceSpecification * Block * Option(Catches) * Option(Finally)
        ResourceSpecification = Term(JavaToken.PARENTHLEFT) * ResourceList * Option(Term(JavaToken.SEMICOLON)) * Term(JavaToken.PARENTHRIGHT)
        ResourceList = Resource * Many(Term(JavaToken.COMMA) * Resource)
        Resource = Many(VariableModifier) * UnannType * VariableDeclaratorId * Term(JavaToken.ASSIGN) * Expression

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
            Term(JavaToken.NEW) * Option(TypeArguments) * ClassOrInterfaceTypeToInstantiate * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) * Option(ClassBody)
        ClassOrInterfaceTypeToInstantiate = Many(Annotation) * Identifier * Many(Term(JavaToken.DOT) * Many(Annotation) * Identifier) * Option(TypeArgumentsOrDiamond)
        TypeArgumentsOrDiamond = TypeArguments or Term(JavaToken.DIAMOND)
        FieldAccess = Primary * Term(JavaToken.DOT) * Identifier or Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Identifier or
                TypeName * Term(JavaToken.DOT) * Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Identifier
        ArrayAccess = ExpressionName * Term(JavaToken.BRACKETLEFT) * Expression * Term(JavaToken.BRACKETRIGHT) or
                PrimaryNoNewArray * Term(JavaToken.BRACKETLEFT) * Expression * Term(JavaToken.BRACKETRIGHT)
        MethodInvocation = MethodName * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                TypeName * Term(JavaToken.DOT) * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                ExpressionName * Term(JavaToken.DOT) * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                TypeName * Term(JavaToken.DOT) * Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT)
        ArgumentList = Expression * Many(Term(JavaToken.COMMA) * Expression)
        MethodReference = ExpressionName * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Identifier or
                ReferenceType * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Identifier or
                Primary * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Identifier or
                Term(JavaToken.SUPER) * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Identifier or
                TypeName * Term(JavaToken.DOT) * Term(JavaToken.SUPER) * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Identifier or
                ClassType * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Term(JavaToken.NEW) or
                ArrayType * Term(JavaToken.DOUBLECOLON) * Term(JavaToken.NEW)
        ArrayCreationExpression = Term(JavaToken.NEW) * PrimitiveType * DimExprs * Option(Dims) or
                Term(JavaToken.NEW) * ClassOrInterfaceType * DimExprs * Option(Dims) or
                Term(JavaToken.NEW) * PrimitiveType * Dims * ArrayInitializer or
                Term(JavaToken.NEW) * ClassOrInterfaceType * Dims * ArrayInitializer
        DimExprs = DimExpr * Many(DimExpr)
        DimExpr = Many(Annotation) * Term(JavaToken.BRACKETLEFT) * Expression * Term(JavaToken.BRACKETRIGHT)
        Expression = LambdaExpression or AssignmentExpression
        LambdaExpression = LambdaParameters * Term(JavaToken.ARROW) * LambdaBody
        LambdaParameters = Identifier or Term(JavaToken.PARENTHLEFT) * Option(FormalParameterList) * Term(JavaToken.PARENTHRIGHT) or
                Term(JavaToken.PARENTHLEFT) * InferredFormalParameterList * Term(JavaToken.PARENTHRIGHT)
        InferredFormalParameterList = Identifier * Many(Term(JavaToken.COMMA) * Identifier)
        LambdaBody = Expression or Block
        AssignmentExpression = ConditionalExpression or Assignment
        Assignment = LeftHandSide * AssignmentOperator * Expression
        LeftHandSide = ExpressionName or FieldAccess or ArrayAccess
        AssignmentOperator = Term(JavaToken.ASSIGN) or Term(JavaToken.STARASSIGN) or Term(JavaToken.SLASHASSIGN) or Term(JavaToken.PERCENTASSIGN) or Term(JavaToken.PLUSASSIGN) or Term(JavaToken.MINUSASSIGN) or
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
        UnaryExpression = PreIncrementExpression or PreDecrementExpression or Term(JavaToken.PLUS) * UnaryExpression or
                Term(JavaToken.MINUS) * UnaryExpression or UnaryExpressionNotPlusMinus
        PreIncrementExpression = Term(JavaToken.PLUSPLUS) * UnaryExpression
        PreDecrementExpression = Term(JavaToken.MINUSMINUS) * UnaryExpression
        UnaryExpressionNotPlusMinus = PostfixExpression or Term(JavaToken.TILDA) * UnaryExpression or Term(JavaToken.EXCLAMATIONMARK) * UnaryExpression or
                CastExpression
        PostfixExpression = Primary or ExpressionName or PostIncrementExpression or PostDecrementExpression
        PostIncrementExpression = PostfixExpression * Term(JavaToken.PLUSPLUS)
        PostDecrementExpression = PostfixExpression * Term(JavaToken.MINUSMINUS)
        CastExpression = Term(JavaToken.PARENTHLEFT) * PrimitiveType * Term(JavaToken.PARENTHRIGHT) * UnaryExpression or
                Term(JavaToken.PARENTHLEFT) * ReferenceType * Many(AdditionalBound) * Term(JavaToken.PARENTHRIGHT) * UnaryExpressionNotPlusMinus or
                Term(JavaToken.PARENTHLEFT) * ReferenceType * Many(AdditionalBound) * Term(JavaToken.PARENTHRIGHT) * LambdaExpression
        ConstantExpression = Expression

        setStart(CompilationUnit)
    }
}