/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PlainIterator<T> implements Iterable<T>, Countable {
    private final List<T> elements;

    private volatile int index;

    @SuppressWarnings("this-escape")
    public PlainIterator(List<T> elements) {
        this.elements = elements;
        reset();
    }

    public void reset() {
        index = 0;
    }

    public int remaining() {
        return elements.size() - index;
    }

    public T nextOrNull() {
        if (index == elements.size()) {
            return null;
        } else {
            return elements.get(index++);
        }
    }

    @Override
    public int size() {
        return elements.size();
    }

    public List<T> asList() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }
}
