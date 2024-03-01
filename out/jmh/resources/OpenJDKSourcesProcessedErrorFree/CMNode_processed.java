/*
 * Copyright (c) 2006, 2009, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A content model node.
 *
 * @xerces.internal
 *
 */
public abstract class CMNode
{
    public CMNode(int type)
    {
        fType = type;
    }


    public abstract boolean isNullable() ;


    public final int type()
    {
        return fType;
    }

    public final CMStateSet firstPos()
    {
        if (fFirstPos == null)
        {
            fFirstPos = new CMStateSet(fMaxStates);
            calcFirstPos(fFirstPos);
        }
        return fFirstPos;
    }

    public final CMStateSet lastPos()
    {
        if (fLastPos == null)
        {
            fLastPos = new CMStateSet(fMaxStates);
            calcLastPos(fLastPos);
        }
        return fLastPos;
    }

    final void setFollowPos(CMStateSet setToAdopt)
    {
        fFollowPos = setToAdopt;
    }

    public final void setMaxStates(int maxStates)
    {
        fMaxStates = maxStates;
    }

    public boolean isCompactedForUPA() {
        return fCompactedForUPA;
    }

    public void setIsCompactUPAModel(boolean value) {
        fCompactedForUPA = value;
    }

    /**
     * Allows the user to set arbitrary data on this content model
     * node. This is used by the a{n,m} optimization that runs
     * in constant space.
     */
    public void setUserData(Object userData) {
        fUserData = userData;
    }

    /**
     * Allows the user to get arbitrary data set on this content
     * model node. This is used by the a{n,m} optimization that runs
     * in constant space.
     */
    public Object getUserData() {
        return fUserData;
    }

    protected abstract void calcFirstPos(CMStateSet toSet) ;

    protected abstract void calcLastPos(CMStateSet toSet) ;


    private final int  fType;
    private CMStateSet fFirstPos   = null;
    private CMStateSet fFollowPos  = null;
    private CMStateSet fLastPos    = null;
    private int        fMaxStates  = -1;
    private Object      fUserData   = null;
    /*
     * This boolean is true if the model represented by the CMNode does not represent
     * the true model from the schema, but has had its min/maxOccurs modified for a
     * more compact representation (for purposes of UPA).
     */
    private boolean fCompactedForUPA = false;
};
