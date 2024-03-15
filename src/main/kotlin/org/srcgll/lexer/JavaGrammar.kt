package org.srcgll.lexer

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class JavaGrammar : Grammar() {
    var CompilationUnit by Nt()
    var Identifier by Nt()
    var Literal by Nt()
    var Type by Nt()
    var PrimitiveType by Nt()
    var ReferenceType by Nt()
    var Annotation by Nt()
    var NumericType by Nt()
    var IntegralType by Nt()
    var FloatingPointType by Nt()
    var ClassOrInterfaceType by Nt()
    var TypeVariable by Nt()
    var ArrayType by Nt()
    var ClassType by Nt()
    var InterfaceType by Nt()
    var TypeArguments by Nt()
    var Dims by Nt()
    var TypeParameter by Nt()
    var TypeParameterModifier by Nt()
    var TypeBound by Nt()
    var AdditionalBound by Nt()
    var TypeArgumentList by Nt()
    var TypeArgument by Nt()
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
    var InterfaceDeclaration by Nt()
    var Throws by Nt()
    var NormalClassDeclaration by Nt()
    var EnumDeclaration by Nt()
    var ClassModifier by Nt()
    var TypeParameters by Nt()
    var Superclass by Nt()
    var Superinterfaces by Nt()
    var ClassBody by Nt()
    var TypeParameterList by Nt()
    var InterfaceTypeList by Nt()
    var ClassBodyDeclaration by Nt()
    var ClassMemberDeclaration by Nt()
    var InstanceInitializer by Nt()
    var StaticInitializer by Nt()
    var ConstructorDeclaration by Nt()
    var FieldDeclaration by Nt()
    var MethodDeclaration by Nt()
    var FieldModifier by Nt()
    var UnannType by Nt()
    var VariableDeclaratorList by Nt()
    var VariableDeclarator by Nt()
    var VariableDeclaratorId by Nt()
    var VariableInitializer by Nt()
    var Expression by Nt()
    var ArrayInitializer by Nt()
    var UnannPrimitiveType by Nt()
    var UnannReferenceType by Nt()
    var UnannClassOrInterfaceType by Nt()
    var UnannTypeVariable by Nt()
    var UnannArrayType by Nt()
    var UnannClassType by Nt()
    var UnannInterfaceType by Nt()
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
    var ExceptionTypeList by Nt()
    var ExceptionType by Nt()
    var Block by Nt()
    var ConstructorModifier by Nt()
    var ConstructorDeclarator by Nt()
    var ConstructorBody by Nt()
    var SimpleTypeName by Nt()
    var ExplicitConstructorInvocation by Nt()
    var EnumBody by Nt()
    var EnumConstantList by Nt()
    var EnumConstant by Nt()
    var EnumConstantModifier by Nt()
    var EnumBodyDeclarations by Nt()
    var BlockStatements by Nt()
    var ArgumentList by Nt()
    var Primary by Nt()
    var NormalInterfaceDeclaration by Nt()
    var InterfaceModifier by Nt()
    var ExtendsInterfaces by Nt()
    var InterfaceBody by Nt()
    var InterfaceMemberDeclaration by Nt()
    var ConstantDeclaration by Nt()
    var ConstantModifier by Nt()
    var AnnotationTypeDeclaration by Nt()
    var AnnotationTypeBody by Nt()
    var AnnotationTypeMemberDeclaration by Nt()
    var AnnotationTypeElementDeclaration by Nt()
    var DefaultValue by Nt()
    var NormalAnnotation by Nt()
    var ElementValuePairList by Nt()
    var ElementValuePair by Nt()
    var ElementValue by Nt()
    var ElementValueArrayInitializer by Nt()
    var ElementValueList by Nt()
    var MarkerAnnotation by Nt()
    var SingleElementAnnotation by Nt()
    var InterfaceMethodDeclaration by Nt()
    var AnnotationTypeElementModifier by Nt()
    var ConditionalExpression by Nt()
    var VariableInitializerList by Nt()
    var BlockStatement by Nt()
    var LocalVariableDeclarationStatement by Nt()
    var LocalVariableDeclaration by Nt()
    var Statement by Nt()
    var StatementNoShortIf by Nt()
    var StatementWithoutTrailingSubstatement by Nt()
    var EmptyStatement by Nt()
    var LabeledStatement by Nt()
    var LabeledStatementNoShortIf by Nt()
    var ExpressionStatement by Nt()
    var StatementExpression by Nt()
    var IfThenStatement by Nt()
    var IfThenElseStatement by Nt()
    var IfThenElseStatementNoShortIf by Nt()
    var AssertStatement by Nt()
    var SwitchStatement by Nt()
    var SwitchBlock by Nt()
    var SwitchBlockStatementGroup by Nt()
    var SwitchLabels by Nt()
    var SwitchLabel by Nt()
    var EnumConstantName by Nt()
    var WhileStatement by Nt()
    var WhileStatementNoShortIf by Nt()
    var DoStatement by Nt()
    var InterfaceMethodModifier by Nt()
    var ForStatement by Nt()
    var ForStatementNoShortIf by Nt()
    var BasicForStatement by Nt()
    var BasicForStatementNoShortIf by Nt()
    var ForInit by Nt()
    var ForUpdate by Nt()
    var StatementExpressionList by Nt()
    var EnhancedForStatement by Nt()
    var EnhancedForStatementNoShortIf by Nt()
    var BreakStatement by Nt()
    var ContinueStatement by Nt()
    var ReturnStatement by Nt()
    var ThrowStatement by Nt()
    var SynchronizedStatement by Nt()
    var TryStatement by Nt()
    var Catches by Nt()
    var CatchClause by Nt()
    var CatchFormalParameter by Nt()
    var CatchType by Nt()
    var Finally by Nt()
    var TryWithResourcesStatement by Nt()
    var ResourceSpecification by Nt()
    var ResourceList by Nt()
    var Resource by Nt()
    var PrimaryNoNewArray by Nt()
    var ClassLiteral by Nt()
    var classOrInterfaceTypeToInstantiate by Nt()
    var UnqualifiedClassInstanceCreationExpression by Nt()
    var ClassInstanceCreationExpression by Nt()
    var FieldAccess by Nt()
    var TypeArgumentsOrDiamond by Nt()
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
    var AssignmentExpression by Nt()
    var Assignment by Nt()
    var LeftHandSide by Nt()
    var AssignmentOperator by Nt()
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
    var PreIncrementExpression by Nt()
    var PreDecrementExpression by Nt()
    var UnaryExpressionNotPlusMinus by Nt()
    var UnaryExpression by Nt()
    var PostfixExpression by Nt()
    var PostIncrementExpression by Nt()
    var PostDecrementExpression by Nt()
    var CastExpression by Nt()
    var ConstantExpression by Nt()

    init {
        Identifier = Term(JavaToken.ID)

        Literal = Term(JavaToken.INTEGERLIT) or Term(JavaToken.FLOATINGLIT) or Term(JavaToken.BOOLEANLIT) or
                Term(JavaToken.CHARLIT) or Term(JavaToken.STRINGLIT) or Term(JavaToken.NULLLIT)

        /**
         * Productions from §4 (Types, Values, and Variables)
         */
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
        TypeArguments = Term(JavaToken.LT) * TypeArgumentList * Term(JavaToken.GT)
        TypeArgumentList = TypeArgument * Many(Term(JavaToken.COMMA) * TypeArgument)
        TypeArgument = ReferenceType or Wildcard
        Wildcard = Many(Annotation) * Term(JavaToken.QUESTIONMARK) * Option(WildcardBounds)
        WildcardBounds = Term(JavaToken.EXTENDS) * ReferenceType or Term(JavaToken.SUPER) * ReferenceType

        /**
         * Productions from §6 (Names)
         */

        TypeName = Identifier or PackageOrTypeName * Term(JavaToken.DOT) * Identifier
        PackageOrTypeName = Identifier or PackageOrTypeName * Term(JavaToken.DOT) * Identifier
        ExpressionName = Identifier or AmbiguousName * Term(JavaToken.DOT) * Identifier
        MethodName = Identifier
        PackageName = Identifier or PackageName * Term(JavaToken.DOT) * Identifier
        AmbiguousName = Identifier or AmbiguousName * Term(JavaToken.DOT) * Identifier

        /**
         * Productions from §7 (Packages)
         */

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

        /**
         * Productions from §8 (Classes)
         */

        ClassDeclaration = NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration = Many(ClassModifier) * Term(JavaToken.CLASS) * Identifier *
                Option(TypeParameters) * Option(Superclass) * Option(Superinterfaces) * ClassBody
        ClassModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or
                Term(JavaToken.ABSTRACT) or Term(JavaToken.STATIC) or Term(JavaToken.FINAL) or Term(JavaToken.STRICTFP)
        TypeParameters = Term(JavaToken.LT) * TypeParameterList * Term(JavaToken.GT)
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
        MethodHeader = Result * MethodDeclarator * Option(Throws) or
                TypeParameters * Many(Annotation) * Result * MethodDeclarator * Option(Throws)
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

        /**
         * Productions from §9 (Interfaces)
         */

        InterfaceDeclaration = NormalInterfaceDeclaration or AnnotationTypeDeclaration
        NormalInterfaceDeclaration =
            Many(InterfaceModifier) * Term(JavaToken.INTERFACE) * Identifier * Option(TypeParameters) * Option(ExtendsInterfaces) * InterfaceBody
        InterfaceModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or
                Term(JavaToken.ABSTRACT) or Term(JavaToken.STATIC) or Term(JavaToken.STRICTFP)
        ExtendsInterfaces = Term(JavaToken.EXTENDS) * InterfaceTypeList
        InterfaceBody = Term(JavaToken.CURLYLEFT) * Many(InterfaceMemberDeclaration) * Term(JavaToken.CURLYRIGHT)
        InterfaceMemberDeclaration = ConstantDeclaration or InterfaceMethodDeclaration or ClassDeclaration or InterfaceDeclaration or Term(JavaToken.SEMICOLON)
        ConstantDeclaration = Many(ConstantModifier) * UnannType * VariableDeclaratorList * Term(JavaToken.SEMICOLON)
        ConstantModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.STATIC) or Term(JavaToken.FINAL)
        InterfaceMethodDeclaration = Many(InterfaceMethodModifier) * MethodHeader * MethodBody
        InterfaceMethodModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.ABSTRACT) or Term(JavaToken.DEFAULT) or Term(JavaToken.STATIC) or Term(JavaToken.STRICTFP)
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

        /**
         * Productions from §10 (Arrays)
         */

        ArrayInitializer = Term(JavaToken.CURLYLEFT) * Option(VariableInitializerList) * Option(Term(JavaToken.COMMA)) * Term(JavaToken.CURLYRIGHT)
        VariableInitializerList = VariableInitializer * Many(Term(JavaToken.COMMA) * VariableInitializer)

        /**
         * Productions from §14 (Blocks and Statements)
         */

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
        SwitchLabels = Some(SwitchLabel)
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
        Catches = Some(CatchClause)
        CatchClause = Term(JavaToken.CATCH) * Term(JavaToken.PARENTHLEFT) * CatchFormalParameter * Term(JavaToken.PARENTHRIGHT) * Block
        CatchFormalParameter = Many(VariableModifier) * CatchType * VariableDeclaratorId
        CatchType = UnannClassType * Many(Term(JavaToken.ORBIT) * ClassType)
        Finally = Term(JavaToken.FINALLY) * Block
        TryWithResourcesStatement = Term(JavaToken.TRY) * ResourceSpecification * Block * Option(Catches) * Option(Finally)
        ResourceSpecification = Term(JavaToken.PARENTHLEFT) * ResourceList * Option(Term(JavaToken.SEMICOLON)) * Term(JavaToken.PARENTHRIGHT)
        ResourceList = Resource * Many(Term(JavaToken.COMMA) * Resource)
        Resource = Many(VariableModifier) * UnannType * VariableDeclaratorId * Term(JavaToken.ASSIGN) * Expression

        /**
         * Productions from §15 (Expressions)
         */

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
            Term(JavaToken.NEW) * Option(TypeArguments) * classOrInterfaceTypeToInstantiate * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) * Option(ClassBody)
        classOrInterfaceTypeToInstantiate = Many(Annotation) * Identifier * Many(Term(JavaToken.DOT) * Many(Annotation) * Identifier) * Option(TypeArgumentsOrDiamond)
        TypeArgumentsOrDiamond = TypeArguments or Term(JavaToken.LT) * Term(JavaToken.GT)
        FieldAccess = Primary * Term(JavaToken.DOT) * Identifier or Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Identifier or
                TypeName * Term(JavaToken.DOT) * Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Identifier
        ArrayAccess = ExpressionName * Term(JavaToken.BRACKETLEFT) * Expression * Term(JavaToken.BRACKETRIGHT) or
                PrimaryNoNewArray * Term(JavaToken.BRACKETLEFT) * Expression * Term(JavaToken.BRACKETRIGHT)
        MethodInvocation = MethodName * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                TypeName * Term(JavaToken.DOT) * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                ExpressionName * Term(JavaToken.DOT) * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                Primary * Term(JavaToken.DOT) * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
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
        DimExprs = Some(DimExpr)
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
        RelationalExpression = ShiftExpression or RelationalExpression * Term(JavaToken.LT) * ShiftExpression or
                RelationalExpression * Term(JavaToken.GT) * ShiftExpression or RelationalExpression * Term(JavaToken.LESSEQ) * ShiftExpression or
                RelationalExpression * Term(JavaToken.GREATEQ) * ShiftExpression or RelationalExpression * Term(JavaToken.INSTANCEOF) * ReferenceType
        ShiftExpression = AdditiveExpression or ShiftExpression * Term(JavaToken.LT) * Term(JavaToken.LT) * AdditiveExpression or
                ShiftExpression * Term(JavaToken.GT) * Term(JavaToken.GT) * AdditiveExpression or
                ShiftExpression * Term(JavaToken.GT) * Term(JavaToken.GT) * Term(JavaToken.GT) * AdditiveExpression
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