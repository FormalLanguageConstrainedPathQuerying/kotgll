package grammars.java8
import org.ucfs.grammar.combinator.Grammar
import org.ucfs.grammar.combinator.regexp.*

class GrammarDsl : Grammar() {
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
        Identifier = Token.ID

        Literal = Token.INTEGERLIT or Token.FLOATINGLIT or Token.BOOLEANLIT or
                Token.CHARLIT or Token.STRINGLIT or Token.NULLLIT

        /**
         * Productions from §4 (Types, Values, and Variables)
         */
        Type = PrimitiveType or ReferenceType
        PrimitiveType = Many(Annotation) * NumericType or Many(Annotation) * Token.BOOLEAN
        NumericType = IntegralType or FloatingPointType
        IntegralType = Token.BYTE or Token.SHORT or Token.INT or Token.LONG or Token.CHAR
        FloatingPointType = Token.FLOAT or Token.DOUBLE
        ReferenceType = ClassOrInterfaceType or TypeVariable or ArrayType
        ClassOrInterfaceType = ClassType or InterfaceType
        ClassType = Many(Annotation) * Identifier * Option(TypeArguments) or
                ClassOrInterfaceType * Token.DOT * Many(Annotation) * Identifier * Option(TypeArguments)
        InterfaceType = ClassType
        TypeVariable = Many(Annotation) * Identifier
        ArrayType = PrimitiveType * Dims or ClassOrInterfaceType * Dims or TypeVariable * Dims
        Dims = Some(Many(Annotation) * Token.BRACKETLEFT * Token.BRACKETRIGHT)
        TypeParameter  = Many(TypeParameterModifier) * Identifier * Option(TypeBound)
        TypeParameterModifier = Annotation
        TypeBound = Token.EXTENDS * TypeVariable or Token.EXTENDS * ClassOrInterfaceType * Many(AdditionalBound)
        AdditionalBound = Token.ANDBIT * InterfaceType
        TypeArguments = Token.LT * TypeArgumentList * Token.GT
        TypeArgumentList = TypeArgument * Many(Token.COMMA * TypeArgument)
        TypeArgument = ReferenceType or Wildcard
        Wildcard = Many(Annotation) * Token.QUESTIONMARK * Option(WildcardBounds)
        WildcardBounds = Token.EXTENDS * ReferenceType or Token.SUPER * ReferenceType

        /**
         * Productions from §6 (Names)
         */

        TypeName = Identifier or PackageOrTypeName * Token.DOT * Identifier
        PackageOrTypeName = Identifier or PackageOrTypeName * Token.DOT * Identifier
        ExpressionName = Identifier or AmbiguousName * Token.DOT * Identifier
        MethodName = Identifier
        PackageName = Identifier or PackageName * Token.DOT * Identifier
        AmbiguousName = Identifier or AmbiguousName * Token.DOT * Identifier

        /**
         * Productions from §7 (Packages)
         */

        CompilationUnit = Option(PackageDeclaration) * Many(ImportDeclaration) * Many(TypeDeclaration)
        PackageDeclaration = Many(PackageModifier) * Token.PACKAGE * Identifier * Many(Token.DOT * Identifier) * Token.SEMICOLON
        PackageModifier = Annotation
        ImportDeclaration = SingleTypeImportDeclaration or TypeImportOnDemandDeclaration or
                SingleStaticImportDeclaration or StaticImportOnDemandDeclaration
        SingleTypeImportDeclaration = Token.IMPORT * TypeName * Token.SEMICOLON
        TypeImportOnDemandDeclaration = Token.IMPORT * PackageOrTypeName * Token.DOT * Token.STAR * Token.SEMICOLON
        SingleStaticImportDeclaration = Token.IMPORT * Token.STATIC * TypeName * Token.DOT * Identifier * Token.SEMICOLON
        StaticImportOnDemandDeclaration = Token.IMPORT * Token.STATIC * TypeName * Token.DOT * Token.STAR * Token.SEMICOLON
        TypeDeclaration = ClassDeclaration or InterfaceDeclaration or Token.SEMICOLON

        /**
         * Productions from §8 (Classes)
         */

