package grammars.java8

import org.ucfs.parser.generator.ParserGeneratorException
import org.ucfs.rsm.symbol.ITerminal

enum class Token : ITerminal {
    ID, EOF, INTEGERLIT, FLOATINGLIT, BOOLEANLIT, CHARLIT, STRINGLIT, NULLLIT,
    BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE, DOT, BRACKETLEFT, BRACKETRIGHT,
    PARENTHLEFT, PARENTHRIGHT, CURLYLEFT, CURLYRIGHT, EXTENDS, ANDBIT, LT, GT,
    DIAMOND, SEMICOLON, COLON, DOUBLECOLON, ELLIPSIS, COMMA, QUESTIONMARK, SUPER, PACKAGE,
    IMPORT, STATIC, STAR, PLUS, MINUS, PERCENT, SLASH, PLUSPLUS, MINUSMINUS, TILDA, EXCLAMATIONMARK,
    CLASS, PUBLIC, PROTECTED, PRIVATE, FINAL, STRICTFP, IMPLEMENTS, TRANSIENT, VOLATILE, ASSIGN,
    STARASSIGN, SLASHASSIGN, PLUSASSIGN, MINUSASSIGN, PERCENTASSIGN, XORASSIGN, SHIFTLEFTASSIGN,
    SHIFTRIGHTASSIGN, USRIGHTSHIFTASSIGN, ANDASSIGN, ORASSIGN, OR, AND, XORBIT, EQ, NOTEQ, LESSEQ,
    GREATEQ, INSTANCEOF, SYNCHRONIZED, NATIVE, VOID, THIS, THROWS, ENUM, INTERFACE, ABSTRACT, AT, DEFAULT, ASSERT,
    SWITCH, CASE, WHILE, FOR, IF, ELSE, DO, BREAK, CONTINUE, RETURN, THROW, TRY, CATCH, FINALLY, ORBIT, NEW, ARROW;

    override fun getComparator(): Comparator<ITerminal> {
        return object : Comparator<ITerminal> {
            override fun compare(a: ITerminal, b: ITerminal): Int {
                if (a !is Token || b !is Token) {
                    throw ParserGeneratorException(
                        "used comparator for $javaClass, " +
                                "but got elements of ${a.javaClass}$ and ${b.javaClass}\$"
                    )
                }
                return a.ordinal - b.ordinal
            }
        }
    }
}