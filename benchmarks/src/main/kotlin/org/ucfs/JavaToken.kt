package org.ucfs

import org.ucfs.parser.ParsingException
import org.ucfs.rsm.symbol.ITerminal

enum class JavaToken : ITerminal {
    IDENTIFIER,
    EOF,


    /*Literal*/
    INTEGER_LITERAL,
    FLOATING_POINT_LITERAL,
    BOOLEAN_LITERAL,
    CHARACTER_LITERAL,
    STRING_LITERAL,
    NULL_LITERAL,


    /*Keywords*/
    ABSTRACT,
    ASSERT,
    BOOLEAN,
    BYTE,
    BREAK,
    CASE,
    CATCH,
    CHAR,
    CLASS,
    CONST,
    CONTINUE,
    DEFAULT,
    DO,
    DOUBLE,
    ELSE,
    ENUM,
    EXTENDS,
    FINAL,
    FINALLY,
    FLOAT,
    FOR,
    IF,
    GOTO,
    IMPLEMENTS,
    IMPORT,
    INSTANCEOF,
    INT,
    INTERFACE,
    LONG,
    NATIVE,
    NEW,
    PACKAGE,
    PRIVATE,
    PROTECTED,
    PUBLIC,
    RETURN,
    SHORT,
    STATIC,
    STRICTFP,
    SUPER,
    SWITCH,
    SYNCHRONIZED,
    THIS,
    THROW,
    THROWS,
    TRANSIENT,
    TRY,
    VOID,
    VOLATILE,
    WHILE,


    /*SEPARATORS*/
    LPAREN, //(
    RPAREN, //)
    LBRACE, //{
    RBRACE, //}
    LBRACK, //[
    RBRACK, //]
    SEMICOLON, //;
    COMMA, //,
    DOT, //.
    ELLIPSIS, //...
    AT,  //@
    COLONCOLON, //::

    /*OPERATORS*/
    EQ,
    LT,
    GT,
    NOT,
    COMP, //~
    QUESTION,
    COLON,
    ARROW,
    EQEQ,
    LTEQ,
    GTEQ,
    NOTEQ,
    ANDAND,
    OROR,
    PLUSPLUS,
    MINUSMINUS,
    PLUS,
    MINUS,
    MULT,
    DIV, // /
    AND,
    OR,
    XOR,
    MOD,  //%
   // LSHIFT,  // <<
   // RSHIFT,  // >>
  //  URSHIFT, // >>>
    PLUSEQ,
    MINUSEQ,
    MULTEQ,
    DIVEQ,
    ANDEQ,
    OREQ,
    XOREQ,
    MODEQ,
    LSHIFTEQ,
    RSHIFTEQ,
    URSHIFTEQ,
     ;

    override fun getComparator(): Comparator<ITerminal> {
        return object : Comparator<ITerminal> {
            override fun compare(a: ITerminal, b: ITerminal): Int {
                if (a !is JavaToken || b !is JavaToken) {
                    throw ParsingException(
                        "used comparator for $javaClass, " +
                                "but got elements of ${a.javaClass}$ and ${b.javaClass}\$"
                    )
                }
                return a.ordinal - b.ordinal
            }
        }
    }
}