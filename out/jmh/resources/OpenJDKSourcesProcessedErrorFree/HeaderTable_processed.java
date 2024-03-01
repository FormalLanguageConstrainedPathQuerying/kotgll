/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import jdk.internal.net.http.hpack.HPACK.Logger;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/*
 * Adds reverse lookup to SimpleHeaderTable. Separated from SimpleHeaderTable
 * for performance reasons. Decoder does not need this functionality. On the
 * other hand, Encoder does.
 */
final class HeaderTable extends SimpleHeaderTable {


    /* An immutable map of static header fields' indexes */
    private static final Map<String, Map<String, Integer>> staticIndexes;

    static {
        Map<String, Map<String, Integer>> map
                = HashMap.newHashMap(STATIC_TABLE_LENGTH);
        for (int i = 1; i <= STATIC_TABLE_LENGTH; i++) {
            HeaderField f = staticTable.get(i);
            Map<String, Integer> values
                    = map.computeIfAbsent(f.name, k -> new HashMap<>());
            values.put(f.value, i);
        }
        Map<String, Map<String, Integer>> copy = HashMap.newHashMap(map.size());
        for (Map.Entry<String, Map<String, Integer>> e : map.entrySet()) {
            copy.put(e.getKey(), Map.copyOf(e.getValue()));
        }
        staticIndexes = Map.copyOf(copy);
    }

    private final Map<String, Map<String, Deque<Long>>> map;
    private long counter = 1;

    public HeaderTable(int maxSize, Logger logger) {
        super(maxSize, logger);
        map = new HashMap<>();
    }

    public int indexOf(CharSequence name, CharSequence value) {
        String n = name.toString();
        String v = value.toString();

        Map<String, Integer> values = staticIndexes.get(n);
        if (values != null) {
            Integer idx = values.get(v);
            if (idx != null) {
                return idx;
            }
        }
        int didx = search(n, v);
        if (didx > 0) {
            return STATIC_TABLE_LENGTH + didx;
        } else if (didx < 0) {
            if (values != null) {
                return -values.values().iterator().next(); 
            } else {
                return -STATIC_TABLE_LENGTH + didx;
            }
        } else {
            if (values != null) {
                return -values.values().iterator().next(); 
            } else {
                return 0;
            }
        }
    }

    @Override
    protected void add(HeaderField f) {
        super.add(f);
        Map<String, Deque<Long>> values = map.computeIfAbsent(f.name, k -> new HashMap<>());
        Deque<Long> indexes = values.computeIfAbsent(f.value, k -> new LinkedList<>());
        long counterSnapshot = counter++;
        indexes.add(counterSnapshot);
        assert indexesUniqueAndOrdered(indexes);
    }

    private boolean indexesUniqueAndOrdered(Deque<Long> indexes) {
        long maxIndexSoFar = -1;
        for (long l : indexes) {
            if (l <= maxIndexSoFar) {
                return false;
            } else {
                maxIndexSoFar = l;
            }
        }
        return true;
    }

    int search(String name, String value) {
        Map<String, Deque<Long>> values = map.get(name);
        if (values == null) {
            return 0;
        }
        Deque<Long> indexes = values.get(value);
        if (indexes != null) {
            return (int) (counter - indexes.peekLast());
        } else {
            assert !values.isEmpty();
            Long any = values.values().iterator().next().peekLast(); 
            return -(int) (counter - any);
        }
    }

    @Override
    protected HeaderField remove() {
        HeaderField f = super.remove();
        Map<String, Deque<Long>> values = map.get(f.name);
        Deque<Long> indexes = values.get(f.value);
        Long index = indexes.pollFirst();
        if (indexes.isEmpty()) {
            values.remove(f.value);
        }
        assert index != null;
        if (values.isEmpty()) {
            map.remove(f.name);
        }
        return f;
    }
}
