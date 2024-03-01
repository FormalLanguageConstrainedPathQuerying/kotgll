/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.cluster.coordination;

import org.elasticsearch.cluster.coordination.LinearizabilityChecker.History;
import org.elasticsearch.cluster.coordination.LinearizabilityChecker.KeyedSpec;
import org.elasticsearch.cluster.coordination.LinearizabilityChecker.LinearizabilityCheckAborted;
import org.elasticsearch.cluster.coordination.LinearizabilityChecker.SequentialSpec;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.test.ESTestCase;

import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;

public class LinearizabilityCheckerTests extends ESTestCase {

    /**
     * Simple specification of a lock that can be exactly locked once. There is no unlocking.
     * Input is always null (and represents lock acquisition), output is a boolean whether lock was acquired.
     */
    final SequentialSpec lockSpec = new SequentialSpec() {

        @Override
        public Object initialState() {
            return false;
        }

        @Override
        public Optional<Object> nextState(Object currentState, Object input, Object output) {
            if (input != null) {
                throw new AssertionError("invalid history: input must be null");
            }
            if (output instanceof Boolean == false) {
                throw new AssertionError("invalid history: output must be boolean");
            }
            if (false == (boolean) currentState) {
                if (false == (boolean) output) {
                    return Optional.empty();
                }
                return Optional.of(true);
            } else if (false == (boolean) output) {
                return Optional.of(currentState);
            }
            return Optional.empty();
        }
    };

    public void testLockConsistent() {
        assertThat(lockSpec.initialState(), equalTo(false));
        assertThat(lockSpec.nextState(false, null, true), equalTo(Optional.of(true)));
        assertThat(lockSpec.nextState(false, null, false), equalTo(Optional.empty()));
        assertThat(lockSpec.nextState(true, null, false), equalTo(Optional.of(true)));
        assertThat(lockSpec.nextState(true, null, true), equalTo(Optional.empty()));
    }

    public void testLockWithLinearizableHistory1() throws LinearizabilityCheckAborted {
        final History history = new History();
        int call0 = history.invoke(null); 
        history.respond(call0, true); 
        int call1 = history.invoke(null); 
        history.respond(call1, false); 
        assertTrue(LinearizabilityChecker.isLinearizable(lockSpec, history));
    }

    public void testLockWithLinearizableHistory2() throws LinearizabilityCheckAborted {
        final History history = new History();
        int call0 = history.invoke(null); 
        int call1 = history.invoke(null); 
        history.respond(call0, false); 
        history.respond(call1, true); 
        assertTrue(LinearizabilityChecker.isLinearizable(lockSpec, history));
    }

    public void testLockWithLinearizableHistory3() throws LinearizabilityCheckAborted {
        final History history = new History();
        int call0 = history.invoke(null); 
        int call1 = history.invoke(null); 
        history.respond(call0, true); 
        history.respond(call1, false); 
        assertTrue(LinearizabilityChecker.isLinearizable(lockSpec, history));
    }

    public void testLockWithNonLinearizableHistory() throws LinearizabilityCheckAborted {
        final History history = new History();
        int call0 = history.invoke(null); 
        history.respond(call0, false); 
        int call1 = history.invoke(null); 
        history.respond(call1, true); 
        assertFalse(LinearizabilityChecker.isLinearizable(lockSpec, history));
    }

    /**
     * Simple specification of a read/write register.
     * Writes are modeled as integer inputs (with corresponding null responses) and
     * reads are modeled as null inputs with integer outputs.
     */
    final SequentialSpec registerSpec = new SequentialSpec() {

        @Override
        public Object initialState() {
            return 0;
        }

        @Override
        public Optional<Object> nextState(Object currentState, Object input, Object output) {
            if ((input == null) == (output == null)) {
                throw new AssertionError("invalid history: exactly one of input or output must be null");
            }
            if (input != null) {
                return Optional.of(input);
            } else if (output.equals(currentState)) {
                return Optional.of(currentState);
            }
            return Optional.empty();
        }
    };

    public void testRegisterConsistent() {
        assertThat(registerSpec.initialState(), equalTo(0));
        assertThat(registerSpec.nextState(7, 42, null), equalTo(Optional.of(42)));
        assertThat(registerSpec.nextState(7, null, 7), equalTo(Optional.of(7)));
        assertThat(registerSpec.nextState(7, null, 42), equalTo(Optional.empty()));
    }

    public void testRegisterWithLinearizableHistory() throws LinearizabilityCheckAborted {
        final History history = new History();
        int call0 = history.invoke(42); 
        int call1 = history.invoke(null); 
        int call2 = history.invoke(null); 
        history.respond(call2, 0); 
        history.respond(call1, 42); 

        expectThrows(AssertionError.class, () -> LinearizabilityChecker.isLinearizable(registerSpec, history));
        assertTrue(LinearizabilityChecker.isLinearizable(registerSpec, history, i -> null));

        history.respond(call0, null); 
        assertTrue(LinearizabilityChecker.isLinearizable(registerSpec, history));
    }

