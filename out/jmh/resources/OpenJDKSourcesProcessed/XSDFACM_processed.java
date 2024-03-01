/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.xs.models;

import com.sun.org.apache.xerces.internal.impl.dtd.models.CMNode;
import com.sun.org.apache.xerces.internal.impl.dtd.models.CMStateSet;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.SubstitutionGroupHandler;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaException;
import com.sun.org.apache.xerces.internal.impl.xs.XSConstraints;
import com.sun.org.apache.xerces.internal.impl.xs.XSElementDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSModelGroupImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSParticleDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSWildcardDecl;
import com.sun.org.apache.xerces.internal.xni.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DFAContentModel is the implementation of XSCMValidator that does
 * all of the non-trivial element content validation. This class does
 * the conversion from the regular expression to the DFA that
 * it then uses in its validation algorithm.
 *
 * @xerces.internal
 *
 * @author Neil Graham, IBM
 * @LastModified: Oct 2017
 */
public class XSDFACM
    implements XSCMValidator {

    private static final boolean DEBUG = false;



    /** Set to true to debug content model validation. */
    private static final boolean DEBUG_VALIDATE_CONTENT = false;


    /**
     * This is the map of unique input symbol elements to indices into
     * each state's per-input symbol transition table entry. This is part
     * of the built DFA information that must be kept around to do the
     * actual validation.  Note tat since either XSElementDecl or XSParticleDecl object
     * can live here, we've got to use an Object.
     */
    private Object fElemMap[] = null;

    /**
     * This is a map of whether the element map contains information
     * related to ANY models.
     */
    private int fElemMapType[] = null;

    /**
     * id of the unique input symbol
     */
    private int fElemMapId[] = null;

    /** The element map size. */
    private int fElemMapSize = 0;

    /**
     * This is an array of booleans, one per state (there are
     * fTransTableSize states in the DFA) that indicates whether that
     * state is a final state.
     */
    private boolean fFinalStateFlags[] = null;

    /**
     * The list of follow positions for each NFA position (i.e. for each
     * non-epsilon leaf node.) This is only used during the building of
     * the DFA, and is let go afterwards.
     */
    private CMStateSet fFollowList[] = null;

    /**
     * This is the head node of our intermediate representation. It is
     * only non-null during the building of the DFA (just so that it
     * does not have to be passed all around.) Once the DFA is built,
     * this is no longer required so its nulled out.
     */
    private CMNode fHeadNode = null;

    /**
     * The count of leaf nodes. This is an important number that set some
     * limits on the sizes of data structures in the DFA process.
     */
    private int fLeafCount = 0;

    /**
     * An array of non-epsilon leaf nodes, which is used during the DFA
     * build operation, then dropped.
     */
    private XSCMLeaf fLeafList[] = null;

    /** Array mapping ANY types to the leaf list. */
    private int fLeafListType[] = null;

    /**
     * This is the transition table that is the main by product of all
     * of the effort here. It is an array of arrays of ints. The first
     * dimension is the number of states we end up with in the DFA. The
     * second dimensions is the number of unique elements in the content
     * model (fElemMapSize). Each entry in the second dimension indicates
     * the new state given that input for the first dimension's start
     * state.
     * <p>
     * The fElemMap array handles mapping from element indexes to
     * positions in the second dimension of the transition table.
     */
    private int fTransTable[][] = null;

    /**
     * Array containing occurence information for looping states
     * which use counters to check minOccurs/maxOccurs.
     */
    private Occurence [] fCountingStates = null;
    static final class Occurence {
        final int minOccurs;
        final int maxOccurs;
        final int elemIndex;
        public Occurence (XSCMRepeatingLeaf leaf, int elemIndex) {
            minOccurs = leaf.getMinOccurs();
            maxOccurs = leaf.getMaxOccurs();
            this.elemIndex = elemIndex;
        }
        public String toString() {
            return "minOccurs=" + minOccurs
                + ";maxOccurs=" +
                ((maxOccurs != SchemaSymbols.OCCURRENCE_UNBOUNDED)
                        ? Integer.toString(maxOccurs) : "unbounded");
        }
    }

    /**
     * The number of valid entries in the transition table, and in the other
     * related tables such as fFinalStateFlags.
     */
    private int fTransTableSize = 0;

    private boolean fIsCompactedForUPA;

    /**
     * Array of counters for all the for elements (or wildcards)
     * of the form a{n,m} where n > 1 and m <= unbounded. Used
     * to count the a's to later check against n and m. Counter
     * set to -1 if element (or wildcard) not optimized by
     * constant space algorithm.
     */
    private int fElemMapCounter[];

    /**
     * Array of lower bounds for all the for elements (or wildcards)
     * of the form a{n,m} where n > 1 and m <= unbounded. This array
     * stores the n's for those elements (or wildcards) for which
     * the constant space algorithm applies (or -1 otherwise).
     */
    private int fElemMapCounterLowerBound[];

    /**
     * Array of upper bounds for all the for elements (or wildcards)
     * of the form a{n,m} where n > 1 and m <= unbounded. This array
     * stores the n's for those elements (or wildcards) for which
     * the constant space algorithm applies, or -1 if algorithm does
     * not apply or m = unbounded.
     */
    private int fElemMapCounterUpperBound[];   



    /**
     * Constructs a DFA content model.
     *
     * @param syntaxTree    The syntax tree of the content model.
     * @param leafCount     The number of leaves.
     *
     * @exception RuntimeException Thrown if DFA can't be built.
     */

   public XSDFACM(CMNode syntaxTree, int leafCount) {

        fLeafCount = leafCount;
        fIsCompactedForUPA = syntaxTree.isCompactedForUPA();



        if(DEBUG_VALIDATE_CONTENT) {
            XSDFACM.time -= System.currentTimeMillis();
        }

        buildDFA(syntaxTree);

        if(DEBUG_VALIDATE_CONTENT) {
            XSDFACM.time += System.currentTimeMillis();
            System.out.println("DFA build: " + XSDFACM.time + "ms");
        }
    }

    private static long time = 0;


    /**
     * check whether the given state is one of the final states
     *
     * @param state       the state to check
     *
     * @return whether it's a final state
     */
    public boolean isFinalState (int state) {
        return (state < 0)? false :
            fFinalStateFlags[state];
    }

    /**
     * one transition only
     *
     * @param curElem The current element's QName
     * @param state stack to store the previous state
     * @param subGroupHandler the substitution group handler
     *
     * @return  null if transition is invalid; otherwise the Object corresponding to the
     *      XSElementDecl or XSWildcardDecl identified.  Also, the
     *      state array will be modified to include the new state; this so that the validator can
     *      store it away.
     *
     * @exception RuntimeException thrown on error
     */
    public Object oneTransition(QName curElem, int[] state, SubstitutionGroupHandler subGroupHandler) {
        int curState = state[0];

        if(curState == XSCMValidator.FIRST_ERROR || curState == XSCMValidator.SUBSEQUENT_ERROR) {
            if(curState == XSCMValidator.FIRST_ERROR)
                state[0] = XSCMValidator.SUBSEQUENT_ERROR;

            return findMatchingDecl(curElem, subGroupHandler);
        }

        int nextState = 0;
        int elemIndex = 0;
        Object matchingDecl = null;

        for (; elemIndex < fElemMapSize; elemIndex++) {
            nextState = fTransTable[curState][elemIndex];
            if (nextState == -1)
                continue;
            int type = fElemMapType[elemIndex] ;
            if (type == XSParticleDecl.PARTICLE_ELEMENT) {
                matchingDecl = subGroupHandler.getMatchingElemDecl(curElem, (XSElementDecl)fElemMap[elemIndex]);
                if (matchingDecl != null) {
                    if (fElemMapCounter[elemIndex] >= 0) {
                        fElemMapCounter[elemIndex]++;
                    }
                    break;
                }
            }
            else if (type == XSParticleDecl.PARTICLE_WILDCARD) {
                if (((XSWildcardDecl)fElemMap[elemIndex]).allowNamespace(curElem.uri)) {
                    matchingDecl = fElemMap[elemIndex];
                    if (fElemMapCounter[elemIndex] >= 0) {
                        fElemMapCounter[elemIndex]++;
                    }
                    break;
                }
            }
        }

        if (elemIndex == fElemMapSize) {
            state[1] = state[0];
            state[0] = XSCMValidator.FIRST_ERROR;
            return findMatchingDecl(curElem, subGroupHandler);
        }

        if (fCountingStates != null) {
            Occurence o = fCountingStates[curState];
            if (o != null) {
                if (curState == nextState) {
                    if (++state[2] > o.maxOccurs &&
                        o.maxOccurs != SchemaSymbols.OCCURRENCE_UNBOUNDED) {
                        return findMatchingDecl(curElem, state, subGroupHandler, elemIndex);
                    }
                }
                else if (state[2] < o.minOccurs) {
                    state[1] = state[0];
                    state[0] = XSCMValidator.FIRST_ERROR;
                    return findMatchingDecl(curElem, subGroupHandler);
                }
                else {
                    o = fCountingStates[nextState];
                    if (o != null) {
                        state[2] = (elemIndex == o.elemIndex) ? 1 : 0;
                    }
                }
            }
            else {
                o = fCountingStates[nextState];
                if (o != null) {
                    state[2] = (elemIndex == o.elemIndex) ? 1 : 0;
                }
            }
        }

        state[0] = nextState;
        return matchingDecl;
    } 

    Object findMatchingDecl(QName curElem, SubstitutionGroupHandler subGroupHandler) {
        Object matchingDecl = null;

        for (int elemIndex = 0; elemIndex < fElemMapSize; elemIndex++) {
            int type = fElemMapType[elemIndex] ;
            if (type == XSParticleDecl.PARTICLE_ELEMENT) {
                matchingDecl = subGroupHandler.getMatchingElemDecl(curElem, (XSElementDecl)fElemMap[elemIndex]);
                if (matchingDecl != null) {
                    return matchingDecl;
                }
            }
            else if (type == XSParticleDecl.PARTICLE_WILDCARD) {
                if(((XSWildcardDecl)fElemMap[elemIndex]).allowNamespace(curElem.uri))
                    return fElemMap[elemIndex];
            }
        }

        return null;
    } 

    Object findMatchingDecl(QName curElem, int[] state, SubstitutionGroupHandler subGroupHandler, int elemIndex) {

        int curState = state[0];
        int nextState = 0;
        Object matchingDecl = null;

        while (++elemIndex < fElemMapSize) {
            nextState = fTransTable[curState][elemIndex];
            if (nextState == -1)
                continue;
            int type = fElemMapType[elemIndex] ;
            if (type == XSParticleDecl.PARTICLE_ELEMENT) {
                matchingDecl = subGroupHandler.getMatchingElemDecl(curElem, (XSElementDecl)fElemMap[elemIndex]);
                if (matchingDecl != null) {
                    break;
                }
            }
            else if (type == XSParticleDecl.PARTICLE_WILDCARD) {
                if (((XSWildcardDecl)fElemMap[elemIndex]).allowNamespace(curElem.uri)) {
                    matchingDecl = fElemMap[elemIndex];
                    break;
                }
            }
        }

        if (elemIndex == fElemMapSize) {
            state[1] = state[0];
            state[0] = XSCMValidator.FIRST_ERROR;
            return findMatchingDecl(curElem, subGroupHandler);
        }

        state[0] = nextState;
        final Occurence o = fCountingStates[nextState];
        if (o != null) {
            state[2] = (elemIndex == o.elemIndex) ? 1 : 0;
        }
        return matchingDecl;
    } 

    public int[] startContentModel() {
        for (int elemIndex = 0; elemIndex < fElemMapSize; elemIndex++) {
            if (fElemMapCounter[elemIndex] != -1) {
                fElemMapCounter[elemIndex] = 0;
            }
        }
        return new int [3];
    } 

    public boolean endContentModel(int[] state) {
        final int curState = state[0];
        if (fFinalStateFlags[curState]) {
            if (fCountingStates != null) {
                Occurence o = fCountingStates[curState];
                if (o != null && state[2] < o.minOccurs) {
                    return false;
                }
            }
            return true;
        }
        return false;
    } 



    /**
     * Builds the internal DFA transition table from the given syntax tree.
     *
     * @param syntaxTree The syntax tree.
     *
     * @exception RuntimeException Thrown if DFA cannot be built.
     */
    private void buildDFA(CMNode syntaxTree) {

        /* MODIFIED (Jan, 2001)
         *
         * Use following rules.
         *   nullable(x+) := nullable(x), first(x+) := first(x),  last(x+) := last(x)
         *   nullable(x?) := true, first(x?) := first(x),  last(x?) := last(x)
         *
         * The same computation of follow as x* is applied to x+
         *
         * The modification drastically reduces computation time of
         * "(a, (b, a+, (c, (b, a+)+, a+, (d,  (c, (b, a+)+, a+)+, (b, a+)+, a+)+)+)+)+"
         */

        int EOCPos = fLeafCount;
        XSCMLeaf nodeEOC = new XSCMLeaf(XSParticleDecl.PARTICLE_ELEMENT, null, -1, fLeafCount++);
        fHeadNode = new XSCMBinOp(
            XSModelGroupImpl.MODELGROUP_SEQUENCE,
            syntaxTree,
            nodeEOC
        );

        fLeafList = new XSCMLeaf[fLeafCount];
        fLeafListType = new int[fLeafCount];
        postTreeBuildInit(fHeadNode);

        fFollowList = new CMStateSet[fLeafCount];
        for (int index = 0; index < fLeafCount; index++)
            fFollowList[index] = new CMStateSet(fLeafCount);
        calcFollowList(fHeadNode);
        fElemMap = new Object[fLeafCount];
        fElemMapType = new int[fLeafCount];
        fElemMapId = new int[fLeafCount];

        fElemMapCounter = new int[fLeafCount];
        fElemMapCounterLowerBound = new int[fLeafCount];
        fElemMapCounterUpperBound = new int[fLeafCount];

        fElemMapSize = 0;
        Occurence [] elemOccurenceMap = null;

        for (int outIndex = 0; outIndex < fLeafCount; outIndex++) {
            fElemMap[outIndex] = null;

            int inIndex = 0;
            final int id = fLeafList[outIndex].getParticleId();
            for (; inIndex < fElemMapSize; inIndex++) {
                if (id == fElemMapId[inIndex])
                    break;
            }

            if (inIndex == fElemMapSize) {
                XSCMLeaf leaf = fLeafList[outIndex];
                fElemMap[fElemMapSize] = leaf.getLeaf();
                if (leaf instanceof XSCMRepeatingLeaf) {
                    if (elemOccurenceMap == null) {
                        elemOccurenceMap = new Occurence[fLeafCount];
                    }
                    elemOccurenceMap[fElemMapSize] = new Occurence((XSCMRepeatingLeaf) leaf, fElemMapSize);
                }

                fElemMapType[fElemMapSize] = fLeafListType[outIndex];
                fElemMapId[fElemMapSize] = id;

                int[] bounds = (int[]) leaf.getUserData();
                if (bounds != null) {
                    fElemMapCounter[fElemMapSize] = 0;
                    fElemMapCounterLowerBound[fElemMapSize] = bounds[0];
                    fElemMapCounterUpperBound[fElemMapSize] = bounds[1];
                } else {
                    fElemMapCounter[fElemMapSize] = -1;
                    fElemMapCounterLowerBound[fElemMapSize] = -1;
                    fElemMapCounterUpperBound[fElemMapSize] = -1;
                }

                fElemMapSize++;
            }
        }

        if (DEBUG) {
            if (fElemMapId[fElemMapSize-1] != -1)
                System.err.println("interal error in DFA: last element is not EOC.");
        }
        fElemMapSize--;

        /***
         * Optimization(Jan, 2001); We sort fLeafList according to
         * elemIndex which is *uniquely* associated to each leaf.
         * We are *assuming* that each element appears in at least one leaf.
         **/

        int[] fLeafSorter = new int[fLeafCount + fElemMapSize];
        int fSortCount = 0;

        for (int elemIndex = 0; elemIndex < fElemMapSize; elemIndex++) {
            final int id = fElemMapId[elemIndex];
            for (int leafIndex = 0; leafIndex < fLeafCount; leafIndex++) {
                if (id == fLeafList[leafIndex].getParticleId())
                    fLeafSorter[fSortCount++] = leafIndex;
            }
            fLeafSorter[fSortCount++] = -1;
        }

        /* Optimization(Jan, 2001) */

        int curArraySize = fLeafCount * 4;
        CMStateSet[] statesToDo = new CMStateSet[curArraySize];
        fFinalStateFlags = new boolean[curArraySize];
        fTransTable = new int[curArraySize][];

        CMStateSet setT = fHeadNode.firstPos();

        int unmarkedState = 0;
        int curState = 0;

        fTransTable[curState] = makeDefStateList();
        statesToDo[curState] = setT;
        curState++;

        /* Optimization(Jan, 2001); This is faster for
         * a large content model such as, "(t001+|t002+|.... |t500+)".
         */

        Map<CMStateSet, Integer> stateTable = new HashMap<>();

        /* Optimization(Jan, 2001) */

        while (unmarkedState < curState) {
            setT = statesToDo[unmarkedState];
            int[] transEntry = fTransTable[unmarkedState];

            fFinalStateFlags[unmarkedState] = setT.getBit(EOCPos);

            unmarkedState++;

            CMStateSet newSet = null;
            /* Optimization(Jan, 2001) */
            int sorterIndex = 0;
            /* Optimization(Jan, 2001) */
            for (int elemIndex = 0; elemIndex < fElemMapSize; elemIndex++) {
                if (newSet == null)
                    newSet = new CMStateSet(fLeafCount);
                else
                    newSet.zeroBits();

                /* Optimization(Jan, 2001) */
                int leafIndex = fLeafSorter[sorterIndex++];

                while (leafIndex != -1) {
                    if (setT.getBit(leafIndex)) {
                        newSet.union(fFollowList[leafIndex]);
                    }

                   leafIndex = fLeafSorter[sorterIndex++];
                }
                /* Optimization(Jan, 2001) */

                if (!newSet.isEmpty()) {

                    /* Optimization(Jan, 2001) */
                    Integer stateObj = stateTable.get(newSet);
                    int stateIndex = (stateObj == null ? curState : stateObj);
                    /* Optimization(Jan, 2001) */

                    if (stateIndex == curState) {
                        statesToDo[curState] = newSet;
                        fTransTable[curState] = makeDefStateList();

                        /* Optimization(Jan, 2001) */
                        stateTable.put(newSet, curState);
                        /* Optimization(Jan, 2001) */

                        curState++;

                        newSet = null;
                    }

                    transEntry[elemIndex] = stateIndex;

                    if (curState == curArraySize) {
                        final int newSize = (int)(curArraySize * 1.5);
                        CMStateSet[] newToDo = new CMStateSet[newSize];
                        boolean[] newFinalFlags = new boolean[newSize];
                        int[][] newTransTable = new int[newSize][];

                        System.arraycopy(statesToDo, 0, newToDo, 0, curArraySize);
                        System.arraycopy(fFinalStateFlags, 0, newFinalFlags, 0, curArraySize);
                        System.arraycopy(fTransTable, 0, newTransTable, 0, curArraySize);

                        curArraySize = newSize;
                        statesToDo = newToDo;
                        fFinalStateFlags = newFinalFlags;
                        fTransTable = newTransTable;
                    }
                }
            }
        }

        if (elemOccurenceMap != null) {
            fCountingStates = new Occurence[curState];
            for (int i = 0; i < curState; ++i) {
                int [] transitions = fTransTable[i];
                for (int j = 0; j < transitions.length; ++j) {
                    if (i == transitions[j]) {
                        fCountingStates[i] = elemOccurenceMap[j];
                        break;
                    }
                }
            }
        }

        if (DEBUG_VALIDATE_CONTENT)
            dumpTree(fHeadNode, 0);
        fHeadNode = null;
        fLeafList = null;
        fFollowList = null;
        fLeafListType = null;
        fElemMapId = null;
    }

    /**
     * Calculates the follow list of the current node.
     *
     * @param nodeCur The curent node.
     *
     * @exception RuntimeException Thrown if follow list cannot be calculated.
     */
    private void calcFollowList(CMNode nodeCur) {
        if (nodeCur.type() == XSModelGroupImpl.MODELGROUP_CHOICE) {
            calcFollowList(((XSCMBinOp)nodeCur).getLeft());
            calcFollowList(((XSCMBinOp)nodeCur).getRight());
        }
         else if (nodeCur.type() == XSModelGroupImpl.MODELGROUP_SEQUENCE) {
            calcFollowList(((XSCMBinOp)nodeCur).getLeft());
            calcFollowList(((XSCMBinOp)nodeCur).getRight());

            final CMStateSet last  = ((XSCMBinOp)nodeCur).getLeft().lastPos();
            final CMStateSet first = ((XSCMBinOp)nodeCur).getRight().firstPos();

            for (int index = 0; index < fLeafCount; index++) {
                if (last.getBit(index))
                    fFollowList[index].union(first);
            }
        }
         else if (nodeCur.type() == XSParticleDecl.PARTICLE_ZERO_OR_MORE
        || nodeCur.type() == XSParticleDecl.PARTICLE_ONE_OR_MORE) {
            calcFollowList(((XSCMUniOp)nodeCur).getChild());

            final CMStateSet first = nodeCur.firstPos();
            final CMStateSet last  = nodeCur.lastPos();

            for (int index = 0; index < fLeafCount; index++) {
                if (last.getBit(index))
                    fFollowList[index].union(first);
            }
        }

        else if (nodeCur.type() == XSParticleDecl.PARTICLE_ZERO_OR_ONE) {
            calcFollowList(((XSCMUniOp)nodeCur).getChild());
        }

    }

    /**
     * Dumps the tree of the current node to standard output.
     *
     * @param nodeCur The current node.
     * @param level   The maximum levels to output.
     *
     * @exception RuntimeException Thrown on error.
     */
    private void dumpTree(CMNode nodeCur, int level) {
        for (int index = 0; index < level; index++)
            System.out.print("   ");

        int type = nodeCur.type();

        switch(type ) {

        case XSModelGroupImpl.MODELGROUP_CHOICE:
        case XSModelGroupImpl.MODELGROUP_SEQUENCE: {
            if (type == XSModelGroupImpl.MODELGROUP_CHOICE)
                System.out.print("Choice Node ");
            else
                System.out.print("Seq Node ");

            if (nodeCur.isNullable())
                System.out.print("Nullable ");

            System.out.print("firstPos=");
            System.out.print(nodeCur.firstPos().toString());
            System.out.print(" lastPos=");
            System.out.println(nodeCur.lastPos().toString());

            dumpTree(((XSCMBinOp)nodeCur).getLeft(), level+1);
            dumpTree(((XSCMBinOp)nodeCur).getRight(), level+1);
            break;
        }
        case XSParticleDecl.PARTICLE_ZERO_OR_MORE:
        case XSParticleDecl.PARTICLE_ONE_OR_MORE:
        case XSParticleDecl.PARTICLE_ZERO_OR_ONE: {
            System.out.print("Rep Node ");

            if (nodeCur.isNullable())
                System.out.print("Nullable ");

            System.out.print("firstPos=");
            System.out.print(nodeCur.firstPos().toString());
            System.out.print(" lastPos=");
            System.out.println(nodeCur.lastPos().toString());

            dumpTree(((XSCMUniOp)nodeCur).getChild(), level+1);
            break;
        }
        case XSParticleDecl.PARTICLE_ELEMENT: {
            System.out.print
            (
                "Leaf: (pos="
                + ((XSCMLeaf)nodeCur).getPosition()
                + "), "
                + "(elemIndex="
                + ((XSCMLeaf)nodeCur).getLeaf()
                + ") "
            );

            if (nodeCur.isNullable())
                System.out.print(" Nullable ");

            System.out.print("firstPos=");
            System.out.print(nodeCur.firstPos().toString());
            System.out.print(" lastPos=");
            System.out.println(nodeCur.lastPos().toString());
            break;
        }
        case XSParticleDecl.PARTICLE_WILDCARD:
              System.out.print("Any Node: ");

            System.out.print("firstPos=");
            System.out.print(nodeCur.firstPos().toString());
            System.out.print(" lastPos=");
            System.out.println(nodeCur.lastPos().toString());
            break;
        default: {
            throw new RuntimeException("ImplementationMessages.VAL_NIICM");
        }
        }

    }


    /**
     * -1 is used to represent bad transitions in the transition table
     * entry for each state. So each entry is initialized to an all -1
     * array. This method creates a new entry and initializes it.
     */
    private int[] makeDefStateList()
    {
        int[] retArray = new int[fElemMapSize];
        for (int index = 0; index < fElemMapSize; index++)
            retArray[index] = -1;
        return retArray;
    }

    /** Post tree build initialization. */
    private void postTreeBuildInit(CMNode nodeCur) throws RuntimeException {
        nodeCur.setMaxStates(fLeafCount);

        XSCMLeaf leaf = null;
        int pos = 0;
        if (nodeCur.type() == XSParticleDecl.PARTICLE_WILDCARD) {
            leaf = (XSCMLeaf)nodeCur;
            pos = leaf.getPosition();
            fLeafList[pos] = leaf;
            fLeafListType[pos] = XSParticleDecl.PARTICLE_WILDCARD;
        }
        else if ((nodeCur.type() == XSModelGroupImpl.MODELGROUP_CHOICE) ||
                 (nodeCur.type() == XSModelGroupImpl.MODELGROUP_SEQUENCE)) {
            postTreeBuildInit(((XSCMBinOp)nodeCur).getLeft());
            postTreeBuildInit(((XSCMBinOp)nodeCur).getRight());
        }
        else if (nodeCur.type() == XSParticleDecl.PARTICLE_ZERO_OR_MORE ||
                 nodeCur.type() == XSParticleDecl.PARTICLE_ONE_OR_MORE ||
                 nodeCur.type() == XSParticleDecl.PARTICLE_ZERO_OR_ONE) {
            postTreeBuildInit(((XSCMUniOp)nodeCur).getChild());
        }
        else if (nodeCur.type() == XSParticleDecl.PARTICLE_ELEMENT) {
            leaf = (XSCMLeaf)nodeCur;
            pos = leaf.getPosition();
            fLeafList[pos] = leaf;
            fLeafListType[pos] = XSParticleDecl.PARTICLE_ELEMENT;
        }
        else {
            throw new RuntimeException("ImplementationMessages.VAL_NIICM");
        }
    }

    /**
     * check whether this content violates UPA constraint.
     *
     * @param subGroupHandler the substitution group handler
     * @return true if this content model contains other or list wildcard
     */
    public boolean checkUniqueParticleAttribution(SubstitutionGroupHandler subGroupHandler) throws XMLSchemaException {
        byte conflictTable[][] = new byte[fElemMapSize][fElemMapSize];

        for (int i = 0; i < fTransTable.length && fTransTable[i] != null; i++) {
            for (int j = 0; j < fElemMapSize; j++) {
                for (int k = j+1; k < fElemMapSize; k++) {
                    if (fTransTable[i][j] != -1 &&
                        fTransTable[i][k] != -1) {
                        if (conflictTable[j][k] == 0) {
                            if (XSConstraints.overlapUPA
                                    (fElemMap[j], fElemMap[k],
                                            subGroupHandler)) {
                                if (fCountingStates != null) {
                                    Occurence o = fCountingStates[i];
                                    if (o != null &&
                                        fTransTable[i][j] == i ^ fTransTable[i][k] == i &&
                                        o.minOccurs == o.maxOccurs) {
                                        conflictTable[j][k] = (byte) -1;
                                        continue;
                                    }
                                }
                                conflictTable[j][k] = (byte) 1;
                            }
                            else {
                                conflictTable[j][k] = (byte) -1;
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < fElemMapSize; i++) {
            for (int j = 0; j < fElemMapSize; j++) {
                if (conflictTable[i][j] == 1) {
                    throw new XMLSchemaException("cos-nonambig", new Object[]{fElemMap[i].toString(),
                                                                              fElemMap[j].toString()});
                }
            }
        }

        for (int i = 0; i < fElemMapSize; i++) {
            if (fElemMapType[i] == XSParticleDecl.PARTICLE_WILDCARD) {
                XSWildcardDecl wildcard = (XSWildcardDecl)fElemMap[i];
                if (wildcard.fType == XSWildcardDecl.NSCONSTRAINT_LIST ||
                    wildcard.fType == XSWildcardDecl.NSCONSTRAINT_NOT) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check which elements are valid to appear at this point. This method also
     * works if the state is in error, in which case it returns what should
     * have been seen.
     *
     * @param state  the current state
     * @return       a list whose entries are instances of
     *               either XSWildcardDecl or XSElementDecl.
     */
    public List<Object> whatCanGoHere(int[] state) {
        int curState = state[0];
        if (curState < 0)
            curState = state[1];
        Occurence o = (fCountingStates != null) ?
                fCountingStates[curState] : null;
        int count = state[2];

        List<Object> ret = new ArrayList<>();
        for (int elemIndex = 0; elemIndex < fElemMapSize; elemIndex++) {
            int nextState = fTransTable[curState][elemIndex];
            if (nextState != -1) {
                if (o != null) {
                    if (curState == nextState) {
                        if (count >= o.maxOccurs &&
                            o.maxOccurs != SchemaSymbols.OCCURRENCE_UNBOUNDED) {
                            continue;
                        }
                    }
                    else if (count < o.minOccurs) {
                        continue;
                    }
                }
                ret.add(fElemMap[elemIndex]);
            }
        }
        return ret;
    }

    /**
     * Used by constant space algorithm for a{n,m} for n > 1 and
     * m <= unbounded. Called by a validator if validation of
     * countent model succeeds after subsuming a{n,m} to a*
     * (or a+) to check the n and m bounds.
     * Returns <code>null</code> if validation of bounds is
     * successful. Returns a list of strings with error info
     * if not. Even entries in list returned are error codes
     * (used to look up properties) and odd entries are parameters
     * to be passed when formatting error message. Each parameter
     * is associated with the error code that preceeds it in
     * the list.
     */
    public List<String> checkMinMaxBounds() {
        List<String> result = null;
        for (int elemIndex = 0; elemIndex < fElemMapSize; elemIndex++) {
            int count = fElemMapCounter[elemIndex];
            if (count == -1) {
                continue;
            }
            final int minOccurs = fElemMapCounterLowerBound[elemIndex];
            final int maxOccurs = fElemMapCounterUpperBound[elemIndex];
            if (count < minOccurs) {
                if (result == null) result = new ArrayList<>();
                result.add("cvc-complex-type.2.4.b");
                result.add("{" + fElemMap[elemIndex] + "}");
            }
            if (maxOccurs != -1 && count > maxOccurs) {
                if (result == null) result = new ArrayList<>();
                result.add("cvc-complex-type.2.4.d.1");
                result.add("{" + fElemMap[elemIndex] + "}");
            }
        }
        return result;
    }

    public int [] occurenceInfo(int[] state) {
        if (fCountingStates != null) {
            int curState = state[0];
            if (curState < 0) {
                curState = state[1];
            }
            Occurence o = fCountingStates[curState];
            if (o != null) {
                int [] occurenceInfo = new int[4];
                occurenceInfo[0] = o.minOccurs;
                occurenceInfo[1] = o.maxOccurs;
                occurenceInfo[2] = state[2];
                occurenceInfo[3] = o.elemIndex;
                return occurenceInfo;
            }
        }
        return null;
    }

    public String getTermName(int termId) {
        Object term = fElemMap[termId];
        return (term != null) ? term.toString() : null;
    }

    public boolean isCompactedForUPA() {
        return fIsCompactedForUPA;
    }
} 
