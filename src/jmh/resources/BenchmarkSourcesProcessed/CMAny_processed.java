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


/**
 * Content model any node.
 *
 * @xerces.internal
 *
 */
public class CMAny
    extends CMNode {


    /**
     * The any content model type. This value is one of the following:
     * XMLContentSpec.CONTENTSPECNODE_ANY,
     * XMLContentSpec.CONTENTSPECNODE_ANY_OTHER,
     * XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL.
     */
    private int fType;

    /**
     * URI of the any content model. This value is set if the type is
     * of the following:
     * XMLContentSpec.CONTENTSPECNODE_ANY,
     * XMLContentSpec.CONTENTSPECNODE_ANY_OTHER.
     */
    private String fURI;

    /**
     * Part of the algorithm to convert a regex directly to a DFA
     * numbers each leaf sequentially. If its -1, that means its an
     * epsilon node. Zero and greater are non-epsilon positions.
     */
    private int fPosition = -1;


    /** Constructs a content model any. */
    public CMAny(int type, String uri, int position)  {
        super(type);

        fType = type;
        fURI = uri;
        fPosition = position;
    }


    final int getType() {
        return fType;
    }

    final String getURI() {
        return fURI;
    }

    final int getPosition()
    {
        return fPosition;
    }

    final void setPosition(int newPosition)
    {
        fPosition = newPosition;
    }



    public boolean isNullable()
    {
        return (fPosition == -1);
    }

    public String toString()
    {
        StringBuilder strRet = new StringBuilder();
        strRet.append("(");
        strRet.append("##any:uri=");
        strRet.append(fURI);
        strRet.append(')');
        if (fPosition >= 0)
        {
            strRet.append
            (
                " (Pos:"
                + fPosition
                + ")"
            );
        }
        return strRet.toString();
    }


    protected void calcFirstPos(CMStateSet toSet)
    {
        if (fPosition == -1)
            toSet.zeroBits();

        else
            toSet.setBit(fPosition);
    }

    protected void calcLastPos(CMStateSet toSet)
    {
        if (fPosition == -1)
            toSet.zeroBits();

        else
            toSet.setBit(fPosition);
    }

} 