        ClassDeclaration = NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration = Many(ClassModifier) * Token.CLASS * Identifier *
                Option(TypeParameters) * Option(Superclass) * Option(Superinterfaces) * ClassBody
        ClassModifier = Annotation or Token.PUBLIC or Token.PROTECTED or Token.PRIVATE or
                Token.ABSTRACT or Token.STATIC or Token.FINAL or Token.STRICTFP
        TypeParameters = Token.LT * TypeParameterList * Token.GT
        TypeParameterList = TypeParameter  * Many(Token.COMMA * TypeParameter)
        Superclass = Token.EXTENDS * ClassType
        Superinterfaces = Token.IMPLEMENTS * InterfaceTypeList
        InterfaceTypeList = InterfaceType  * Many(Token.COMMA * InterfaceType)
        ClassBody = Token.CURLYLEFT * Many(ClassBodyDeclaration) * Token.CURLYRIGHT
        ClassBodyDeclaration = ClassMemberDeclaration or InstanceInitializer or StaticInitializer or ConstructorDeclaration
        ClassMemberDeclaration = FieldDeclaration or MethodDeclaration or ClassDeclaration or InterfaceDeclaration or Token.SEMICOLON
        FieldDeclaration = Many(FieldModifier) * UnannType * VariableDeclaratorList * Token.SEMICOLON
        FieldModifier = Annotation or Token.PUBLIC or Token.PROTECTED or Token.PRIVATE or Token.STATIC or
                Token.FINAL or Token.TRANSIENT or Token.VOLATILE
        VariableDeclaratorList = VariableDeclarator * Many(Token.COMMA * VariableDeclarator)
        VariableDeclarator = VariableDeclaratorId * Option(Token.ASSIGN * VariableInitializer)
        VariableDeclaratorId = Identifier * Option(Dims)
        VariableInitializer = Expression or ArrayInitializer
        UnannType = UnannPrimitiveType or UnannReferenceType
        UnannPrimitiveType = NumericType or Token.BOOLEAN
        UnannReferenceType = UnannClassOrInterfaceType or UnannTypeVariable or UnannArrayType
        UnannClassOrInterfaceType = UnannClassType or UnannInterfaceType
        UnannClassType = Identifier * Option(TypeArguments) or
                UnannClassOrInterfaceType * Token.DOT * Many(Annotation) * Identifier * Option(TypeArguments)
        UnannInterfaceType = UnannClassType
        UnannTypeVariable = Identifier
        UnannArrayType = UnannPrimitiveType * Dims or UnannClassOrInterfaceType * Dims or UnannTypeVariable * Dims
        MethodDeclaration = Many(MethodModifier) * MethodHeader * MethodBody
        MethodModifier = Annotation or Token.PUBLIC or Token.PROTECTED or Token.PRIVATE or Token.ABSTRACT or
                Token.STATIC or Token.FINAL or Token.SYNCHRONIZED or Token.NATIVE or Token.STRICTFP
        MethodHeader = Result * MethodDeclarator * Option(Throws) or
                TypeParameters * Many(Annotation) * Result * MethodDeclarator * Option(Throws)
        Result = UnannType or Token.VOID
        MethodDeclarator = Identifier * Token.PARENTHLEFT * Option(FormalParameterList) * Token.PARENTHRIGHT * Option(Dims)
        FormalParameterList = ReceiverParameter or FormalParameters * Token.COMMA * LastFormalParameter or
                LastFormalParameter
        FormalParameters = FormalParameter * Many(Token.COMMA * FormalParameter) or
                ReceiverParameter * Many(Token.COMMA * FormalParameter)
        FormalParameter = Many(VariableModifier) * UnannType * VariableDeclaratorId
        VariableModifier = Annotation or Token.FINAL
        LastFormalParameter = Many(VariableModifier) * UnannType * Many(Annotation) * Token.ELLIPSIS * VariableDeclaratorId or FormalParameter
        ReceiverParameter = Many(Annotation) * UnannType * Option(Identifier * Token.DOT) * Token.THIS
        Throws = Token.THROWS * ExceptionTypeList
        ExceptionTypeList = ExceptionType * Many(Token.COMMA * ExceptionType)
        ExceptionType = ClassType or TypeVariable
        MethodBody = Block or Token.SEMICOLON
        InstanceInitializer = Block
        StaticInitializer = Token.STATIC * Block
        ConstructorDeclaration = Many(ConstructorModifier) * ConstructorDeclarator * Option(Throws) * ConstructorBody
        ConstructorModifier = Annotation or Token.PUBLIC or Token.PROTECTED or Token.PRIVATE
        ConstructorDeclarator = Option(TypeParameters) * SimpleTypeName * Token.PARENTHLEFT * Option(FormalParameterList) * Token.PARENTHRIGHT
        SimpleTypeName = Identifier
        ConstructorBody = Token.CURLYLEFT * Option(ExplicitConstructorInvocation) * Option(BlockStatements) * Token.CURLYRIGHT
        ExplicitConstructorInvocation = Option(TypeArguments) * Token.THIS * Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT * Token.SEMICOLON or
                Option(TypeArguments) * Token.SUPER * Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT * Token.SEMICOLON or
                ExpressionName * Token.DOT * Option(TypeArguments) * Token.SUPER * Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT * Token.SEMICOLON or
                Primary * Token.DOT * Option(TypeArguments) * Token.SUPER * Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT * Token.SEMICOLON
        EnumDeclaration = Many(ClassModifier) * Token.ENUM * Identifier * Option(Superinterfaces) * EnumBody
        EnumBody = Token.CURLYLEFT * Option(EnumConstantList) * Option(Token.COMMA) * Option(EnumBodyDeclarations) * Token.CURLYRIGHT
        EnumConstantList = EnumConstant * Many(Token.COMMA * EnumConstant)
        EnumConstant = Many(EnumConstantModifier) * Identifier * Option(Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT * Option(ClassBody))
        EnumConstantModifier = Annotation
        EnumBodyDeclarations = Token.SEMICOLON * Many(ClassBodyDeclaration)

