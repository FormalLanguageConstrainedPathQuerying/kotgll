package org.srcgll.lexer

import org.srcgll.grammar.combinator.Grammar
import org.srcgll.grammar.combinator.regexp.*

class JavaGrammarScannerless : Grammar() {
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
    var JavaLetter by Nt()
    var JavaLetterOrDigit by Nt()
    var BOOLEAN by Nt()
    var BYTE by Nt()
    var SHORT by Nt()
    var INT by Nt()
    var LONG by Nt()
    var CHAR by Nt()
    var FLOAT by Nt()
    var DOUBLE by Nt()
    var EXTENDS by Nt()
    var DIAMOND by Nt()
    var DOUBLECOLON by Nt()
    var ELLIPSIS by Nt()
    var SUPER by Nt()
    var PACKAGE by Nt()
    var IMPORT by Nt()
    var STATIC by Nt()
    var PLUSPLUS by Nt()
    var MINUSMINUS by Nt()
    var CLASS by Nt()
    var PUBLIC by Nt()
    var PROTECTED by Nt()
    var PRIVATE by Nt()
    var ABSTRACT by Nt()
    var FINAL by Nt()
    var STRICTFP by Nt()
    var IMPLEMENTS by Nt()
    var TRANSIENT by Nt()
    var VOLATILE by Nt()
    var STARASSIGN by Nt()
    var SLASHASSIGN by Nt()
    var PLUSASSIGN by Nt()
    var MINUSASSIGN by Nt()
    var PERCENTASSIGN by Nt()
    var XORASSIGN by Nt()
    var SHIFTLEFTASSIGN by Nt()
    var SHIFTRIGHTASSIGN by Nt()
    var USRIGHTSHIFTASSIGN by Nt()
    var ANDASSIGN by Nt()
    var ORASSIGN by Nt()
    var OR by Nt()
    var AND by Nt()
    var EQ by Nt()
    var NOTEQ by Nt()
    var LESSEQ by Nt()
    var GREATEQ by Nt()
    var INSTANCEOF by Nt()
    var RIGHTSHIFT by Nt()
    var LEFTSHIFT by Nt()
    var USRIGHTSHIFT by Nt()
    var SYNCHRONIZED by Nt()
    var NATIVE by Nt()
    var VOID by Nt()
    var THIS by Nt()
    var THROWS by Nt()
    var ENUM by Nt()
    var INTERFACE by Nt()
    var DEFAULT by Nt()
    var ASSERT by Nt()
    var SWITCH by Nt()
    var CASE by Nt()
    var WHILE by Nt()
    var FOR by Nt()
    var IF by Nt()
    var ELSE by Nt()
    var DO by Nt()
    var BREAK by Nt()
    var CONTINUE by Nt()
    var RETURN by Nt()
    var THROW by Nt()
    var TRY by Nt()
    var CATCH by Nt()
    var FINALLY by Nt()
    var NEW by Nt()
    var ARROW by Nt()
    var IntegerLiteral by Nt()
    var DecimalIntegerLiteral by Nt()
    var HexIntegerLiteral by Nt()
    var OctalIntegerLiteral by Nt()
    var BinaryIntegerLiteral by Nt()
    var DigitRange by Nt()
    var NonZeroDigitRange by Nt()
    var DecimalNumeral by Nt()
    var Digits by Nt()
    var HexNumeral by Nt()
    var HexRange by Nt()
    var HexDigits by Nt()
    var OctalNumeral by Nt()
    var OctalDigits by Nt()
    var BinaryNumeral by Nt()
    var BinaryDigits by Nt()
    var FloatingPointLiteral by Nt()
    var DecimalFloatingPointLiteral by Nt()
    var ExponentPart by Nt()
    var HexadecimalFloatingPointLiteral by Nt()
    var HexSignificand by Nt()
    var BinaryExponent by Nt()
    var InputCharacter by Nt()
    var EscapeSequence by Nt()
    var LineTerminator by Nt()
    var CharacterLiteral by Nt()
    var StringLiteral by Nt()
    var StringCharacter by Nt()
    var WhiteSpace by Nt()
    var Comment by Nt()
    var TraditionalComment by Nt()
    var BooleanLiteral by Nt()
    var NullLiteral by Nt()
    var CommentTail by Nt()
    var CommentTailStar by Nt()
    var NotStar by Nt()
    var NotStarNotSlash by Nt()
    init {
        JavaLetter = "a" or "b" or "c" or "d" or "e" or "f" or "g" or "h" or "i" or "j" or "k" or "l" or "m" or "n" or
                "o" or "p" or "q" or "r" or "s" or "t" or "u" or "v" or "w" or "x" or "y" or "z" or
                "A" or "B" or "C" or "D" or "E" or "F" or "G" or "H" or "I" or "J" or "K" or "L" or "M" or "N" or
                "O" or "P" or "Q" or "R" or "S" or "T" or "U" or "V" or "W" or "X" or "Y" or "Z" or "$" or "_"

        JavaLetterOrDigit = JavaLetter or DigitRange

        Identifier = JavaLetter * Many(JavaLetterOrDigit)

        IntegerLiteral = DecimalIntegerLiteral or HexIntegerLiteral or OctalIntegerLiteral or BinaryIntegerLiteral

        DecimalIntegerLiteral = DecimalNumeral * Option("l" or "L")
        HexIntegerLiteral = HexNumeral * Option("l" or "L")
        OctalIntegerLiteral = OctalNumeral * Option("l" or "L")
        BinaryIntegerLiteral = BinaryNumeral * Option("l" or "L")

        DigitRange = "0" or "1" or "2" or "3" or "4" or "5" or "6" or "7" or "8" or "9"
        NonZeroDigitRange = "1" or "2" or "3" or "4" or "5" or "6" or "7" or "8" or "9"

        DecimalNumeral = "0" or NonZeroDigitRange * Option(Digits) or NonZeroDigitRange * Some("_") * Digits
        Digits = DigitRange *  Option(Option(Some(DigitRange or "_")) * DigitRange)

        HexNumeral = ("0x" or "0X") * HexDigits
        HexRange = DigitRange or "a" or "b" or "c" or "d" or "e" or "f" or "A" or "B" or "C" or "D" or "E" or "F"
        HexDigits = HexRange * Option(Option(Some(HexRange or "_")) * HexRange)

        OctalNumeral = "0" * Option(Some("_")) * OctalDigits
        OctalDigits = ("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7") *
                Option(Option(Some("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7" or "_")) *
                        ("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7"))

        BinaryNumeral = "0" * ("b" or "B") * BinaryDigits
        BinaryDigits = ("0" or "1") * Option(Option(Some("0" or "1" or "_")) * ("0" or "1"))

        FloatingPointLiteral = DecimalFloatingPointLiteral or HexadecimalFloatingPointLiteral
        DecimalFloatingPointLiteral = DigitRange * "." * Option(DigitRange) * Option(ExponentPart) * Option("f" or "F" or "d" or "D") or
                "." * DigitRange * Option(ExponentPart) * Option("f" or "F" or "d" or "D") or
                DigitRange * ExponentPart * ("f" or "F" or "d" or "D") or
                DigitRange * Option(ExponentPart) * ("f" or "F" or "d" or "D")

        ExponentPart = ("e" or "E") * Option("-" or "+") * Digits
        HexadecimalFloatingPointLiteral = HexSignificand * BinaryExponent * Option("f" or "F" or "d" or "D")
        HexSignificand = HexNumeral * Option(".") or "0" * ("x" or "X") * Option(HexDigits) * "." * HexDigits
        BinaryExponent = ("p" or "P") * Option("+" or "-") * Digits

        BooleanLiteral = "f" * "a" * "l" * "s" * "e" or "t" * "r" * "u" * "e"
        NullLiteral = "n" * "u" * "l" * "l"

        InputCharacter = "\\" * "u" * HexRange * HexRange * HexRange * HexRange or JavaLetterOrDigit or
                " " or "!" or "#" or "%" or "&" or "(" or ")" or "*" or "+" or "," or "-" or "." or "/" or ":" or
                ";" or "<" or "=" or ">" or "?" or "@" or "[" or "]" or "^" or "`" or "{" or "|" or "}" or "~"

        EscapeSequence = "\\" * ("b" or "t" or "n" or "f" or "r" or "\"" or "\'" or "\\") or
                "\\" * (("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7") or
                ("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7") * ("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7") or
                ("0" or "1" or "2" or "3") * ("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7") * ("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7"))
        LineTerminator = "\r" or "\n" or "\r\n"

        CharacterLiteral = "\'" * (JavaLetterOrDigit or " " or "!" or "#" or "%" or "&" or "(" or ")" or
                "*" or "+" or "," or "-" or "." or "/" or ":" or ";" or "<" or "=" or ">" or "?" or "@" or "[" or
                "]" or "^" or "`" or "{" or "|" or "}" or "~") * "\'" or "\'" * EscapeSequence * "\'"

        StringLiteral = "\"" * Many(StringCharacter) * "\""
        StringCharacter = InputCharacter or EscapeSequence
        WhiteSpace = LineTerminator or " " or "\t"

        Comment = TraditionalComment

        TraditionalComment = "/" * "*" * CommentTail

        CommentTail = "*" * CommentTailStar or NotStar * CommentTail
        CommentTailStar = "/" or "*" * CommentTailStar or NotStarNotSlash * CommentTail

        // InputCharacter but not *
        NotStar = "\\" * "u" * HexRange * HexRange * HexRange * HexRange or JavaLetterOrDigit or
                " " or "!" or "#" or "%" or "&" or "(" or ")" or "+" or "," or "-" or "." or "/" or ":" or ";" or
                "<" or "=" or ">" or "?" or "@" or "[" or "]" or "^" or "`" or "{" or "|" or "}" or "~" or LineTerminator

        // InputCharacter but not * or /
        NotStarNotSlash = "\\" * "u" * HexRange * HexRange * HexRange * HexRange or JavaLetterOrDigit or
                " " or "!" or "#" or "%" or "&" or "(" or ")" or "+" or "," or "-" or "." or ":" or ";" or "<" or
                "=" or ">" or "?" or "@" or "[" or "]" or "^" or "`" or "{" or "|" or "}" or "~" or LineTerminator

        BOOLEAN = "b" * "o" * "o" * "l" * "e" * "a" * "n"
        BYTE = "b" * "y" * "t" * "e"
        SHORT = "s" * "h" * "o" * "r" * "t"
        INT = "i" * "n" * "t"
        LONG = "l" * "o" * "n" * "g"
        CHAR = "c" * "h" * "a" * "r"
        FLOAT = "f" * "l" * "o" * "a" * "t"
        DOUBLE = "d" * "o" * "u" * "b" * "l" * "e"
        EXTENDS = "e" * "x" * "t" * "e" * "n" * "d" * "s"
        DIAMOND = Term("<>")
        ELLIPSIS = Term("...")
        SUPER = "s" * "u" * "p" * "e" * "r"
        PACKAGE = "p" * "a" * "c" * "k" * "a" * "g" * "e"
        IMPORT = "i" * "m" * "p" * "o" * "r" * "t"
        STATIC = "s" * "t" * "a" * "t" * "i" * "c"
        PLUSPLUS = "+" * "+"
        MINUSMINUS = "-" * "-"
        CLASS = "c" * "l" * "a" * "s" * "s"
        PUBLIC = "p" * "u" * "b" * "l" * "i" * "c"
        PROTECTED = "p" * "r" * "o" * "t" * "e" * "c" * "t" * "e" * "d"
        PRIVATE = "p" * "r" * "i" * "v" * "a" * "t" * "e"
        ABSTRACT = "a" * "b" * "s" * "t" * "r" * "a" * "c" * "t"
        FINAL = "f" * "i" * "n" * "a" * "l"
        STRICTFP = "s" * "t" * "r" * "i" * "c" * "t" * "f" * "p"
        IMPLEMENTS = "i" * "m" * "p" * "l" * "e" * "m" * "e" * "n" * "t" * "s"
        TRANSIENT = "t" * "r" * "a" * "n" * "s" * "i" * "e" * "n" * "t"
        VOLATILE = "v" * "o" * "l" * "a" * "t" * "i" * "l" * "e"
        STARASSIGN = "*" * "="
        SLASHASSIGN = "/" * "="
        PLUSASSIGN = "+" * "="
        MINUSASSIGN = "-" * "="
        PERCENTASSIGN = "%" * "="
        XORASSIGN = "^" * "="
        SHIFTLEFTASSIGN = "<" * "<" * "="
        SHIFTRIGHTASSIGN = ">" * ">" * "="
        USRIGHTSHIFTASSIGN = ">" * ">" * ">" * "="
        ANDASSIGN = "&" * "="
        ORASSIGN = "|" * "="
        OR = "|" * "|"
        AND = "&" * "&"
        EQ = "=" * "="
        NOTEQ = "!" * "="
        LESSEQ = "<" * "="
        GREATEQ = ">" * "="
        INSTANCEOF = "i" * "n" * "s" * "t" * "a" * "n" * "c" * "e" * "o" * "f"
        RIGHTSHIFT = ">" * ">"
        LEFTSHIFT = "<" * "<"
        USRIGHTSHIFT = ">" * ">" * ">"
        SYNCHRONIZED = "s" * "y" * "n" * "c" * "h" * "r" * "o" * "n" * "i" * "z" * "e" * "d"
        NATIVE = "n" * "a" * "t" * "i" * "v" * "e"
        VOID = "v" * "o" * "i" * "d"
        THIS = "t" * "h" * "i" * "s"
        THROWS = "t" * "h" * "r" * "o" * "w" * "s"
        ENUM = "e" * "n" * "u" * "m"
        INTERFACE = "i" * "n" * "t" * "e" * "r" * "f" * "a" * "c" * "e"
        DEFAULT = "d" * "e" * "f" * "a" * "u" * "l" * "t"
        ASSERT = "a" * "s" * "s" * "e" * "r" * "t"
        SWITCH = "s" * "w" * "i" * "t" * "c" * "h"
        CASE = "c" * "a" * "s" * "e"
        DOUBLECOLON = ":" * ":"
        WHILE = "w" * "h" * "i" * "l" * "e"
        FOR = "f" * "o" * "r"
        IF = "i" * "f"
        ELSE = "e" * "l" * "s" * "e"
        DO = "d" * "o"
        BREAK = "b" * "r" * "e" * "a" * "k"
        CONTINUE = "c" * "o" * "n" * "t" * "i" * "n" * "u" * "e"
        RETURN = "r" * "e" * "t" * "u" * "r" * "n"
        THROW = "t" * "h" * "r" * "o" * "w"
        TRY = "t" * "r" * "y"
        CATCH = "c" * "a" * "t" * "c" * "h"
        FINALLY = "f" * "i" * "n" * "a" * "l" * "l" * "y"
        NEW = "n" * "e" * "w"
        ARROW = "-" * ">"

        Literal = IntegerLiteral or FloatingPointLiteral or BooleanLiteral or
                CharacterLiteral or StringLiteral or NullLiteral

        /**
         * Productions from §4 (Types, Values, and Variables)
         */
        Type = PrimitiveType or ReferenceType
        PrimitiveType = Many(Annotation) * NumericType or Many(Annotation) * BOOLEAN
        NumericType = IntegralType or FloatingPointType
        IntegralType = BYTE or SHORT or INT or LONG or CHAR
        FloatingPointType = FLOAT or DOUBLE
        ReferenceType = ClassOrInterfaceType or TypeVariable or ArrayType
        ClassOrInterfaceType = ClassType or InterfaceType
        ClassType = Many(Annotation) * Identifier * Option(TypeArguments) or
                ClassOrInterfaceType * "." * Many(Annotation) * Identifier * Option(TypeArguments)
        InterfaceType = ClassType
        TypeVariable = Many(Annotation) * Identifier
        ArrayType = PrimitiveType * Dims or ClassOrInterfaceType * Dims or TypeVariable * Dims
        Dims = Some(Many(Annotation) * "[" * "]")
        TypeParameter  = Many(TypeParameterModifier) * Identifier * Option(TypeBound)
        TypeParameterModifier = Annotation
        TypeBound = EXTENDS * TypeVariable or EXTENDS * ClassOrInterfaceType * Many(AdditionalBound)
        AdditionalBound = "&" * InterfaceType
        TypeArguments = "<" * TypeArgumentList * ">"
        TypeArgumentList = TypeArgument * Many("," * TypeArgument)
        TypeArgument = ReferenceType or Wildcard
        Wildcard = Many(Annotation) * "?" * Option(WildcardBounds)
        WildcardBounds = EXTENDS * ReferenceType or SUPER * ReferenceType

        /**
         * Productions from §6 (Names)
         */

        TypeName = Identifier or PackageOrTypeName * "." * Identifier
        PackageOrTypeName = Identifier or PackageOrTypeName * "." * Identifier
        ExpressionName = Identifier or AmbiguousName * "." * Identifier
        MethodName = Identifier
        PackageName = Identifier or PackageName * "." * Identifier
        AmbiguousName = Identifier or AmbiguousName * "." * Identifier

        /**
         * Productions from §7 (Packages)
         */

        CompilationUnit = Many(Comment) * Option(PackageDeclaration) * Many(Many(Comment) * ImportDeclaration) * Many(Many(Comment) * TypeDeclaration)
        PackageDeclaration = Many(PackageModifier) * PACKAGE * Identifier * Many("." * Identifier) * ";"
        PackageModifier = Annotation
        ImportDeclaration = SingleTypeImportDeclaration or TypeImportOnDemandDeclaration or
                SingleStaticImportDeclaration or StaticImportOnDemandDeclaration
        SingleTypeImportDeclaration = IMPORT * TypeName * ";"
        TypeImportOnDemandDeclaration = IMPORT * PackageOrTypeName * "." * "*" * ";"
        SingleStaticImportDeclaration = IMPORT * STATIC * TypeName * "." * Identifier * ";"
        StaticImportOnDemandDeclaration = IMPORT * STATIC * TypeName * "." * "*" * ";"
        TypeDeclaration = ClassDeclaration or InterfaceDeclaration or ";"

        /**
         * Productions from §8 (Classes)
         */

        ClassDeclaration = NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration = Many(ClassModifier) * CLASS * Identifier *
                Option(TypeParameters) * Option(Superclass) * Option(Superinterfaces) * ClassBody
        ClassModifier = Annotation or PUBLIC or PROTECTED or PRIVATE or
                ABSTRACT or STATIC or FINAL or STRICTFP
        TypeParameters = "<" * TypeParameterList * ">"
        TypeParameterList = TypeParameter  * Many("," * TypeParameter)
        Superclass = EXTENDS * ClassType
        Superinterfaces = IMPLEMENTS * InterfaceTypeList
        InterfaceTypeList = InterfaceType  * Many("," * InterfaceType)
        ClassBody = "{" * Many(Many(Comment) * ClassBodyDeclaration) * "}"
        ClassBodyDeclaration = ClassMemberDeclaration or InstanceInitializer or StaticInitializer or ConstructorDeclaration
        ClassMemberDeclaration = FieldDeclaration or MethodDeclaration or ClassDeclaration or InterfaceDeclaration or ";"
        FieldDeclaration = Many(FieldModifier) * UnannType * VariableDeclaratorList * ";"
        FieldModifier = Annotation or PUBLIC or PROTECTED or PRIVATE or STATIC or
                FINAL or TRANSIENT or VOLATILE
        VariableDeclaratorList = VariableDeclarator * Many("," * VariableDeclarator)
        VariableDeclarator = VariableDeclaratorId * Option("=" * VariableInitializer)
        VariableDeclaratorId = Identifier * Option(Dims)
        VariableInitializer = Expression or ArrayInitializer
        UnannType = UnannPrimitiveType or UnannReferenceType
        UnannPrimitiveType = NumericType or BOOLEAN
        UnannReferenceType = UnannClassOrInterfaceType or UnannTypeVariable or UnannArrayType
        UnannClassOrInterfaceType = UnannClassType or UnannInterfaceType
        UnannClassType = Identifier * Option(TypeArguments) or
                UnannClassOrInterfaceType * "." * Many(Annotation) * Identifier * Option(TypeArguments)
        UnannInterfaceType = UnannClassType
        UnannTypeVariable = Identifier
        UnannArrayType = UnannPrimitiveType * Dims or UnannClassOrInterfaceType * Dims or UnannTypeVariable * Dims
        MethodDeclaration = Many(MethodModifier) * MethodHeader * MethodBody
        MethodModifier = Annotation or PUBLIC or PROTECTED or PRIVATE or ABSTRACT or
                STATIC or FINAL or SYNCHRONIZED or NATIVE or STRICTFP
        MethodHeader = Result * MethodDeclarator * Option(Throws) or
                TypeParameters * Many(Annotation) * Result * MethodDeclarator * Option(Throws)
        Result = UnannType or VOID
        MethodDeclarator = Identifier * "(" * Option(FormalParameterList) * ")" * Option(Dims)
        FormalParameterList = ReceiverParameter or FormalParameters * "," * LastFormalParameter or
                LastFormalParameter
        FormalParameters = FormalParameter * Many("," * FormalParameter) or
                ReceiverParameter * Many("," * FormalParameter)
        FormalParameter = Many(VariableModifier) * UnannType * VariableDeclaratorId
        VariableModifier = Annotation or FINAL
        LastFormalParameter = Many(VariableModifier) * UnannType * Many(Annotation) * ELLIPSIS * VariableDeclaratorId or FormalParameter
        ReceiverParameter = Many(Annotation) * UnannType * Option(Identifier * ".") * THIS
        Throws = THROWS * ExceptionTypeList
        ExceptionTypeList = ExceptionType * Many("," * ExceptionType)
        ExceptionType = ClassType or TypeVariable
        MethodBody = Comment or Block or ";"
        InstanceInitializer = Block
        StaticInitializer = STATIC * Block
        ConstructorDeclaration = Many(ConstructorModifier) * ConstructorDeclarator * Option(Throws) * ConstructorBody
        ConstructorModifier = Annotation or PUBLIC or PROTECTED or PRIVATE
        ConstructorDeclarator = Option(TypeParameters) * SimpleTypeName * "(" * Option(FormalParameterList) * ")"
        SimpleTypeName = Identifier
        ConstructorBody = "{" * Option(ExplicitConstructorInvocation) * Option(BlockStatements) * "}"
        ExplicitConstructorInvocation = Option(TypeArguments) * THIS * "(" * Option(ArgumentList) * ")" * ";" or
                Option(TypeArguments) * SUPER * "(" * Option(ArgumentList) * ")" * ";" or
                ExpressionName * "." * Option(TypeArguments) * SUPER * "(" * Option(ArgumentList) * ")" * ";" or
                Primary * "." * Option(TypeArguments) * SUPER * "(" * Option(ArgumentList) * ")" * ";"
        EnumDeclaration = Many(ClassModifier) * ENUM * Identifier * Option(Superinterfaces) * EnumBody
        EnumBody = "{" * Option(EnumConstantList) * Option(",") * Option(EnumBodyDeclarations) * "}"
        EnumConstantList = EnumConstant * Many("," * EnumConstant)
        EnumConstant = Many(EnumConstantModifier) * Identifier * Option("(" * Option(ArgumentList) * ")" * Option(ClassBody))
        EnumConstantModifier = Annotation
        EnumBodyDeclarations = ";" * Many(ClassBodyDeclaration)

        /**
         * Productions from §9 (Interfaces)
         */

        InterfaceDeclaration = NormalInterfaceDeclaration or AnnotationTypeDeclaration
        NormalInterfaceDeclaration =
            Many(InterfaceModifier) * INTERFACE * Identifier * Option(TypeParameters) * Option(ExtendsInterfaces) * InterfaceBody
        InterfaceModifier = Annotation or PUBLIC or PROTECTED or PRIVATE or
                ABSTRACT or STATIC or STRICTFP
        ExtendsInterfaces = EXTENDS * InterfaceTypeList
        InterfaceBody = "{" * Many(InterfaceMemberDeclaration) * "}"
        InterfaceMemberDeclaration = ConstantDeclaration or InterfaceMethodDeclaration or ClassDeclaration or InterfaceDeclaration or ";"
        ConstantDeclaration = Many(ConstantModifier) * UnannType * VariableDeclaratorList * ";"
        ConstantModifier = Annotation or PUBLIC or STATIC or FINAL
        InterfaceMethodDeclaration = Many(InterfaceMethodModifier) * MethodHeader * MethodBody
        InterfaceMethodModifier = Annotation or PUBLIC or ABSTRACT or DEFAULT or STATIC or STRICTFP
        AnnotationTypeDeclaration = Many(InterfaceModifier) * "@" * INTERFACE * Identifier * AnnotationTypeBody
        AnnotationTypeBody = "{" * Many(AnnotationTypeMemberDeclaration) * "}"
        AnnotationTypeMemberDeclaration = AnnotationTypeElementDeclaration or ConstantDeclaration or ClassDeclaration or InterfaceDeclaration or ";"
        AnnotationTypeElementDeclaration =
            Many(AnnotationTypeElementModifier) * UnannType * Identifier * "(" * ")" * Option(Dims) * Option(DefaultValue) * ";"
        AnnotationTypeElementModifier = Annotation or PUBLIC or ABSTRACT
        DefaultValue = DEFAULT * ElementValue
        Annotation = NormalAnnotation or MarkerAnnotation or SingleElementAnnotation
        NormalAnnotation = "@" * TypeName * "(" * Option(ElementValuePairList) * ")"
        ElementValuePairList = ElementValuePair * Many("," * ElementValuePair)
        ElementValuePair = Identifier * "=" * ElementValue
        ElementValue = ConditionalExpression or ElementValueArrayInitializer or Annotation
        ElementValueArrayInitializer = "{" * Option(ElementValueList) * Option(",") * "}"
        ElementValueList = ElementValue * Many("," * ElementValue)
        MarkerAnnotation = "@" * TypeName
        SingleElementAnnotation = "@" * TypeName * "(" * ElementValue * ")"

        /**
         * Productions from §10 (Arrays)
         */

        ArrayInitializer = "{" * Option(VariableInitializerList) * Option(",") * "}"
        VariableInitializerList = VariableInitializer * Many("," * VariableInitializer)

        /**
         * Productions from §14 (Blocks and Statements)
         */

        Block = "{" * Option(BlockStatements) * "}"
        BlockStatements = BlockStatement * Many(BlockStatement)
        BlockStatement = LocalVariableDeclarationStatement or ClassDeclaration or Statement or Comment
        LocalVariableDeclarationStatement = LocalVariableDeclaration * ";"
        LocalVariableDeclaration = Many(VariableModifier) * UnannType * VariableDeclaratorList
        Statement = StatementWithoutTrailingSubstatement or LabeledStatement or IfThenStatement or IfThenElseStatement or
                WhileStatement or ForStatement
        StatementNoShortIf = StatementWithoutTrailingSubstatement or LabeledStatementNoShortIf or IfThenElseStatementNoShortIf or
                WhileStatementNoShortIf or ForStatementNoShortIf
        StatementWithoutTrailingSubstatement = Block or EmptyStatement or ExpressionStatement or AssertStatement or
                SwitchStatement or DoStatement or BreakStatement or ContinueStatement or ReturnStatement or SynchronizedStatement or
                ThrowStatement or TryStatement
        EmptyStatement = Term(";")
        LabeledStatement = Identifier * ":" * Statement
        LabeledStatementNoShortIf = Identifier * ":" * StatementNoShortIf
        ExpressionStatement = StatementExpression * ";"
        StatementExpression = Assignment or PreIncrementExpression or PreDecrementExpression or PostIncrementExpression or
                PostDecrementExpression or MethodInvocation or ClassInstanceCreationExpression
        IfThenStatement = IF * "(" * Expression * ")" * Statement
        IfThenElseStatement = IF * "(" * Expression * ")" * StatementNoShortIf * ELSE * Statement
        IfThenElseStatementNoShortIf =
            IF * "(" * Expression * ")" * StatementNoShortIf * ELSE * StatementNoShortIf
        AssertStatement = ASSERT * Expression * ";" or
                ASSERT * Expression * ":" * Expression * ";"
        SwitchStatement = SWITCH * "(" * Expression * ")" * SwitchBlock
        SwitchBlock = "{" * Many(SwitchBlockStatementGroup) * Many(SwitchLabel) * "}"
        SwitchBlockStatementGroup = SwitchLabels * BlockStatements
        SwitchLabels = Some(SwitchLabel)
        SwitchLabel = CASE * ConstantExpression * ":" or
                CASE * EnumConstantName * ":" or DEFAULT * ":"
        EnumConstantName = Identifier
        WhileStatement = WHILE * "(" * Expression * ")" * Statement
        WhileStatementNoShortIf = WHILE * "(" * Expression * ")" * StatementNoShortIf
        DoStatement = DO * Statement * WHILE * "(" * Expression * ")" * ";"
        ForStatement = BasicForStatement or EnhancedForStatement
        ForStatementNoShortIf = BasicForStatementNoShortIf or EnhancedForStatementNoShortIf
        BasicForStatement = FOR * "(" * Option(ForInit) * ";" * Option(Expression) * ";" * Option(ForUpdate) * ")" * Statement
        BasicForStatementNoShortIf = FOR * "(" * Option(ForInit) * ";" * Option(Expression) * ";" * Option(ForUpdate) * ")" * StatementNoShortIf
        ForInit = StatementExpressionList or LocalVariableDeclaration
        ForUpdate = StatementExpressionList
        StatementExpressionList = StatementExpression * Many("," * StatementExpression)
        EnhancedForStatement = FOR * "(" * Many(VariableModifier) * UnannType * VariableDeclaratorId * ":" * Expression * ")" * Statement
        EnhancedForStatementNoShortIf = FOR * "(" * Many(VariableModifier) * UnannType * VariableDeclaratorId * ":" * Expression * ")" * StatementNoShortIf
        BreakStatement = BREAK * Option(Identifier) * ";"
        ContinueStatement = CONTINUE * Option(Identifier) * ";"
        ReturnStatement = RETURN * Option(Expression) * ";"
        ThrowStatement = THROW * Expression * ";"
        SynchronizedStatement = SYNCHRONIZED * "(" * Expression * ")" * Block
        TryStatement = TRY * Block * Catches or TRY * Block * Option(Catches) * Finally or TryWithResourcesStatement
        Catches = Some(CatchClause)
        CatchClause = CATCH * "(" * CatchFormalParameter * ")" * Block
        CatchFormalParameter = Many(VariableModifier) * CatchType * VariableDeclaratorId
        CatchType = UnannClassType * Many("|" * ClassType)
        Finally = FINALLY * Block
        TryWithResourcesStatement = TRY * ResourceSpecification * Block * Option(Catches) * Option(Finally)
        ResourceSpecification = "(" * ResourceList * Option(";") * ")"
        ResourceList = Resource * Many("," * Resource)
        Resource = Many(VariableModifier) * UnannType * VariableDeclaratorId * "=" * Expression

        /**
         * Productions from §15 (Expressions)
         */

        Primary = PrimaryNoNewArray or ArrayCreationExpression
        PrimaryNoNewArray = Literal or ClassLiteral or THIS or TypeName * "." * THIS or
                "(" * Expression * ")" or ClassInstanceCreationExpression or FieldAccess or
                ArrayAccess or MethodInvocation or MethodReference
        ClassLiteral = TypeName * Many("[" * "]") * "." * CLASS or
                NumericType * Many("[" * "]") * "." * CLASS or
                BOOLEAN * Many("[" * "]") * "." * CLASS or
                VOID * "." * CLASS
        ClassInstanceCreationExpression = UnqualifiedClassInstanceCreationExpression or
                ExpressionName * "." * UnqualifiedClassInstanceCreationExpression or
                Primary * "." * UnqualifiedClassInstanceCreationExpression
        UnqualifiedClassInstanceCreationExpression =
            NEW * Option(TypeArguments) * classOrInterfaceTypeToInstantiate * "(" * Option(ArgumentList) * ")" * Option(ClassBody)
        classOrInterfaceTypeToInstantiate = Many(Annotation) * Identifier * Many("." * Many(Annotation) * Identifier) * Option(TypeArgumentsOrDiamond)
        TypeArgumentsOrDiamond = TypeArguments or DIAMOND
        FieldAccess = Primary * "." * Identifier or SUPER * "." * Identifier or
                TypeName * "." * SUPER * "." * Identifier
        ArrayAccess = ExpressionName * "[" * Expression * "]" or
                PrimaryNoNewArray * "[" * Expression * "]"
        MethodInvocation = MethodName * "(" * Option(ArgumentList) * ")" or
                TypeName * "." * Option(TypeArguments) * Identifier * "(" * Option(ArgumentList) * ")" or
                ExpressionName * "." * Option(TypeArguments) * Identifier * "(" * Option(ArgumentList) * ")" or
                Primary * "." * Option(TypeArguments) * Identifier * "(" * Option(ArgumentList) * ")" or
                SUPER * "." * Option(TypeArguments) * Identifier * "(" * Option(ArgumentList) * ")" or
                TypeName * "." * SUPER * "." * Option(TypeArguments) * Identifier * "(" * Option(ArgumentList) * ")"
        ArgumentList = Expression * Many("," * Expression)
        MethodReference = ExpressionName * DOUBLECOLON * Option(TypeArguments) * Identifier or
                ReferenceType * DOUBLECOLON * Option(TypeArguments) * Identifier or
                Primary * DOUBLECOLON * Option(TypeArguments) * Identifier or
                SUPER * DOUBLECOLON * Option(TypeArguments) * Identifier or
                TypeName * "." * SUPER * DOUBLECOLON * Option(TypeArguments) * Identifier or
                ClassType * DOUBLECOLON * Option(TypeArguments) * NEW or
                ArrayType * DOUBLECOLON * NEW
        ArrayCreationExpression = NEW * PrimitiveType * DimExprs * Option(Dims) or
                NEW * ClassOrInterfaceType * DimExprs * Option(Dims) or
                NEW * PrimitiveType * Dims * ArrayInitializer or
                NEW * ClassOrInterfaceType * Dims * ArrayInitializer
        DimExprs = Some(DimExpr)
        DimExpr = Many(Annotation) * "[" * Expression * "]"
        Expression = LambdaExpression or AssignmentExpression
        LambdaExpression = LambdaParameters * ARROW * LambdaBody
        LambdaParameters = Identifier or "(" * Option(FormalParameterList) * ")" or
                "(" * InferredFormalParameterList * ")"
        InferredFormalParameterList = Identifier * Many("," * Identifier)
        LambdaBody = Expression or Block
        AssignmentExpression = ConditionalExpression or Assignment
        Assignment = LeftHandSide * AssignmentOperator * Expression
        LeftHandSide = ExpressionName or FieldAccess or ArrayAccess
        AssignmentOperator = "=" or STARASSIGN or SLASHASSIGN or PERCENTASSIGN or PLUSASSIGN or MINUSASSIGN or
                SHIFTLEFTASSIGN or SHIFTRIGHTASSIGN or USRIGHTSHIFTASSIGN or ANDASSIGN or XORASSIGN or ORASSIGN
        ConditionalExpression = ConditionalOrExpression or
                ConditionalOrExpression * "?" * Expression * ":" * ConditionalExpression or
                ConditionalOrExpression * "?" * Expression * ":" * LambdaExpression
        ConditionalOrExpression = ConditionalAndExpression or
                ConditionalOrExpression * SLASHASSIGN * ConditionalAndExpression
        ConditionalAndExpression = InclusiveOrExpression or
                ConditionalAndExpression * AND * InclusiveOrExpression
        InclusiveOrExpression = ExclusiveOrExpression or
                InclusiveOrExpression * "|" * ExclusiveOrExpression
        ExclusiveOrExpression = AndExpression or ExclusiveOrExpression * "^" * AndExpression
        AndExpression = EqualityExpression or AndExpression * "&" * EqualityExpression
        EqualityExpression = RelationalExpression or EqualityExpression * EQ * RelationalExpression or
                EqualityExpression * NOTEQ * RelationalExpression
        RelationalExpression = ShiftExpression or RelationalExpression * "<" * ShiftExpression or
                RelationalExpression * ">" * ShiftExpression or RelationalExpression * LESSEQ * ShiftExpression or
                RelationalExpression * GREATEQ * ShiftExpression or RelationalExpression * INSTANCEOF * ReferenceType
        ShiftExpression = AdditiveExpression or ShiftExpression * LEFTSHIFT * AdditiveExpression or
                ShiftExpression * RIGHTSHIFT * AdditiveExpression or
                ShiftExpression * USRIGHTSHIFT * AdditiveExpression
        AdditiveExpression = MultiplicativeExpression or AdditiveExpression * "+" * MultiplicativeExpression or
                AdditiveExpression * "-" * MultiplicativeExpression
        MultiplicativeExpression = UnaryExpression or MultiplicativeExpression * "*" * UnaryExpression or
                MultiplicativeExpression * "/" * UnaryExpression or
                MultiplicativeExpression * "%" * UnaryExpression
        UnaryExpression = PreIncrementExpression or PreDecrementExpression or "+" * UnaryExpression or
                "-" * UnaryExpression or UnaryExpressionNotPlusMinus
        PreIncrementExpression = PLUSPLUS * UnaryExpression
        PreDecrementExpression = MINUSMINUS * UnaryExpression
        UnaryExpressionNotPlusMinus = PostfixExpression or "~" * UnaryExpression or "!" * UnaryExpression or
                CastExpression
        PostfixExpression = Primary or ExpressionName or PostIncrementExpression or PostDecrementExpression
        PostIncrementExpression = PostfixExpression * PLUSPLUS
        PostDecrementExpression = PostfixExpression * MINUSMINUS
        CastExpression = "(" * PrimitiveType * ")" * UnaryExpression or
                "(" * ReferenceType * Many(AdditionalBound) * ")" * UnaryExpressionNotPlusMinus or
                "(" * ReferenceType * Many(AdditionalBound) * ")" * LambdaExpression
        ConstantExpression = Expression

        setStart(CompilationUnit)
    }
}