    public void testRegisterHistoryVisualisation() {
        final History history = new History();
        int write0 = history.invoke(42); 
        history.respond(history.invoke(null), 42); 
        history.respond(write0, null); 

        int write1 = history.invoke(24); 
        history.respond(history.invoke(null), 42); 
        history.respond(history.invoke(null), 24); 
        history.respond(write1, null); 

        assertEquals("""
            Partition 0
                                   42   XXX   null  (0)
                                  null   X   42  (1)
                                       24   XXXXX   null  (2)
                                      null   X   42  (3)
                                        null   X   24  (4)
            """, LinearizabilityChecker.visualize(registerSpec, history, o -> { throw new AssertionError("history was complete"); }));
    }

    public void testRegisterWithNonLinearizableHistory() throws LinearizabilityCheckAborted {
        final History history = new History();
        int call0 = history.invoke(42); 
        int call1 = history.invoke(null); 
        history.respond(call1, 42); 
        int call2 = history.invoke(null); 
        history.respond(call2, 0); 

        expectThrows(AssertionError.class, () -> LinearizabilityChecker.isLinearizable(registerSpec, history));
        assertFalse(LinearizabilityChecker.isLinearizable(registerSpec, history, i -> null));

        history.respond(call0, null); 
        assertFalse(LinearizabilityChecker.isLinearizable(registerSpec, history));
    }

    public void testRegisterObservedSequenceOfUpdatesWitLinearizableHistory() throws LinearizabilityCheckAborted {
        final History history = new History();
        int call0 = history.invoke(42); 
        int call1 = history.invoke(43); 
        int call2 = history.invoke(null); 
        history.respond(call2, 42); 
        int call3 = history.invoke(null); 
        history.respond(call3, 43); 
        int call4 = history.invoke(null); 
        history.respond(call4, 43); 

        history.respond(call0, null); 
        history.respond(call1, null); 

        assertTrue(LinearizabilityChecker.isLinearizable(registerSpec, history));
    }

    public void testRegisterObservedSequenceOfUpdatesWithNonLinearizableHistory() throws LinearizabilityCheckAborted {
        final History history = new History();
        int call0 = history.invoke(42); 
        int call1 = history.invoke(43); 
        int call2 = history.invoke(null); 
        history.respond(call2, 42); 
        int call3 = history.invoke(null); 
        history.respond(call3, 43); 
        int call4 = history.invoke(null); 
        history.respond(call4, 42); 

        history.respond(call0, null); 
        history.respond(call1, null); 

        assertFalse(LinearizabilityChecker.isLinearizable(registerSpec, history));
    }

    final SequentialSpec multiRegisterSpec = new KeyedSpec() {

        @Override
        public Object getKey(Object value) {
            return ((Tuple) value).v1();
        }

        @Override
        public Object getValue(Object value) {
            return ((Tuple) value).v2();
        }

        @Override
        public Object initialState() {
            return registerSpec.initialState();
        }

        @Override
        public Optional<Object> nextState(Object currentState, Object input, Object output) {
            return registerSpec.nextState(currentState, input, output);
        }
    };

    public void testMultiRegisterWithLinearizableHistory() throws LinearizabilityCheckAborted {
        final History history = new History();
        int callX0 = history.invoke(new Tuple<>("x", 42)); 
        int callX1 = history.invoke(new Tuple<>("x", null)); 
        int callY0 = history.invoke(new Tuple<>("y", 42)); 
        int callY1 = history.invoke(new Tuple<>("y", null)); 
        int callX2 = history.invoke(new Tuple<>("x", null)); 
        int callY2 = history.invoke(new Tuple<>("y", null)); 
        history.respond(callX2, 0); 
        history.respond(callY2, 0); 
        history.respond(callY1, 42); 
        history.respond(callX1, 42); 

        expectThrows(AssertionError.class, () -> LinearizabilityChecker.isLinearizable(multiRegisterSpec, history));
        assertTrue(LinearizabilityChecker.isLinearizable(multiRegisterSpec, history, i -> null));

        history.respond(callX0, null); 
        history.respond(callY0, null); 
        assertTrue(LinearizabilityChecker.isLinearizable(multiRegisterSpec, history));
    }

    public void testMultiRegisterWithNonLinearizableHistory() throws LinearizabilityCheckAborted {
        final History history = new History();
        int callX0 = history.invoke(new Tuple<>("x", 42)); 
        int callX1 = history.invoke(new Tuple<>("x", null)); 
        int callY0 = history.invoke(new Tuple<>("y", 42)); 
        int callY1 = history.invoke(new Tuple<>("y", null)); 
        int callX2 = history.invoke(new Tuple<>("x", null)); 
        history.respond(callY1, 42); 
        int callY2 = history.invoke(new Tuple<>("y", null)); 
        history.respond(callX2, 0); 
        history.respond(callY2, 0); 
        history.respond(callX1, 42); 

        expectThrows(AssertionError.class, () -> LinearizabilityChecker.isLinearizable(multiRegisterSpec, history));
        assertFalse(LinearizabilityChecker.isLinearizable(multiRegisterSpec, history, i -> null));

        history.respond(callX0, null); 
        history.respond(callY0, null); 
        assertFalse(LinearizabilityChecker.isLinearizable(multiRegisterSpec, history));
    }
}
