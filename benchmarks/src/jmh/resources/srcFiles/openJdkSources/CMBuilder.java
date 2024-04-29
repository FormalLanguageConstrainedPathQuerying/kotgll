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
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSComplexTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSDeclarationPool;
import com.sun.org.apache.xerces.internal.impl.xs.XSElementDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSModelGroupImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSParticleDecl;

/**
 * This class constructs content models for a given grammar.
 *
 * @xerces.internal
 *
 * @author Elena Litani, IBM
 * @author Sandy Gao, IBM
 *
 * @LastModified: Nov 2017
 */
public class CMBuilder {

    private XSDeclarationPool fDeclPool = null;

    private static XSEmptyCM fEmptyCM = new XSEmptyCM();

    private int fLeafCount;
    private int fParticleCount;
    private CMNodeFactory fNodeFactory ;

    public CMBuilder(CMNodeFactory nodeFactory) {
        fDeclPool = null;
        fNodeFactory = nodeFactory ;
    }

    public void setDeclPool(XSDeclarationPool declPool) {
        fDeclPool = declPool;
    }

    /**
     * Get content model for the a given type
     *
     * @param typeDecl  get content model for which complex type
     * @param forUPA    a flag indicating whether it is for UPA
     * @return          a content model validator
     */
    public XSCMValidator getContentModel(XSComplexTypeDecl typeDecl, boolean forUPA) {

        short contentType = typeDecl.getContentType();
        if (contentType == XSComplexTypeDecl.CONTENTTYPE_SIMPLE ||
            contentType == XSComplexTypeDecl.CONTENTTYPE_EMPTY) {
            return null;
        }

        XSParticleDecl particle = (XSParticleDecl)typeDecl.getParticle();

        if (particle == null)
            return fEmptyCM;

        XSCMValidator cmValidator = null;
        if (particle.fType == XSParticleDecl.PARTICLE_MODELGROUP &&
            ((XSModelGroupImpl)particle.fValue).fCompositor == XSModelGroupImpl.MODELGROUP_ALL) {
            cmValidator = createAllCM(particle);
        }
        else {
            cmValidator = createDFACM(particle, forUPA);
        }

        fNodeFactory.resetNodeCount() ;

        if (cmValidator == null)
            cmValidator = fEmptyCM;

        return cmValidator;
    }

    XSCMValidator createAllCM(XSParticleDecl particle) {
        if (particle.fMaxOccurs == 0)
            return null;

        XSModelGroupImpl group = (XSModelGroupImpl)particle.fValue;
        XSAllCM allContent = new XSAllCM(particle.fMinOccurs == 0, group.fParticleCount);
        for (int i = 0; i < group.fParticleCount; i++) {
            allContent.addElement((XSElementDecl)group.fParticles[i].fValue,
            group.fParticles[i].fMinOccurs == 0);
        }
        return allContent;
    }

    XSCMValidator createDFACM(XSParticleDecl particle, boolean forUPA) {
        fLeafCount = 0;
        fParticleCount = 0;
        CMNode node = useRepeatingLeafNodes(particle) ? buildCompactSyntaxTree(particle) : buildSyntaxTree(particle, forUPA, true);
        if (node == null)
            return null;
        return new XSDFACM(node, fLeafCount);
    }

    private CMNode buildSyntaxTree(XSParticleDecl particle, boolean forUPA, boolean optimize) {

        int maxOccurs = particle.fMaxOccurs;
        int minOccurs = particle.fMinOccurs;

        boolean compactedForUPA = false;
        if (forUPA) {
            if (minOccurs > 1) {
                if (maxOccurs > minOccurs || particle.getMaxOccursUnbounded()) {
                    minOccurs = 1;
                    compactedForUPA = true;
                }
                else { 
                    minOccurs = 2;
                    compactedForUPA = true;
                }
            }
            if (maxOccurs > 1) {
                maxOccurs = 2;
                compactedForUPA = true;
            }
        }

        short type = particle.fType;
        CMNode nodeRet = null;

        if ((type == XSParticleDecl.PARTICLE_WILDCARD) ||
            (type == XSParticleDecl.PARTICLE_ELEMENT)) {
            nodeRet = fNodeFactory.getCMLeafNode(particle.fType, particle.fValue, fParticleCount++, fLeafCount++);
            nodeRet = expandContentModel(nodeRet, minOccurs, maxOccurs, optimize);
            if (nodeRet != null) {
                nodeRet.setIsCompactUPAModel(compactedForUPA);
            }
        }
        else if (type == XSParticleDecl.PARTICLE_MODELGROUP) {
            XSModelGroupImpl group = (XSModelGroupImpl)particle.fValue;
            CMNode temp = null;
            boolean twoChildren = false;
            for (int i = 0; i < group.fParticleCount; i++) {
                temp = buildSyntaxTree(group.fParticles[i],
                        forUPA,
                        optimize &&
                        minOccurs == 1 && maxOccurs == 1 &&
                        (group.fCompositor == XSModelGroupImpl.MODELGROUP_SEQUENCE ||
                         group.fParticleCount == 1));
                if (temp != null) {
                    compactedForUPA |= temp.isCompactedForUPA();
                    if (nodeRet == null) {
                        nodeRet = temp;
                    }
                    else {
                        nodeRet = fNodeFactory.getCMBinOpNode(group.fCompositor, nodeRet, temp);
                        twoChildren = true;
                    }
                }
            }
            if (nodeRet != null) {
                if (group.fCompositor == XSModelGroupImpl.MODELGROUP_CHOICE &&
                    !twoChildren && group.fParticleCount > 1) {
                    nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_ONE, nodeRet);
                }
                nodeRet = expandContentModel(nodeRet, minOccurs, maxOccurs, false);
                nodeRet.setIsCompactUPAModel(compactedForUPA);
            }
        }

