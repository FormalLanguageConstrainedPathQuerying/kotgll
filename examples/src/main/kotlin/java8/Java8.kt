package java8
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.*

class Java8 : Grammar() {
    val CompilationUnit by Nt().asStart()
    val Identifier by Nt()
    val Literal by Nt()
    val Type by Nt()
    val PrimitiveType by Nt()
    val ReferenceType by Nt()
    val Annotation by Nt()
    val NumericType by Nt()
    val IntegralType by Nt()
    val FloatingPointType by Nt()
    val ClassOrInterfaceType by Nt()
    val TypeVariable by Nt()
    val ArrayType by Nt()
    val ClassType by Nt()
    val InterfaceType by Nt()
    val TypeArguments by Nt()
    val Dims by Nt()
    val TypeParameter by Nt()
    val TypeParameterModifier by Nt()
    val TypeBound by Nt()
    val AdditionalBound by Nt()
    val TypeArgumentList by Nt()
    val TypeArgument by Nt()
    val Wildcard by Nt()
    val WildcardBounds by Nt()
    val TypeName by Nt()
    val PackageOrTypeName by Nt()
    val ExpressionName by Nt()
    val AmbiguousName by Nt()
    val MethodName by Nt()
    val PackageName by Nt()
    val Result by Nt()
    val PackageDeclaration by Nt()
    val ImportDeclaration by Nt()
    val TypeDeclaration by Nt()
    val PackageModifier by Nt()
    val SingleTypeImportDeclaration by Nt()
    val TypeImportOnDemandDeclaration by Nt()
    val SingleStaticImportDeclaration by Nt()
    val StaticImportOnDemandDeclaration by Nt()
    val ClassDeclaration by Nt()
    val InterfaceDeclaration by Nt()
    val Throws by Nt()
    val NormalClassDeclaration by Nt()
    val EnumDeclaration by Nt()
    val ClassModifier by Nt()
    val TypeParameters by Nt()
    val Superclass by Nt()
    val Superinterfaces by Nt()
    val ClassBody by Nt()
    val TypeParameterList by Nt()
    val InterfaceTypeList by Nt()
    val ClassBodyDeclaration by Nt()
    val ClassMemberDeclaration by Nt()
    val InstanceInitializer by Nt()
    val StaticInitializer by Nt()
    val ConstructorDeclaration by Nt()
    val FieldDeclaration by Nt()
    val MethodDeclaration by Nt()
    val FieldModifier by Nt()
    val UnannType by Nt()
    val VariableDeclaratorList by Nt()
    val VariableDeclarator by Nt()
    val VariableDeclaratorId by Nt()
    val VariableInitializer by Nt()
    val Expression by Nt()
    val ArrayInitializer by Nt()
    val UnannPrimitiveType by Nt()
    val UnannReferenceType by Nt()
    val UnannClassOrInterfaceType by Nt()
    val UnannTypeVariable by Nt()
    val UnannArrayType by Nt()
    val UnannClassType by Nt()
    val UnannInterfaceType by Nt()
    val MethodModifier by Nt()
    val MethodHeader by Nt()
    val MethodBody by Nt()
    val MethodDeclarator by Nt()
    val FormalParameterList by Nt()
    val ReceiverParameter by Nt()
    val FormalParameters by Nt()
    val LastFormalParameter by Nt()
    val FormalParameter by Nt()
    val VariableModifier by Nt()
    val ExceptionTypeList by Nt()
    val ExceptionType by Nt()
    val Block by Nt()
    val ConstructorModifier by Nt()
    val ConstructorDeclarator by Nt()
    val ConstructorBody by Nt()
    val SimpleTypeName by Nt()
    val ExplicitConstructorInvocation by Nt()
    val EnumBody by Nt()
    val EnumConstantList by Nt()
    val EnumConstant by Nt()
    val EnumConstantModifier by Nt()
    val EnumBodyDeclarations by Nt()
    val BlockStatements by Nt()
    val ArgumentList by Nt()
    val Primary by Nt()
    val NormalInterfaceDeclaration by Nt()
    val InterfaceModifier by Nt()
    val ExtendsInterfaces by Nt()
    val InterfaceBody by Nt()
    val InterfaceMemberDeclaration by Nt()
    val ConstantDeclaration by Nt()
    val ConstantModifier by Nt()
    val AnnotationTypeDeclaration by Nt()
    val AnnotationTypeBody by Nt()
    val AnnotationTypeMemberDeclaration by Nt()
    val AnnotationTypeElementDeclaration by Nt()
    val DefaultValue by Nt()
    val NormalAnnotation by Nt()
    val ElementValuePairList by Nt()
    val ElementValuePair by Nt()
    val ElementValue by Nt()
    val ElementValueArrayInitializer by Nt()
    val ElementValueList by Nt()
    val MarkerAnnotation by Nt()
    val SingleElementAnnotation by Nt()
    val InterfaceMethodDeclaration by Nt()
    val AnnotationTypeElementModifier by Nt()
    val ConditionalExpression by Nt()
    val VariableInitializerList by Nt()
    val BlockStatement by Nt()
    val LocalVariableDeclarationStatement by Nt()
    val LocalVariableDeclaration by Nt()
    val Statement by Nt()
    val StatementNoShortIf by Nt()
    val StatementWithoutTrailingSubstatement by Nt()
    val EmptyStatement by Nt()
    val LabeledStatement by Nt()
    val LabeledStatementNoShortIf by Nt()
    val ExpressionStatement by Nt()
    val StatementExpression by Nt()
    val IfThenStatement by Nt()
    val IfThenElseStatement by Nt()
    val IfThenElseStatementNoShortIf by Nt()
    val AssertStatement by Nt()
    val SwitchStatement by Nt()
    val SwitchBlock by Nt()
    val SwitchBlockStatementGroup by Nt()
    val SwitchLabels by Nt()
    val SwitchLabel by Nt()
    val EnumConstantName by Nt()
    val WhileStatement by Nt()
    val WhileStatementNoShortIf by Nt()
    val DoStatement by Nt()
    val InterfaceMethodModifier by Nt()
    val ForStatement by Nt()
    val ForStatementNoShortIf by Nt()
    val BasicForStatement by Nt()
    val BasicForStatementNoShortIf by Nt()
    val ForInit by Nt()
    val ForUpdate by Nt()
    val StatementExpressionList by Nt()
    val EnhancedForStatement by Nt()
    val EnhancedForStatementNoShortIf by Nt()
    val BreakStatement by Nt()
    val ContinueStatement by Nt()
    val ReturnStatement by Nt()
    val ThrowStatement by Nt()
    val SynchronizedStatement by Nt()
    val TryStatement by Nt()
    val Catches by Nt()
    val CatchClause by Nt()
    val CatchFormalParameter by Nt()
    val CatchType by Nt()
    val Finally by Nt()
    val TryWithResourcesStatement by Nt()
    val ResourceSpecification by Nt()
    val ResourceList by Nt()
    val Resource by Nt()
    val PrimaryNoNewArray by Nt()
    val ClassLiteral by Nt()
    val classOrInterfaceTypeToInstantiate by Nt()
    val UnqualifiedClassInstanceCreationExpression by Nt()
    val ClassInstanceCreationExpression by Nt()
    val FieldAccess by Nt()
    val TypeArgumentsOrDiamond by Nt()
    val ArrayAccess by Nt()
    val MethodInvocation by Nt()
    val MethodReference by Nt()
    val ArrayCreationExpression by Nt()
    val DimExprs by Nt()
    val DimExpr by Nt()
    val LambdaExpression by Nt()
    val LambdaParameters by Nt()
    val InferredFormalParameterList by Nt()
    val LambdaBody by Nt()
    val AssignmentExpression by Nt()
    val Assignment by Nt()
    val LeftHandSide by Nt()
    val AssignmentOperator by Nt()
    val ConditionalOrExpression by Nt()
    val ConditionalAndExpression by Nt()
    val InclusiveOrExpression by Nt()
    val ExclusiveOrExpression by Nt()
    val AndExpression by Nt()
    val EqualityExpression by Nt()
    val RelationalExpression by Nt()
    val ShiftExpression by Nt()
    val AdditiveExpression by Nt()
    val MultiplicativeExpression by Nt()
    val PreIncrementExpression by Nt()
    val PreDecrementExpression by Nt()
    val UnaryExpressionNotPlusMinus by Nt()
    val UnaryExpression by Nt()
    val PostfixExpression by Nt()
    val PostIncrementExpression by Nt()
    val PostDecrementExpression by Nt()
    val CastExpression by Nt()
    val ConstantExpression by Nt()

