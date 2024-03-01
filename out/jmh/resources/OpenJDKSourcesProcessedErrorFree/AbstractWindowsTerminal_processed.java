/*
 * Copyright (c) 2002-2019, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https:
 */
package jdk.internal.org.jline.terminal.impl;

import jdk.internal.org.jline.terminal.Attributes;
import jdk.internal.org.jline.terminal.Size;
import jdk.internal.org.jline.utils.Curses;
import jdk.internal.org.jline.utils.InfoCmp;
import jdk.internal.org.jline.utils.Log;
import jdk.internal.org.jline.utils.NonBlocking;
import jdk.internal.org.jline.utils.NonBlockingInputStream;
import jdk.internal.org.jline.utils.NonBlockingPumpReader;
import jdk.internal.org.jline.utils.NonBlockingReader;
import jdk.internal.org.jline.utils.ShutdownHooks;
import jdk.internal.org.jline.utils.Signals;
import jdk.internal.org.jline.utils.WriterOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The AbstractWindowsTerminal is used as the base class for windows terminal.
 * Due to windows limitations, mostly the missing support for ansi sequences,
 * the only way to create a correct terminal is to use the windows api to set
 * character attributes, move the cursor, erasing, etc...
 *
 * UTF-8 support is also lacking in windows and the code page supposed to
 * emulate UTF-8 is a bit broken. In order to work around this broken
 * code page, windows api WriteConsoleW is used directly.  This means that
 * the writer() becomes the primary output, while the output() is bridged
 * to the writer() using a WriterOutputStream wrapper.
 */
public abstract class AbstractWindowsTerminal extends AbstractTerminal {

    public static final String TYPE_WINDOWS = "windows";
    public static final String TYPE_WINDOWS_256_COLOR = "windows-256color";
    public static final String TYPE_WINDOWS_CONEMU = "windows-conemu";
    public static final String TYPE_WINDOWS_VTP = "windows-vtp";

    public static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    private static final int UTF8_CODE_PAGE = 65001;

    protected static final int ENABLE_PROCESSED_INPUT = 0x0001;
    protected static final int ENABLE_LINE_INPUT      = 0x0002;
    protected static final int ENABLE_ECHO_INPUT      = 0x0004;
    protected static final int ENABLE_WINDOW_INPUT    = 0x0008;
    protected static final int ENABLE_MOUSE_INPUT     = 0x0010;
    protected static final int ENABLE_INSERT_MODE     = 0x0020;
    protected static final int ENABLE_QUICK_EDIT_MODE = 0x0040;

    protected final Writer slaveInputPipe;
    protected final InputStream input;
    protected final OutputStream output;
    protected final NonBlockingReader reader;
    protected final PrintWriter writer;
    protected final Map<Signal, Object> nativeHandlers = new HashMap<>();
    protected final ShutdownHooks.Task closer;
    protected final Attributes attributes = new Attributes();
    protected final int originalConsoleMode;

    protected final Object lock = new Object();
    protected boolean paused = true;
    protected Thread pump;

    protected MouseTracking tracking = MouseTracking.Off;
    protected boolean focusTracking = false;
    private volatile boolean closing;

    public AbstractWindowsTerminal(Writer writer, String name, String type, Charset encoding, boolean nativeSignals, SignalHandler signalHandler, Function<InputStream, InputStream> inputStreamWrapper) throws IOException {
        super(name, type, encoding, signalHandler);
        NonBlockingPumpReader reader = NonBlocking.nonBlockingPumpReader();
        this.slaveInputPipe = reader.getWriter();
        this.input = inputStreamWrapper.apply(NonBlocking.nonBlockingStream(reader, encoding()));
        this.reader = NonBlocking.nonBlocking(name, input, encoding());
        this.writer = new PrintWriter(writer);
        this.output = new WriterOutputStream(writer, encoding());
        parseInfoCmp();
        originalConsoleMode = getConsoleMode();
        attributes.setLocalFlag(Attributes.LocalFlag.ISIG, true);
        attributes.setControlChar(Attributes.ControlChar.VINTR, ctrl('C'));
        attributes.setControlChar(Attributes.ControlChar.VEOF,  ctrl('D'));
        attributes.setControlChar(Attributes.ControlChar.VSUSP, ctrl('Z'));
        if (nativeSignals) {
            for (final Signal signal : Signal.values()) {
                if (signalHandler == SignalHandler.SIG_DFL) {
                    nativeHandlers.put(signal, Signals.registerDefault(signal.name()));
                } else {
                    nativeHandlers.put(signal, Signals.register(signal.name(), () -> raise(signal)));
                }
            }
        }
        closer = this::close;
        ShutdownHooks.add(closer);
        if (TYPE_WINDOWS_CONEMU.equals(getType())
                && !Boolean.getBoolean("org.jline.terminal.conemu.disable-activate")) {
            writer.write("\u001b[9999E");
            writer.flush();
        }
    }

