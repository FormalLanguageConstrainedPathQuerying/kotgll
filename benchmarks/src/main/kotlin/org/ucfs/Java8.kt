package org.ucfs
import org.ucfs.JavaToken.*
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
        Identifier /= ID

        Literal /= INTEGERLIT or FLOATINGLIT or BOOLEANLIT or
                CHARLIT or STRINGLIT or NULLLIT

        /**
         * Productions from §4 (Types, Values, and Variables)
         */
        Type /= PrimitiveType or ReferenceType
        PrimitiveType /= Many(Annotation) * NumericType or Many(Annotation) * BOOLEAN
        NumericType /= IntegralType or FloatingPointType
        IntegralType /= BYTE or SHORT or INT or LONG or CHAR
        FloatingPointType /= FLOAT or DOUBLE
        ReferenceType /= ClassOrInterfaceType or TypeVariable or ArrayType
        ClassOrInterfaceType /= ClassType or InterfaceType
        ClassType /= Many(Annotation) * Identifier * Option(TypeArguments) or
                ClassOrInterfaceType * DOT * Many(Annotation) * Identifier * Option(TypeArguments)
        InterfaceType /= ClassType
        TypeVariable /= Many(Annotation) * Identifier
        ArrayType /= PrimitiveType * Dims or ClassOrInterfaceType * Dims or TypeVariable * Dims
        Dims /= some(Many(Annotation) * BRACKETLEFT * BRACKETRIGHT)
        TypeParameter  /= Many(TypeParameterModifier) * Identifier * Option(TypeBound)
        TypeParameterModifier /= Annotation
        TypeBound /= EXTENDS * TypeVariable or EXTENDS * ClassOrInterfaceType * Many(AdditionalBound)
        AdditionalBound /= ANDBIT * InterfaceType
        TypeArguments /= LT * TypeArgumentList * GT
        TypeArgumentList /= TypeArgument * Many(COMMA * TypeArgument)
        TypeArgument /= ReferenceType or Wildcard
        Wildcard /= Many(Annotation) * QUESTIONMARK * Option(WildcardBounds)
        WildcardBounds /= EXTENDS * ReferenceType or SUPER * ReferenceType

        /**
         * Productions from §6 (Names)
         */

        TypeName /= Identifier or PackageOrTypeName * DOT * Identifier
        PackageOrTypeName /= Identifier or PackageOrTypeName * DOT * Identifier
        ExpressionName /= Identifier or AmbiguousName * DOT * Identifier
        MethodName /= Identifier
        PackageName /= Identifier or PackageName * DOT * Identifier
        AmbiguousName /= Identifier or AmbiguousName * DOT * Identifier

        /**
         * Productions from §7 (Packages)
         */

        CompilationUnit /= Option(PackageDeclaration) * Many(ImportDeclaration) * Many(TypeDeclaration)
        PackageDeclaration /= Many(PackageModifier) * PACKAGE * Identifier * Many(DOT * Identifier) * SEMICOLON
        PackageModifier /= Annotation
        ImportDeclaration /= SingleTypeImportDeclaration or TypeImportOnDemandDeclaration or
                SingleStaticImportDeclaration or StaticImportOnDemandDeclaration
        SingleTypeImportDeclaration /= IMPORT * TypeName * SEMICOLON
        TypeImportOnDemandDeclaration /= IMPORT * PackageOrTypeName * DOT * STAR * SEMICOLON
        SingleStaticImportDeclaration /= IMPORT * STATIC * TypeName * DOT * Identifier * SEMICOLON
        StaticImportOnDemandDeclaration /= IMPORT * STATIC * TypeName * DOT * STAR * SEMICOLON
        TypeDeclaration /= ClassDeclaration or InterfaceDeclaration or SEMICOLON

        /**
         * Productions from §8 (Classes)
         */

        ClassDeclaration /= NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration /= Many(ClassModifier) * CLASS * Identifier *
                Option(TypeParameters) * Option(Superclass) * Option(Superinterfaces) * ClassBody
        ClassModifier /= Annotation or PUBLIC or PROTECTED or PRIVATE or
                ABSTRACT or STATIC or FINAL or STRICTFP
        TypeParameters /= LT * TypeParameterList * GT
        TypeParameterList /= TypeParameter  * Many(COMMA * TypeParameter)
        Superclass /= EXTENDS * ClassType
        Superinterfaces /= IMPLEMENTS * InterfaceTypeList
        InterfaceTypeList /= InterfaceType  * Many(COMMA * InterfaceType)
        ClassBody /= CURLYLEFT * Many(ClassBodyDeclaration) * CURLYRIGHT
        ClassBodyDeclaration /= ClassMemberDeclaration or InstanceInitializer or StaticInitializer or ConstructorDeclaration
        ClassMemberDeclaration /= FieldDeclaration or MethodDeclaration or ClassDeclaration or InterfaceDeclaration or SEMICOLON
        FieldDeclaration /= Many(FieldModifier) * UnannType * VariableDeclaratorList * SEMICOLON
        FieldModifier /= Annotation or PUBLIC or PROTECTED or PRIVATE or STATIC or
                FINAL or TRANSIENT or VOLATILE
        VariableDeclaratorList /= VariableDeclarator * Many(COMMA * VariableDeclarator)
        VariableDeclarator /= VariableDeclaratorId * Option(ASSIGN * VariableInitializer)
        VariableDeclaratorId /= Identifier * Option(Dims)
        VariableInitializer /= Expression or ArrayInitializer
        UnannType /= UnannPrimitiveType or UnannReferenceType
        UnannPrimitiveType /= NumericType or BOOLEAN
        UnannReferenceType /= UnannClassOrInterfaceType or UnannTypeVariable or UnannArrayType
        UnannClassOrInterfaceType /= UnannClassType or UnannInterfaceType
        UnannClassType /= Identifier * Option(TypeArguments) or
                UnannClassOrInterfaceType * DOT * Many(Annotation) * Identifier * Option(TypeArguments)
        UnannInterfaceType /= UnannClassType
        UnannTypeVariable /= Identifier
        UnannArrayType /= UnannPrimitiveType * Dims or UnannClassOrInterfaceType * Dims or UnannTypeVariable * Dims
        MethodDeclaration /= Many(MethodModifier) * MethodHeader * MethodBody
        MethodModifier /= Annotation or PUBLIC or PROTECTED or PRIVATE or ABSTRACT or
                STATIC or FINAL or SYNCHRONIZED or NATIVE or STRICTFP
        MethodHeader /= Result * MethodDeclarator * Option(Throws) or
                TypeParameters * Many(Annotation) * Result * MethodDeclarator * Option(Throws)
        Result /= UnannType or VOID
        MethodDeclarator /= Identifier * PARENTHLEFT * Option(FormalParameterList) * PARENTHRIGHT * Option(Dims)
        FormalParameterList /= ReceiverParameter or FormalParameters * COMMA * LastFormalParameter or
                LastFormalParameter
        FormalParameters /= FormalParameter * Many(COMMA * FormalParameter) or
                ReceiverParameter * Many(COMMA * FormalParameter)
        FormalParameter /= Many(VariableModifier) * UnannType * VariableDeclaratorId
        VariableModifier /= Annotation or FINAL
        LastFormalParameter /= Many(VariableModifier) * UnannType * Many(Annotation) * ELLIPSIS * VariableDeclaratorId or FormalParameter
        ReceiverParameter /= Many(Annotation) * UnannType * Option(Identifier * DOT) * THIS
        Throws /= THROWS * ExceptionTypeList
        ExceptionTypeList /= ExceptionType * Many(COMMA * ExceptionType)
        ExceptionType /= ClassType or TypeVariable
        MethodBody /= Block or SEMICOLON
        InstanceInitializer /= Block
        StaticInitializer /= STATIC * Block
        ConstructorDeclaration /= Many(ConstructorModifier) * ConstructorDeclarator * Option(Throws) * ConstructorBody
        ConstructorModifier /= Annotation or PUBLIC or PROTECTED or PRIVATE
        ConstructorDeclarator /= Option(TypeParameters) * SimpleTypeName * PARENTHLEFT * Option(FormalParameterList) * PARENTHRIGHT
        SimpleTypeName /= Identifier
        ConstructorBody /= CURLYLEFT * Option(ExplicitConstructorInvocation) * Option(BlockStatements) * CURLYRIGHT
        ExplicitConstructorInvocation /= Option(TypeArguments) * THIS * PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT * SEMICOLON or
                Option(TypeArguments) * SUPER * PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT * SEMICOLON or
                ExpressionName * DOT * Option(TypeArguments) * SUPER * PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT * SEMICOLON or
                Primary * DOT * Option(TypeArguments) * SUPER * PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT * SEMICOLON
        EnumDeclaration /= Many(ClassModifier) * ENUM * Identifier * Option(Superinterfaces) * EnumBody
        EnumBody /= CURLYLEFT * Option(EnumConstantList) * Option(COMMA) * Option(EnumBodyDeclarations) * CURLYRIGHT
        EnumConstantList /= EnumConstant * Many(COMMA * EnumConstant)
        EnumConstant /= Many(EnumConstantModifier) * Identifier * Option(PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT * Option(ClassBody))
        EnumConstantModifier /= Annotation
        EnumBodyDeclarations /= SEMICOLON * Many(ClassBodyDeclaration)

        /**
         * Productions from §9 (Interfaces)
         */

        InterfaceDeclaration /= NormalInterfaceDeclaration or AnnotationTypeDeclaration
        NormalInterfaceDeclaration /=
            Many(InterfaceModifier) * INTERFACE * Identifier * Option(TypeParameters) * Option(ExtendsInterfaces) * InterfaceBody
        InterfaceModifier /= Annotation or PUBLIC or PROTECTED or PRIVATE or
                ABSTRACT or STATIC or STRICTFP
        ExtendsInterfaces /= EXTENDS * InterfaceTypeList
        InterfaceBody /= CURLYLEFT * Many(InterfaceMemberDeclaration) * CURLYRIGHT
        InterfaceMemberDeclaration /= ConstantDeclaration or InterfaceMethodDeclaration or ClassDeclaration or InterfaceDeclaration or SEMICOLON
        ConstantDeclaration /= Many(ConstantModifier) * UnannType * VariableDeclaratorList * SEMICOLON
        ConstantModifier /= Annotation or PUBLIC or STATIC or FINAL
        InterfaceMethodDeclaration /= Many(InterfaceMethodModifier) * MethodHeader * MethodBody
        InterfaceMethodModifier /= Annotation or PUBLIC or ABSTRACT or DEFAULT or STATIC or STRICTFP
        AnnotationTypeDeclaration /= Many(InterfaceModifier) * AT * INTERFACE * Identifier * AnnotationTypeBody
        AnnotationTypeBody /= CURLYLEFT * Many(AnnotationTypeMemberDeclaration) * CURLYRIGHT
        AnnotationTypeMemberDeclaration /= AnnotationTypeElementDeclaration or ConstantDeclaration or ClassDeclaration or InterfaceDeclaration or SEMICOLON
        AnnotationTypeElementDeclaration /=
            Many(AnnotationTypeElementModifier) * UnannType * Identifier * PARENTHLEFT * PARENTHRIGHT * Option(Dims) * Option(DefaultValue) * SEMICOLON
        AnnotationTypeElementModifier /= Annotation or PUBLIC or ABSTRACT
        DefaultValue /= DEFAULT * ElementValue
        Annotation /= NormalAnnotation or MarkerAnnotation or SingleElementAnnotation
        NormalAnnotation /= AT * TypeName * PARENTHLEFT * Option(ElementValuePairList) * PARENTHRIGHT
        ElementValuePairList /= ElementValuePair * Many(COMMA * ElementValuePair)
        ElementValuePair /= Identifier * ASSIGN * ElementValue
        ElementValue /= ConditionalExpression or ElementValueArrayInitializer or Annotation
        ElementValueArrayInitializer /= CURLYLEFT * Option(ElementValueList) * Option(COMMA) * CURLYRIGHT
        ElementValueList /= ElementValue * Many(COMMA * ElementValue)
        MarkerAnnotation /= AT * TypeName
        SingleElementAnnotation /= AT * TypeName * PARENTHLEFT * ElementValue * PARENTHRIGHT

        /**
         * Productions from §10 (Arrays)
         */

        ArrayInitializer /= CURLYLEFT * Option(VariableInitializerList) * Option(COMMA) * CURLYRIGHT
        VariableInitializerList /= VariableInitializer * Many(COMMA * VariableInitializer)

        /**
         * Productions from §14 (Blocks and Statements)
         */

        Block /= CURLYLEFT * Option(BlockStatements) * CURLYRIGHT
        BlockStatements /= BlockStatement * Many(BlockStatement)
        BlockStatement /= LocalVariableDeclarationStatement or ClassDeclaration or Statement
        LocalVariableDeclarationStatement /= LocalVariableDeclaration * SEMICOLON
        LocalVariableDeclaration /= Many(VariableModifier) * UnannType * VariableDeclaratorList
        Statement /= StatementWithoutTrailingSubstatement or LabeledStatement or IfThenStatement or IfThenElseStatement or
                WhileStatement or ForStatement
        StatementNoShortIf /= StatementWithoutTrailingSubstatement or LabeledStatementNoShortIf or IfThenElseStatementNoShortIf or
                WhileStatementNoShortIf or ForStatementNoShortIf
        StatementWithoutTrailingSubstatement /= Block or EmptyStatement or ExpressionStatement or AssertStatement or
                SwitchStatement or DoStatement or BreakStatement or ContinueStatement or ReturnStatement or SynchronizedStatement or
                ThrowStatement or TryStatement
        EmptyStatement /= SEMICOLON
        LabeledStatement /= Identifier * COLON * Statement
        LabeledStatementNoShortIf /= Identifier * COLON * StatementNoShortIf
        ExpressionStatement /= StatementExpression * SEMICOLON
        StatementExpression /= Assignment or PreIncrementExpression or PreDecrementExpression or PostIncrementExpression or
                PostDecrementExpression or MethodInvocation or ClassInstanceCreationExpression
        IfThenStatement /= IF * PARENTHLEFT * Expression * PARENTHRIGHT * Statement
        IfThenElseStatement /= IF * PARENTHLEFT * Expression * PARENTHRIGHT * StatementNoShortIf * ELSE * Statement
        IfThenElseStatementNoShortIf /=
            IF * PARENTHLEFT * Expression * PARENTHRIGHT * StatementNoShortIf * ELSE * StatementNoShortIf
        AssertStatement /= ASSERT * Expression * SEMICOLON or
                ASSERT * Expression * COLON * Expression * SEMICOLON
        SwitchStatement /= SWITCH * PARENTHLEFT * Expression * PARENTHRIGHT * SwitchBlock
        SwitchBlock /= CURLYLEFT * Many(SwitchBlockStatementGroup) * Many(SwitchLabel) * CURLYRIGHT
        SwitchBlockStatementGroup /= SwitchLabels * BlockStatements
        SwitchLabels /= some(SwitchLabel)
        SwitchLabel /= CASE * ConstantExpression * COLON or
                CASE * EnumConstantName * COLON or DEFAULT * COLON
        EnumConstantName /= Identifier
        WhileStatement /= WHILE * PARENTHLEFT * Expression * PARENTHRIGHT * Statement
        WhileStatementNoShortIf /= WHILE * PARENTHLEFT * Expression * PARENTHRIGHT * StatementNoShortIf
        DoStatement /= DO * Statement * WHILE * PARENTHLEFT * Expression * PARENTHRIGHT * SEMICOLON
        ForStatement /= BasicForStatement or EnhancedForStatement
        ForStatementNoShortIf /= BasicForStatementNoShortIf or EnhancedForStatementNoShortIf
        BasicForStatement /= FOR * PARENTHLEFT * Option(ForInit) * SEMICOLON * Option(Expression) * SEMICOLON * Option(ForUpdate) * PARENTHRIGHT * Statement
        BasicForStatementNoShortIf /= FOR * PARENTHLEFT * Option(ForInit) * SEMICOLON * Option(Expression) * SEMICOLON * Option(ForUpdate) * PARENTHRIGHT * StatementNoShortIf
        ForInit /= StatementExpressionList or LocalVariableDeclaration
        ForUpdate /= StatementExpressionList
        StatementExpressionList /= StatementExpression * Many(COMMA * StatementExpression)
        EnhancedForStatement /= FOR * PARENTHLEFT * Many(VariableModifier) * UnannType * VariableDeclaratorId * COLON * Expression * PARENTHRIGHT * Statement
        EnhancedForStatementNoShortIf /= FOR * PARENTHLEFT * Many(VariableModifier) * UnannType * VariableDeclaratorId * COLON * Expression * PARENTHRIGHT * StatementNoShortIf
        BreakStatement /= BREAK * Option(Identifier) * SEMICOLON
        ContinueStatement /= CONTINUE * Option(Identifier) * SEMICOLON
        ReturnStatement /= RETURN * Option(Expression) * SEMICOLON
        ThrowStatement /= THROW * Expression * SEMICOLON
        SynchronizedStatement /= SYNCHRONIZED * PARENTHLEFT * Expression * PARENTHRIGHT * Block
        TryStatement /= TRY * Block * Catches or TRY * Block * Option(Catches) * Finally or TryWithResourcesStatement
        Catches /= some(CatchClause)
        CatchClause /= CATCH * PARENTHLEFT * CatchFormalParameter * PARENTHRIGHT * Block
        CatchFormalParameter /= Many(VariableModifier) * CatchType * VariableDeclaratorId
        CatchType /= UnannClassType * Many(ORBIT * ClassType)
        Finally /= FINALLY * Block
        TryWithResourcesStatement /= TRY * ResourceSpecification * Block * Option(Catches) * Option(Finally)
        ResourceSpecification /= PARENTHLEFT * ResourceList * Option(SEMICOLON) * PARENTHRIGHT
        ResourceList /= Resource * Many(COMMA * Resource)
        Resource /= Many(VariableModifier) * UnannType * VariableDeclaratorId * ASSIGN * Expression

        /**
         * Productions from §15 (Expressions)
         */

        Primary /= PrimaryNoNewArray or ArrayCreationExpression
        PrimaryNoNewArray /= Literal or ClassLiteral or THIS or TypeName * DOT * THIS or
                PARENTHLEFT * Expression * PARENTHRIGHT or ClassInstanceCreationExpression or FieldAccess or
                ArrayAccess or MethodInvocation or MethodReference
        ClassLiteral /= TypeName * Many(BRACKETLEFT * BRACKETRIGHT) * DOT * CLASS or
                NumericType * Many(BRACKETLEFT * BRACKETRIGHT) * DOT * CLASS or
                BOOLEAN * Many(BRACKETLEFT * BRACKETRIGHT) * DOT * CLASS or
                VOID * DOT * CLASS
        ClassInstanceCreationExpression /= UnqualifiedClassInstanceCreationExpression or
                ExpressionName * DOT * UnqualifiedClassInstanceCreationExpression or
                Primary * DOT * UnqualifiedClassInstanceCreationExpression
        UnqualifiedClassInstanceCreationExpression /=
            NEW * Option(TypeArguments) * classOrInterfaceTypeToInstantiate * PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT * Option(ClassBody)
        classOrInterfaceTypeToInstantiate /= Many(Annotation) * Identifier * Many(DOT * Many(Annotation) * Identifier) * Option(TypeArgumentsOrDiamond)
        TypeArgumentsOrDiamond /= TypeArguments or LT * GT
        FieldAccess /= Primary * DOT * Identifier or SUPER * DOT * Identifier or
                TypeName * DOT * SUPER * DOT * Identifier
        ArrayAccess /= ExpressionName * BRACKETLEFT * Expression * BRACKETRIGHT or
                PrimaryNoNewArray * BRACKETLEFT * Expression * BRACKETRIGHT
        MethodInvocation /= MethodName * PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT or
                TypeName * DOT * Option(TypeArguments) * Identifier * PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT or
                ExpressionName * DOT * Option(TypeArguments) * Identifier * PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT or
                Primary * DOT * Option(TypeArguments) * Identifier * PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT or
                SUPER * DOT * Option(TypeArguments) * Identifier * PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT or
                TypeName * DOT * SUPER * DOT * Option(TypeArguments) * Identifier * PARENTHLEFT * Option(ArgumentList) * PARENTHRIGHT
        ArgumentList /= Expression * Many(COMMA * Expression)
        MethodReference /= ExpressionName * DOUBLECOLON * Option(TypeArguments) * Identifier or
                ReferenceType * DOUBLECOLON * Option(TypeArguments) * Identifier or
                Primary * DOUBLECOLON * Option(TypeArguments) * Identifier or
                SUPER * DOUBLECOLON * Option(TypeArguments) * Identifier or
                TypeName * DOT * SUPER * DOUBLECOLON * Option(TypeArguments) * Identifier or
                ClassType * DOUBLECOLON * Option(TypeArguments) * NEW or
                ArrayType * DOUBLECOLON * NEW
        ArrayCreationExpression /= NEW * PrimitiveType * DimExprs * Option(Dims) or
                NEW * ClassOrInterfaceType * DimExprs * Option(Dims) or
                NEW * PrimitiveType * Dims * ArrayInitializer or
                NEW * ClassOrInterfaceType * Dims * ArrayInitializer
        DimExprs /= some(DimExpr)
        DimExpr /= Many(Annotation) * BRACKETLEFT * Expression * BRACKETRIGHT
        Expression /= LambdaExpression or AssignmentExpression
        LambdaExpression /= LambdaParameters * ARROW * LambdaBody
        LambdaParameters /= Identifier or PARENTHLEFT * Option(FormalParameterList) * PARENTHRIGHT or
                PARENTHLEFT * InferredFormalParameterList * PARENTHRIGHT
        InferredFormalParameterList /= Identifier * Many(COMMA * Identifier)
        LambdaBody /= Expression or Block
        AssignmentExpression /= ConditionalExpression or Assignment
        Assignment /= LeftHandSide * AssignmentOperator * Expression
        LeftHandSide /= ExpressionName or FieldAccess or ArrayAccess
        AssignmentOperator /= ASSIGN or STARASSIGN or SLASHASSIGN or PERCENTASSIGN or PLUSASSIGN or MINUSASSIGN or
                SHIFTLEFTASSIGN or SHIFTRIGHTASSIGN or USRIGHTSHIFTASSIGN or ANDASSIGN or XORASSIGN or ORASSIGN
        ConditionalExpression /= ConditionalOrExpression or
                ConditionalOrExpression * QUESTIONMARK * Expression * COLON * ConditionalExpression or
                ConditionalOrExpression * QUESTIONMARK * Expression * COLON * LambdaExpression
        ConditionalOrExpression /= ConditionalAndExpression or
                ConditionalOrExpression * OR * ConditionalAndExpression
        ConditionalAndExpression /= InclusiveOrExpression or
                ConditionalAndExpression * AND * InclusiveOrExpression
        InclusiveOrExpression /= ExclusiveOrExpression or
                InclusiveOrExpression * ORBIT * ExclusiveOrExpression
        ExclusiveOrExpression /= AndExpression or ExclusiveOrExpression * XORBIT * AndExpression
        AndExpression /= EqualityExpression or AndExpression * ANDBIT * EqualityExpression
        EqualityExpression /= RelationalExpression or EqualityExpression * EQ * RelationalExpression or
                EqualityExpression * NOTEQ * RelationalExpression
        RelationalExpression /= ShiftExpression or RelationalExpression * LT * ShiftExpression or
                RelationalExpression * GT * ShiftExpression or RelationalExpression * LESSEQ * ShiftExpression or
                RelationalExpression * GREATEQ * ShiftExpression or RelationalExpression * INSTANCEOF * ReferenceType
        ShiftExpression /= AdditiveExpression or ShiftExpression * LT * LT * AdditiveExpression or
                ShiftExpression * GT * GT * AdditiveExpression or
                ShiftExpression * GT * GT * GT * AdditiveExpression
        AdditiveExpression /= MultiplicativeExpression or AdditiveExpression * PLUS * MultiplicativeExpression or
                AdditiveExpression * MINUS * MultiplicativeExpression
        MultiplicativeExpression /= UnaryExpression or MultiplicativeExpression * STAR * UnaryExpression or
                MultiplicativeExpression * SLASH * UnaryExpression or
                MultiplicativeExpression * PERCENT * UnaryExpression
        UnaryExpression /= PreIncrementExpression or PreDecrementExpression or PLUS * UnaryExpression or
                MINUS * UnaryExpression or UnaryExpressionNotPlusMinus
        PreIncrementExpression /= PLUSPLUS * UnaryExpression
        PreDecrementExpression /= MINUSMINUS * UnaryExpression
        UnaryExpressionNotPlusMinus /= PostfixExpression or TILDA * UnaryExpression or EXCLAMATIONMARK * UnaryExpression or
                CastExpression
        PostfixExpression /= Primary or ExpressionName or PostIncrementExpression or PostDecrementExpression
        PostIncrementExpression /= PostfixExpression * PLUSPLUS
        PostDecrementExpression /= PostfixExpression * MINUSMINUS
        CastExpression /= PARENTHLEFT * PrimitiveType * PARENTHRIGHT * UnaryExpression or
                PARENTHLEFT * ReferenceType * Many(AdditionalBound) * PARENTHRIGHT * UnaryExpressionNotPlusMinus or
                PARENTHLEFT * ReferenceType * Many(AdditionalBound) * PARENTHRIGHT * LambdaExpression
        ConstantExpression /= Expression
    }
}