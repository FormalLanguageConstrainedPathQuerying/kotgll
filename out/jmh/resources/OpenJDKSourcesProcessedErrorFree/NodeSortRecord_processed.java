/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.xsltc.dom;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.utils.StringComparable;
import java.text.Collator;
import java.util.Locale;

/**
 * Base class for sort records containing application specific sort keys
 *
 * @LastModified: May 2019
 */
public abstract class NodeSortRecord {
    public static final int COMPARE_STRING     = 0;
    public static final int COMPARE_NUMERIC    = 1;

    public static final int COMPARE_ASCENDING  = 0;
    public static final int COMPARE_DESCENDING = 1;

    /**
     * A reference to a collator. May be updated by subclass if the stylesheet
     * specifies a different language (will be updated iff _locale is updated).
     * @deprecated This field continues to exist for binary compatibility.
     *             New code should not refer to it.
     */
    @Deprecated
    private static final Collator DEFAULT_COLLATOR = Collator.getInstance();

    /**
     * A reference to the first Collator
     * @deprecated This field continues to exist for binary compatibility.
     *             New code should not refer to it.
     */
    @Deprecated
    protected Collator _collator = DEFAULT_COLLATOR;
    protected Collator[] _collators;

    /**
     * A locale field that might be set by an instance of a subclass.
     * @deprecated This field continues to exist for binary compatibility.
     *             New code should not refer to it.
     */
    @Deprecated
    protected Locale _locale;

    protected SortSettings _settings;

    private DOM    _dom = null;
    private int    _node;           
    private int    _last = 0;       
    private int    _scanned = 0;    

    private Object[] _values; 

    /**
     * This constructor is run by a call to ClassLoader in the
     * makeNodeSortRecord method in the NodeSortRecordFactory class. Since we
     * cannot pass any parameters to the constructor in that case we just set
     * the default values here and wait for new values through initialize().
     */
    public NodeSortRecord(int node) {
        _node = node;
    }

    public NodeSortRecord() {
        this(0);
    }

    /**
     * This method allows the caller to set the values that could not be passed
     * to the default constructor.
     */
    public final void initialize(int node, int last, DOM dom,
         SortSettings settings)
        throws TransletException
    {
        _dom = dom;
        _node = node;
        _last = last;
        _settings = settings;

        int levels = settings.getSortOrders().length;
        _values = new Object[levels];

        _collators = settings.getCollators();
        _collator = _collators[0];
    }

    /**
     * Returns the node for this sort object
     */
    public final int getNode() {
        return _node;
    }

    /**
     *
     */
    public final int compareDocOrder(NodeSortRecord other) {
        return _node - other._node;
    }

    /**
     * Get the string or numeric value of a specific level key for this sort
     * element. The value is extracted from the DOM if it is not already in
     * our sort key vector.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private final Comparable stringValue(int level) {
        if (_scanned <= level) {
            AbstractTranslet translet = _settings.getTranslet();
            Locale[] locales = _settings.getLocales();
            String[] caseOrder = _settings.getCaseOrders();

            final String str = extractValueFromDOM(_dom, _node, level,
                                                   translet, _last);
            final Comparable key = StringComparable.getComparator(
                    str, locales[level], _collators[level], caseOrder[level]);
            _values[_scanned++] = key;
            return(key);
        }
        return((Comparable)_values[level]);
  }

    private final Double numericValue(int level) {
        if (_scanned <= level) {
            AbstractTranslet translet = _settings.getTranslet();

            final String str = extractValueFromDOM(_dom, _node, level,
                                                   translet, _last);
            Double num;
            try {
                num = Double.parseDouble(str);
            }
            catch (NumberFormatException e) {
                num = Double.NEGATIVE_INFINITY;
            }
            _values[_scanned++] = num;
            return(num);
        }
        return((Double)_values[level]);
    }

    /**
     * Compare this sort element to another. The first level is checked first,
     * and we proceed to the next level only if the first level keys are
     * identical (and so the key values may not even be extracted from the DOM)
     *
     * !!!!MUST OPTIMISE - THIS IS REALLY, REALLY SLOW!!!!
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public int compareTo(NodeSortRecord other) {
        int cmp, level;
        int[] sortOrder = _settings.getSortOrders();
        int levels = _settings.getSortOrders().length;
        int[] compareTypes = _settings.getTypes();

        for (level = 0; level < levels; level++) {
            if (compareTypes[level] == COMPARE_NUMERIC) {
                final Double our = numericValue(level);
                final Double their = other.numericValue(level);
                cmp = our.compareTo(their);
            }
            else {
                final Comparable our = stringValue(level);
                final Comparable their = other.stringValue(level);
                cmp = our.compareTo(their);
            }

            if (cmp != 0) {
                return sortOrder[level] == COMPARE_DESCENDING ? 0 - cmp : cmp;
            }
        }
        return(_node - other._node);
    }

    /**
     * Returns the array of Collators used for text comparisons in this object.
     * May be overridden by inheriting classes
     */
    public Collator[] getCollator() {
        return _collators;
    }

    /**
     * Extract the sort value for a level of this key.
     */
    public abstract String extractValueFromDOM(DOM dom, int current, int level,
                                               AbstractTranslet translet,
                                               int last);

}
