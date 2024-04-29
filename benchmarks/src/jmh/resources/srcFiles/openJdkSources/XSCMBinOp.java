/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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
import com.sun.org.apache.xerces.internal.impl.xs.XSModelGroupImpl;

/**
 *
 * Content model Bin-Op node.
 *
 * @xerces.internal
 *
 * @author Neil Graham, IBM
 */
public class XSCMBinOp extends CMNode {
    public XSCMBinOp(int type, CMNode leftNode, CMNode rightNode)
    {
        super(type);

        if ((type() != XSModelGroupImpl.MODELGROUP_CHOICE)
        &&  (type() != XSModelGroupImpl.MODELGROUP_SEQUENCE)) {
            throw new RuntimeException("ImplementationMessages.VAL_BST");
        }

        fLeftChild = leftNode;
        fRightChild = rightNode;
    }


    final CMNode getLeft() {
        return fLeftChild;
    }

    final CMNode getRight() {
        return fRightChild;
    }


    public boolean isNullable() {
        if (type() == XSModelGroupImpl.MODELGROUP_CHOICE)
            return (fLeftChild.isNullable() || fRightChild.isNullable());
        else if (type() == XSModelGroupImpl.MODELGROUP_SEQUENCE)
            return (fLeftChild.isNullable() && fRightChild.isNullable());
        else
            throw new RuntimeException("ImplementationMessages.VAL_BST");
    }


    protected void calcFirstPos(CMStateSet toSet) {
        if (type() == XSModelGroupImpl.MODELGROUP_CHOICE) {
            toSet.setTo(fLeftChild.firstPos());
            toSet.union(fRightChild.firstPos());
        }
         else if (type() == XSModelGroupImpl.MODELGROUP_SEQUENCE) {
            toSet.setTo(fLeftChild.firstPos());
            if (fLeftChild.isNullable())
                toSet.union(fRightChild.firstPos());
        }
         else {
            throw new RuntimeException("ImplementationMessages.VAL_BST");
        }
    }

    protected void calcLastPos(CMStateSet toSet) {
        if (type() == XSModelGroupImpl.MODELGROUP_CHOICE) {
            toSet.setTo(fLeftChild.lastPos());
            toSet.union(fRightChild.lastPos());
        }
        else if (type() == XSModelGroupImpl.MODELGROUP_SEQUENCE) {
            toSet.setTo(fRightChild.lastPos());
            if (fRightChild.isNullable())
                toSet.union(fLeftChild.lastPos());
        }
        else {
            throw new RuntimeException("ImplementationMessages.VAL_BST");
        }
    }


    private CMNode  fLeftChild;
    private CMNode  fRightChild;
} 