        /**
         * Productions from §9 (Interfaces)
         */

        InterfaceDeclaration = NormalInterfaceDeclaration or AnnotationTypeDeclaration
        NormalInterfaceDeclaration =
            Many(InterfaceModifier) * Token.INTERFACE * Identifier * Option(TypeParameters) * Option(ExtendsInterfaces) * InterfaceBody
        InterfaceModifier = Annotation or Token.PUBLIC or Token.PROTECTED or Token.PRIVATE or
                Token.ABSTRACT or Token.STATIC or Token.STRICTFP
        ExtendsInterfaces = Token.EXTENDS * InterfaceTypeList
        InterfaceBody = Token.CURLYLEFT * Many(InterfaceMemberDeclaration) * Token.CURLYRIGHT
        InterfaceMemberDeclaration = ConstantDeclaration or InterfaceMethodDeclaration or ClassDeclaration or InterfaceDeclaration or Token.SEMICOLON
        ConstantDeclaration = Many(ConstantModifier) * UnannType * VariableDeclaratorList * Token.SEMICOLON
        ConstantModifier = Annotation or Token.PUBLIC or Token.STATIC or Token.FINAL
        InterfaceMethodDeclaration = Many(InterfaceMethodModifier) * MethodHeader * MethodBody
        InterfaceMethodModifier = Annotation or Token.PUBLIC or Token.ABSTRACT or Token.DEFAULT or Token.STATIC or Token.STRICTFP
        AnnotationTypeDeclaration = Many(InterfaceModifier) * Token.AT * Token.INTERFACE * Identifier * AnnotationTypeBody
        AnnotationTypeBody = Token.CURLYLEFT * Many(AnnotationTypeMemberDeclaration) * Token.CURLYRIGHT
        AnnotationTypeMemberDeclaration = AnnotationTypeElementDeclaration or ConstantDeclaration or ClassDeclaration or InterfaceDeclaration or Token.SEMICOLON
        AnnotationTypeElementDeclaration =
            Many(AnnotationTypeElementModifier) * UnannType * Identifier * Token.PARENTHLEFT * Token.PARENTHRIGHT * Option(Dims) * Option(DefaultValue) * Token.SEMICOLON
        AnnotationTypeElementModifier = Annotation or Token.PUBLIC or Token.ABSTRACT
        DefaultValue = Token.DEFAULT * ElementValue
        Annotation = NormalAnnotation or MarkerAnnotation or SingleElementAnnotation
        NormalAnnotation = Token.AT * TypeName * Token.PARENTHLEFT * Option(ElementValuePairList) * Token.PARENTHRIGHT
        ElementValuePairList = ElementValuePair * Many(Token.COMMA * ElementValuePair)
        ElementValuePair = Identifier * Token.ASSIGN * ElementValue
        ElementValue = ConditionalExpression or ElementValueArrayInitializer or Annotation
        ElementValueArrayInitializer = Token.CURLYLEFT * Option(ElementValueList) * Option(Token.COMMA) * Token.CURLYRIGHT
        ElementValueList = ElementValue * Many(Token.COMMA * ElementValue)
        MarkerAnnotation = Token.AT * TypeName
        SingleElementAnnotation = Token.AT * TypeName * Token.PARENTHLEFT * ElementValue * Token.PARENTHRIGHT

