/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.dtd.models;

import com.sun.org.apache.xerces.internal.impl.dtd.XMLContentSpec;
import com.sun.org.apache.xerces.internal.xni.QName;
import java.util.HashMap;
import java.util.Map;

/**

 * DFAContentModel is the derivative of ContentModel that does
 * all of the non-trivial element content validation. This class does
 * the conversion from the regular expression to the DFA that
 * it then uses in its validation algorithm.
 * <p>
 * <b>Note:</b> Upstream work insures that this class will never see
 * a content model with PCDATA in it. Any model with PCDATA is 'mixed'
 * and is handled via the MixedContentModel class since mixed models
 * are very constrained in form and easily handled via a special case.
 * This also makes implementation of this class much easier.
 *
 * @xerces.internal
 *
 * @LastModified: Oct 2017
 */
public class DFAContentModel
    implements ContentModelValidator {


    /** Epsilon string. */
    private static String fEpsilonString = "<<CMNODE_EPSILON>>";

    /** End-of-content string. */
    private static String fEOCString = "<<CMNODE_EOC>>";

    /** initializing static members **/
    static {
        fEpsilonString = fEpsilonString.intern();
        fEOCString = fEOCString.intern();
    }


    /** Set to true to debug content model validation. */
    private static final boolean DEBUG_VALIDATE_CONTENT = false;


    /* this is the EquivClassComparator object */

    /**
     * This is the map of unique input symbol elements to indices into
     * each state's per-input symbol transition table entry. This is part
     * of the built DFA information that must be kept around to do the
     * actual validation.
     */
    private QName fElemMap[] = null;

    /**
     * This is a map of whether the element map contains information
     * related to ANY models.
     */
    private int fElemMapType[] = null;

    /** The element map size. */
    private int fElemMapSize = 0;

    /** Boolean to distinguish Schema Mixed Content */
    private boolean fMixed;

    /**
     * The NFA position of the special EOC (end of content) node. This
     * is saved away since it's used during the DFA build.
     */
    private int fEOCPos = 0;


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
    private CMLeaf fLeafList[] = null;

    /** Array mapping ANY types to the leaf list. */
    private int fLeafListType[] = null;


    /**
     * The string pool of our parser session. This is set during construction
     * and kept around.
     */

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
     * The number of valid entries in the transition table, and in the other
     * related tables such as fFinalStateFlags.
     */
    private int fTransTableSize = 0;

    /**
     * Flag that indicates that even though we have a "complicated"
     * content model, it is valid to have no content. In other words,
     * all parts of the content model are optional. For example:
     * <pre>
     *      &lt;!ELEMENT AllOptional (Optional*,NotRequired?)&gt;
     * </pre>
     */
    private boolean fEmptyContentIsValid = false;


    /** Temporary qualified name. */
    private final QName fQName = new QName();




    /**
     * Constructs a DFA content model.
     *
     * @param syntaxTree    The syntax tree of the content model.
     * @param leafCount     The number of leaves.
     * @param mixed
     *
     */
    public DFAContentModel(CMNode syntaxTree, int leafCount, boolean mixed) {
        fLeafCount = leafCount;


        fMixed = mixed;

        buildDFA(syntaxTree);
    }


    /**
     * Check that the specified content is valid according to this
     * content model. This method can also be called to do 'what if'
     * testing of content models just to see if they would be valid.
     * <p>
     * A value of -1 in the children array indicates a PCDATA node. All other
     * indexes will be positive and represent child elements. The count can be
     * zero, since some elements have the EMPTY content model and that must be
     * confirmed.
     *
     * @param children The children of this element.  Each integer is an index within
     *                 the <code>StringPool</code> of the child element name.  An index
     *                 of -1 is used to indicate an occurrence of non-whitespace character
     *                 data.
     * @param offset Offset into the array where the children starts.
     * @param length The number of entries in the <code>children</code> array.
     *
     * @return The value -1 if fully valid, else the 0 based index of the child
     *         that first failed. If the value returned is equal to the number
     *         of children, then the specified children are valid but additional
     *         content is required to reach a valid ending state.
     *
     */
    public int validate(QName[] children, int offset, int length) {

        if (DEBUG_VALIDATE_CONTENT)
            System.out.println("DFAContentModel#validateContent");

        if (length == 0) {
            if (DEBUG_VALIDATE_CONTENT) {
                System.out.println("!!! no children");
                System.out.println("elemMap="+fElemMap);
                for (int i = 0; i < fElemMap.length; i++) {
                    String uri = fElemMap[i].uri;
                    String localpart = fElemMap[i].localpart;

                    System.out.println("fElemMap["+i+"]="+uri+","+
                                       localpart+" ("+
                                       uri+", "+
                                       localpart+
                                       ')');

                }
                System.out.println("EOCIndex="+fEOCString);
            }

            return fEmptyContentIsValid ? -1 : 0;

        } 

        int curState = 0;
        for (int childIndex = 0; childIndex < length; childIndex++)
        {
            final QName curElem = children[offset + childIndex];
            if (fMixed && curElem.localpart == null) {
                continue;
            }

            int elemIndex = 0;
            for (; elemIndex < fElemMapSize; elemIndex++)
            {
                int type = fElemMapType[elemIndex] & 0x0f ;
                if (type == XMLContentSpec.CONTENTSPECNODE_LEAF) {
                    if (fElemMap[elemIndex].rawname == curElem.rawname) {
                        break;
                    }
                }
                else if (type == XMLContentSpec.CONTENTSPECNODE_ANY) {
                    String uri = fElemMap[elemIndex].uri;
                    if (uri == null || uri == curElem.uri) {
                        break;
                    }
                }
                else if (type == XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL) {
                    if (curElem.uri == null) {
                        break;
                    }
                }
                else if (type == XMLContentSpec.CONTENTSPECNODE_ANY_OTHER) {
                    if (fElemMap[elemIndex].uri != curElem.uri) {
                        break;
                    }
                }
            }

            if (elemIndex == fElemMapSize) {
                if (DEBUG_VALIDATE_CONTENT) {
                    System.out.println("!!! didn't find it");

                    System.out.println("curElem : " +curElem );
                    for (int i=0; i<fElemMapSize; i++) {
                        System.out.println("fElemMap["+i+"] = " +fElemMap[i] );
                        System.out.println("fElemMapType["+i+"] = " +fElemMapType[i] );
                    }
                }

                return childIndex;
            }

            curState = fTransTable[curState][elemIndex];

            if (curState == -1) {
                if (DEBUG_VALIDATE_CONTENT)
                    System.out.println("!!! not a legal transition");
                return childIndex;
            }
        }

        if (DEBUG_VALIDATE_CONTENT)
            System.out.println("curState="+curState+", childCount="+length);
        if (!fFinalStateFlags[curState])
            return length;

        return -1;
    } 



    /**
     * Builds the internal DFA transition table from the given syntax tree.
     *
     * @param syntaxTree The syntax tree.
     *
     * @exception CMException Thrown if DFA cannot be built.
     */
    private void buildDFA(CMNode syntaxTree)
    {

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

        fQName.setValues(null, fEOCString, fEOCString, null);
        CMLeaf nodeEOC = new CMLeaf(fQName);
        fHeadNode = new CMBinOp
        (
            XMLContentSpec.CONTENTSPECNODE_SEQ
            , syntaxTree
            , nodeEOC
        );

        fEOCPos = fLeafCount;
        nodeEOC.setPosition(fLeafCount++);

        fLeafList = new CMLeaf[fLeafCount];
        fLeafListType = new int[fLeafCount];
        postTreeBuildInit(fHeadNode, 0);

        fFollowList = new CMStateSet[fLeafCount];
        for (int index = 0; index < fLeafCount; index++)
            fFollowList[index] = new CMStateSet(fLeafCount);
        calcFollowList(fHeadNode);
        fElemMap = new QName[fLeafCount];
        fElemMapType = new int[fLeafCount];
        fElemMapSize = 0;
        for (int outIndex = 0; outIndex < fLeafCount; outIndex++)
        {
            fElemMap[outIndex] = new QName();

            /****
            if ( (fLeafListType[outIndex] & 0x0f) != 0 ) {
                if (fLeafNameTypeVector == null) {
                    fLeafNameTypeVector = new ContentLeafNameTypeVector();
                }
            }
            /****/

            final QName element = fLeafList[outIndex].getElement();

            int inIndex = 0;
            for (; inIndex < fElemMapSize; inIndex++)
            {
                if (fElemMap[inIndex].rawname == element.rawname) {
                    break;
                }
            }

            if (inIndex == fElemMapSize) {
                fElemMap[fElemMapSize].setValues(element);
                fElemMapType[fElemMapSize] = fLeafListType[outIndex];
                fElemMapSize++;
            }
        }
        /*****
        if (fLeafNameTypeVector != null) {
            fLeafNameTypeVector.setValues(fElemMap, fElemMapType, fElemMapSize);
        }

        /***
        * Optimization(Jan, 2001); We sort fLeafList according to
        * elemIndex which is *uniquely* associated to each leaf.
        * We are *assuming* that each element appears in at least one leaf.
        **/

        int[] fLeafSorter = new int[fLeafCount + fElemMapSize];
        int fSortCount = 0;

        for (int elemIndex = 0; elemIndex < fElemMapSize; elemIndex++) {
            for (int leafIndex = 0; leafIndex < fLeafCount; leafIndex++) {
                    final QName leaf = fLeafList[leafIndex].getElement();
                    final QName element = fElemMap[elemIndex];
                    if (leaf.rawname == element.rawname) {
                            fLeafSorter[fSortCount++] = leafIndex;
                    }
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

        while (unmarkedState < curState)
        {
            setT = statesToDo[unmarkedState];
            int[] transEntry = fTransTable[unmarkedState];

            fFinalStateFlags[unmarkedState] = setT.getBit(fEOCPos);

            unmarkedState++;

            CMStateSet newSet = null;
            /* Optimization(Jan, 2001) */
            int sorterIndex = 0;
            /* Optimization(Jan, 2001) */
            for (int elemIndex = 0; elemIndex < fElemMapSize; elemIndex++)
            {
                if (newSet == null)
                    newSet = new CMStateSet(fLeafCount);
                else
                    newSet.zeroBits();

            /* Optimization(Jan, 2001) */
                int leafIndex = fLeafSorter[sorterIndex++];

                while (leafIndex != -1) {
                    if (setT.getBit(leafIndex))
                    {
                                newSet.union(fFollowList[leafIndex]);
                            }

                   leafIndex = fLeafSorter[sorterIndex++];
        }
            /* Optimization(Jan, 2001) */

                if (!newSet.isEmpty())
                {

            /* Optimization(Jan, 2001) */
            Integer stateObj = stateTable.get(newSet);
            int stateIndex = (stateObj == null ? curState : stateObj.intValue());
            /* Optimization(Jan, 2001) */

                    if (stateIndex == curState)
                    {
                        statesToDo[curState] = newSet;
                        fTransTable[curState] = makeDefStateList();

            /* Optimization(Jan, 2001) */
                        stateTable.put(newSet, curState);
            /* Optimization(Jan, 2001) */

                        curState++;

                        newSet = null;
                    }

                    transEntry[elemIndex] = stateIndex;

                    if (curState == curArraySize)
                    {
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

        fEmptyContentIsValid = ((CMBinOp)fHeadNode).getLeft().isNullable();

        if (DEBUG_VALIDATE_CONTENT)
            dumpTree(fHeadNode, 0);
        fHeadNode = null;
        fLeafList = null;
        fFollowList = null;

    }

    /**
     * Calculates the follow list of the current node.
     *
     * @param nodeCur The curent node.
     *
     * @exception CMException Thrown if follow list cannot be calculated.
     */
    private void calcFollowList(CMNode nodeCur)
    {
        if (nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_CHOICE)
        {
            calcFollowList(((CMBinOp)nodeCur).getLeft());
            calcFollowList(((CMBinOp)nodeCur).getRight());
        }
         else if (nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_SEQ)
        {
            calcFollowList(((CMBinOp)nodeCur).getLeft());
            calcFollowList(((CMBinOp)nodeCur).getRight());

            final CMStateSet last  = ((CMBinOp)nodeCur).getLeft().lastPos();
            final CMStateSet first = ((CMBinOp)nodeCur).getRight().firstPos();

            for (int index = 0; index < fLeafCount; index++)
            {
                if (last.getBit(index))
                    fFollowList[index].union(first);
            }
        }
         /***
         else if (nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE)
        {
            calcFollowList(((CMUniOp)nodeCur).getChild());

            final CMStateSet first = nodeCur.firstPos();
            final CMStateSet last  = nodeCur.lastPos();

            for (int index = 0; index < fLeafCount; index++)
            {
                if (last.getBit(index))
                    fFollowList[index].union(first);
            }
        }
         else if ((nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE)
              ||  (nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE))
        {
            throw new RuntimeException("ImplementationMessages.VAL_NIICM");
        }
        /***/
         else if (nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE
            || nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE)
        {
            calcFollowList(((CMUniOp)nodeCur).getChild());

            final CMStateSet first = nodeCur.firstPos();
            final CMStateSet last  = nodeCur.lastPos();

            for (int index = 0; index < fLeafCount; index++)
            {
                if (last.getBit(index))
                    fFollowList[index].union(first);
            }
        }

        else if (nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE) {
            calcFollowList(((CMUniOp)nodeCur).getChild());
        }
         /***/
    }

    /**
     * Dumps the tree of the current node to standard output.
     *
     * @param nodeCur The current node.
     * @param level   The maximum levels to output.
     *
     * @exception CMException Thrown on error.
     */
    private void dumpTree(CMNode nodeCur, int level)
    {
        for (int index = 0; index < level; index++)
            System.out.print("   ");

        int type = nodeCur.type();
        if ((type == XMLContentSpec.CONTENTSPECNODE_CHOICE)
        ||  (type == XMLContentSpec.CONTENTSPECNODE_SEQ))
        {
            if (type == XMLContentSpec.CONTENTSPECNODE_CHOICE)
                System.out.print("Choice Node ");
            else
                System.out.print("Seq Node ");

            if (nodeCur.isNullable())
                System.out.print("Nullable ");

            System.out.print("firstPos=");
            System.out.print(nodeCur.firstPos().toString());
            System.out.print(" lastPos=");
            System.out.println(nodeCur.lastPos().toString());

            dumpTree(((CMBinOp)nodeCur).getLeft(), level+1);
            dumpTree(((CMBinOp)nodeCur).getRight(), level+1);
        }
         else if (nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE)
        {
            System.out.print("Rep Node ");

            if (nodeCur.isNullable())
                System.out.print("Nullable ");

            System.out.print("firstPos=");
            System.out.print(nodeCur.firstPos().toString());
            System.out.print(" lastPos=");
            System.out.println(nodeCur.lastPos().toString());

            dumpTree(((CMUniOp)nodeCur).getChild(), level+1);
        }
         else if (nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_LEAF)
        {
            System.out.print
            (
                "Leaf: (pos="
                + ((CMLeaf)nodeCur).getPosition()
                + "), "
                + ((CMLeaf)nodeCur).getElement()
                + "(elemIndex="
                + ((CMLeaf)nodeCur).getElement()
                + ") "
            );

            if (nodeCur.isNullable())
                System.out.print(" Nullable ");

            System.out.print("firstPos=");
            System.out.print(nodeCur.firstPos().toString());
            System.out.print(" lastPos=");
            System.out.println(nodeCur.lastPos().toString());
        }
         else
        {
            throw new RuntimeException("ImplementationMessages.VAL_NIICM");
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
    private int postTreeBuildInit(CMNode nodeCur, int curIndex)
    {
        nodeCur.setMaxStates(fLeafCount);

        if ((nodeCur.type() & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY ||
            (nodeCur.type() & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL ||
            (nodeCur.type() & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY_OTHER) {
            QName qname = new QName(null, null, null, ((CMAny)nodeCur).getURI());
            fLeafList[curIndex] = new CMLeaf(qname, ((CMAny)nodeCur).getPosition());
            fLeafListType[curIndex] = nodeCur.type();
            curIndex++;
        }
        else if ((nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_CHOICE)
        ||  (nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_SEQ))
        {
            curIndex = postTreeBuildInit(((CMBinOp)nodeCur).getLeft(), curIndex);
            curIndex = postTreeBuildInit(((CMBinOp)nodeCur).getRight(), curIndex);
        }
         else if (nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE
             || nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE
             || nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE)
        {
            curIndex = postTreeBuildInit(((CMUniOp)nodeCur).getChild(), curIndex);
        }
         else if (nodeCur.type() == XMLContentSpec.CONTENTSPECNODE_LEAF)
        {
             final QName node = ((CMLeaf)nodeCur).getElement();
            if (node.localpart != fEpsilonString) {
                fLeafList[curIndex] = (CMLeaf)nodeCur;
                fLeafListType[curIndex] = XMLContentSpec.CONTENTSPECNODE_LEAF;
                curIndex++;
            }
        }
         else
        {
            throw new RuntimeException("ImplementationMessages.VAL_NIICM: type="+nodeCur.type());
        }
        return curIndex;
    }

} 
