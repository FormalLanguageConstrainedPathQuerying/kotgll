/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.security.smartcardio;

import java.util.*;
import java.lang.ref.*;

import javax.smartcardio.*;
import static javax.smartcardio.CardTerminals.State.*;

import static sun.security.smartcardio.PCSC.*;

/**
 * TerminalFactorySpi implementation class.
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 */
final class PCSCTerminals extends CardTerminals {

    private static long contextId;

    private Map<String,ReaderState> stateMap;

    PCSCTerminals() {
    }

    static synchronized void initContext() throws PCSCException {
        if (contextId == 0) {
            contextId = SCardEstablishContext(SCARD_SCOPE_USER);
        }
    }

    private static final Map<String,Reference<TerminalImpl>> terminals
        = new HashMap<String,Reference<TerminalImpl>>();

    private static synchronized TerminalImpl implGetTerminal(String name) {
        Reference<TerminalImpl> ref = terminals.get(name);
        TerminalImpl terminal = (ref != null) ? ref.get() : null;
        if (terminal != null) {
            return terminal;
        }
        terminal = new TerminalImpl(contextId, name);
        terminals.put(name, new WeakReference<TerminalImpl>(terminal));
        return terminal;

    }

    public synchronized List<CardTerminal> list(State state) throws CardException {
        if (state == null) {
            throw new NullPointerException();
        }
        try {
            String[] readerNames = SCardListReaders(contextId);
            List<CardTerminal> list = new ArrayList<CardTerminal>(readerNames.length);
            if (stateMap == null) {
                if (state == CARD_INSERTION) {
                    state = CARD_PRESENT;
                } else if (state == CARD_REMOVAL) {
                    state = CARD_ABSENT;
                }
            }
            for (String readerName : readerNames) {
                CardTerminal terminal = implGetTerminal(readerName);
                ReaderState readerState;
                switch (state) {
                case ALL:
                    list.add(terminal);
                    break;
                case CARD_PRESENT:
                    if (terminal.isCardPresent()) {
                        list.add(terminal);
                    }
                    break;
                case CARD_ABSENT:
                    if (terminal.isCardPresent() == false) {
                        list.add(terminal);
                    }
                    break;
                case CARD_INSERTION:
                    readerState = stateMap.get(readerName);
                    if ((readerState != null) && readerState.isInsertion()) {
                        list.add(terminal);
                    }
                    break;
                case CARD_REMOVAL:
                    readerState = stateMap.get(readerName);
                    if ((readerState != null) && readerState.isRemoval()) {
                        list.add(terminal);
                    }
                    break;
                default:
                    throw new CardException("Unknown state: " + state);
                }
            }
            return Collections.unmodifiableList(list);
        } catch (PCSCException e) {
            throw new CardException("list() failed", e);
        }
    }

    private static class ReaderState {
        private int current, previous;
        ReaderState() {
            current = SCARD_STATE_UNAWARE;
            previous = SCARD_STATE_UNAWARE;
        }
        int get() {
            return current;
        }
        void update(int newState) {
            previous = current;
            current = newState;
        }
        boolean isInsertion() {
            return !present(previous) && present(current);
        }
        boolean isRemoval() {
            return present(previous) && !present(current);
        }
        static boolean present(int state) {
            return (state & SCARD_STATE_PRESENT) != 0;
        }
    }

    public synchronized boolean waitForChange(long timeout) throws CardException {
        if (timeout < 0) {
            throw new IllegalArgumentException
                ("Timeout must not be negative: " + timeout);
        }
        if (stateMap == null) {
            stateMap = new HashMap<String,ReaderState>();
            waitForChange(0);
        }
        if (timeout == 0) {
            timeout = TIMEOUT_INFINITE;
        }
        try {
            String[] readerNames = SCardListReaders(contextId);
            int n = readerNames.length;
            if (n == 0) {
                throw new IllegalStateException("No terminals available");
            }
            int[] status = new int[n];
            ReaderState[] readerStates = new ReaderState[n];
            for (int i = 0; i < readerNames.length; i++) {
                String name = readerNames[i];
                ReaderState state = stateMap.get(name);
                if (state == null) {
                    state = new ReaderState();
                }
                readerStates[i] = state;
                status[i] = state.get();
            }
            status = SCardGetStatusChange(contextId, timeout, status, readerNames);
            stateMap.clear(); 
            for (int i = 0; i < n; i++) {
                ReaderState state = readerStates[i];
                state.update(status[i]);
                stateMap.put(readerNames[i], state);
            }
            return true;
        } catch (PCSCException e) {
            if (e.code == SCARD_E_TIMEOUT) {
                return false;
            } else {
                throw new CardException("waitForChange() failed", e);
            }
        }
    }

    static List<CardTerminal> waitForCards(List<? extends CardTerminal> terminals,
            long timeout, boolean wantPresent) throws CardException {

        long thisTimeout;
        if (timeout == 0) {
            timeout = TIMEOUT_INFINITE;
            thisTimeout = TIMEOUT_INFINITE;
        } else {
            thisTimeout = 0;
        }

        String[] names = new String[terminals.size()];
        int i = 0;
        for (CardTerminal terminal : terminals) {
            if (terminal instanceof TerminalImpl == false) {
                throw new IllegalArgumentException
                    ("Invalid terminal type: " + terminal.getClass().getName());
            }
            TerminalImpl impl = (TerminalImpl)terminal;
            names[i++] = impl.name;
        }

        int[] status = new int[names.length];
        Arrays.fill(status, SCARD_STATE_UNAWARE);

        try {
            while (true) {
                status = SCardGetStatusChange(contextId, thisTimeout, status, names);
                thisTimeout = timeout;

                List<CardTerminal> results = null;
                for (i = 0; i < names.length; i++) {
                    boolean nowPresent = (status[i] & SCARD_STATE_PRESENT) != 0;
                    if (nowPresent == wantPresent) {
                        if (results == null) {
                            results = new ArrayList<CardTerminal>();
                        }
                        results.add(implGetTerminal(names[i]));
                    }
                }

                if (results != null) {
                    return Collections.unmodifiableList(results);
                }
            }
        } catch (PCSCException e) {
            if (e.code == SCARD_E_TIMEOUT) {
                return Collections.emptyList();
            } else {
                throw new CardException("waitForCard() failed", e);
            }
        }
    }

}