    @Override
    public SignalHandler handle(Signal signal, SignalHandler handler) {
        SignalHandler prev = super.handle(signal, handler);
        if (prev != handler) {
            if (handler == SignalHandler.SIG_DFL) {
                Signals.registerDefault(signal.name());
            } else {
                Signals.register(signal.name(), () -> raise(signal));
            }
        }
        return prev;
    }

    public NonBlockingReader reader() {
        return reader;
    }

    public PrintWriter writer() {
        return writer;
    }

    @Override
    public InputStream input() {
        return input;
    }

    @Override
    public OutputStream output() {
        return output;
    }

    public Attributes getAttributes() {
        int mode = getConsoleMode();
        if ((mode & ENABLE_ECHO_INPUT) != 0) {
            attributes.setLocalFlag(Attributes.LocalFlag.ECHO, true);
        }
        if ((mode & ENABLE_LINE_INPUT) != 0) {
            attributes.setLocalFlag(Attributes.LocalFlag.ICANON, true);
        }
        return new Attributes(attributes);
    }

    public void setAttributes(Attributes attr) {
        attributes.copy(attr);
        updateConsoleMode();
    }

    protected void updateConsoleMode() {
        int mode = ENABLE_WINDOW_INPUT;
        if (attributes.getLocalFlag(Attributes.LocalFlag.ECHO)) {
            mode |= ENABLE_ECHO_INPUT;
        }
        if (attributes.getLocalFlag(Attributes.LocalFlag.ICANON)) {
            mode |= ENABLE_LINE_INPUT;
        }
        if (tracking != MouseTracking.Off) {
            mode |= ENABLE_MOUSE_INPUT;
        }
        setConsoleMode(mode);
    }

    protected int ctrl(char key) {
        return (Character.toUpperCase(key) & 0x1f);
    }

    public void setSize(Size size) {
        throw new UnsupportedOperationException("Can not resize windows terminal");
    }

