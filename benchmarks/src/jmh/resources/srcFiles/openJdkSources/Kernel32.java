/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https:
 */
package jdk.internal.org.jline.terminal.impl.jna.win;


import jdk.internal.org.jline.terminal.impl.jna.LastErrorException;

interface Kernel32 {

    Kernel32 INSTANCE = new Kernel32Impl();


    int STD_INPUT_HANDLE =  -10;
    int STD_OUTPUT_HANDLE = -11;
    int STD_ERROR_HANDLE =  -12;

    int ENABLE_PROCESSED_INPUT =    0x0001;
    int ENABLE_LINE_INPUT =         0x0002;
    int ENABLE_ECHO_INPUT =         0x0004;
    int ENABLE_WINDOW_INPUT =       0x0008;
    int ENABLE_MOUSE_INPUT =        0x0010;
    int ENABLE_INSERT_MODE =        0x0020;
    int ENABLE_QUICK_EDIT_MODE =    0x0040;
    int ENABLE_EXTENDED_FLAGS =     0x0080;

    int RIGHT_ALT_PRESSED =     0x0001;
    int LEFT_ALT_PRESSED =      0x0002;
    int RIGHT_CTRL_PRESSED =    0x0004;
    int LEFT_CTRL_PRESSED =     0x0008;
    int SHIFT_PRESSED =         0x0010;

    int FOREGROUND_BLUE =       0x0001;
    int FOREGROUND_GREEN =      0x0002;
    int FOREGROUND_RED =        0x0004;
    int FOREGROUND_INTENSITY =  0x0008;
    int BACKGROUND_BLUE =       0x0010;
    int BACKGROUND_GREEN =      0x0020;
    int BACKGROUND_RED =        0x0040;
    int BACKGROUND_INTENSITY =  0x0080;

    int FROM_LEFT_1ST_BUTTON_PRESSED = 0x0001;
    int RIGHTMOST_BUTTON_PRESSED     = 0x0002;
    int FROM_LEFT_2ND_BUTTON_PRESSED = 0x0004;
    int FROM_LEFT_3RD_BUTTON_PRESSED = 0x0008;
    int FROM_LEFT_4TH_BUTTON_PRESSED = 0x0010;

    int MOUSE_MOVED                  = 0x0001;
    int DOUBLE_CLICK                 = 0x0002;
    int MOUSE_WHEELED                = 0x0004;
    int MOUSE_HWHEELED               = 0x0008;

    int WaitForSingleObject(Pointer in_hHandle, int in_dwMilliseconds);

    Pointer GetStdHandle(int nStdHandle);

    int GetConsoleOutputCP();

    void FillConsoleOutputCharacter(Pointer in_hConsoleOutput,
                                    char in_cCharacter, int in_nLength, COORD in_dwWriteCoord,
                                    IntByReference out_lpNumberOfCharsWritten)
            throws LastErrorException;

    void FillConsoleOutputAttribute(Pointer in_hConsoleOutput,
                                    short in_wAttribute, int in_nLength, COORD in_dwWriteCoord,
                                    IntByReference out_lpNumberOfAttrsWritten)
            throws LastErrorException;
    void GetConsoleMode(
            Pointer in_hConsoleOutput,
            IntByReference out_lpMode)
            throws LastErrorException;

    void GetConsoleScreenBufferInfo(
            Pointer in_hConsoleOutput,
            CONSOLE_SCREEN_BUFFER_INFO out_lpConsoleScreenBufferInfo)
            throws LastErrorException;
    void ReadConsoleInput(Pointer in_hConsoleOutput,
                          INPUT_RECORD[] out_lpBuffer, int in_nLength,
                          IntByReference out_lpNumberOfEventsRead) throws LastErrorException;

    void SetConsoleCursorPosition(Pointer in_hConsoleOutput,
                                  COORD in_dwCursorPosition) throws LastErrorException;

    void SetConsoleMode(
            Pointer in_hConsoleOutput,
            int in_dwMode) throws LastErrorException;

