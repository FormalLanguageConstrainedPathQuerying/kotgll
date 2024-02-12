package org.srcgll.lexer

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class JavaGrammar : Grammar() {
    var CompilationUnit by Nt()
    var Literal by Nt()
    var Type by Nt()
    var PrimitiveType by Nt()
    var ReferenceType by Nt()
    var Annotation by Nt()
    var NumericType by Nt()
    var IntegralType by Nt()
    var FloatingPointType by Nt()
    var TypeVariable by Nt()
    var ArrayType by Nt()
    var ClassType by Nt()
    var TypeArguments by Nt()
    var Dims by Nt()
    var TypeParameter by Nt()
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
    var UnannArrayType by Nt()
    var UnannClassType by Nt()
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
    var ExplicitConstructorInvocation by Nt()
    var EnumBody by Nt()
    var EnumConstantList by Nt()
    var EnumConstant by Nt()
    var EnumBodyDeclarations by Nt()
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
    var SwitchLabel by Nt()
    var WhileStatement by Nt()
    var WhileStatementNoShortIf by Nt()
    var DoStatement by Nt()
    var InterfaceMethodModifier by Nt()
    var ForStatement by Nt()
    var ForStatementNoShortIf by Nt()
    var BasicForStatement by Nt()
    var BasicForStatementNoShortIf by Nt()
    var ForInit by Nt()
    var StatementExpressionList by Nt()
    var EnhancedForStatement by Nt()
    var EnhancedForStatementNoShortIf by Nt()
    var BreakStatement by Nt()
    var ContinueStatement by Nt()
    var ReturnStatement by Nt()
    var ThrowStatement by Nt()
    var SynchronizedStatement by Nt()
    var TryStatement by Nt()
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

    init {
        Literal = Term(JavaToken.INTEGER_LITERAL) or Term(JavaToken.FLOATING_POINT_LITERAL) or Term(JavaToken.BOOLEAN_LITERAL) or
                Term(JavaToken.CHARACTER_LITERAL) or Term(JavaToken.STRING_LITERAL) or Term(JavaToken.NULL_LITERAL)

        /**
         * Productions from §4 (Types, Values, and Variables)
         */
        Type = PrimitiveType or ReferenceType
        PrimitiveType = Many(Annotation) * NumericType or Many(Annotation) * Term(JavaToken.BOOLEAN)
        NumericType = IntegralType or FloatingPointType
        IntegralType = Term(JavaToken.BYTE) or Term(JavaToken.SHORT) or Term(JavaToken.INT) or Term(JavaToken.LONG) or Term(JavaToken.CHAR)
        FloatingPointType = Term(JavaToken.FLOAT) or Term(JavaToken.DOUBLE)
        ReferenceType = ClassType or TypeVariable or ArrayType
        ClassType = Many(Annotation) * Term(JavaToken.IDENTIFIER) * Option(TypeArguments) or
                ClassType * Term(JavaToken.DOT) * Many(Annotation) * Term(JavaToken.IDENTIFIER) * Option(TypeArguments)
        TypeVariable = Many(Annotation) * Term(JavaToken.IDENTIFIER)
        ArrayType = PrimitiveType * Dims or ClassType * Dims or TypeVariable * Dims
        Dims = Some(Many(Annotation) * Term(JavaToken.LBRACK) * Term(JavaToken.RBRACK))
        TypeParameter  = Many(Annotation) * Term(JavaToken.IDENTIFIER) * Option(TypeBound)
        TypeBound = Term(JavaToken.EXTENDS) * TypeVariable or Term(JavaToken.EXTENDS) * ClassType * Many(AdditionalBound)
        AdditionalBound = Term(JavaToken.AND) * ClassType
        TypeArguments = Term(JavaToken.LT) * TypeArgumentList * Term(JavaToken.GT)
        TypeArgumentList = TypeArgument * Many(Term(JavaToken.COMMA) * TypeArgument)
        TypeArgument = ReferenceType or Wildcard
        Wildcard = Many(Annotation) * Term(JavaToken.QUESTION) * Option(WildcardBounds)
        WildcardBounds = Term(JavaToken.EXTENDS) * ReferenceType or Term(JavaToken.SUPER) * ReferenceType

        /**
         * Productions from §6 (Names)
         */

        TypeName = Term(JavaToken.IDENTIFIER) or PackageOrTypeName * Term(JavaToken.DOT) * Term(JavaToken.IDENTIFIER)
        PackageOrTypeName = Term(JavaToken.IDENTIFIER) or PackageOrTypeName * Term(JavaToken.DOT) * Term(JavaToken.IDENTIFIER)
        ExpressionName = Term(JavaToken.IDENTIFIER) or AmbiguousName * Term(JavaToken.DOT) * Term(JavaToken.IDENTIFIER)
        PackageName = Term(JavaToken.IDENTIFIER) or PackageName * Term(JavaToken.DOT) * Term(JavaToken.IDENTIFIER)
        AmbiguousName = Term(JavaToken.IDENTIFIER) or AmbiguousName * Term(JavaToken.DOT) * Term(JavaToken.IDENTIFIER)

        /**
         * Productions from §7 (Packages)
         */

        CompilationUnit = Option(PackageDeclaration) * Many(ImportDeclaration) * Many(TypeDeclaration)
        PackageDeclaration = Many(Annotation) * Term(JavaToken.PACKAGE) * Term(JavaToken.IDENTIFIER) * Many(Term(JavaToken.DOT) * Term(JavaToken.IDENTIFIER)) * Term(JavaToken.SEMICOLON)
        ImportDeclaration = SingleTypeImportDeclaration or TypeImportOnDemandDeclaration or
                SingleStaticImportDeclaration or StaticImportOnDemandDeclaration
        SingleTypeImportDeclaration = Term(JavaToken.IMPORT) * TypeName * Term(JavaToken.SEMICOLON)
        TypeImportOnDemandDeclaration = Term(JavaToken.IMPORT) * PackageOrTypeName * Term(JavaToken.DOT) * Term(JavaToken.MULT) * Term(JavaToken.SEMICOLON)
        SingleStaticImportDeclaration = Term(JavaToken.IMPORT) * Term(JavaToken.STATIC) * TypeName * Term(JavaToken.DOT) * Term(JavaToken.IDENTIFIER) * Term(JavaToken.SEMICOLON)
        StaticImportOnDemandDeclaration = Term(JavaToken.IMPORT) * Term(JavaToken.STATIC) * TypeName * Term(JavaToken.DOT) * Term(JavaToken.MULT) * Term(JavaToken.SEMICOLON)
        TypeDeclaration = ClassDeclaration or InterfaceDeclaration or Term(JavaToken.SEMICOLON)

        /**
         * Productions from §8 (Classes)
         */

        ClassDeclaration = NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration = Many(ClassModifier) * Term(JavaToken.CLASS) * Term(JavaToken.IDENTIFIER) *
                Option(TypeParameters) * Option(Superclass) * Option(Superinterfaces) * ClassBody
        ClassModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or
                Term(JavaToken.ABSTRACT) or Term(JavaToken.STATIC) or Term(JavaToken.FINAL) or Term(JavaToken.STRICTFP)
        TypeParameters = Term(JavaToken.LT) * TypeParameterList * Term(JavaToken.GT)
        TypeParameterList = TypeParameter  * Many(Term(JavaToken.COMMA) * TypeParameter)
        Superclass = Term(JavaToken.EXTENDS) * ClassType
        Superinterfaces = Term(JavaToken.IMPLEMENTS) * InterfaceTypeList
        InterfaceTypeList = ClassType  * Many(Term(JavaToken.COMMA) * ClassType)
        ClassBody = Term(JavaToken.LBRACE) * Many(ClassBodyDeclaration) * Term(JavaToken.RBRACE)
        ClassBodyDeclaration = ClassMemberDeclaration or Block or StaticInitializer or ConstructorDeclaration
        ClassMemberDeclaration = FieldDeclaration or MethodDeclaration or ClassDeclaration or InterfaceDeclaration or Term(JavaToken.SEMICOLON)
        FieldDeclaration = Many(FieldModifier) * UnannType * VariableDeclaratorList * Term(JavaToken.SEMICOLON)
        FieldModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or Term(JavaToken.STATIC) or
                Term(JavaToken.FINAL) or Term(JavaToken.TRANSIENT) or Term(JavaToken.VOLATILE)
        VariableDeclaratorList = VariableDeclarator * Many(Term(JavaToken.COMMA) * VariableDeclarator)
        VariableDeclarator = VariableDeclaratorId * Option(Term(JavaToken.EQ) * VariableInitializer)
        VariableDeclaratorId = Term(JavaToken.IDENTIFIER) * Option(Dims)
        VariableInitializer = Expression or ArrayInitializer
        UnannType = UnannPrimitiveType or UnannReferenceType
        UnannPrimitiveType = NumericType or Term(JavaToken.BOOLEAN)
        UnannReferenceType = UnannClassType or Term(JavaToken.IDENTIFIER) or UnannArrayType
        UnannClassType = Term(JavaToken.IDENTIFIER) * Option(TypeArguments) or
                UnannClassType * Term(JavaToken.DOT) * Many(Annotation) * Term(JavaToken.IDENTIFIER) * Option(TypeArguments)
        UnannArrayType = UnannPrimitiveType * Dims or UnannClassType * Dims or Term(JavaToken.IDENTIFIER) * Dims
        MethodDeclaration = Many(MethodModifier) * MethodHeader * MethodBody
        MethodModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or Term(JavaToken.ABSTRACT) or
                Term(JavaToken.STATIC) or Term(JavaToken.FINAL) or Term(JavaToken.SYNCHRONIZED) or Term(JavaToken.NATIVE) or Term(JavaToken.STRICTFP)
        MethodHeader = Result * MethodDeclarator * Option(Throws) or TypeParameters * Many(Annotation) * Result * MethodDeclarator * Option(Throws)
        Result = UnannType or Term(JavaToken.VOID)
        MethodDeclarator = Term(JavaToken.IDENTIFIER) * Term(JavaToken.LPAREN) * Option(FormalParameterList) * Term(JavaToken.RPAREN) * Option(Dims)
        FormalParameterList = ReceiverParameter or FormalParameters * Term(JavaToken.COMMA) * LastFormalParameter or LastFormalParameter
        FormalParameters = FormalParameter * Many(Term(JavaToken.COMMA) * FormalParameter) or ReceiverParameter * Many(Term(JavaToken.COMMA) * FormalParameter)
        FormalParameter = Many(VariableModifier) * UnannType * VariableDeclaratorId
        VariableModifier = Annotation or Term(JavaToken.FINAL)
        LastFormalParameter = Many(VariableModifier) * UnannType * Many(Annotation) * Term(JavaToken.ELLIPSIS) * VariableDeclaratorId or FormalParameter
        ReceiverParameter = Many(Annotation) * UnannType * Option(Term(JavaToken.IDENTIFIER) * Term(JavaToken.DOT)) * Term(JavaToken.THIS)
        Throws = Term(JavaToken.THROWS) * ExceptionTypeList
        ExceptionTypeList = ExceptionType * Many(Term(JavaToken.COMMA) * ExceptionType)
        ExceptionType = ClassType or TypeVariable
        MethodBody = Block or Term(JavaToken.SEMICOLON)
        StaticInitializer = Term(JavaToken.STATIC) * Block
        ConstructorDeclaration = Many(ConstructorModifier) * ConstructorDeclarator * Option(Throws) * ConstructorBody
        ConstructorModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE)
        ConstructorDeclarator = Option(TypeParameters) * Term(JavaToken.IDENTIFIER) * Term(JavaToken.LPAREN) * Option(FormalParameterList) * Term(JavaToken.RPAREN)
        ConstructorBody = Term(JavaToken.LBRACE) * Option(ExplicitConstructorInvocation) * Option(Some(BlockStatement)) * Term(JavaToken.RBRACE)
        ExplicitConstructorInvocation = Option(TypeArguments) * Term(JavaToken.THIS) * Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN) * Term(JavaToken.SEMICOLON) or
                Option(TypeArguments) * Term(JavaToken.SUPER) * Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN) * Term(JavaToken.SEMICOLON) or
                ExpressionName * Term(JavaToken.DOT) * Option(TypeArguments) * Term(JavaToken.SUPER) * Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN) * Term(JavaToken.SEMICOLON) or
                Primary * Term(JavaToken.DOT) * Option(TypeArguments) * Term(JavaToken.SUPER) * Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN) * Term(JavaToken.SEMICOLON)
        EnumDeclaration = Many(ClassModifier) * Term(JavaToken.ENUM) * Term(JavaToken.IDENTIFIER) * Option(Superinterfaces) * EnumBody
        EnumBody = Term(JavaToken.LBRACE) * Option(EnumConstantList) * Option(Term(JavaToken.COMMA)) * Option(EnumBodyDeclarations) * Term(JavaToken.RBRACE)
        EnumConstantList = EnumConstant * Many(Term(JavaToken.COMMA) * EnumConstant)
        EnumConstant = Many(Annotation) * Term(JavaToken.IDENTIFIER) * Option(Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN)) * Option(ClassBody)
        EnumBodyDeclarations = Term(JavaToken.SEMICOLON) * Many(ClassBodyDeclaration)

        /**
         * Productions from §9 (Interfaces)
         */

        InterfaceDeclaration = NormalInterfaceDeclaration or AnnotationTypeDeclaration
        NormalInterfaceDeclaration =
            Many(InterfaceModifier) * Term(JavaToken.INTERFACE) * Term(JavaToken.IDENTIFIER) * Option(TypeParameters) * Option(ExtendsInterfaces) * InterfaceBody
        InterfaceModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or
                Term(JavaToken.ABSTRACT) or Term(JavaToken.STATIC) or Term(JavaToken.STRICTFP)
        ExtendsInterfaces = Term(JavaToken.EXTENDS) * InterfaceTypeList
        InterfaceBody = Term(JavaToken.LBRACE) * Many(InterfaceMemberDeclaration) * Term(JavaToken.RBRACE)
        InterfaceMemberDeclaration = ConstantDeclaration or InterfaceMethodDeclaration or ClassDeclaration or InterfaceDeclaration or Term(JavaToken.SEMICOLON)
        ConstantDeclaration = Many(ConstantModifier) * UnannType * VariableDeclaratorList * Term(JavaToken.SEMICOLON)
        ConstantModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.STATIC) or Term(JavaToken.FINAL)
        InterfaceMethodDeclaration = Many(InterfaceMethodModifier) * MethodHeader * MethodBody
        InterfaceMethodModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.ABSTRACT) or Term(JavaToken.DEFAULT) or Term(JavaToken.STATIC) or Term(JavaToken.STRICTFP)
        AnnotationTypeDeclaration = Many(InterfaceModifier) * Term(JavaToken.AT) * Term(JavaToken.INTERFACE) * Term(JavaToken.IDENTIFIER) * AnnotationTypeBody
        AnnotationTypeBody = Term(JavaToken.LBRACE) * Many(AnnotationTypeMemberDeclaration) * Term(JavaToken.RBRACE)
        AnnotationTypeMemberDeclaration = AnnotationTypeElementDeclaration or ConstantDeclaration or ClassDeclaration or InterfaceDeclaration or Term(JavaToken.SEMICOLON)
        AnnotationTypeElementDeclaration =
            Many(AnnotationTypeElementModifier) * UnannType * Term(JavaToken.IDENTIFIER) * Term(JavaToken.LPAREN) * Term(JavaToken.RPAREN) * Option(Dims) * Option(DefaultValue) * Term(JavaToken.SEMICOLON)
        AnnotationTypeElementModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.ABSTRACT)
        DefaultValue = Term(JavaToken.DEFAULT) * ElementValue
        Annotation = NormalAnnotation or MarkerAnnotation or SingleElementAnnotation
        NormalAnnotation = Term(JavaToken.AT) * TypeName * Term(JavaToken.LPAREN) * Option(ElementValuePairList) * Term(JavaToken.RPAREN)
        ElementValuePairList = ElementValuePair * Many(Term(JavaToken.COMMA) * ElementValuePair)
        ElementValuePair = Term(JavaToken.IDENTIFIER) * Term(JavaToken.EQ) * ElementValue
        ElementValue = ConditionalExpression or ElementValueArrayInitializer or Annotation
        ElementValueArrayInitializer = Term(JavaToken.LBRACE) * Option(ElementValueList) * Option(Term(JavaToken.COMMA)) * Term(JavaToken.RBRACE)
        ElementValueList = ElementValue * Many(Term(JavaToken.COMMA) * ElementValue)
        MarkerAnnotation = Term(JavaToken.AT) * TypeName
        SingleElementAnnotation = Term(JavaToken.AT) * TypeName * Term(JavaToken.LPAREN) * ElementValue * Term(JavaToken.RPAREN)

        /**
         * Productions from §10 (Arrays)
         */

        ArrayInitializer = Term(JavaToken.LBRACE) * Option(VariableInitializerList) * Option(Term(JavaToken.COMMA)) * Term(JavaToken.RBRACE)
        VariableInitializerList = VariableInitializer * Many(Term(JavaToken.COMMA) * VariableInitializer)

        /**
         * Productions from §14 (Blocks and Statements)
         */

        Block = Term(JavaToken.LBRACE) * Option(Some(BlockStatement)) * Term(JavaToken.RBRACE)
        BlockStatement = LocalVariableDeclarationStatement or ClassDeclaration or Statement
        LocalVariableDeclarationStatement = LocalVariableDeclaration * Term(JavaToken.SEMICOLON)
        LocalVariableDeclaration = Many(VariableModifier) * UnannType * VariableDeclaratorList
        Statement = StatementWithoutTrailingSubstatement or LabeledStatement or IfThenStatement or IfThenElseStatement or
                WhileStatement or ForStatement
        StatementNoShortIf = StatementWithoutTrailingSubstatement or LabeledStatementNoShortIf or IfThenElseStatementNoShortIf or
                WhileStatementNoShortIf or ForStatementNoShortIf
        StatementWithoutTrailingSubstatement = Block or Term(JavaToken.SEMICOLON) or ExpressionStatement or AssertStatement or
                SwitchStatement or DoStatement or BreakStatement or ContinueStatement or ReturnStatement or SynchronizedStatement or
                ThrowStatement or TryStatement
        LabeledStatement = Term(JavaToken.IDENTIFIER) * Term(JavaToken.COLON) * Statement
        LabeledStatementNoShortIf = Term(JavaToken.IDENTIFIER) * Term(JavaToken.COLON) * StatementNoShortIf
        ExpressionStatement = StatementExpression * Term(JavaToken.SEMICOLON)
        StatementExpression = Assignment or PreIncrementExpression or PreDecrementExpression or PostIncrementExpression or
                PostDecrementExpression or MethodInvocation or ClassInstanceCreationExpression
        IfThenStatement = Term(JavaToken.IF) * Term(JavaToken.LPAREN) * Expression * Term(JavaToken.RPAREN) * Statement
        IfThenElseStatement = Term(JavaToken.IF) * Term(JavaToken.LPAREN) * Expression * Term(JavaToken.RPAREN) * StatementNoShortIf * Term(JavaToken.ELSE) * Statement
        IfThenElseStatementNoShortIf =
            Term(JavaToken.IF) * Term(JavaToken.LPAREN) * Expression * Term(JavaToken.RPAREN) * StatementNoShortIf * Term(JavaToken.ELSE) * StatementNoShortIf
        AssertStatement = Term(JavaToken.ASSERT) * Expression * Term(JavaToken.SEMICOLON) or
                Term(JavaToken.ASSERT) * Expression * Term(JavaToken.COLON) * Expression * Term(JavaToken.SEMICOLON)
        SwitchStatement = Term(JavaToken.SWITCH) * Term(JavaToken.LPAREN) * Expression * Term(JavaToken.RPAREN) * SwitchBlock
        SwitchBlock = Term(JavaToken.LBRACE) * Many(SwitchBlockStatementGroup) * Many(SwitchLabel) * Term(JavaToken.RBRACE)
        SwitchBlockStatementGroup = Some(SwitchLabel) * Some(BlockStatement)
        SwitchLabel = Term(JavaToken.CASE) * Expression * Term(JavaToken.COLON) or
                Term(JavaToken.CASE) * Term(JavaToken.IDENTIFIER) * Term(JavaToken.COLON) or Term(JavaToken.DEFAULT) * Term(JavaToken.COLON)
        WhileStatement = Term(JavaToken.WHILE) * Term(JavaToken.LPAREN) * Expression * Term(JavaToken.RPAREN) * Statement
        WhileStatementNoShortIf = Term(JavaToken.WHILE) * Term(JavaToken.LPAREN) * Expression * Term(JavaToken.RPAREN) * StatementNoShortIf
        DoStatement = Term(JavaToken.DO) * Statement * Term(JavaToken.WHILE) * Term(JavaToken.LPAREN) * Expression * Term(JavaToken.RPAREN) * Term(JavaToken.SEMICOLON)
        ForStatement = BasicForStatement or EnhancedForStatement
        ForStatementNoShortIf = BasicForStatementNoShortIf or EnhancedForStatementNoShortIf
        BasicForStatement = Term(JavaToken.FOR) * Term(JavaToken.LPAREN) * Option(ForInit) * Term(JavaToken.SEMICOLON) * Option(Expression) * Term(JavaToken.SEMICOLON) * Option(StatementExpressionList) * Term(JavaToken.RPAREN) * Statement
        BasicForStatementNoShortIf = Term(JavaToken.FOR) * Term(JavaToken.LPAREN) * Option(ForInit) * Term(JavaToken.SEMICOLON) * Option(Expression) * Term(JavaToken.SEMICOLON) * Option(StatementExpressionList) * Term(JavaToken.RPAREN) * StatementNoShortIf
        ForInit = StatementExpressionList or LocalVariableDeclaration
        StatementExpressionList = StatementExpression * Many(Term(JavaToken.COMMA) * StatementExpression)
        EnhancedForStatement = Term(JavaToken.FOR) * Term(JavaToken.LPAREN) * Many(VariableModifier) * UnannType * VariableDeclaratorId * Term(JavaToken.COLON) * Expression * Term(JavaToken.RPAREN) * Statement
        EnhancedForStatementNoShortIf = Term(JavaToken.FOR) * Term(JavaToken.LPAREN) * Many(VariableModifier) * UnannType * VariableDeclaratorId * Term(JavaToken.COLON) * Expression * Term(JavaToken.RPAREN) * StatementNoShortIf
        BreakStatement = Term(JavaToken.BREAK) * Option(Term(JavaToken.IDENTIFIER)) * Term(JavaToken.SEMICOLON)
        ContinueStatement = Term(JavaToken.CONTINUE) * Option(Term(JavaToken.IDENTIFIER)) * Term(JavaToken.SEMICOLON)
        ReturnStatement = Term(JavaToken.RETURN) * Option(Expression) * Term(JavaToken.SEMICOLON)
        ThrowStatement = Term(JavaToken.THROW) * Expression * Term(JavaToken.SEMICOLON)
        SynchronizedStatement = Term(JavaToken.SYNCHRONIZED) * Term(JavaToken.LPAREN) * Expression * Term(JavaToken.RPAREN) * Block
        TryStatement = Term(JavaToken.TRY) * Block * Some(CatchClause) or Term(JavaToken.TRY) * Block * Option(Some(CatchClause)) * Finally or TryWithResourcesStatement
        CatchClause = Term(JavaToken.CATCH) * Term(JavaToken.LPAREN) * CatchFormalParameter * Term(JavaToken.RPAREN) * Block
        CatchFormalParameter = Many(VariableModifier) * CatchType * VariableDeclaratorId
        CatchType = UnannClassType * Many(Term(JavaToken.OR) * ClassType)
        Finally = Term(JavaToken.FINALLY) * Block
        TryWithResourcesStatement = Term(JavaToken.TRY) * ResourceSpecification * Block * Option(Some(CatchClause)) * Option(Finally)
        ResourceSpecification = Term(JavaToken.LPAREN) * ResourceList * Option(Term(JavaToken.SEMICOLON)) * Term(JavaToken.RPAREN)
        ResourceList = Resource * Many(Term(JavaToken.COMMA) * Resource)
        Resource = Many(VariableModifier) * UnannType * VariableDeclaratorId * Term(JavaToken.EQ) * Expression

        /**
         * Productions from §15 (Expressions)
         */

        Primary = PrimaryNoNewArray or ArrayCreationExpression
        PrimaryNoNewArray = Literal or ClassLiteral or Term(JavaToken.THIS) or TypeName * Term(JavaToken.DOT) * Term(JavaToken.THIS) or
                Term(JavaToken.LPAREN) * Expression * Term(JavaToken.RPAREN) or ClassInstanceCreationExpression or FieldAccess or
                ArrayAccess or MethodInvocation or MethodReference
        ClassLiteral = TypeName * Many(Term(JavaToken.LBRACK) * Term(JavaToken.RBRACK)) * Term(JavaToken.DOT) * Term(JavaToken.CLASS) or
                NumericType * Many(Term(JavaToken.LBRACK) * Term(JavaToken.RBRACK)) * Term(JavaToken.DOT) * Term(JavaToken.CLASS) or
                Term(JavaToken.BOOLEAN) * Many(Term(JavaToken.LBRACK) * Term(JavaToken.RBRACK)) * Term(JavaToken.DOT) * Term(JavaToken.CLASS) or
                Term(JavaToken.VOID) * Term(JavaToken.DOT) * Term(JavaToken.CLASS)
        ClassInstanceCreationExpression = UnqualifiedClassInstanceCreationExpression or
                ExpressionName * Term(JavaToken.DOT) * UnqualifiedClassInstanceCreationExpression or
                Primary * Term(JavaToken.DOT) * UnqualifiedClassInstanceCreationExpression
        UnqualifiedClassInstanceCreationExpression =
            Term(JavaToken.NEW) * Option(TypeArguments) * classOrInterfaceTypeToInstantiate * Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN) * Option(ClassBody)
        classOrInterfaceTypeToInstantiate = Many(Annotation) * Term(JavaToken.IDENTIFIER) * Many(Term(JavaToken.DOT) * Many(Annotation) * Term(JavaToken.IDENTIFIER)) * Option(TypeArgumentsOrDiamond)
        TypeArgumentsOrDiamond = TypeArguments or Term(JavaToken.LT) * Term(JavaToken.GT)
        FieldAccess = Primary * Term(JavaToken.DOT) * Term(JavaToken.IDENTIFIER) or Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Term(JavaToken.IDENTIFIER) or
                TypeName * Term(JavaToken.DOT) * Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Term(JavaToken.IDENTIFIER)
        ArrayAccess = ExpressionName * Term(JavaToken.LBRACK) * Expression * Term(JavaToken.RBRACK) or
                PrimaryNoNewArray * Term(JavaToken.LBRACK) * Expression * Term(JavaToken.RBRACK)
        MethodInvocation = Term(JavaToken.IDENTIFIER) * Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN) or
                TypeName * Term(JavaToken.DOT) * Option(TypeArguments) * Term(JavaToken.IDENTIFIER) * Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN) or
                ExpressionName * Term(JavaToken.DOT) * Option(TypeArguments) * Term(JavaToken.IDENTIFIER) * Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN) or
                Primary * Term(JavaToken.DOT) * Option(TypeArguments) * Term(JavaToken.IDENTIFIER) * Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN) or
                Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Option(TypeArguments) * Term(JavaToken.IDENTIFIER) * Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN) or
                TypeName * Term(JavaToken.DOT) * Term(JavaToken.SUPER) * Term(JavaToken.DOT) * Option(TypeArguments) * Term(JavaToken.IDENTIFIER) * Term(JavaToken.LPAREN) * Option(ArgumentList) * Term(JavaToken.RPAREN)
        ArgumentList = Expression * Many(Term(JavaToken.COMMA) * Expression)
        MethodReference = ExpressionName * Term(JavaToken.COLONCOLON) * Option(TypeArguments) * Term(JavaToken.IDENTIFIER) or
                ReferenceType * Term(JavaToken.COLONCOLON) * Option(TypeArguments) * Term(JavaToken.IDENTIFIER) or
                Primary * Term(JavaToken.COLONCOLON) * Option(TypeArguments) * Term(JavaToken.IDENTIFIER) or
                Term(JavaToken.SUPER) * Term(JavaToken.COLONCOLON) * Option(TypeArguments) * Term(JavaToken.IDENTIFIER) or
                TypeName * Term(JavaToken.DOT) * Term(JavaToken.SUPER) * Term(JavaToken.COLONCOLON) * Option(TypeArguments) * Term(JavaToken.IDENTIFIER) or
                ClassType * Term(JavaToken.COLONCOLON) * Option(TypeArguments) * Term(JavaToken.NEW) or
                ArrayType * Term(JavaToken.COLONCOLON) * Term(JavaToken.NEW)
        ArrayCreationExpression = Term(JavaToken.NEW) * PrimitiveType * Some(DimExpr) * Option(Dims) or
                Term(JavaToken.NEW) * ClassType * Some(DimExpr) * Option(Dims) or
                Term(JavaToken.NEW) * PrimitiveType * Dims * ArrayInitializer or
                Term(JavaToken.NEW) * ClassType * Dims * ArrayInitializer
        DimExpr = Many(Annotation) * Term(JavaToken.LBRACK) * Expression * Term(JavaToken.RBRACK)
        Expression = LambdaExpression or AssignmentExpression
        LambdaExpression = LambdaParameters * Term(JavaToken.ARROW) * LambdaBody
        LambdaParameters = Term(JavaToken.IDENTIFIER) or Term(JavaToken.LPAREN) * Option(FormalParameterList) * Term(JavaToken.RPAREN) or
                Term(JavaToken.LPAREN) * InferredFormalParameterList * Term(JavaToken.RPAREN)
        InferredFormalParameterList = Term(JavaToken.IDENTIFIER) * Many(Term(JavaToken.COMMA) * Term(JavaToken.IDENTIFIER))
        LambdaBody = Expression or Block
        AssignmentExpression = ConditionalExpression or Assignment
        Assignment = LeftHandSide * AssignmentOperator * Expression
        LeftHandSide = ExpressionName or FieldAccess or ArrayAccess
        AssignmentOperator = Term(JavaToken.EQ) or Term(JavaToken.MULTEQ) or Term(JavaToken.DIVEQ) or Term(JavaToken.MODEQ) or Term(JavaToken.PLUSEQ) or Term(JavaToken.MINUSEQ) or
                Term(JavaToken.LSHIFTEQ) or Term(JavaToken.RSHIFTEQ) or Term(JavaToken.URSHIFTEQ) or Term(JavaToken.ANDEQ) or Term(JavaToken.XOREQ) or Term(JavaToken.OREQ)
        ConditionalExpression = ConditionalOrExpression or
                ConditionalOrExpression * Term(JavaToken.QUESTION) * Expression * Term(JavaToken.COLON) * ConditionalExpression or
                ConditionalOrExpression * Term(JavaToken.QUESTION) * Expression * Term(JavaToken.COLON) * LambdaExpression
        ConditionalOrExpression = ConditionalAndExpression or ConditionalOrExpression * Term(JavaToken.OROR) * ConditionalAndExpression
        ConditionalAndExpression = InclusiveOrExpression or ConditionalAndExpression * Term(JavaToken.ANDAND) * InclusiveOrExpression
        InclusiveOrExpression = ExclusiveOrExpression or InclusiveOrExpression * Term(JavaToken.OR) * ExclusiveOrExpression
        ExclusiveOrExpression = AndExpression or ExclusiveOrExpression * Term(JavaToken.XOR) * AndExpression
        AndExpression = EqualityExpression or AndExpression * Term(JavaToken.AND) * EqualityExpression
        EqualityExpression = RelationalExpression or EqualityExpression * Term(JavaToken.EQEQ) * RelationalExpression or
                EqualityExpression * Term(JavaToken.NOTEQ) * RelationalExpression
        RelationalExpression = ShiftExpression or RelationalExpression * Term(JavaToken.LT) * ShiftExpression or
                RelationalExpression * Term(JavaToken.GT) * ShiftExpression or
                RelationalExpression * Term(JavaToken.LTEQ) * ShiftExpression or
                RelationalExpression * Term(JavaToken.GTEQ) * ShiftExpression or
                RelationalExpression * Term(JavaToken.INSTANCEOF) * ReferenceType
        ShiftExpression = AdditiveExpression or ShiftExpression * Term(JavaToken.LT) * Term(JavaToken.LT) * AdditiveExpression or
                ShiftExpression * Term(JavaToken.GT) * Term(JavaToken.GT) * AdditiveExpression or
                ShiftExpression * Term(JavaToken.GT) * Term(JavaToken.GT) * Term(JavaToken.GT) * AdditiveExpression
        AdditiveExpression = MultiplicativeExpression or AdditiveExpression * Term(JavaToken.PLUS) * MultiplicativeExpression or
                AdditiveExpression * Term(JavaToken.MINUS) * MultiplicativeExpression
        MultiplicativeExpression = UnaryExpression or MultiplicativeExpression * Term(JavaToken.MULT) * UnaryExpression or
                MultiplicativeExpression * Term(JavaToken.DIV) * UnaryExpression or
                MultiplicativeExpression * Term(JavaToken.MOD) * UnaryExpression
        UnaryExpression = PreIncrementExpression or PreDecrementExpression or Term(JavaToken.PLUS) * UnaryExpression or
                Term(JavaToken.MINUS) * UnaryExpression or UnaryExpressionNotPlusMinus
        PreIncrementExpression = Term(JavaToken.PLUSPLUS) * UnaryExpression
        PreDecrementExpression = Term(JavaToken.MINUSMINUS) * UnaryExpression
        UnaryExpressionNotPlusMinus = PostfixExpression or Term(JavaToken.COMP) * UnaryExpression or Term(JavaToken.NOT) * UnaryExpression or
                CastExpression
        PostfixExpression = Primary or ExpressionName or PostIncrementExpression or PostDecrementExpression
        PostIncrementExpression = PostfixExpression * Term(JavaToken.PLUSPLUS)
        PostDecrementExpression = PostfixExpression * Term(JavaToken.MINUSMINUS)
        CastExpression = Term(JavaToken.LPAREN) * PrimitiveType * Term(JavaToken.RPAREN) * UnaryExpression or
                Term(JavaToken.LPAREN) * ReferenceType * Many(AdditionalBound) * Term(JavaToken.RPAREN) * UnaryExpressionNotPlusMinus or
                Term(JavaToken.LPAREN) * ReferenceType * Many(AdditionalBound) * Term(JavaToken.RPAREN) * LambdaExpression

        setStart(CompilationUnit)
    }
}