        return nodeRet;
    }

    private CMNode expandContentModel(CMNode node,
                                      int minOccurs, int maxOccurs, boolean optimize) {

        CMNode nodeRet = null;

        if (minOccurs==1 && maxOccurs==1) {
            nodeRet = node;
        }
        else if (minOccurs==0 && maxOccurs==1) {
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_ONE, node);
        }
        else if (minOccurs == 0 && maxOccurs==SchemaSymbols.OCCURRENCE_UNBOUNDED) {
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_MORE, node);
        }
        else if (minOccurs == 1 && maxOccurs==SchemaSymbols.OCCURRENCE_UNBOUNDED) {
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ONE_OR_MORE, node);
        }
        else if (optimize && node.type() == XSParticleDecl.PARTICLE_ELEMENT ||
                 node.type() == XSParticleDecl.PARTICLE_WILDCARD) {

            nodeRet = fNodeFactory.getCMUniOpNode(
                    minOccurs == 0 ? XSParticleDecl.PARTICLE_ZERO_OR_MORE
                        : XSParticleDecl.PARTICLE_ONE_OR_MORE, node);
            nodeRet.setUserData(new int[] { minOccurs, maxOccurs });
        }
        else if (maxOccurs == SchemaSymbols.OCCURRENCE_UNBOUNDED) {
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ONE_OR_MORE, node);
            nodeRet = fNodeFactory.getCMBinOpNode(XSModelGroupImpl.MODELGROUP_SEQUENCE,
                                                  multiNodes(node, minOccurs-1, true), nodeRet);
        }
        else {
            if (minOccurs > 0) {
                nodeRet = multiNodes(node, minOccurs, false);
            }
            if (maxOccurs > minOccurs) {
                node = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_ONE, node);
                if (nodeRet == null) {
                    nodeRet = multiNodes(node, maxOccurs-minOccurs, false);
                }
                else {
                    nodeRet = fNodeFactory.getCMBinOpNode(XSModelGroupImpl.MODELGROUP_SEQUENCE,
                                                          nodeRet, multiNodes(node, maxOccurs-minOccurs, true));
                }
            }
        }

        return nodeRet;
    }

    private CMNode multiNodes(CMNode node, int num, boolean copyFirst) {
        if (num == 0) {
            return null;
        }
        if (num == 1) {
            return copyFirst ? copyNode(node) : node;
        }
        int num1 = num/2;
        return fNodeFactory.getCMBinOpNode(XSModelGroupImpl.MODELGROUP_SEQUENCE,
                                           multiNodes(node, num1, copyFirst),
                                           multiNodes(node, num-num1, true));
    }

    private CMNode copyNode(CMNode node) {
        int type = node.type();
        if (type == XSModelGroupImpl.MODELGROUP_CHOICE ||
            type == XSModelGroupImpl.MODELGROUP_SEQUENCE) {
            XSCMBinOp bin = (XSCMBinOp)node;
            node = fNodeFactory.getCMBinOpNode(type, copyNode(bin.getLeft()),
                                 copyNode(bin.getRight()));
        }
        else if (type == XSParticleDecl.PARTICLE_ZERO_OR_MORE ||
                 type == XSParticleDecl.PARTICLE_ONE_OR_MORE ||
                 type == XSParticleDecl.PARTICLE_ZERO_OR_ONE) {
            XSCMUniOp uni = (XSCMUniOp)node;
            node = fNodeFactory.getCMUniOpNode(type, copyNode(uni.getChild()));
        }
        else if (type == XSParticleDecl.PARTICLE_ELEMENT ||
                 type == XSParticleDecl.PARTICLE_WILDCARD) {
            XSCMLeaf leaf = (XSCMLeaf)node;
            node = fNodeFactory.getCMLeafNode(leaf.type(), leaf.getLeaf(), leaf.getParticleId(), fLeafCount++);
        }

        return node;
    }

    private CMNode buildCompactSyntaxTree(XSParticleDecl particle) {
        int maxOccurs = particle.fMaxOccurs;
        int minOccurs = particle.fMinOccurs;
        short type = particle.fType;
        CMNode nodeRet = null;

        if ((type == XSParticleDecl.PARTICLE_WILDCARD) ||
            (type == XSParticleDecl.PARTICLE_ELEMENT)) {
            return buildCompactSyntaxTree2(particle, minOccurs, maxOccurs);
        }
        else if (type == XSParticleDecl.PARTICLE_MODELGROUP) {
            XSModelGroupImpl group = (XSModelGroupImpl)particle.fValue;
            if (group.fParticleCount == 1 && (minOccurs != 1 || maxOccurs != 1)) {
                return buildCompactSyntaxTree2(group.fParticles[0], minOccurs, maxOccurs);
            }
            else {
                CMNode temp = null;

                int count = 0;
                for (int i = 0; i < group.fParticleCount; i++) {
                    temp = buildCompactSyntaxTree(group.fParticles[i]);
                    if (temp != null) {
                        ++count;
                        if (nodeRet == null) {
                            nodeRet = temp;
                        }
                        else {
                            nodeRet = fNodeFactory.getCMBinOpNode(group.fCompositor, nodeRet, temp);
                        }
                    }
                }
                if (nodeRet != null) {
                    if (group.fCompositor == XSModelGroupImpl.MODELGROUP_CHOICE && count < group.fParticleCount) {
                        nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_ONE, nodeRet);
                    }
                }
            }
        }
        return nodeRet;
    }

    private CMNode buildCompactSyntaxTree2(XSParticleDecl particle, int minOccurs, int maxOccurs) {
        CMNode nodeRet = null;
        if (minOccurs == 1 && maxOccurs == 1) {
            nodeRet = fNodeFactory.getCMLeafNode(particle.fType, particle.fValue, fParticleCount++, fLeafCount++);
        }
        else if (minOccurs == 0 && maxOccurs == 1) {
            nodeRet = fNodeFactory.getCMLeafNode(particle.fType, particle.fValue, fParticleCount++, fLeafCount++);
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_ONE, nodeRet);
        }
        else if (minOccurs == 0 && maxOccurs==SchemaSymbols.OCCURRENCE_UNBOUNDED) {
            nodeRet = fNodeFactory.getCMLeafNode(particle.fType, particle.fValue, fParticleCount++, fLeafCount++);
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_MORE, nodeRet);
        }
        else if (minOccurs == 1 && maxOccurs==SchemaSymbols.OCCURRENCE_UNBOUNDED) {
            nodeRet = fNodeFactory.getCMLeafNode(particle.fType, particle.fValue, fParticleCount++, fLeafCount++);
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ONE_OR_MORE, nodeRet);
        }
        else {
            nodeRet = fNodeFactory.getCMRepeatingLeafNode(particle.fType, particle.fValue, minOccurs, maxOccurs, fParticleCount++, fLeafCount++);
            if (minOccurs == 0) {
                nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_MORE, nodeRet);
            }
            else {
                nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ONE_OR_MORE, nodeRet);
            }
        }
        return nodeRet;
    }

    private boolean useRepeatingLeafNodes(XSParticleDecl particle) {
        int maxOccurs = particle.fMaxOccurs;
        int minOccurs = particle.fMinOccurs;
        short type = particle.fType;

        if (type == XSParticleDecl.PARTICLE_MODELGROUP) {
            XSModelGroupImpl group = (XSModelGroupImpl) particle.fValue;
            if (minOccurs != 1 || maxOccurs != 1) {
                if (group.fParticleCount == 1) {
                    XSParticleDecl particle2 = group.fParticles[0];
                    short type2 = particle2.fType;
                    return ((type2 == XSParticleDecl.PARTICLE_ELEMENT ||
                            type2 == XSParticleDecl.PARTICLE_WILDCARD) &&
                            particle2.fMinOccurs == 1 &&
                            particle2.fMaxOccurs == 1);
                }
                return (group.fParticleCount == 0);
            }
            for (int i = 0; i < group.fParticleCount; ++i) {
                if (!useRepeatingLeafNodes(group.fParticles[i])) {
                    return false;
                }
            }
        }
        return true;
    }
}