    protected void doClose() throws IOException {
        super.doClose();
        closing = true;
        if (pump != null) {
            pump.interrupt();
        }
        ShutdownHooks.remove(closer);
        for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
            Signals.unregister(entry.getKey().name(), entry.getValue());
        }
        reader.close();
        writer.close();
        setConsoleMode(originalConsoleMode);
    }

    static final int SHIFT_FLAG = 0x01;
    static final int ALT_FLAG =   0x02;
    static final int CTRL_FLAG =  0x04;

    static final int RIGHT_ALT_PRESSED =   0x0001;
    static final int LEFT_ALT_PRESSED =    0x0002;
    static final int RIGHT_CTRL_PRESSED =  0x0004;
    static final int LEFT_CTRL_PRESSED =   0x0008;
    static final int SHIFT_PRESSED =       0x0010;
    static final int NUMLOCK_ON =          0x0020;
    static final int SCROLLLOCK_ON =       0x0040;
    static final int CAPSLOCK_ON =         0x0080;

    protected void processKeyEvent(final boolean isKeyDown, final short virtualKeyCode, char ch, final int controlKeyState) throws IOException {
        final boolean isCtrl = (controlKeyState & (RIGHT_CTRL_PRESSED | LEFT_CTRL_PRESSED)) > 0;
        final boolean isAlt = (controlKeyState & (RIGHT_ALT_PRESSED | LEFT_ALT_PRESSED)) > 0;
        final boolean isShift = (controlKeyState & SHIFT_PRESSED) > 0;
        if (isKeyDown && ch != '\3') {
            if (ch != 0
                    && (controlKeyState & (RIGHT_ALT_PRESSED | LEFT_ALT_PRESSED | RIGHT_CTRL_PRESSED | LEFT_CTRL_PRESSED | SHIFT_PRESSED))
                        == (RIGHT_ALT_PRESSED | LEFT_CTRL_PRESSED)) {
                processInputChar(ch);
            } else {
                final String keySeq = getEscapeSequence(virtualKeyCode, (isCtrl ? CTRL_FLAG : 0) + (isAlt ? ALT_FLAG : 0) + (isShift ? SHIFT_FLAG : 0));
                if (keySeq != null) {
                    for (char c : keySeq.toCharArray()) {
                        processInputChar(c);
                    }
                    return;
                }
                /* uchar value in Windows when CTRL is pressed:
                 * 1). Ctrl +  <0x41 to 0x5e>      : uchar=<keyCode> - 'A' + 1
                 * 2). Ctrl + Backspace(0x08)      : uchar=0x7f
                 * 3). Ctrl + Enter(0x0d)          : uchar=0x0a
                 * 4). Ctrl + Space(0x20)          : uchar=0x20
                 * 5). Ctrl + <Other key>          : uchar=0
                 * 6). Ctrl + Alt + <Any key>      : uchar=0
                */
                if (ch > 0) {
                    if (isAlt) {
                        processInputChar('\033');
                    }
                    if (isCtrl && ch != ' ' && ch != '\n' && ch != 0x7f) {
                        processInputChar((char) (ch == '?' ? 0x7f : Character.toUpperCase(ch) & 0x1f));
                    } else if (isCtrl && ch == '\n') {
                        processInputChar('\033');
                        processInputChar('\r');
                    } else {
                        processInputChar(ch);
                    }
                } else if (isCtrl) { 
                    if (virtualKeyCode >= 'A' && virtualKeyCode <= 'Z') {
                        ch = (char) (virtualKeyCode - 0x40);
                    } else if (virtualKeyCode == 191) { 
                        ch = 127;
                    }
                    if (ch > 0) {
                        if (isAlt) {
                            processInputChar('\033');
                        }
                        processInputChar(ch);
                    }
                }
            }
        } else if (isKeyDown && ch == '\3') {
            processInputChar('\3');
        }
        else {
            if (virtualKeyCode == 0x12 /*VK_MENU ALT key*/ && ch > 0) {
                processInputChar(ch);  
            }
        }
    }

    protected String getEscapeSequence(short keyCode, int keyState) {
        String escapeSequence = null;
        switch (keyCode) {
            case 0x08: 
                escapeSequence = (keyState & ALT_FLAG) > 0 ? "\\E^H" : getRawSequence(InfoCmp.Capability.key_backspace);
                break;
            case 0x09:
                escapeSequence = (keyState & SHIFT_FLAG) > 0 ? getRawSequence(InfoCmp.Capability.key_btab) : null;
                break;
            case 0x21: 
                escapeSequence = getRawSequence(InfoCmp.Capability.key_ppage);
                break;
            case 0x22: 
                escapeSequence = getRawSequence(InfoCmp.Capability.key_npage);
                break;
            case 0x23: 
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dF" : getRawSequence(InfoCmp.Capability.key_end);
                break;
            case 0x24: 
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dH" : getRawSequence(InfoCmp.Capability.key_home);
                break;
            case 0x25: 
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dD" : getRawSequence(InfoCmp.Capability.key_left);
                break;
            case 0x26: 
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dA" : getRawSequence(InfoCmp.Capability.key_up);
                break;
            case 0x27: 
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dC" : getRawSequence(InfoCmp.Capability.key_right);
                break;
            case 0x28: 
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dB" : getRawSequence(InfoCmp.Capability.key_down);
                break;
            case 0x2D: 
                escapeSequence = getRawSequence(InfoCmp.Capability.key_ic);
                break;
            case 0x2E: 
                escapeSequence = getRawSequence(InfoCmp.Capability.key_dc);
                break;
            case 0x70: 
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dP" : getRawSequence(InfoCmp.Capability.key_f1);
                break;
            case 0x71: 
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dQ" : getRawSequence(InfoCmp.Capability.key_f2);
                break;
            case 0x72: 
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dR" : getRawSequence(InfoCmp.Capability.key_f3);
                break;
            case 0x73: 
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dS" : getRawSequence(InfoCmp.Capability.key_f4);
                break;
            case 0x74: 
                escapeSequence = keyState > 0 ? "\\E[15;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f5);
                break;
            case 0x75: 
                escapeSequence = keyState > 0 ? "\\E[17;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f6);
                break;
            case 0x76: 
                escapeSequence = keyState > 0 ? "\\E[18;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f7);
                break;
            case 0x77: 
                escapeSequence = keyState > 0 ? "\\E[19;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f8);
                break;
            case 0x78: 
                escapeSequence = keyState > 0 ? "\\E[20;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f9);
                break;
            case 0x79: 
                escapeSequence = keyState > 0 ? "\\E[21;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f10);
                break;
            case 0x7A: 
                escapeSequence = keyState > 0 ? "\\E[23;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f11);
                break;
            case 0x7B: 
                escapeSequence = keyState > 0 ? "\\E[24;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f12);
                break;
            case 0x5D: 
            case 0x5B: 
            default:
                return null;
        }
        return Curses.tputs(escapeSequence, keyState + 1);
    }

    protected String getRawSequence(InfoCmp.Capability cap) {
        return strings.get(cap);
    }

    @Override
    public boolean hasFocusSupport() {
        return true;
    }

    @Override
    public boolean trackFocus(boolean tracking) {
        focusTracking = tracking;
        return true;
    }

    @Override
    public boolean canPauseResume() {
        return true;
    }

    @Override
    public void pause() {
        synchronized (lock) {
            paused = true;
        }
    }

    @Override
    public void pause(boolean wait) throws InterruptedException {
        Thread p;
        synchronized (lock) {
            paused = true;
            p = pump;
        }
        if (p != null) {
            p.interrupt();
            p.join();
        }
    }

    @Override
    public void resume() {
        synchronized (lock) {
            paused = false;
            if (pump == null) {
                pump = new Thread(this::pump, "WindowsStreamPump");
                pump.setDaemon(true);
                pump.start();
            }
        }
    }

    @Override
    public boolean paused() {
        synchronized (lock) {
            return paused;
        }
    }

    protected void pump() {
        try {
            while (!closing) {
                synchronized (lock) {
                    if (paused) {
                        pump = null;
                        break;
                    }
                }
                if (processConsoleInput()) {
                    slaveInputPipe.flush();
                }
            }
        } catch (IOException e) {
            if (!closing) {
                Log.warn("Error in WindowsStreamPump", e);
                try {
                    close();
                } catch (IOException e1) {
                    Log.warn("Error closing terminal", e);
                }
            }
        } finally {
            synchronized (lock) {
                pump = null;
            }
        }
    }

    public void processInputChar(char c) throws IOException {
        if (attributes.getLocalFlag(Attributes.LocalFlag.ISIG)) {
            if (c == attributes.getControlChar(Attributes.ControlChar.VINTR)) {
                raise(Signal.INT);
                return;
            } else if (c == attributes.getControlChar(Attributes.ControlChar.VQUIT)) {
                raise(Signal.QUIT);
                return;
            } else if (c == attributes.getControlChar(Attributes.ControlChar.VSUSP)) {
                raise(Signal.TSTP);
                return;
            } else if (c == attributes.getControlChar(Attributes.ControlChar.VSTATUS)) {
                raise(Signal.INFO);
            }
        }
        if (c == '\r') {
            if (attributes.getInputFlag(Attributes.InputFlag.IGNCR)) {
                return;
            }
            if (attributes.getInputFlag(Attributes.InputFlag.ICRNL)) {
                c = '\n';
            }
        } else if (c == '\n' && attributes.getInputFlag(Attributes.InputFlag.INLCR)) {
            c = '\r';
        }
        slaveInputPipe.write(c);
    }

    @Override
    public boolean trackMouse(MouseTracking tracking) {
        this.tracking = tracking;
        updateConsoleMode();
        return true;
    }

    protected abstract int getConsoleMode();

    protected abstract void setConsoleMode(int mode);

    /**
     * Read a single input event from the input buffer and process it.
     *
     * @return true if new input was generated from the event
     * @throws IOException if anything wrong happens
     */
    protected abstract boolean processConsoleInput() throws IOException;

}