    void SetConsoleTextAttribute(Pointer in_hConsoleOutput,
                                 short in_wAttributes)
            throws LastErrorException;

    void SetConsoleTitle(String in_lpConsoleTitle)
            throws LastErrorException;


    void WriteConsoleW(Pointer in_hConsoleOutput, char[] in_lpBuffer, int in_nNumberOfCharsToWrite,
                          IntByReference out_lpNumberOfCharsWritten, Pointer reserved_lpReserved) throws LastErrorException;

    void ScrollConsoleScreenBuffer(Pointer in_hConsoleOutput,
                                   SMALL_RECT in_lpScrollRectangle,
                                   SMALL_RECT in_lpClipRectangle,
                                   COORD in_dwDestinationOrigin,
                                   CHAR_INFO in_lpFill)
            throws LastErrorException;

    class CHAR_INFO {
        public CHAR_INFO() {
        }

        public CHAR_INFO(char c, short attr) {
            uChar = new UnionChar(c);
            Attributes = attr;
        }


        public UnionChar uChar;
        public short Attributes;

    }

    class CONSOLE_CURSOR_INFO {
        public int dwSize;
        public boolean bVisible;

    }

    class CONSOLE_SCREEN_BUFFER_INFO {
        public COORD      dwSize;
        public COORD      dwCursorPosition;
        public short      wAttributes;
        public SMALL_RECT srWindow;
        public COORD      dwMaximumWindowSize;


        public int windowWidth() {
            return this.srWindow.width() + 1;
        }

        public int windowHeight() {
            return this.srWindow.height() + 1;
        }
    }

    class COORD {
        public COORD() {
        }

        public COORD(short X, short Y) {
            this.X = X;
            this.Y = Y;
        }

        public short X;
        public short Y;

    }

    class INPUT_RECORD {
        public static final short KEY_EVENT = 0x0001;
        public static final short MOUSE_EVENT = 0x0002;
        public static final short WINDOW_BUFFER_SIZE_EVENT = 0x0004;
        public static final short MENU_EVENT = 0x0008;
        public static final short FOCUS_EVENT = 0x0010;

        public short EventType;
        public EventUnion Event;

        public static class EventUnion {
            public KEY_EVENT_RECORD KeyEvent;
            public MOUSE_EVENT_RECORD MouseEvent;
            public WINDOW_BUFFER_SIZE_RECORD WindowBufferSizeEvent;
            public MENU_EVENT_RECORD MenuEvent;
            public FOCUS_EVENT_RECORD FocusEvent;
        }


    }

    class KEY_EVENT_RECORD {
        public boolean bKeyDown;
        public short wRepeatCount;
        public short wVirtualKeyCode;
        public short wVirtualScanCode;
        public UnionChar uChar;
        public int dwControlKeyState;

    }

    class MOUSE_EVENT_RECORD {
        public COORD dwMousePosition;
        public int dwButtonState;
        public int dwControlKeyState;
        public int dwEventFlags;

    }

    class WINDOW_BUFFER_SIZE_RECORD {
        public COORD dwSize;

    }

    class MENU_EVENT_RECORD {

        public int dwCommandId;

    }

    class FOCUS_EVENT_RECORD {
        public boolean bSetFocus;

    }

    class SMALL_RECT {
        public SMALL_RECT() {
        }

        public SMALL_RECT(SMALL_RECT org) {
            this(org.Top, org.Left, org.Bottom, org.Right);
        }

        public SMALL_RECT(short Top, short Left, short Bottom, short Right) {
            this.Top = Top;
            this.Left = Left;
            this.Bottom = Bottom;
            this.Right = Right;
        }

        public short Left;
        public short Top;
        public short Right;
        public short Bottom;


        public short width() {
            return (short)(this.Right - this.Left);
        }

        public short height() {
            return (short)(this.Bottom - this.Top);
        }

    }

    class UnionChar {
        public UnionChar() {
        }

        public UnionChar(char c) {
            UnicodeChar = c;
        }


        public void set(char c) {
            UnicodeChar = c;
        }


        public char UnicodeChar;
    }
}