        /**
         * Productions from §10 (Arrays)
         */

        ArrayInitializer = Token.CURLYLEFT * Option(VariableInitializerList) * Option(Token.COMMA) * Token.CURLYRIGHT
        VariableInitializerList = VariableInitializer * Many(Token.COMMA * VariableInitializer)

        /**
         * Productions from §14 (Blocks and Statements)
         */

        Block = Token.CURLYLEFT * Option(BlockStatements) * Token.CURLYRIGHT
        BlockStatements = BlockStatement * Many(BlockStatement)
        BlockStatement = LocalVariableDeclarationStatement or ClassDeclaration or Statement
        LocalVariableDeclarationStatement = LocalVariableDeclaration * Token.SEMICOLON
        LocalVariableDeclaration = Many(VariableModifier) * UnannType * VariableDeclaratorList
        Statement = StatementWithoutTrailingSubstatement or LabeledStatement or IfThenStatement or IfThenElseStatement or
                WhileStatement or ForStatement
        StatementNoShortIf = StatementWithoutTrailingSubstatement or LabeledStatementNoShortIf or IfThenElseStatementNoShortIf or
                WhileStatementNoShortIf or ForStatementNoShortIf
        StatementWithoutTrailingSubstatement = Block or EmptyStatement or ExpressionStatement or AssertStatement or
                SwitchStatement or DoStatement or BreakStatement or ContinueStatement or ReturnStatement or SynchronizedStatement or
                ThrowStatement or TryStatement
        EmptyStatement = Token.SEMICOLON
        LabeledStatement = Identifier * Token.COLON * Statement
        LabeledStatementNoShortIf = Identifier * Token.COLON * StatementNoShortIf
        ExpressionStatement = StatementExpression * Token.SEMICOLON
        StatementExpression = Assignment or PreIncrementExpression or PreDecrementExpression or PostIncrementExpression or
                PostDecrementExpression or MethodInvocation or ClassInstanceCreationExpression
        IfThenStatement = Token.IF * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * Statement
        IfThenElseStatement = Token.IF * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * StatementNoShortIf * Token.ELSE * Statement
        IfThenElseStatementNoShortIf =
            Token.IF * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * StatementNoShortIf * Token.ELSE * StatementNoShortIf
        AssertStatement = Token.ASSERT * Expression * Token.SEMICOLON or
                Token.ASSERT * Expression * Token.COLON * Expression * Token.SEMICOLON
        SwitchStatement = Token.SWITCH * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * SwitchBlock
        SwitchBlock = Token.CURLYLEFT * Many(SwitchBlockStatementGroup) * Many(SwitchLabel) * Token.CURLYRIGHT
        SwitchBlockStatementGroup = SwitchLabels * BlockStatements
        SwitchLabels = Some(SwitchLabel)
        SwitchLabel = Token.CASE * ConstantExpression * Token.COLON or
                Token.CASE * EnumConstantName * Token.COLON or Token.DEFAULT * Token.COLON
        EnumConstantName = Identifier
        WhileStatement = Token.WHILE * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * Statement
        WhileStatementNoShortIf = Token.WHILE * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * StatementNoShortIf
        DoStatement = Token.DO * Statement * Token.WHILE * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * Token.SEMICOLON
        ForStatement = BasicForStatement or EnhancedForStatement
        ForStatementNoShortIf = BasicForStatementNoShortIf or EnhancedForStatementNoShortIf
        BasicForStatement = Token.FOR * Token.PARENTHLEFT * Option(ForInit) * Token.SEMICOLON * Option(Expression) * Token.SEMICOLON * Option(ForUpdate) * Token.PARENTHRIGHT * Statement
        BasicForStatementNoShortIf = Token.FOR * Token.PARENTHLEFT * Option(ForInit) * Token.SEMICOLON * Option(Expression) * Token.SEMICOLON * Option(ForUpdate) * Token.PARENTHRIGHT * StatementNoShortIf
        ForInit = StatementExpressionList or LocalVariableDeclaration
        ForUpdate = StatementExpressionList
        StatementExpressionList = StatementExpression * Many(Token.COMMA * StatementExpression)
        EnhancedForStatement = Token.FOR * Token.PARENTHLEFT * Many(VariableModifier) * UnannType * VariableDeclaratorId * Token.COLON * Expression * Token.PARENTHRIGHT * Statement
        EnhancedForStatementNoShortIf = Token.FOR * Token.PARENTHLEFT * Many(VariableModifier) * UnannType * VariableDeclaratorId * Token.COLON * Expression * Token.PARENTHRIGHT * StatementNoShortIf
        BreakStatement = Token.BREAK * Option(Identifier) * Token.SEMICOLON
        ContinueStatement = Token.CONTINUE * Option(Identifier) * Token.SEMICOLON
        ReturnStatement = Token.RETURN * Option(Expression) * Token.SEMICOLON
        ThrowStatement = Token.THROW * Expression * Token.SEMICOLON
        SynchronizedStatement = Token.SYNCHRONIZED * Token.PARENTHLEFT * Expression * Token.PARENTHRIGHT * Block
        TryStatement = Token.TRY * Block * Catches or Token.TRY * Block * Option(Catches) * Finally or TryWithResourcesStatement
        Catches = Some(CatchClause)
        CatchClause = Token.CATCH * Token.PARENTHLEFT * CatchFormalParameter * Token.PARENTHRIGHT * Block
        CatchFormalParameter = Many(VariableModifier) * CatchType * VariableDeclaratorId
        CatchType = UnannClassType * Many(Token.ORBIT * ClassType)
        Finally = Token.FINALLY * Block
        TryWithResourcesStatement = Token.TRY * ResourceSpecification * Block * Option(Catches) * Option(Finally)
        ResourceSpecification = Token.PARENTHLEFT * ResourceList * Option(Token.SEMICOLON) * Token.PARENTHRIGHT
        ResourceList = Resource * Many(Token.COMMA * Resource)
        Resource = Many(VariableModifier) * UnannType * VariableDeclaratorId * Token.ASSIGN * Expression

