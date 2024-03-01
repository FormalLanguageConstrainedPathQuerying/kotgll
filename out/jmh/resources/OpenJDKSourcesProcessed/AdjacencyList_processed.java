/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates. All rights reserved.
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
package sun.security.provider.certpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * An AdjacencyList is used to store the history of certification paths
 * attempted in constructing a path from an initiator to a target. The
 * AdjacencyList is initialized with a <code>List</code> of
 * <code>List</code>s, where each sub-<code>List</code> contains objects of
 * type <code>Vertex</code>. A <code>Vertex</code> describes one possible or
 * actual step in the chain building process, and the associated
 * <code>Certificate</code>. Specifically, a <code>Vertex</code> object
 * contains a <code>Certificate</code> and an index value referencing the
 * next sub-list in the process. If the index value is -1 then this
 * <code>Vertex</code> doesn't continue the attempted build path.
 * <p>
 * Example:
 * <p>
 * Attempted Paths:<ul>
 * <li>C1-&gt;C2-&gt;C3
 * <li>C1-&gt;C4-&gt;C5
 * <li>C1-&gt;C4-&gt;C6
 * <li>C1-&gt;C4-&gt;C7
 * <li>C1-&gt;C8-&gt;C9
 * <li>C1-&gt;C10-&gt;C11
 * </ul>
 * <p>
 * AdjacencyList structure:<ul>
 * <li>AL[0] = C1,1
 * <li>AL[1] = C2,2   =&gt;C4,3   =&gt;C8,4     =&gt;C10,5
 * <li>AL[2] = C3,-1
 * <li>AL[3] = C5,-1  =&gt;C6,-1  =&gt;C7,-1
 * <li>AL[4] = C9,-1
 * <li>AL[5] = C11,-1
 * </ul>
 * <p>
 * The iterator method returns objects of type <code>BuildStep</code>, not
 * objects of type <code>Vertex</code>.
 * A <code>BuildStep</code> contains a <code>Vertex</code> and a result code,
 * accessible via getResult method. There are five result values.
 * <code>POSSIBLE</code> denotes that the current step represents a
 * <code>Certificate</code> that the builder is considering at this point in
 * the build. <code>FOLLOW</code> denotes a <code>Certificate</code> (one of
 * those noted as <code>POSSIBLE</code>) that the builder is using to try
 * extending the chain. <code>BACK</code> represents that a
 * <code>FOLLOW</code> was incorrect, and is being removed from the chain.
 * There is exactly one <code>FOLLOW</code> for each <code>BACK</code>. The
 * values <code>SUCCEED</code> and <code>FAIL</code> mean that we've come to
 * the end of the build process, and there will not be any more entries in
 * the list.
 *
 * @see sun.security.provider.certpath.BuildStep
 * @see sun.security.provider.certpath.Vertex
 *
 * @author  seth proctor
 * @since   1.4
 */
public class AdjacencyList {

    private final ArrayList<BuildStep> mStepList;

    private final List<List<Vertex>> mOrigList;

    /**
     * Constructs a new <code>AdjacencyList</code> based on the specified
     * <code>List</code>. See the example above.
     *
     * @param list a <code>List</code> of <code>List</code>s of
     *             <code>Vertex</code> objects
     */
    public AdjacencyList(List<List<Vertex>> list) {
        mStepList = new ArrayList<>();
        mOrigList = list;
        buildList(list, 0, null);
    }

    /**
     * Gets an <code>Iterator</code> to iterate over the set of
     * <code>BuildStep</code>s in build-order. Any attempts to change
     * the list through the remove method will fail.
     *
     * @return an <code>Iterator</code> over the <code>BuildStep</code>s
     */
    public Iterator<BuildStep> iterator() {
        return Collections.unmodifiableList(mStepList).iterator();
    }

    /**
     * Returns the number of attempted paths (useful for debugging).
     */
    public int numAttemptedPaths() {
        return mOrigList.size();
    }

    /**
     * Recursive, private method which actually builds the step list from
     * the given adjacency list. <code>Follow</code> is the parent BuildStep
     * that we followed to get here, and if it's null, it means that we're
     * at the start.
     */
    private boolean buildList(List<List<Vertex>> theList, int index,
                              BuildStep follow) {

        List<Vertex> l = theList.get(index);

        boolean allNegOne = true;
        boolean allXcps = true;

        for (Vertex v : l) {
            if (v.getIndex() != -1) {
                if (theList.get(v.getIndex()).size() != 0)
                    allNegOne = false;
            } else {
                if (v.getThrowable() == null)
                    allXcps = false;
            }
            mStepList.add(new BuildStep(v, BuildStep.POSSIBLE));
        }

        if (allNegOne) {
            if (allXcps) {
                if (follow == null)
                    mStepList.add(new BuildStep(null, BuildStep.FAIL));
                else
                    mStepList.add(new BuildStep(follow.getVertex(),
                                                BuildStep.BACK));

                return false;
            } else {
                List<Vertex> possibles = new ArrayList<>();
                for (Vertex v : l) {
                    if (v.getThrowable() == null)
                        possibles.add(v);
                }

                if (possibles.size() == 1) {
                    mStepList.add(new BuildStep(possibles.get(0),
                                                BuildStep.SUCCEED));
                } else {
                    mStepList.add(new BuildStep(possibles.get(0),
                                                BuildStep.SUCCEED));
                }

                return true;
            }
        } else {
            boolean success = false;

            for (Vertex v : l) {


                if (v.getIndex() != -1) {
                    if (theList.get(v.getIndex()).size() != 0) {
                        BuildStep bs = new BuildStep(v, BuildStep.FOLLOW);
                        mStepList.add(bs);
                        success = buildList(theList, v.getIndex(), bs);
                    }
                }
            }

            if (success) {
                return true;
            } else {
                if (follow == null)
                    mStepList.add(new BuildStep(null, BuildStep.FAIL));
                else
                    mStepList.add(new BuildStep(follow.getVertex(),
                                                BuildStep.BACK));

                return false;
            }
        }
    }

    /**
     * Prints out a string representation of this AdjacencyList.
     *
     * @return String representation
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[\n");

        int i = 0;
        for (List<Vertex> l : mOrigList) {
            sb.append("LinkedList[").append(i++).append("]:\n");

            for (Vertex step : l) {
                sb.append(step.toString()).append("\n");
            }
        }
        sb.append("]\n");

        return sb.toString();
    }
}
