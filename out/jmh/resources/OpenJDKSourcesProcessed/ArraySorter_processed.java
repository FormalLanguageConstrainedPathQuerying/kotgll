/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package jdk.test.lib.hprof.util;

/**
 * A singleton utility class that sorts an array of objects.
 * <p>
 * Use:
 * <pre>
 *
 *  Stuff[] arr = ...;
 *  ArraySorter.sort(arr, new Comparer() {
 *      public int compare(Object lhs, Object rhs) {
 *          return ((String) lhs).compareTo((String) rhs);
 *      }
 *  });
 * </pre>
 *
 * @author      Bill Foote
 */

public class ArraySorter {

    /**
     * Sort the given array, using c for comparison
    **/
    static public void sort(Object[] arr, Comparer c)  {
        quickSort(arr, c, 0, arr.length-1);
    }


    /**
     * Sort an array of strings, using String.compareTo()
    **/
    static public void sortArrayOfStrings(Object[] arr) {
        sort(arr, new Comparer() {
            public int compare(Object lhs, Object rhs) {
                return ((String) lhs).compareTo((String) rhs);
            }
        });
    }


    static private void swap(Object[] arr, int a, int b) {
        Object tmp = arr[a];
        arr[a] = arr[b];
        arr[b] = tmp;
    }

    static private void quickSort(Object[] arr, Comparer c, int from, int to) {
        if (to <= from)
            return;
        int mid = (from + to) / 2;
        if (mid != from)
            swap(arr, mid, from);
        Object pivot = arr[from];   
        int highestBelowPivot = from - 1;
        int low = from+1;
        int high = to;
        while (low <= high) {
            int cmp = c.compare(arr[low], pivot);
            if (cmp <= 0) {   
                if (cmp < 0) {
                    highestBelowPivot = low;
                }
                low++;
            } else {
                int c2;
                for (;;) {
                    c2 = c.compare(arr[high], pivot);
                    if (c2 > 0) {
                        high--;
                        if (low > high) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (low <= high) {
                    swap(arr, low, high);
                    if (c2 < 0) {
                        highestBelowPivot = low;
                    }
                    low++;
                    high--;
                }
            }
        }
        if (highestBelowPivot > from) {
            swap(arr, from, highestBelowPivot);
            quickSort(arr, c, from, highestBelowPivot-1);
        }
        quickSort(arr, c, high+1, to);
    }
}
