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
    var DOT by Nt()
    var BRACKETLEFT by Nt()
    var BRACKETRIGHT by Nt()
    var PARENTHLEFT by Nt()
    var PARENTHRIGHT by Nt()
    var CURLYLEFT by Nt()
    var CURLYRIGHT by Nt()
    var EXTENDS by Nt()
    var ANDBIT by Nt()
    var DIAMONDLEFT by Nt()
    var DIAMONDRIGHT by Nt()
    var DIAMOND by Nt()
    var SEMICOLON by Nt()
    var COLON by Nt()
    var DOUBLECOLON by Nt()
    var ELLIPSIS by Nt()
    var COMMA by Nt()
    var QUESTIONMARK by Nt()
    var SUPER by Nt()
    var PACKAGE by Nt()
    var IMPORT by Nt()
    var STATIC by Nt()
    var STAR by Nt()
    var PLUS by Nt()
    var MINUS by Nt()
    var PERCENT by Nt()
    var SLASH by Nt()
    var PLUSPLUS by Nt()
    var MINUSMINUS by Nt()
    var TILDA by Nt()
    var EXCLAMATIONMARK by Nt()
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
    var ASSIGN by Nt()
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
    var XORBIT by Nt()
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
    var AT by Nt()
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
    var ORBIT by Nt()
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
    var DocumentationComment by Nt()
    var CommentContent by Nt()
    var BooleanLiteral by Nt()
    var NullLiteral by Nt()
    init {
        JavaLetter = "a" or "b" or "c" or "d" or "e" or "f" or "g" or "h" or "i" or "j" or "k" or "l" or "m" or "n" or
                "o" or "p" or "q" or "r" or "s" or "t" or "u" or "v" or "w" or "x" or "y" or "z" or "$" or "_"

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

        InputCharacter = "\\" * "u" * HexRange * HexRange * HexRange * HexRange or
                JavaLetter or DigitRange or " " or "!" or "#" or "$" or "%" or "&" or "(" or ")" or
                "*" or "+" or "," or "-" or "." or "/" or "0" or "1" or "2" or "3" or "4" or "5" or "6" or "7" or
                "8" or "9" or ":" or ";" or "<" or "=" or ">" or "?" or "@" or "A" or "B" or "C" or "D" or "E" or
                "F" or "G" or "H" or "I" or "J" or "K" or "L" or "M" or "N" or "O" or "P" or "Q" or "R" or "S" or
                "T" or "U" or "V" or "W" or "X" or "Y" or "Z" or "[" or "]" or "^" or "_" or "`" or "a" or
                "b" or "c" or "d" or "e" or "f" or "g" or "h" or "i" or "j" or "k" or "l" or "m" or "n" or "o" or
                "p" or "q" or "r" or "s" or "t" or "u" or "v" or "w" or "x" or "y" or "z" or "{" or "|" or "}" or "~"

        EscapeSequence = "\\" * ("b" or "t" or "n" or "f" or "r" or "\"" or "\'" or "\\") or
                "\\" * (("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7") or
                ("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7") * ("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7") or
                ("0" or "1" or "2" or "3") * ("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7") * ("0" or "1" or "2" or "3" or "4" or "5" or "6" or "7"))
        LineTerminator = "\r" or "\n" or "\r\n"

        CharacterLiteral = "\'" * (JavaLetter or DigitRange or " " or "!" or "#" or "$" or "%" or "&" or "(" or ")" or
                "*" or "+" or "," or "-" or "." or "/" or "0" or "1" or "2" or "3" or "4" or "5" or "6" or "7" or
                "8" or "9" or ":" or ";" or "<" or "=" or ">" or "?" or "@" or "A" or "B" or "C" or "D" or "E" or
                "F" or "G" or "H" or "I" or "J" or "K" or "L" or "M" or "N" or "O" or "P" or "Q" or "R" or "S" or
                "T" or "U" or "V" or "W" or "X" or "Y" or "Z" or "[" or "]" or "^" or "_" or "`" or "a" or
                "b" or "c" or "d" or "e" or "f" or "g" or "h" or "i" or "j" or "k" or "l" or "m" or "n" or "o" or
                "p" or "q" or "r" or "s" or "t" or "u" or "v" or "w" or "x" or "y" or "z" or "{" or "|" or "}" or "~") * "\'" or
                "\'" * EscapeSequence * "\'"

        StringLiteral = "\"" * Many(StringCharacter) * "\""
        StringCharacter = InputCharacter or EscapeSequence
        WhiteSpace = LineTerminator or (" " or "\t")

        Comment = TraditionalComment or DocumentationComment
        TraditionalComment = "/*" * Many(JavaLetter or DigitRange or " " or "!" or "#" or "$" or "%" or "&" or "(" or ")" or
                "+" or "," or "-" or "." or "/" or "0" or "1" or "2" or "3" or "4" or "5" or "6" or "7" or
                "8" or "9" or ":" or ";" or "<" or "=" or ">" or "?" or "@" or "A" or "B" or "C" or "D" or "E" or
                "F" or "G" or "H" or "I" or "J" or "K" or "L" or "M" or "N" or "O" or "P" or "Q" or "R" or "S" or
                "T" or "U" or "V" or "W" or "X" or "Y" or "Z" or "[" or "]" or "^" or "_" or "`" or "a" or
                "b" or "c" or "d" or "e" or "f" or "g" or "h" or "i" or "j" or "k" or "l" or "m" or "n" or "o" or
                "p" or "q" or "r" or "s" or "t" or "u" or "v" or "w" or "x" or "y" or "z" or "{" or "|" or "}" or "~") *
                "*" * "/" or
                "/" * "*" * Some("*") * "/"
        DocumentationComment = "/" * "*" * "*" * CommentContent * Some("*") * "/"
        CommentContent = Many(JavaLetter or DigitRange or " " or "!" or "#" or "$" or "%" or "&" or "(" or ")" or
                "+" or "," or "-" or "." or "/" or "0" or "1" or "2" or "3" or "4" or "5" or "6" or "7" or
                "8" or "9" or ":" or ";" or "<" or "=" or ">" or "?" or "@" or "A" or "B" or "C" or "D" or "E" or
                "F" or "G" or "H" or "I" or "J" or "K" or "L" or "M" or "N" or "O" or "P" or "Q" or "R" or "S" or
                "T" or "U" or "V" or "W" or "X" or "Y" or "Z" or "[" or "]" or "^" or "_" or "`" or "a" or
                "b" or "c" or "d" or "e" or "f" or "g" or "h" or "i" or "j" or "k" or "l" or "m" or "n" or "o" or
                "p" or "q" or "r" or "s" or "t" or "u" or "v" or "w" or "x" or "y" or "z" or "{" or "|" or "}" or "~" or
                Some("*") * (JavaLetter or DigitRange or " " or "!" or "#" or "$" or "%" or "&" or "(" or ")" or
                "+" or "," or "-" or "." or "/" or "0" or "1" or "2" or "3" or "4" or "5" or "6" or "7" or
                "8" or "9" or ":" or ";" or "<" or "=" or ">" or "?" or "@" or "A" or "B" or "C" or "D" or "E" or
                "F" or "G" or "H" or "I" or "J" or "K" or "L" or "M" or "N" or "O" or "P" or "Q" or "R" or "S" or
                "T" or "U" or "V" or "W" or "X" or "Y" or "Z" or "[" or "]" or "^" or "_" or "`" or "a" or
                "b" or "c" or "d" or "e" or "f" or "g" or "h" or "i" or "j" or "k" or "l" or "m" or "n" or "o" or
                "p" or "q" or "r" or "s" or "t" or "u" or "v" or "w" or "x" or "y" or "z" or "{" or "|" or "}" or "~"))


        BOOLEAN = "b" * "o" * "o" * "l" * "e" * "a" * "n"
        BYTE = "b" * "y" * "t" * "e"
        SHORT = "s" * "h" * "o" * "r" * "t"
        INT = "i" * "n" * "t"
        LONG = "l" * "o" * "n" * "g"
        CHAR = "c" * "h" * "a" * "r"
        FLOAT = "f" * "l" * "o" * "a" * "t"
        DOUBLE = "d" * "o" * "u" * "b" * "l" * "e"
        DOT = Term(".")
        BRACKETLEFT = Term("[")
        BRACKETRIGHT = Term("]")
        PARENTHLEFT = Term("(")
        PARENTHRIGHT = Term(")")
        CURLYLEFT = Term("{")
        CURLYRIGHT = Term("}")
        EXTENDS = "e" * "x" * "t" * "e" * "n" * "d" * "s"
        ANDBIT = Term("&")
        DIAMONDLEFT = Term("<")
        DIAMONDRIGHT = Term(">")
        DIAMOND = Term("<>")
        SEMICOLON = Term(";")
        COLON = Term(":")
        DOUBLECOLON = Term("::")
        ELLIPSIS = Term("...")
        COMMA = Term(",")
        QUESTIONMARK = Term("?")
        SUPER = "s" * "u" * "p" * "e" * "r"
        PACKAGE = "p" * "a" * "c" * "k" * "a" * "g" * "e"
        IMPORT = "i" * "m" * "p" * "o" * "r" * "t"
        STATIC = "s" * "t" * "a" * "t" * "i" * "c"
        STAR = Term("*")
        PLUS = Term("+")
        MINUS = Term("-")
        PERCENT = Term("%")
        SLASH = Term("/")
        PLUSPLUS = "+" * "+"
        MINUSMINUS = "-" * "-"
        TILDA = Term("~")
        EXCLAMATIONMARK = Term("!")
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
        ASSIGN = Term("=")
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
        XORBIT = Term("^")
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
        AT = Term("@")
        DEFAULT = "d" * "e" * "f" * "a" * "u" * "l" * "t"
        ASSERT = "a" * "s" * "s" * "e" * "r" * "t"
        SWITCH = "s" * "w" * "i" * "t" * "c" * "h"
        CASE = "c" * "a" * "s" * "e"
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
        ORBIT = Term("|")
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
                ClassOrInterfaceType * DOT * Many(Annotation) * Identifier * Option(TypeArguments)
        InterfaceType = ClassType
        TypeVariable = Many(Annotation) * Identifier
        ArrayType = PrimitiveType * Dims or ClassOrInterfaceType * Dims or TypeVariable * Dims
        Dims = Some(Many(Annotation) * BRACKETLEFT * BRACKETRIGHT)
        TypeParameter  = Many(TypeParameterModifier) * Identifier * Option(TypeBound)
        TypeParameterModifier = Annotation
        TypeBound = EXTENDS * TypeVariable or EXTENDS * ClassOrInterfaceType * Many(AdditionalBound)
        AdditionalBound = ANDBIT * InterfaceType
        TypeArguments = DIAMONDLEFT * TypeArgumentList * DIAMONDRIGHT
        TypeArgumentList = TypeArgument * Many(COMMA * TypeArgument)
        TypeArgument = ReferenceType or Wildcard
        Wildcard = Many(Annotation) * Term(JavaToken.QUESTIONMARK) * Option(WildcardBounds)
        WildcardBounds = EXTENDS * ReferenceType or Term(JavaToken.SUPER) * ReferenceType

        /**
         * Productions from §6 (Names)
         */

        TypeName = Identifier or PackageOrTypeName * DOT * Identifier
        PackageOrTypeName = Identifier or PackageOrTypeName * DOT * Identifier
        ExpressionName = Identifier or AmbiguousName * DOT * Identifier
        MethodName = Identifier
        PackageName = Identifier or PackageName * DOT * Identifier
        AmbiguousName = Identifier or AmbiguousName * DOT * Identifier

        /**
         * Productions from §7 (Packages)
         */

        CompilationUnit = Option(PackageDeclaration) * Many(ImportDeclaration) * Many(TypeDeclaration)
        PackageDeclaration = Many(PackageModifier) * Term(JavaToken.PACKAGE) * Identifier * Many(DOT * Identifier) * Term(JavaToken.SEMICOLON)
        PackageModifier = Annotation
        ImportDeclaration = SingleTypeImportDeclaration or TypeImportOnDemandDeclaration or
                SingleStaticImportDeclaration or StaticImportOnDemandDeclaration
        SingleTypeImportDeclaration = Term(JavaToken.IMPORT) * TypeName * Term(JavaToken.SEMICOLON)
        TypeImportOnDemandDeclaration = Term(JavaToken.IMPORT) * PackageOrTypeName * DOT * Term(JavaToken.STAR) * Term(JavaToken.SEMICOLON)
        SingleStaticImportDeclaration = Term(JavaToken.IMPORT) * Term(JavaToken.STATIC) * TypeName * DOT * Identifier * Term(JavaToken.SEMICOLON)
        StaticImportOnDemandDeclaration = Term(JavaToken.IMPORT) * Term(JavaToken.STATIC) * TypeName * DOT * Term(JavaToken.STAR) * Term(JavaToken.SEMICOLON)
        TypeDeclaration = ClassDeclaration or InterfaceDeclaration or Term(JavaToken.SEMICOLON)

        /**
         * Productions from §8 (Classes)
         */

        ClassDeclaration = NormalClassDeclaration or EnumDeclaration
        NormalClassDeclaration = Many(ClassModifier) * Term(JavaToken.CLASS) * Identifier *
                Option(TypeParameters) * Option(Superclass) * Option(Superinterfaces) * ClassBody
        ClassModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or
                Term(JavaToken.ABSTRACT) or Term(JavaToken.STATIC) or Term(JavaToken.FINAL) or Term(JavaToken.STRICTFP)
        TypeParameters = DIAMONDLEFT * TypeParameterList * DIAMONDRIGHT
        TypeParameterList = TypeParameter  * Many(COMMA * TypeParameter)
        Superclass = EXTENDS * ClassType
        Superinterfaces = Term(JavaToken.IMPLEMENTS) * InterfaceTypeList
        InterfaceTypeList = InterfaceType  * Many(COMMA * InterfaceType)
        ClassBody = Term(JavaToken.CURLYLEFT) * Many(ClassBodyDeclaration) * Term(JavaToken.CURLYRIGHT)
        ClassBodyDeclaration = ClassMemberDeclaration or InstanceInitializer or StaticInitializer or ConstructorDeclaration
        ClassMemberDeclaration = FieldDeclaration or MethodDeclaration or ClassDeclaration or InterfaceDeclaration or Term(JavaToken.SEMICOLON)
        FieldDeclaration = Many(FieldModifier) * UnannType * VariableDeclaratorList * Term(JavaToken.SEMICOLON)
        FieldModifier = Annotation or Term(JavaToken.PUBLIC) or Term(JavaToken.PROTECTED) or Term(JavaToken.PRIVATE) or Term(JavaToken.STATIC) or
                Term(JavaToken.FINAL) or Term(JavaToken.TRANSIENT) or Term(JavaToken.VOLATILE)
        VariableDeclaratorList = VariableDeclarator * Many(COMMA * VariableDeclarator)
        VariableDeclarator = VariableDeclaratorId * Option(Term(JavaToken.ASSIGN) * VariableInitializer)
        VariableDeclaratorId = Identifier * Option(Dims)
        VariableInitializer = Expression or ArrayInitializer
        UnannType = UnannPrimitiveType or UnannReferenceType
        UnannPrimitiveType = NumericType or BOOLEAN
        UnannReferenceType = UnannClassOrInterfaceType or UnannTypeVariable or UnannArrayType
        UnannClassOrInterfaceType = UnannClassType or UnannInterfaceType
        UnannClassType = Identifier * Option(TypeArguments) or
                UnannClassOrInterfaceType * DOT * Many(Annotation) * Identifier * Option(TypeArguments)
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
        FormalParameterList = ReceiverParameter or FormalParameters * COMMA * LastFormalParameter or
                LastFormalParameter
        FormalParameters = FormalParameter * Many(COMMA * FormalParameter) or
                ReceiverParameter * Many(COMMA * FormalParameter)
        FormalParameter = Many(VariableModifier) * UnannType * VariableDeclaratorId
        VariableModifier = Annotation or Term(JavaToken.FINAL)
        LastFormalParameter = Many(VariableModifier) * UnannType * Many(Annotation) * Term(JavaToken.ELLIPSIS) * VariableDeclaratorId or FormalParameter
        ReceiverParameter = Many(Annotation) * UnannType * Option(Identifier * Term(JavaToken.DOT)) * Term(JavaToken.THIS)
        Throws = Term(JavaToken.THROWS) * ExceptionTypeList
        ExceptionTypeList = ExceptionType * Many(COMMA * ExceptionType)
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
                ExpressionName * DOT * Option(TypeArguments) * Term(JavaToken.SUPER) * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON) or
                Primary * DOT * Option(TypeArguments) * Term(JavaToken.SUPER) * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) * Term(JavaToken.SEMICOLON)
        EnumDeclaration = Many(ClassModifier) * Term(JavaToken.ENUM) * Identifier * Option(Superinterfaces) * EnumBody
        EnumBody = Term(JavaToken.CURLYLEFT) * Option(EnumConstantList) * Option(Term(JavaToken.COMMA)) * Option(EnumBodyDeclarations) * Term(JavaToken.CURLYRIGHT)
        EnumConstantList = EnumConstant * Many(COMMA * EnumConstant)
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
        ExtendsInterfaces = EXTENDS * InterfaceTypeList
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
        ElementValuePairList = ElementValuePair * Many(COMMA * ElementValuePair)
        ElementValuePair = Identifier * Term(JavaToken.ASSIGN) * ElementValue
        ElementValue = ConditionalExpression or ElementValueArrayInitializer or Annotation
        ElementValueArrayInitializer = Term(JavaToken.CURLYLEFT) * Option(ElementValueList) * Option(Term(JavaToken.COMMA)) * Term(JavaToken.CURLYRIGHT)
        ElementValueList = ElementValue * Many(COMMA * ElementValue)
        MarkerAnnotation = Term(JavaToken.AT) * TypeName
        SingleElementAnnotation = Term(JavaToken.AT) * TypeName * Term(JavaToken.PARENTHLEFT) * ElementValue * Term(JavaToken.PARENTHRIGHT)

        /**
         * Productions from §10 (Arrays)
         */

        ArrayInitializer = Term(JavaToken.CURLYLEFT) * Option(VariableInitializerList) * Option(Term(JavaToken.COMMA)) * Term(JavaToken.CURLYRIGHT)
        VariableInitializerList = VariableInitializer * Many(COMMA * VariableInitializer)

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
        StatementExpressionList = StatementExpression * Many(COMMA * StatementExpression)
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
        ResourceList = Resource * Many(COMMA * Resource)
        Resource = Many(VariableModifier) * UnannType * VariableDeclaratorId * Term(JavaToken.ASSIGN) * Expression

        /**
         * Productions from §15 (Expressions)
         */

        Primary = PrimaryNoNewArray or ArrayCreationExpression
        PrimaryNoNewArray = Literal or ClassLiteral or Term(JavaToken.THIS) or TypeName * DOT * Term(JavaToken.THIS) or
                Term(JavaToken.PARENTHLEFT) * Expression * Term(JavaToken.PARENTHRIGHT) or ClassInstanceCreationExpression or FieldAccess or
                ArrayAccess or MethodInvocation or MethodReference
        ClassLiteral = TypeName * Many(BRACKETLEFT * Term(JavaToken.BRACKETRIGHT)) * DOT * Term(JavaToken.CLASS) or
                NumericType * Many(BRACKETLEFT * Term(JavaToken.BRACKETRIGHT)) * DOT * Term(JavaToken.CLASS) or
                BOOLEAN * Many(BRACKETLEFT * Term(JavaToken.BRACKETRIGHT)) * DOT * Term(JavaToken.CLASS) or
                Term(JavaToken.VOID) * DOT * Term(JavaToken.CLASS)
        ClassInstanceCreationExpression = UnqualifiedClassInstanceCreationExpression or
                ExpressionName * DOT * UnqualifiedClassInstanceCreationExpression or
                Primary * DOT * UnqualifiedClassInstanceCreationExpression
        UnqualifiedClassInstanceCreationExpression =
            Term(JavaToken.NEW) * Option(TypeArguments) * classOrInterfaceTypeToInstantiate * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) * Option(ClassBody)
        classOrInterfaceTypeToInstantiate = Many(Annotation) * Identifier * Many(DOT * Many(Annotation) * Identifier) * Option(TypeArgumentsOrDiamond)
        TypeArgumentsOrDiamond = TypeArguments or Term(JavaToken.DIAMOND)
        FieldAccess = Primary * DOT * Identifier or Term(JavaToken.SUPER) * DOT * Identifier or
                TypeName * DOT * Term(JavaToken.SUPER) * DOT * Identifier
        ArrayAccess = ExpressionName * BRACKETLEFT * Expression * BRACKETRIGHT or
                PrimaryNoNewArray * BRACKETLEFT * Expression * BRACKETRIGHT
        MethodInvocation = MethodName * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                TypeName * DOT * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                ExpressionName * DOT * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                Primary * DOT * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                Term(JavaToken.SUPER) * DOT * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT) or
                TypeName * DOT * Term(JavaToken.SUPER) * DOT * Option(TypeArguments) * Identifier * Term(JavaToken.PARENTHLEFT) * Option(ArgumentList) * Term(JavaToken.PARENTHRIGHT)
        ArgumentList = Expression * Many(COMMA * Expression)
        MethodReference = ExpressionName * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Identifier or
                ReferenceType * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Identifier or
                Primary * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Identifier or
                Term(JavaToken.SUPER) * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Identifier or
                TypeName * DOT * Term(JavaToken.SUPER) * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Identifier or
                ClassType * Term(JavaToken.DOUBLECOLON) * Option(TypeArguments) * Term(JavaToken.NEW) or
                ArrayType * Term(JavaToken.DOUBLECOLON) * Term(JavaToken.NEW)
        ArrayCreationExpression = Term(JavaToken.NEW) * PrimitiveType * DimExprs * Option(Dims) or
                Term(JavaToken.NEW) * ClassOrInterfaceType * DimExprs * Option(Dims) or
                Term(JavaToken.NEW) * PrimitiveType * Dims * ArrayInitializer or
                Term(JavaToken.NEW) * ClassOrInterfaceType * Dims * ArrayInitializer
        DimExprs = Some(DimExpr)
        DimExpr = Many(Annotation) * BRACKETLEFT * Expression * BRACKETRIGHT
        Expression = LambdaExpression or AssignmentExpression
        LambdaExpression = LambdaParameters * Term(JavaToken.ARROW) * LambdaBody
        LambdaParameters = Identifier or Term(JavaToken.PARENTHLEFT) * Option(FormalParameterList) * Term(JavaToken.PARENTHRIGHT) or
                Term(JavaToken.PARENTHLEFT) * InferredFormalParameterList * Term(JavaToken.PARENTHRIGHT)
        InferredFormalParameterList = Identifier * Many(COMMA * Identifier)
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
        AndExpression = EqualityExpression or AndExpression * ANDBIT * EqualityExpression
        EqualityExpression = RelationalExpression or EqualityExpression * Term(JavaToken.EQ) * RelationalExpression or
                EqualityExpression * Term(JavaToken.NOTEQ) * RelationalExpression
        RelationalExpression = ShiftExpression or RelationalExpression * DIAMONDLEFT * ShiftExpression or
                RelationalExpression * DIAMONDRIGHT * ShiftExpression or RelationalExpression * Term(JavaToken.LESSEQ) * ShiftExpression or
                RelationalExpression * Term(JavaToken.GREATEQ) * ShiftExpression or RelationalExpression * Term(JavaToken.INSTANCEOF) * ReferenceType
        ShiftExpression = AdditiveExpression or ShiftExpression * Term(JavaToken.LEFTSHIFT) * AdditiveExpression or
                ShiftExpression * Term(JavaToken.RIGHTSHIFT) * AdditiveExpression or
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