package grammars
import lexers.JavaToken
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.*

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
        Identifier = JavaToken.ID

        Literal = JavaToken.INTEGERLIT or JavaToken.FLOATINGLIT or JavaToken.BOOLEANLIT or
                JavaToken.CHARLIT or JavaToken.STRINGLIT or JavaToken.NULLLIT

        /**
         * Productions from §4 (Types, Values, and Variables)
         */
        Type = PrimitiveType or ReferenceType
        PrimitiveType = Many(Annotation) * NumericType or Many(Annotation) * JavaToken.BOOLEAN
        NumericType = IntegralType or FloatingPointType
        IntegralType = JavaToken.BYTE or JavaToken.SHORT or JavaToken.INT or JavaToken.LONG or JavaToken.CHAR
        FloatingPointType = JavaToken.FLOAT or JavaToken.DOUBLE
        ReferenceType = ClassOrInterfaceType or TypeVariable or ArrayType
        ClassOrInterfaceType = ClassType or InterfaceType
        ClassType = Many(Annotation) * Identifier * Option(TypeArguments) or
                ClassOrInterfaceType * JavaToken.DOT * Many(Annotation) * Identifier * Option(TypeArguments)
        InterfaceType = ClassType
        TypeVariable = Many(Annotation) * Identifier
        ArrayType = PrimitiveType * Dims or ClassOrInterfaceType * Dims or TypeVariable * Dims
        Dims = Some(Many(Annotation) * JavaToken.BRACKETLEFT * JavaToken.BRACKETRIGHT)
        TypeParameter  = Many(TypeParameterModifier) * Identifier * Option(TypeBound)
        TypeParameterModifier = Annotation
        TypeBound = JavaToken.EXTENDS * TypeVariable or JavaToken.EXTENDS * ClassOrInterfaceType * Many(AdditionalBound)
        AdditionalBound = JavaToken.ANDBIT * InterfaceType
        TypeArguments = JavaToken.LT * TypeArgumentList * JavaToken.GT
        TypeArgumentList = TypeArgument * Many(JavaToken.COMMA * TypeArgument)
        TypeArgument = ReferenceType or Wildcard
        Wildcard = Many(Annotation) * JavaToken.QUESTIONMARK * Option(WildcardBounds)
        WildcardBounds = JavaToken.EXTENDS * ReferenceType or JavaToken.SUPER * ReferenceType

        /**
         * Productions from §6 (Names)
         */

        TypeName = Identifier or PackageOrTypeName * JavaToken.DOT * Identifier
        PackageOrTypeName = Identifier or PackageOrTypeName * JavaToken.DOT * Identifier
        ExpressionName = Identifier or AmbiguousName * JavaToken.DOT * Identifier
        MethodName = Identifier
        PackageName = Identifier or PackageName * JavaToken.DOT * Identifier
        AmbiguousName = Identifier or AmbiguousName * JavaToken.DOT * Identifier

        /**
         * Productions from §7 (Packages)
         */

        CompilationUnit = Option(PackageDeclaration) * Many(ImportDeclaration) * Many(TypeDeclaration)
        PackageDeclaration = Many(PackageModifier) * JavaToken.PACKAGE * Identifier * Many(JavaToken.DOT * Identifier) * JavaToken.SEMICOLON
        PackageModifier = Annotation
        ImportDeclaration = SingleTypeImportDeclaration or TypeImportOnDemandDeclaration or
                SingleStaticImportDeclaration or StaticImportOnDemandDeclaration
        SingleTypeImportDeclaration = JavaToken.IMPORT * TypeName * JavaToken.SEMICOLON
        TypeImportOnDemandDeclaration = JavaToken.IMPORT * PackageOrTypeName * JavaToken.DOT * JavaToken.STAR * JavaToken.SEMICOLON
        SingleStaticImportDeclaration = JavaToken.IMPORT * JavaToken.STATIC * TypeName * JavaToken.DOT * Identifier * JavaToken.SEMICOLON
        StaticImportOnDemandDeclaration = JavaToken.IMPORT * JavaToken.STATIC * TypeName * JavaToken.DOT * JavaToken.STAR * JavaToken.SEMICOLON
        TypeDeclaration = ClassDeclaration or InterfaceDeclaration or JavaToken.SEMICOLON

        /**
         * Productions from §8 (Classes)
         */

        ClassDeclaration = NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration = Many(ClassModifier) * JavaToken.CLASS * Identifier *
                Option(TypeParameters) * Option(Superclass) * Option(Superinterfaces) * ClassBody
        ClassModifier = Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or
                JavaToken.ABSTRACT or JavaToken.STATIC or JavaToken.FINAL or JavaToken.STRICTFP
        TypeParameters = JavaToken.LT * TypeParameterList * JavaToken.GT
        TypeParameterList = TypeParameter  * Many(JavaToken.COMMA * TypeParameter)
        Superclass = JavaToken.EXTENDS * ClassType
        Superinterfaces = JavaToken.IMPLEMENTS * InterfaceTypeList
        InterfaceTypeList = InterfaceType  * Many(JavaToken.COMMA * InterfaceType)
        ClassBody = JavaToken.CURLYLEFT * Many(ClassBodyDeclaration) * JavaToken.CURLYRIGHT
        ClassBodyDeclaration = ClassMemberDeclaration or InstanceInitializer or StaticInitializer or ConstructorDeclaration
        ClassMemberDeclaration = FieldDeclaration or MethodDeclaration or ClassDeclaration or InterfaceDeclaration or JavaToken.SEMICOLON
        FieldDeclaration = Many(FieldModifier) * UnannType * VariableDeclaratorList * JavaToken.SEMICOLON
        FieldModifier = Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or JavaToken.STATIC or
                JavaToken.FINAL or JavaToken.TRANSIENT or JavaToken.VOLATILE
        VariableDeclaratorList = VariableDeclarator * Many(JavaToken.COMMA * VariableDeclarator)
        VariableDeclarator = VariableDeclaratorId * Option(JavaToken.ASSIGN * VariableInitializer)
        VariableDeclaratorId = Identifier * Option(Dims)
        VariableInitializer = Expression or ArrayInitializer
        UnannType = UnannPrimitiveType or UnannReferenceType
        UnannPrimitiveType = NumericType or JavaToken.BOOLEAN
        UnannReferenceType = UnannClassOrInterfaceType or UnannTypeVariable or UnannArrayType
        UnannClassOrInterfaceType = UnannClassType or UnannInterfaceType
        UnannClassType = Identifier * Option(TypeArguments) or
                UnannClassOrInterfaceType * JavaToken.DOT * Many(Annotation) * Identifier * Option(TypeArguments)
        UnannInterfaceType = UnannClassType
        UnannTypeVariable = Identifier
        UnannArrayType = UnannPrimitiveType * Dims or UnannClassOrInterfaceType * Dims or UnannTypeVariable * Dims
        MethodDeclaration = Many(MethodModifier) * MethodHeader * MethodBody
        MethodModifier = Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or JavaToken.ABSTRACT or
                JavaToken.STATIC or JavaToken.FINAL or JavaToken.SYNCHRONIZED or JavaToken.NATIVE or JavaToken.STRICTFP
        MethodHeader = Result * MethodDeclarator * Option(Throws) or
                TypeParameters * Many(Annotation) * Result * MethodDeclarator * Option(Throws)
        Result = UnannType or JavaToken.VOID
        MethodDeclarator = Identifier * JavaToken.PARENTHLEFT * Option(FormalParameterList) * JavaToken.PARENTHRIGHT * Option(Dims)
        FormalParameterList = ReceiverParameter or FormalParameters * JavaToken.COMMA * LastFormalParameter or
                LastFormalParameter
        FormalParameters = FormalParameter * Many(JavaToken.COMMA * FormalParameter) or
                ReceiverParameter * Many(JavaToken.COMMA * FormalParameter)
        FormalParameter = Many(VariableModifier) * UnannType * VariableDeclaratorId
        VariableModifier = Annotation or JavaToken.FINAL
        LastFormalParameter = Many(VariableModifier) * UnannType * Many(Annotation) * JavaToken.ELLIPSIS * VariableDeclaratorId or FormalParameter
        ReceiverParameter = Many(Annotation) * UnannType * Option(Identifier * JavaToken.DOT) * JavaToken.THIS
        Throws = JavaToken.THROWS * ExceptionTypeList
        ExceptionTypeList = ExceptionType * Many(JavaToken.COMMA * ExceptionType)
        ExceptionType = ClassType or TypeVariable
        MethodBody = Block or JavaToken.SEMICOLON
        InstanceInitializer = Block
        StaticInitializer = JavaToken.STATIC * Block
        ConstructorDeclaration = Many(ConstructorModifier) * ConstructorDeclarator * Option(Throws) * ConstructorBody
        ConstructorModifier = Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE
        ConstructorDeclarator = Option(TypeParameters) * SimpleTypeName * JavaToken.PARENTHLEFT * Option(FormalParameterList) * JavaToken.PARENTHRIGHT
        SimpleTypeName = Identifier
        ConstructorBody = JavaToken.CURLYLEFT * Option(ExplicitConstructorInvocation) * Option(BlockStatements) * JavaToken.CURLYRIGHT
        ExplicitConstructorInvocation = Option(TypeArguments) * JavaToken.THIS * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON or
                Option(TypeArguments) * JavaToken.SUPER * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON or
                ExpressionName * JavaToken.DOT * Option(TypeArguments) * JavaToken.SUPER * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON or
                Primary * JavaToken.DOT * Option(TypeArguments) * JavaToken.SUPER * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON
        EnumDeclaration = Many(ClassModifier) * JavaToken.ENUM * Identifier * Option(Superinterfaces) * EnumBody
        EnumBody = JavaToken.CURLYLEFT * Option(EnumConstantList) * Option(JavaToken.COMMA) * Option(EnumBodyDeclarations) * JavaToken.CURLYRIGHT
        EnumConstantList = EnumConstant * Many(JavaToken.COMMA * EnumConstant)
        EnumConstant = Many(EnumConstantModifier) * Identifier * Option(JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * Option(ClassBody))
        EnumConstantModifier = Annotation
        EnumBodyDeclarations = JavaToken.SEMICOLON * Many(ClassBodyDeclaration)

        /**
         * Productions from §9 (Interfaces)
         */

        InterfaceDeclaration = NormalInterfaceDeclaration or AnnotationTypeDeclaration
        NormalInterfaceDeclaration =
            Many(InterfaceModifier) * JavaToken.INTERFACE * Identifier * Option(TypeParameters) * Option(ExtendsInterfaces) * InterfaceBody
        InterfaceModifier = Annotation or JavaToken.PUBLIC or JavaToken.PROTECTED or JavaToken.PRIVATE or
                JavaToken.ABSTRACT or JavaToken.STATIC or JavaToken.STRICTFP
        ExtendsInterfaces = JavaToken.EXTENDS * InterfaceTypeList
        InterfaceBody = JavaToken.CURLYLEFT * Many(InterfaceMemberDeclaration) * JavaToken.CURLYRIGHT
        InterfaceMemberDeclaration = ConstantDeclaration or InterfaceMethodDeclaration or ClassDeclaration or InterfaceDeclaration or JavaToken.SEMICOLON
        ConstantDeclaration = Many(ConstantModifier) * UnannType * VariableDeclaratorList * JavaToken.SEMICOLON
        ConstantModifier = Annotation or JavaToken.PUBLIC or JavaToken.STATIC or JavaToken.FINAL
        InterfaceMethodDeclaration = Many(InterfaceMethodModifier) * MethodHeader * MethodBody
        InterfaceMethodModifier = Annotation or JavaToken.PUBLIC or JavaToken.ABSTRACT or JavaToken.DEFAULT or JavaToken.STATIC or JavaToken.STRICTFP
        AnnotationTypeDeclaration = Many(InterfaceModifier) * JavaToken.AT * JavaToken.INTERFACE * Identifier * AnnotationTypeBody
        AnnotationTypeBody = JavaToken.CURLYLEFT * Many(AnnotationTypeMemberDeclaration) * JavaToken.CURLYRIGHT
        AnnotationTypeMemberDeclaration = AnnotationTypeElementDeclaration or ConstantDeclaration or ClassDeclaration or InterfaceDeclaration or JavaToken.SEMICOLON
        AnnotationTypeElementDeclaration =
            Many(AnnotationTypeElementModifier) * UnannType * Identifier * JavaToken.PARENTHLEFT * JavaToken.PARENTHRIGHT * Option(Dims) * Option(DefaultValue) * JavaToken.SEMICOLON
        AnnotationTypeElementModifier = Annotation or JavaToken.PUBLIC or JavaToken.ABSTRACT
        DefaultValue = JavaToken.DEFAULT * ElementValue
        Annotation = NormalAnnotation or MarkerAnnotation or SingleElementAnnotation
        NormalAnnotation = JavaToken.AT * TypeName * JavaToken.PARENTHLEFT * Option(ElementValuePairList) * JavaToken.PARENTHRIGHT
        ElementValuePairList = ElementValuePair * Many(JavaToken.COMMA * ElementValuePair)
        ElementValuePair = Identifier * JavaToken.ASSIGN * ElementValue
        ElementValue = ConditionalExpression or ElementValueArrayInitializer or Annotation
        ElementValueArrayInitializer = JavaToken.CURLYLEFT * Option(ElementValueList) * Option(JavaToken.COMMA) * JavaToken.CURLYRIGHT
        ElementValueList = ElementValue * Many(JavaToken.COMMA * ElementValue)
        MarkerAnnotation = JavaToken.AT * TypeName
        SingleElementAnnotation = JavaToken.AT * TypeName * JavaToken.PARENTHLEFT * ElementValue * JavaToken.PARENTHRIGHT

        /**
         * Productions from §10 (Arrays)
         */

        ArrayInitializer = JavaToken.CURLYLEFT * Option(VariableInitializerList) * Option(JavaToken.COMMA) * JavaToken.CURLYRIGHT
        VariableInitializerList = VariableInitializer * Many(JavaToken.COMMA * VariableInitializer)

        /**
         * Productions from §14 (Blocks and Statements)
         */

        Block = JavaToken.CURLYLEFT * Option(BlockStatements) * JavaToken.CURLYRIGHT
        BlockStatements = BlockStatement * Many(BlockStatement)
        BlockStatement = LocalVariableDeclarationStatement or ClassDeclaration or Statement
        LocalVariableDeclarationStatement = LocalVariableDeclaration * JavaToken.SEMICOLON
        LocalVariableDeclaration = Many(VariableModifier) * UnannType * VariableDeclaratorList
        Statement = StatementWithoutTrailingSubstatement or LabeledStatement or IfThenStatement or IfThenElseStatement or
                WhileStatement or ForStatement
        StatementNoShortIf = StatementWithoutTrailingSubstatement or LabeledStatementNoShortIf or IfThenElseStatementNoShortIf or
                WhileStatementNoShortIf or ForStatementNoShortIf
        StatementWithoutTrailingSubstatement = Block or EmptyStatement or ExpressionStatement or AssertStatement or
                SwitchStatement or DoStatement or BreakStatement or ContinueStatement or ReturnStatement or SynchronizedStatement or
                ThrowStatement or TryStatement
        EmptyStatement = JavaToken.SEMICOLON
        LabeledStatement = Identifier * JavaToken.COLON * Statement
        LabeledStatementNoShortIf = Identifier * JavaToken.COLON * StatementNoShortIf
        ExpressionStatement = StatementExpression * JavaToken.SEMICOLON
        StatementExpression = Assignment or PreIncrementExpression or PreDecrementExpression or PostIncrementExpression or
                PostDecrementExpression or MethodInvocation or ClassInstanceCreationExpression
        IfThenStatement = JavaToken.IF * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * Statement
        IfThenElseStatement = JavaToken.IF * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * StatementNoShortIf * JavaToken.ELSE * Statement
        IfThenElseStatementNoShortIf =
            JavaToken.IF * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * StatementNoShortIf * JavaToken.ELSE * StatementNoShortIf
        AssertStatement = JavaToken.ASSERT * Expression * JavaToken.SEMICOLON or
                JavaToken.ASSERT * Expression * JavaToken.COLON * Expression * JavaToken.SEMICOLON
        SwitchStatement = JavaToken.SWITCH * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * SwitchBlock
        SwitchBlock = JavaToken.CURLYLEFT * Many(SwitchBlockStatementGroup) * Many(SwitchLabel) * JavaToken.CURLYRIGHT
        SwitchBlockStatementGroup = SwitchLabels * BlockStatements
        SwitchLabels = Some(SwitchLabel)
        SwitchLabel = JavaToken.CASE * ConstantExpression * JavaToken.COLON or
                JavaToken.CASE * EnumConstantName * JavaToken.COLON or JavaToken.DEFAULT * JavaToken.COLON
        EnumConstantName = Identifier
        WhileStatement = JavaToken.WHILE * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * Statement
        WhileStatementNoShortIf = JavaToken.WHILE * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * StatementNoShortIf
        DoStatement = JavaToken.DO * Statement * JavaToken.WHILE * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * JavaToken.SEMICOLON
        ForStatement = BasicForStatement or EnhancedForStatement
        ForStatementNoShortIf = BasicForStatementNoShortIf or EnhancedForStatementNoShortIf
        BasicForStatement = JavaToken.FOR * JavaToken.PARENTHLEFT * Option(ForInit) * JavaToken.SEMICOLON * Option(Expression) * JavaToken.SEMICOLON * Option(ForUpdate) * JavaToken.PARENTHRIGHT * Statement
        BasicForStatementNoShortIf = JavaToken.FOR * JavaToken.PARENTHLEFT * Option(ForInit) * JavaToken.SEMICOLON * Option(Expression) * JavaToken.SEMICOLON * Option(ForUpdate) * JavaToken.PARENTHRIGHT * StatementNoShortIf
        ForInit = StatementExpressionList or LocalVariableDeclaration
        ForUpdate = StatementExpressionList
        StatementExpressionList = StatementExpression * Many(JavaToken.COMMA * StatementExpression)
        EnhancedForStatement = JavaToken.FOR * JavaToken.PARENTHLEFT * Many(VariableModifier) * UnannType * VariableDeclaratorId * JavaToken.COLON * Expression * JavaToken.PARENTHRIGHT * Statement
        EnhancedForStatementNoShortIf = JavaToken.FOR * JavaToken.PARENTHLEFT * Many(VariableModifier) * UnannType * VariableDeclaratorId * JavaToken.COLON * Expression * JavaToken.PARENTHRIGHT * StatementNoShortIf
        BreakStatement = JavaToken.BREAK * Option(Identifier) * JavaToken.SEMICOLON
        ContinueStatement = JavaToken.CONTINUE * Option(Identifier) * JavaToken.SEMICOLON
        ReturnStatement = JavaToken.RETURN * Option(Expression) * JavaToken.SEMICOLON
        ThrowStatement = JavaToken.THROW * Expression * JavaToken.SEMICOLON
        SynchronizedStatement = JavaToken.SYNCHRONIZED * JavaToken.PARENTHLEFT * Expression * JavaToken.PARENTHRIGHT * Block
        TryStatement = JavaToken.TRY * Block * Catches or JavaToken.TRY * Block * Option(Catches) * Finally or TryWithResourcesStatement
        Catches = Some(CatchClause)
        CatchClause = JavaToken.CATCH * JavaToken.PARENTHLEFT * CatchFormalParameter * JavaToken.PARENTHRIGHT * Block
        CatchFormalParameter = Many(VariableModifier) * CatchType * VariableDeclaratorId
        CatchType = UnannClassType * Many(JavaToken.ORBIT * ClassType)
        Finally = JavaToken.FINALLY * Block
        TryWithResourcesStatement = JavaToken.TRY * ResourceSpecification * Block * Option(Catches) * Option(Finally)
        ResourceSpecification = JavaToken.PARENTHLEFT * ResourceList * Option(JavaToken.SEMICOLON) * JavaToken.PARENTHRIGHT
        ResourceList = Resource * Many(JavaToken.COMMA * Resource)
        Resource = Many(VariableModifier) * UnannType * VariableDeclaratorId * JavaToken.ASSIGN * Expression

        /**
         * Productions from §15 (Expressions)
         */

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
            JavaToken.NEW * Option(TypeArguments) * classOrInterfaceTypeToInstantiate * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT * Option(ClassBody)
        classOrInterfaceTypeToInstantiate = Many(Annotation) * Identifier * Many(JavaToken.DOT * Many(Annotation) * Identifier) * Option(TypeArgumentsOrDiamond)
        TypeArgumentsOrDiamond = TypeArguments or JavaToken.LT * JavaToken.GT
        FieldAccess = Primary * JavaToken.DOT * Identifier or JavaToken.SUPER * JavaToken.DOT * Identifier or
                TypeName * JavaToken.DOT * JavaToken.SUPER * JavaToken.DOT * Identifier
        ArrayAccess = ExpressionName * JavaToken.BRACKETLEFT * Expression * JavaToken.BRACKETRIGHT or
                PrimaryNoNewArray * JavaToken.BRACKETLEFT * Expression * JavaToken.BRACKETRIGHT
        MethodInvocation = MethodName * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT or
                TypeName * JavaToken.DOT * Option(TypeArguments) * Identifier * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT or
                ExpressionName * JavaToken.DOT * Option(TypeArguments) * Identifier * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT or
                Primary * JavaToken.DOT * Option(TypeArguments) * Identifier * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT or
                JavaToken.SUPER * JavaToken.DOT * Option(TypeArguments) * Identifier * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT or
                TypeName * JavaToken.DOT * JavaToken.SUPER * JavaToken.DOT * Option(TypeArguments) * Identifier * JavaToken.PARENTHLEFT * Option(ArgumentList) * JavaToken.PARENTHRIGHT
        ArgumentList = Expression * Many(JavaToken.COMMA * Expression)
        MethodReference = ExpressionName * JavaToken.DOUBLECOLON * Option(TypeArguments) * Identifier or
                ReferenceType * JavaToken.DOUBLECOLON * Option(TypeArguments) * Identifier or
                Primary * JavaToken.DOUBLECOLON * Option(TypeArguments) * Identifier or
                JavaToken.SUPER * JavaToken.DOUBLECOLON * Option(TypeArguments) * Identifier or
                TypeName * JavaToken.DOT * JavaToken.SUPER * JavaToken.DOUBLECOLON * Option(TypeArguments) * Identifier or
                ClassType * JavaToken.DOUBLECOLON * Option(TypeArguments) * JavaToken.NEW or
                ArrayType * JavaToken.DOUBLECOLON * JavaToken.NEW
        ArrayCreationExpression = JavaToken.NEW * PrimitiveType * DimExprs * Option(Dims) or
                JavaToken.NEW * ClassOrInterfaceType * DimExprs * Option(Dims) or
                JavaToken.NEW * PrimitiveType * Dims * ArrayInitializer or
                JavaToken.NEW * ClassOrInterfaceType * Dims * ArrayInitializer
        DimExprs = Some(DimExpr)
        DimExpr = Many(Annotation) * JavaToken.BRACKETLEFT * Expression * JavaToken.BRACKETRIGHT
        Expression = LambdaExpression or AssignmentExpression
        LambdaExpression = LambdaParameters * JavaToken.ARROW * LambdaBody
        LambdaParameters = Identifier or JavaToken.PARENTHLEFT * Option(FormalParameterList) * JavaToken.PARENTHRIGHT or
                JavaToken.PARENTHLEFT * InferredFormalParameterList * JavaToken.PARENTHRIGHT
        InferredFormalParameterList = Identifier * Many(JavaToken.COMMA * Identifier)
        LambdaBody = Expression or Block
        AssignmentExpression = ConditionalExpression or Assignment
        Assignment = LeftHandSide * AssignmentOperator * Expression
        LeftHandSide = ExpressionName or FieldAccess or ArrayAccess
        AssignmentOperator = JavaToken.ASSIGN or JavaToken.STARASSIGN or JavaToken.SLASHASSIGN or JavaToken.PERCENTASSIGN or JavaToken.PLUSASSIGN or JavaToken.MINUSASSIGN or
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
        RelationalExpression = ShiftExpression or RelationalExpression * JavaToken.LT * ShiftExpression or
                RelationalExpression * JavaToken.GT * ShiftExpression or RelationalExpression * JavaToken.LESSEQ * ShiftExpression or
                RelationalExpression * JavaToken.GREATEQ * ShiftExpression or RelationalExpression * JavaToken.INSTANCEOF * ReferenceType
        ShiftExpression = AdditiveExpression or ShiftExpression * JavaToken.LT * JavaToken.LT * AdditiveExpression or
                ShiftExpression * JavaToken.GT * JavaToken.GT * AdditiveExpression or
                ShiftExpression * JavaToken.GT * JavaToken.GT * JavaToken.GT * AdditiveExpression
        AdditiveExpression = MultiplicativeExpression or AdditiveExpression * JavaToken.PLUS * MultiplicativeExpression or
                AdditiveExpression * JavaToken.MINUS * MultiplicativeExpression
        MultiplicativeExpression = UnaryExpression or MultiplicativeExpression * JavaToken.STAR * UnaryExpression or
                MultiplicativeExpression * JavaToken.SLASH * UnaryExpression or
                MultiplicativeExpression * JavaToken.PERCENT * UnaryExpression
        UnaryExpression = PreIncrementExpression or PreDecrementExpression or JavaToken.PLUS * UnaryExpression or
                JavaToken.MINUS * UnaryExpression or UnaryExpressionNotPlusMinus
        PreIncrementExpression = JavaToken.PLUSPLUS * UnaryExpression
        PreDecrementExpression = JavaToken.MINUSMINUS * UnaryExpression
        UnaryExpressionNotPlusMinus = PostfixExpression or JavaToken.TILDA * UnaryExpression or JavaToken.EXCLAMATIONMARK * UnaryExpression or
                CastExpression
        PostfixExpression = Primary or ExpressionName or PostIncrementExpression or PostDecrementExpression
        PostIncrementExpression = PostfixExpression * JavaToken.PLUSPLUS
        PostDecrementExpression = PostfixExpression * JavaToken.MINUSMINUS
        CastExpression = JavaToken.PARENTHLEFT * PrimitiveType * JavaToken.PARENTHRIGHT * UnaryExpression or
                JavaToken.PARENTHLEFT * ReferenceType * Many(AdditionalBound) * JavaToken.PARENTHRIGHT * UnaryExpressionNotPlusMinus or
                JavaToken.PARENTHLEFT * ReferenceType * Many(AdditionalBound) * JavaToken.PARENTHRIGHT * LambdaExpression
        ConstantExpression = Expression

        setStart(CompilationUnit)
    }
}