    init {
        Identifier /= JavaToken.ID

        Literal /= JavaToken.INTEGERLIT or JavaToken.FLOATINGLIT or JavaToken.BOOLEANLIT or
                JavaToken.CHARLIT or JavaToken.STRINGLIT or JavaToken.NULLLIT

        /**
         * Productions from §4 (Types, Values, and Variables)
         */
        Type /= PrimitiveType or ReferenceType
        PrimitiveType /= Many(Annotation) * NumericType or Many(Annotation) * JavaToken.BOOLEAN
        NumericType /= IntegralType or FloatingPointType
        IntegralType /= JavaToken.BYTE or JavaToken.SHORT or JavaToken.INT or JavaToken.LONG or JavaToken.CHAR
        FloatingPointType /= JavaToken.FLOAT or JavaToken.DOUBLE
        ReferenceType /= ClassOrInterfaceType or TypeVariable or ArrayType
        ClassOrInterfaceType /= ClassType or InterfaceType
        ClassType /= Many(Annotation) * Identifier * Option(TypeArguments) or
                ClassOrInterfaceType * JavaToken.DOT * Many(Annotation) * Identifier * Option(TypeArguments)
        InterfaceType /= ClassType
        TypeVariable /= Many(Annotation) * Identifier
        ArrayType /= PrimitiveType * Dims or ClassOrInterfaceType * Dims or TypeVariable * Dims
        Dims /= some(Many(Annotation) * JavaToken.BRACKETLEFT * JavaToken.BRACKETRIGHT)
        TypeParameter  /= Many(TypeParameterModifier) * Identifier * Option(TypeBound)
        TypeParameterModifier /= Annotation
        TypeBound /= JavaToken.EXTENDS * TypeVariable or JavaToken.EXTENDS * ClassOrInterfaceType * Many(AdditionalBound)
        AdditionalBound /= JavaToken.ANDBIT * InterfaceType
        TypeArguments /= JavaToken.LT * TypeArgumentList * JavaToken.GT
        TypeArgumentList /= TypeArgument * Many(JavaToken.COMMA * TypeArgument)
        TypeArgument /= ReferenceType or Wildcard
        Wildcard /= Many(Annotation) * JavaToken.QUESTIONMARK * Option(WildcardBounds)
        WildcardBounds /= JavaToken.EXTENDS * ReferenceType or JavaToken.SUPER * ReferenceType

        /**
         * Productions from §6 (Names)
         */

        TypeName /= Identifier or PackageOrTypeName * JavaToken.DOT * Identifier
        PackageOrTypeName /= Identifier or PackageOrTypeName * JavaToken.DOT * Identifier
        ExpressionName /= Identifier or AmbiguousName * JavaToken.DOT * Identifier
        MethodName /= Identifier
        PackageName /= Identifier or PackageName * JavaToken.DOT * Identifier
        AmbiguousName /= Identifier or AmbiguousName * JavaToken.DOT * Identifier

        /**
         * Productions from §7 (Packages)
         */

        CompilationUnit /= Option(PackageDeclaration) * Many(ImportDeclaration) * Many(TypeDeclaration)
        PackageDeclaration /= Many(PackageModifier) * JavaToken.PACKAGE * Identifier * Many(JavaToken.DOT * Identifier) * JavaToken.SEMICOLON
        PackageModifier /= Annotation
        ImportDeclaration /= SingleTypeImportDeclaration or TypeImportOnDemandDeclaration or
                SingleStaticImportDeclaration or StaticImportOnDemandDeclaration
        SingleTypeImportDeclaration /= JavaToken.IMPORT * TypeName * JavaToken.SEMICOLON
        TypeImportOnDemandDeclaration /= JavaToken.IMPORT * PackageOrTypeName * JavaToken.DOT * JavaToken.STAR * JavaToken.SEMICOLON
        SingleStaticImportDeclaration /= JavaToken.IMPORT * JavaToken.STATIC * TypeName * JavaToken.DOT * Identifier * JavaToken.SEMICOLON
        StaticImportOnDemandDeclaration /= JavaToken.IMPORT * JavaToken.STATIC * TypeName * JavaToken.DOT * JavaToken.STAR * JavaToken.SEMICOLON
        TypeDeclaration /= ClassDeclaration or InterfaceDeclaration or JavaToken.SEMICOLON

        /**
         * Productions from §8 (Classes)
         */

        ClassDeclaration /= NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration /= Many(ClassModifier) * JavaToken.CLASS * Identifier *
                Option(TypeParameters) * Option(Superclass) * Option(Superinterfaces) * ClassBody
        ClassModifier /= Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or
                JavaToken.ABSTRACT or JavaToken.STATIC or JavaToken.FINAL or JavaToken.STRICTFP
        TypeParameters /= JavaToken.LT * TypeParameterList * JavaToken.GT
        TypeParameterList /= TypeParameter  * Many(JavaToken.COMMA * TypeParameter)
        Superclass /= JavaToken.EXTENDS * ClassType
        Superinterfaces /= JavaToken.IMPLEMENTS * InterfaceTypeList
        InterfaceTypeList /= InterfaceType  * Many(JavaToken.COMMA * InterfaceType)
        ClassBody /= JavaToken.CURLYLEFT * Many(ClassBodyDeclaration) * JavaToken.CURLYRIGHT
        ClassBodyDeclaration /= ClassMemberDeclaration or InstanceInitializer or StaticInitializer or ConstructorDeclaration
        ClassMemberDeclaration /= FieldDeclaration or MethodDeclaration or ClassDeclaration or InterfaceDeclaration or JavaToken.SEMICOLON
        FieldDeclaration /= Many(FieldModifier) * UnannType * VariableDeclaratorList * JavaToken.SEMICOLON
        FieldModifier /= Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or JavaToken.STATIC or
                JavaToken.FINAL or JavaToken.TRANSIENT or JavaToken.VOLATILE
        VariableDeclaratorList /= VariableDeclarator * Many(JavaToken.COMMA * VariableDeclarator)
        VariableDeclarator /= VariableDeclaratorId * Option(JavaToken.ASSIGN * VariableInitializer)
        VariableDeclaratorId /= Identifier * Option(Dims)
        VariableInitializer /= Expression or ArrayInitializer
        UnannType /= UnannPrimitiveType or UnannReferenceType
        UnannPrimitiveType /= NumericType or JavaToken.BOOLEAN
        UnannReferenceType /= UnannClassOrInterfaceType or UnannTypeVariable or UnannArrayType
        UnannClassOrInterfaceType /= UnannClassType or UnannInterfaceType
        UnannClassType /= Identifier * Option(TypeArguments) or
                UnannClassOrInterfaceType * JavaToken.DOT * Many(Annotation) * Identifier * Option(TypeArguments)
        UnannInterfaceType /= UnannClassType
        UnannTypeVariable /= Identifier
        UnannArrayType /= UnannPrimitiveType * Dims or UnannClassOrInterfaceType * Dims or UnannTypeVariable * Dims
        MethodDeclaration /= Many(MethodModifier) * MethodHeader * MethodBody
        MethodModifier /= Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or JavaToken.ABSTRACT or
                JavaToken.STATIC or JavaToken.FINAL or JavaToken.SYNCHRONIZED or JavaToken.NATIVE or JavaToken.STRICTFP
        MethodHeader /= Result * MethodDeclarator * Option(Throws) or
                TypeParameters * Many(Annotation) * Result * MethodDeclarator * Option(Throws)
        Result /= UnannType or JavaToken.VOID
        MethodDeclarator /= Identifier * JavaToken.PARENTHLEFT * Option(FormalParameterList) * JavaToken.PARENTHRIGHT * Option(Dims)
        FormalParameterList /= ReceiverParameter or FormalParameters * JavaToken.COMMA * LastFormalParameter or
                LastFormalParameter
        FormalParameters /= FormalParameter * Many(JavaToken.COMMA * FormalParameter) or
                ReceiverParameter * Many(JavaToken.COMMA * FormalParameter)
        FormalParameter /= Many(VariableModifier) * UnannType * VariableDeclaratorId
        VariableModifier /= Annotation or JavaToken.FINAL
        LastFormalParameter /= Many(VariableModifier) * UnannType * Many(Annotation) * JavaToken.ELLIPSIS * VariableDeclaratorId or FormalParameter
        ReceiverParameter /= Many(Annotation) * UnannType * Option(Identifier * JavaToken.DOT) * JavaToken.THIS
        Throws /= JavaToken.THROWS * ExceptionTypeList
        ExceptionTypeList /= ExceptionType * Many(JavaToken.COMMA * ExceptionType)
        ExceptionType /= ClassType or TypeVariable
        MethodBody /= Block or JavaToken.SEMICOLON
        InstanceInitializer /= Block
        StaticInitializer /= JavaToken.STATIC * Block
        ConstructorDeclaration /= Many(ConstructorModifier) * ConstructorDeclarator * Option(Throws) * ConstructorBody
        ConstructorModifier /= Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE
        ConstructorDeclarator /= Option(TypeParameters) * SimpleTypeName * JavaToken.PARENTHLEFT * Option(FormalParameterList) * JavaToken.PARENTHRIGHT
        SimpleTypeName /= Identifier
        ConstructorBody /= JavaToken.CURLYLEFT * Option(ExplicitConstructorInvocation) * Option(BlockStatements) * JavaToken.CURLYRIGHT
        ExplicitConstructorInvocation /= Option(TypeArguments) * JavaToken.THIS * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON or
                Option(TypeArguments) * JavaToken.SUPER * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON or
                ExpressionName * JavaToken.DOT * Option(TypeArguments) * JavaToken.SUPER * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON or
                Primary * JavaToken.DOT * Option(TypeArguments) * JavaToken.SUPER * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON
        EnumDeclaration /= Many(ClassModifier) * JavaToken.ENUM * Identifier * Option(Superinterfaces) * EnumBody
        EnumBody /= JavaToken.CURLYLEFT * Option(EnumConstantList) * Option(JavaToken.COMMA) * Option(EnumBodyDeclarations) * JavaToken.CURLYRIGHT
        EnumConstantList /= EnumConstant * Many(JavaToken.COMMA * EnumConstant)
        EnumConstant /= Many(EnumConstantModifier) * Identifier * Option(JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * Option(ClassBody))
        EnumConstantModifier /= Annotation
        EnumBodyDeclarations /= JavaToken.SEMICOLON * Many(ClassBodyDeclaration)

        /**
         * Productions from §9 (Interfaces)
         */

        InterfaceDeclaration /= NormalInterfaceDeclaration or AnnotationTypeDeclaration
        NormalInterfaceDeclaration /=
            Many(InterfaceModifier) * JavaToken.INTERFACE * Identifier * Option(TypeParameters) * Option(ExtendsInterfaces) * InterfaceBody
        InterfaceModifier /= Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or
                JavaToken.ABSTRACT or JavaToken.STATIC or JavaToken.STRICTFP
        ExtendsInterfaces /= JavaToken.EXTENDS * InterfaceTypeList
        InterfaceBody /= JavaToken.CURLYLEFT * Many(InterfaceMemberDeclaration) * JavaToken.CURLYRIGHT
        InterfaceMemberDeclaration /= ConstantDeclaration or InterfaceMethodDeclaration or ClassDeclaration or InterfaceDeclaration or JavaToken.SEMICOLON
        ConstantDeclaration /= Many(ConstantModifier) * UnannType * VariableDeclaratorList * JavaToken.SEMICOLON
        ConstantModifier /= Annotation or JavaToken.PUBLIC or JavaToken.STATIC or JavaToken.FINAL
        InterfaceMethodDeclaration /= Many(InterfaceMethodModifier) * MethodHeader * MethodBody
        InterfaceMethodModifier /= Annotation or JavaToken.PUBLIC or JavaToken.ABSTRACT or JavaToken.DEFAULT or JavaToken.STATIC or JavaToken.STRICTFP
        AnnotationTypeDeclaration /= Many(InterfaceModifier) * JavaToken.AT * JavaToken.INTERFACE * Identifier * AnnotationTypeBody
        AnnotationTypeBody /= JavaToken.CURLYLEFT * Many(AnnotationTypeMemberDeclaration) * JavaToken.CURLYRIGHT
        AnnotationTypeMemberDeclaration /= AnnotationTypeElementDeclaration or ConstantDeclaration or ClassDeclaration or InterfaceDeclaration or JavaToken.SEMICOLON
        AnnotationTypeElementDeclaration /=
            Many(AnnotationTypeElementModifier) * UnannType * Identifier * JavaToken.PARENTHLEFT * JavaToken.PARENTHRIGHT * Option(Dims) * Option(DefaultValue) * JavaToken.SEMICOLON
        AnnotationTypeElementModifier /= Annotation or JavaToken.PUBLIC or JavaToken.ABSTRACT
        DefaultValue /= JavaToken.DEFAULT * ElementValue
        Annotation /= NormalAnnotation or MarkerAnnotation or SingleElementAnnotation
        NormalAnnotation /= JavaToken.AT * TypeName * JavaToken.PARENTHLEFT * Option(ElementValuePairList) * JavaToken.PARENTHRIGHT
        ElementValuePairList /= ElementValuePair * Many(JavaToken.COMMA * ElementValuePair)
        ElementValuePair /= Identifier * JavaToken.ASSIGN * ElementValue
        ElementValue /= ConditionalExpression or ElementValueArrayInitializer or Annotation
        ElementValueArrayInitializer /= JavaToken.CURLYLEFT * Option(ElementValueList) * Option(JavaToken.COMMA) * JavaToken.CURLYRIGHT
        ElementValueList /= ElementValue * Many(JavaToken.COMMA * ElementValue)
        MarkerAnnotation /= JavaToken.AT * TypeName
        SingleElementAnnotation /= JavaToken.AT * TypeName * JavaToken.PARENTHLEFT * ElementValue * JavaToken.PARENTHRIGHT

        /**
         * Productions from §10 (Arrays)
         */

        ArrayInitializer /= JavaToken.CURLYLEFT * Option(VariableInitializerList) * Option(JavaToken.COMMA) * JavaToken.CURLYRIGHT
        VariableInitializerList /= VariableInitializer * Many(JavaToken.COMMA * VariableInitializer)

        /**
         * Productions from §14 (Blocks and Statements)
         */

        Block /= JavaToken.CURLYLEFT * Option(BlockStatements) * JavaToken.CURLYRIGHT
        BlockStatements /= BlockStatement * Many(BlockStatement)
        BlockStatement /= LocalVariableDeclarationStatement or ClassDeclaration or Statement
        LocalVariableDeclarationStatement /= LocalVariableDeclaration * JavaToken.SEMICOLON
        LocalVariableDeclaration /= Many(VariableModifier) * UnannType * VariableDeclaratorList
        Statement /= StatementWithoutTrailingSubstatement or LabeledStatement or IfThenStatement or IfThenElseStatement or
                WhileStatement or ForStatement
        StatementNoShortIf /= StatementWithoutTrailingSubstatement or LabeledStatementNoShortIf or IfThenElseStatementNoShortIf or
                WhileStatementNoShortIf or ForStatementNoShortIf
        StatementWithoutTrailingSubstatement /= Block or EmptyStatement or ExpressionStatement or AssertStatement or
                SwitchStatement or DoStatement or BreakStatement or ContinueStatement or ReturnStatement or SynchronizedStatement or
                ThrowStatement or TryStatement
        EmptyStatement /= JavaToken.SEMICOLON
        LabeledStatement /= Identifier * JavaToken.COLON * Statement
        LabeledStatementNoShortIf /= Identifier * JavaToken.COLON * StatementNoShortIf
        ExpressionStatement /= StatementExpression * JavaToken.SEMICOLON
        StatementExpression /= Assignment or PreIncrementExpression or PreDecrementExpression or PostIncrementExpression or
                PostDecrementExpression or MethodInvocation or ClassInstanceCreationExpression
        IfThenStatement /= JavaToken.IF * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * Statement
        IfThenElseStatement /= JavaToken.IF * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * StatementNoShortIf * JavaToken.ELSE * Statement
        IfThenElseStatementNoShortIf /=
            JavaToken.IF * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * StatementNoShortIf * JavaToken.ELSE * StatementNoShortIf
        AssertStatement /= JavaToken.ASSERT * Expression * JavaToken.SEMICOLON or
                JavaToken.ASSERT * Expression * JavaToken.COLON * Expression * JavaToken.SEMICOLON
        SwitchStatement /= JavaToken.SWITCH * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * SwitchBlock
        SwitchBlock /= JavaToken.CURLYLEFT * Many(SwitchBlockStatementGroup) * Many(SwitchLabel) * JavaToken.CURLYRIGHT
        SwitchBlockStatementGroup /= SwitchLabels * BlockStatements
        SwitchLabels /= some(SwitchLabel)
        SwitchLabel /= JavaToken.CASE * ConstantExpression * JavaToken.COLON or
                JavaToken.CASE * EnumConstantName * JavaToken.COLON or JavaToken.DEFAULT * JavaToken.COLON
        EnumConstantName /= Identifier
        WhileStatement /= JavaToken.WHILE * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * Statement
        WhileStatementNoShortIf /= JavaToken.WHILE * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * StatementNoShortIf
        DoStatement /= JavaToken.DO * Statement * JavaToken.WHILE * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON
        ForStatement /= BasicForStatement or EnhancedForStatement
        ForStatementNoShortIf /= BasicForStatementNoShortIf or EnhancedForStatementNoShortIf
        BasicForStatement /= JavaToken.FOR * JavaToken.PARENTHLEFT * Option(ForInit) * JavaToken.SEMICOLON * Option(Expression) * JavaToken.SEMICOLON * Option(ForUpdate) * JavaToken.PARENTHRIGHT * Statement
        BasicForStatementNoShortIf /= JavaToken.FOR * JavaToken.PARENTHLEFT * Option(ForInit) * JavaToken.SEMICOLON * Option(Expression) * JavaToken.SEMICOLON * Option(ForUpdate) * JavaToken.PARENTHRIGHT * StatementNoShortIf
        ForInit /= StatementExpressionList or LocalVariableDeclaration
        ForUpdate /= StatementExpressionList
        StatementExpressionList /= StatementExpression * Many(JavaToken.COMMA * StatementExpression)
        EnhancedForStatement /= JavaToken.FOR * JavaToken.PARENTHLEFT * Many(VariableModifier) * UnannType * VariableDeclaratorId * JavaToken.COLON * Expression * JavaToken.PARENTHRIGHT * Statement
        EnhancedForStatementNoShortIf /= JavaToken.FOR * JavaToken.PARENTHLEFT * Many(VariableModifier) * UnannType * VariableDeclaratorId * JavaToken.COLON * Expression * JavaToken.PARENTHRIGHT * StatementNoShortIf
        BreakStatement /= JavaToken.BREAK * Option(Identifier) * JavaToken.SEMICOLON
        ContinueStatement /= JavaToken.CONTINUE * Option(Identifier) * JavaToken.SEMICOLON
        ReturnStatement /= JavaToken.RETURN * Option(Expression) * JavaToken.SEMICOLON
        ThrowStatement /= JavaToken.THROW * Expression * JavaToken.SEMICOLON
        SynchronizedStatement /= JavaToken.SYNCHRONIZED * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * Block
        TryStatement /= JavaToken.TRY * Block * Catches or JavaToken.TRY * Block * Option(Catches) * Finally or TryWithResourcesStatement
        Catches /= some(CatchClause)
        CatchClause /= JavaToken.CATCH * JavaToken.PARENTHLEFT * CatchFormalParameter * JavaToken.PARENTHRIGHT * Block
        CatchFormalParameter /= Many(VariableModifier) * CatchType * VariableDeclaratorId
        CatchType /= UnannClassType * Many(JavaToken.ORBIT * ClassType)
        Finally /= JavaToken.FINALLY * Block
        TryWithResourcesStatement /= JavaToken.TRY * ResourceSpecification * Block * Option(Catches) * Option(Finally)
        ResourceSpecification /= JavaToken.PARENTHLEFT * ResourceList * Option(JavaToken.SEMICOLON) * JavaToken.PARENTHRIGHT
        ResourceList /= Resource * Many(JavaToken.COMMA * Resource)
        Resource /= Many(VariableModifier) * UnannType * VariableDeclaratorId * JavaToken.ASSIGN * Expression

        /**
         * Productions from §15 (Expressions)
         */

        Primary /= PrimaryNoNewArray or ArrayCreationExpression
        PrimaryNoNewArray /= Literal or ClassLiteral or JavaToken.THIS or TypeName * JavaToken.DOT * JavaToken.THIS or
                JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT or ClassInstanceCreationExpression or FieldAccess or
                ArrayAccess or MethodInvocation or MethodReference
        ClassLiteral /= TypeName * Many(JavaToken.BRACKETLEFT * JavaToken.BRACKETRIGHT) * JavaToken.DOT * JavaToken.CLASS or
                NumericType * Many(JavaToken.BRACKETLEFT * JavaToken.BRACKETRIGHT) * JavaToken.DOT * JavaToken.CLASS or
                JavaToken.BOOLEAN * Many(JavaToken.BRACKETLEFT * JavaToken.BRACKETRIGHT) * JavaToken.DOT * JavaToken.CLASS or
                JavaToken.VOID * JavaToken.DOT * JavaToken.CLASS
        ClassInstanceCreationExpression /= UnqualifiedClassInstanceCreationExpression or
                ExpressionName * JavaToken.DOT * UnqualifiedClassInstanceCreationExpression or
                Primary * JavaToken.DOT * UnqualifiedClassInstanceCreationExpression
        UnqualifiedClassInstanceCreationExpression /=
            JavaToken.NEW * Option(TypeArguments) * classOrInterfaceTypeToInstantiate * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * Option(ClassBody)
        classOrInterfaceTypeToInstantiate /= Many(Annotation) * Identifier * Many(JavaToken.DOT * Many(Annotation) * Identifier) * Option(TypeArgumentsOrDiamond)
        TypeArgumentsOrDiamond /= TypeArguments or JavaToken.LT * JavaToken.GT
        FieldAccess /= Primary * JavaToken.DOT * Identifier or JavaToken.SUPER * JavaToken.DOT * Identifier or
                TypeName * JavaToken.DOT * JavaToken.SUPER * JavaToken.DOT * Identifier
        ArrayAccess /= ExpressionName * JavaToken.BRACKETLEFT * Expression * JavaToken.BRACKETRIGHT or
                PrimaryNoNewArray * JavaToken.BRACKETLEFT * Expression * JavaToken.BRACKETRIGHT
        MethodInvocation /= MethodName * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT or
                TypeName * JavaToken.DOT * Option(TypeArguments) * Identifier * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT or
                ExpressionName * JavaToken.DOT * Option(TypeArguments) * Identifier * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT or
                Primary * JavaToken.DOT * Option(TypeArguments) * Identifier * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT or
                JavaToken.SUPER * JavaToken.DOT * Option(TypeArguments) * Identifier * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT or
                TypeName * JavaToken.DOT * JavaToken.SUPER * JavaToken.DOT * Option(TypeArguments) * Identifier * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT
        ArgumentList /= Expression * Many(JavaToken.COMMA * Expression)
        MethodReference /= ExpressionName * JavaToken.DOUBLECOLON * Option(TypeArguments) * Identifier or
                ReferenceType * JavaToken.DOUBLECOLON * Option(TypeArguments) * Identifier or
                Primary * JavaToken.DOUBLECOLON * Option(TypeArguments) * Identifier or
                JavaToken.SUPER * JavaToken.DOUBLECOLON * Option(TypeArguments) * Identifier or
                TypeName * JavaToken.DOT * JavaToken.SUPER * JavaToken.DOUBLECOLON * Option(TypeArguments) * Identifier or
                ClassType * JavaToken.DOUBLECOLON * Option(TypeArguments) * JavaToken.NEW or
                ArrayType * JavaToken.DOUBLECOLON * JavaToken.NEW
        ArrayCreationExpression /= JavaToken.NEW * PrimitiveType * DimExprs * Option(Dims) or
                JavaToken.NEW * ClassOrInterfaceType * DimExprs * Option(Dims) or
                JavaToken.NEW * PrimitiveType * Dims * ArrayInitializer or
                JavaToken.NEW * ClassOrInterfaceType * Dims * ArrayInitializer
        DimExprs /= some(DimExpr)
        DimExpr /= Many(Annotation) * JavaToken.BRACKETLEFT * Expression * JavaToken.BRACKETRIGHT
        Expression /= LambdaExpression or AssignmentExpression
        LambdaExpression /= LambdaParameters * JavaToken.ARROW * LambdaBody
        LambdaParameters /= Identifier or JavaToken.PARENTHLEFT * Option(FormalParameterList) * JavaToken.PARENTHRIGHT or
                JavaToken.PARENTHLEFT * InferredFormalParameterList * JavaToken.PARENTHRIGHT
        InferredFormalParameterList /= Identifier * Many(JavaToken.COMMA * Identifier)
        LambdaBody /= Expression or Block
        AssignmentExpression /= ConditionalExpression or Assignment
        Assignment /= LeftHandSide * AssignmentOperator * Expression
        LeftHandSide /= ExpressionName or FieldAccess or ArrayAccess
        AssignmentOperator /= JavaToken.ASSIGN or JavaToken.STARASSIGN or JavaToken.SLASHASSIGN or JavaToken.PERCENTASSIGN or JavaToken.PLUSASSIGN or JavaToken.MINUSASSIGN or
                JavaToken.SHIFTLEFTASSIGN or JavaToken.SHIFTRIGHTASSIGN or JavaToken.USRIGHTSHIFTASSIGN or JavaToken.ANDASSIGN or JavaToken.XORASSIGN or JavaToken.ORASSIGN
        ConditionalExpression /= ConditionalOrExpression or
                ConditionalOrExpression * JavaToken.QUESTIONMARK * Expression * JavaToken.COLON * ConditionalExpression or
                ConditionalOrExpression * JavaToken.QUESTIONMARK * Expression * JavaToken.COLON * LambdaExpression
        ConditionalOrExpression /= ConditionalAndExpression or
                ConditionalOrExpression * JavaToken.OR * ConditionalAndExpression
        ConditionalAndExpression /= InclusiveOrExpression or
                ConditionalAndExpression * JavaToken.AND * InclusiveOrExpression
        InclusiveOrExpression /= ExclusiveOrExpression or
                InclusiveOrExpression * JavaToken.ORBIT * ExclusiveOrExpression
        ExclusiveOrExpression /= AndExpression or ExclusiveOrExpression * JavaToken.XORBIT * AndExpression
        AndExpression /= EqualityExpression or AndExpression * JavaToken.ANDBIT * EqualityExpression
        EqualityExpression /= RelationalExpression or EqualityExpression * JavaToken.EQ * RelationalExpression or
                EqualityExpression * JavaToken.NOTEQ * RelationalExpression
        RelationalExpression /= ShiftExpression or RelationalExpression * JavaToken.LT * ShiftExpression or
                RelationalExpression * JavaToken.GT * ShiftExpression or RelationalExpression * JavaToken.LESSEQ * ShiftExpression or
                RelationalExpression * JavaToken.GREATEQ * ShiftExpression or RelationalExpression * JavaToken.INSTANCEOF * ReferenceType
        ShiftExpression /= AdditiveExpression or ShiftExpression * JavaToken.LT * JavaToken.LT * AdditiveExpression or
                ShiftExpression * JavaToken.GT * JavaToken.GT * AdditiveExpression or
                ShiftExpression * JavaToken.GT * JavaToken.GT * JavaToken.GT * AdditiveExpression
        AdditiveExpression /= MultiplicativeExpression or AdditiveExpression * JavaToken.PLUS * MultiplicativeExpression or
                AdditiveExpression * JavaToken.MINUS * MultiplicativeExpression
        MultiplicativeExpression /= UnaryExpression or MultiplicativeExpression * JavaToken.STAR * UnaryExpression or
                MultiplicativeExpression * JavaToken.SLASH * UnaryExpression or
                MultiplicativeExpression * JavaToken.PERCENT * UnaryExpression
        UnaryExpression /= PreIncrementExpression or PreDecrementExpression or JavaToken.PLUS * UnaryExpression or
                JavaToken.MINUS * UnaryExpression or UnaryExpressionNotPlusMinus
        PreIncrementExpression /= JavaToken.PLUSPLUS * UnaryExpression
        PreDecrementExpression /= JavaToken.MINUSMINUS * UnaryExpression
        UnaryExpressionNotPlusMinus /= PostfixExpression or JavaToken.TILDA * UnaryExpression or JavaToken.EXCLAMATIONMARK * UnaryExpression or
                CastExpression
        PostfixExpression /= Primary or ExpressionName or PostIncrementExpression or PostDecrementExpression
        PostIncrementExpression /= PostfixExpression * JavaToken.PLUSPLUS
        PostDecrementExpression /= PostfixExpression * JavaToken.MINUSMINUS
        CastExpression /= JavaToken.PARENTHLEFT * PrimitiveType * JavaToken.PARENTHRIGHT * UnaryExpression or
                JavaToken.PARENTHLEFT * ReferenceType * Many(AdditionalBound) * JavaToken.PARENTHRIGHT * UnaryExpressionNotPlusMinus or
                JavaToken.PARENTHLEFT * ReferenceType * Many(AdditionalBound) * JavaToken.PARENTHRIGHT * LambdaExpression
        ConstantExpression /= Expression
    }
}