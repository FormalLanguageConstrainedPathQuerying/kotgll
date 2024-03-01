/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.watcher.condition;

import org.elasticsearch.xpack.core.watcher.support.WatcherDateTimeUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

public class LenientCompare {
    @SuppressWarnings("unchecked")
    public static Integer compare(Object v1, Object v2) {
        if (Objects.equals(v1, v2)) {
            return 0;
        }
        if (v1 == null || v2 == null) {
            return null;
        }

        if (v1.equals(Double.NaN) || v2.equals(Double.NaN) || v1.equals(Float.NaN) || v2.equals(Float.NaN)) {
            return null;
        }

        if (v2 instanceof Number) {
            if ((v1 instanceof Number) == false) {
                try {
                    v1 = Double.valueOf(String.valueOf(v1));
                } catch (NumberFormatException nfe) {
                    return null;
                }
            }
            return ((Number) v1).doubleValue() > ((Number) v2).doubleValue() ? 1
                : ((Number) v1).doubleValue() < ((Number) v2).doubleValue() ? -1
                : 0;
        }

        if (v2 instanceof String) {
            v1 = String.valueOf(v1);
            return ((String) v1).compareTo((String) v2);
        }

        if (v2 instanceof ZonedDateTime) {
            if (v1 instanceof ZonedDateTime) {
                return ((ZonedDateTime) v1).compareTo((ZonedDateTime) v2);
            }
            if (v1 instanceof String) {
                try {
                    v1 = WatcherDateTimeUtils.parseDate((String) v1);
                } catch (Exception e) {
                    return null;
                }
            } else if (v1 instanceof Number) {
                v1 = Instant.ofEpochMilli(((Number) v1).longValue()).atZone(ZoneOffset.UTC);
            } else {
                return null;
            }
            return ((ZonedDateTime) v1).compareTo((ZonedDateTime) v2);
        }

        if (v1.getClass() != v2.getClass() || Comparable.class.isAssignableFrom(v1.getClass())) {
            return null;
        }

        try {
            return ((Comparable) v1).compareTo(v2);
        } catch (Exception e) {
            return null;
        }
    }

}