        /**
         * Productions from §15 (Expressions)
         */

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
            Token.NEW * Option(TypeArguments) * classOrInterfaceTypeToInstantiate * Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT * Option(ClassBody)
        classOrInterfaceTypeToInstantiate = Many(Annotation) * Identifier * Many(Token.DOT * Many(Annotation) * Identifier) * Option(TypeArgumentsOrDiamond)
        TypeArgumentsOrDiamond = TypeArguments or Token.LT * Token.GT
        FieldAccess = Primary * Token.DOT * Identifier or Token.SUPER * Token.DOT * Identifier or
                TypeName * Token.DOT * Token.SUPER * Token.DOT * Identifier
        ArrayAccess = ExpressionName * Token.BRACKETLEFT * Expression * Token.BRACKETRIGHT or
                PrimaryNoNewArray * Token.BRACKETLEFT * Expression * Token.BRACKETRIGHT
        MethodInvocation = MethodName * Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT or
                TypeName * Token.DOT * Option(TypeArguments) * Identifier * Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT or
                ExpressionName * Token.DOT * Option(TypeArguments) * Identifier * Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT or
                Primary * Token.DOT * Option(TypeArguments) * Identifier * Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT or
                Token.SUPER * Token.DOT * Option(TypeArguments) * Identifier * Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT or
                TypeName * Token.DOT * Token.SUPER * Token.DOT * Option(TypeArguments) * Identifier * Token.PARENTHLEFT * Option(ArgumentList) * Token.PARENTHRIGHT
        ArgumentList = Expression * Many(Token.COMMA * Expression)
        MethodReference = ExpressionName * Token.DOUBLECOLON * Option(TypeArguments) * Identifier or
                ReferenceType * Token.DOUBLECOLON * Option(TypeArguments) * Identifier or
                Primary * Token.DOUBLECOLON * Option(TypeArguments) * Identifier or
                Token.SUPER * Token.DOUBLECOLON * Option(TypeArguments) * Identifier or
                TypeName * Token.DOT * Token.SUPER * Token.DOUBLECOLON * Option(TypeArguments) * Identifier or
                ClassType * Token.DOUBLECOLON * Option(TypeArguments) * Token.NEW or
                ArrayType * Token.DOUBLECOLON * Token.NEW
        ArrayCreationExpression = Token.NEW * PrimitiveType * DimExprs * Option(Dims) or
                Token.NEW * ClassOrInterfaceType * DimExprs * Option(Dims) or
                Token.NEW * PrimitiveType * Dims * ArrayInitializer or
                Token.NEW * ClassOrInterfaceType * Dims * ArrayInitializer
        DimExprs = Some(DimExpr)
        DimExpr = Many(Annotation) * Token.BRACKETLEFT * Expression * Token.BRACKETRIGHT
        Expression = LambdaExpression or AssignmentExpression
        LambdaExpression = LambdaParameters * Token.ARROW * LambdaBody
        LambdaParameters = Identifier or Token.PARENTHLEFT * Option(FormalParameterList) * Token.PARENTHRIGHT or
                Token.PARENTHLEFT * InferredFormalParameterList * Token.PARENTHRIGHT
        InferredFormalParameterList = Identifier * Many(Token.COMMA * Identifier)
        LambdaBody = Expression or Block
        AssignmentExpression = ConditionalExpression or Assignment
        Assignment = LeftHandSide * AssignmentOperator * Expression
        LeftHandSide = ExpressionName or FieldAccess or ArrayAccess
        AssignmentOperator = Token.ASSIGN or Token.STARASSIGN or Token.SLASHASSIGN or Token.PERCENTASSIGN or Token.PLUSASSIGN or Token.MINUSASSIGN or
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
        RelationalExpression = ShiftExpression or RelationalExpression * Token.LT * ShiftExpression or
                RelationalExpression * Token.GT * ShiftExpression or RelationalExpression * Token.LESSEQ * ShiftExpression or
                RelationalExpression * Token.GREATEQ * ShiftExpression or RelationalExpression * Token.INSTANCEOF * ReferenceType
        ShiftExpression = AdditiveExpression or ShiftExpression * Token.LT * Token.LT * AdditiveExpression or
                ShiftExpression * Token.GT * Token.GT * AdditiveExpression or
                ShiftExpression * Token.GT * Token.GT * Token.GT * AdditiveExpression
        AdditiveExpression = MultiplicativeExpression or AdditiveExpression * Token.PLUS * MultiplicativeExpression or
                AdditiveExpression * Token.MINUS * MultiplicativeExpression
        MultiplicativeExpression = UnaryExpression or MultiplicativeExpression * Token.STAR * UnaryExpression or
                MultiplicativeExpression * Token.SLASH * UnaryExpression or
                MultiplicativeExpression * Token.PERCENT * UnaryExpression
        UnaryExpression = PreIncrementExpression or PreDecrementExpression or Token.PLUS * UnaryExpression or
                Token.MINUS * UnaryExpression or UnaryExpressionNotPlusMinus
        PreIncrementExpression = Token.PLUSPLUS * UnaryExpression
        PreDecrementExpression = Token.MINUSMINUS * UnaryExpression
        UnaryExpressionNotPlusMinus = PostfixExpression or Token.TILDA * UnaryExpression or Token.EXCLAMATIONMARK * UnaryExpression or
                CastExpression
        PostfixExpression = Primary or ExpressionName or PostIncrementExpression or PostDecrementExpression
        PostIncrementExpression = PostfixExpression * Token.PLUSPLUS
        PostDecrementExpression = PostfixExpression * Token.MINUSMINUS
        CastExpression = Token.PARENTHLEFT * PrimitiveType * Token.PARENTHRIGHT * UnaryExpression or
                Token.PARENTHLEFT * ReferenceType * Many(AdditionalBound) * Token.PARENTHRIGHT * UnaryExpressionNotPlusMinus or
                Token.PARENTHLEFT * ReferenceType * Many(AdditionalBound) * Token.PARENTHRIGHT * LambdaExpression
        ConstantExpression = Expression

        setStart(CompilationUnit)
    }
}