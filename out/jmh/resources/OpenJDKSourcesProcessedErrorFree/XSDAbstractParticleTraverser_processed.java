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

package com.sun.org.apache.xerces.internal.impl.xs.traversers;

import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSAnnotationImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSModelGroupImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSParticleDecl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XInt;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.xs.XSObject;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import org.w3c.dom.Element;

/**
 * @xerces.internal
 *
 * @author Elena Litani, IBM
 * @author Sandy Gao, IBM
 */
abstract class XSDAbstractParticleTraverser extends XSDAbstractTraverser {

    XSDAbstractParticleTraverser (XSDHandler handler,
            XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }

    /**
     *
     * Traverse the "All" declaration
     *
     * &lt;all
     *   id = ID
     *   maxOccurs = 1 : 1
     *   minOccurs = (0 | 1) : 1&gt;
     *   Content: (annotation? , element*)
     * &lt;/all&gt;
     **/
    XSParticleDecl traverseAll(Element allDecl,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar,
            int allContextFlags,
            XSObject parent) {


        Object[] attrValues = fAttrChecker.checkAttributes(allDecl, false, schemaDoc);

        Element child = DOMUtil.getFirstChildElement(allDecl);

        XSAnnotationImpl annotation = null;
        if (child !=null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
            annotation = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
            child = DOMUtil.getNextSiblingElement(child);
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(allDecl);
            if (text != null) {
                annotation = traverseSyntheticAnnotation(allDecl, text, attrValues, false, schemaDoc);
            }
        }
        String childName = null;
        XSParticleDecl particle;
        fPArray.pushContext();

        for (; child != null; child = DOMUtil.getNextSiblingElement(child)) {

            particle = null;
            childName = DOMUtil.getLocalName(child);

            if (childName.equals(SchemaSymbols.ELT_ELEMENT)) {
                particle = fSchemaHandler.fElementTraverser.traverseLocal(child, schemaDoc, grammar, PROCESSING_ALL_EL, parent);
            }
            else {
                Object[] args = {"all", "(annotation?, element*)", DOMUtil.getLocalName(child)};
                reportSchemaError("s4s-elt-must-match.1", args, child);
            }

            if (particle != null)
                fPArray.addParticle(particle);
        }

        particle = null;
        XInt minAtt = (XInt)attrValues[XSAttributeChecker.ATTIDX_MINOCCURS];
        XInt maxAtt = (XInt)attrValues[XSAttributeChecker.ATTIDX_MAXOCCURS];
        Long defaultVals = (Long)attrValues[XSAttributeChecker.ATTIDX_FROMDEFAULT];

        XSModelGroupImpl group = new XSModelGroupImpl();
        group.fCompositor = XSModelGroupImpl.MODELGROUP_ALL;
        group.fParticleCount = fPArray.getParticleCount();
        group.fParticles = fPArray.popContext();
        XSObjectList annotations;
        if (annotation != null) {
            annotations = new XSObjectListImpl();
            ((XSObjectListImpl)annotations).addXSObject (annotation);
        } else {
            annotations = XSObjectListImpl.EMPTY_LIST;
        }
        group.fAnnotations = annotations;
        particle = new XSParticleDecl();
        particle.fType = XSParticleDecl.PARTICLE_MODELGROUP;
        particle.fMinOccurs = minAtt.intValue();
        particle.fMaxOccurs = maxAtt.intValue();
        particle.fValue = group;
        particle.fAnnotations = annotations;

        particle = checkOccurrences(particle,
                SchemaSymbols.ELT_ALL,
                (Element)allDecl.getParentNode(),
                allContextFlags,
                defaultVals.longValue());
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);

        return particle;
    }

    /**
     * Traverse the Sequence declaration
     *
     * <sequence
     *   id = ID
     *   maxOccurs = string
     *   minOccurs = nonNegativeInteger>
     *   Content: (annotation? , (element | group | choice | sequence | any)*)
     * </sequence>
     *
     * @param seqDecl
     * @param schemaDoc
     * @param grammar
     * @return
     */
    XSParticleDecl traverseSequence(Element seqDecl,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar,
            int allContextFlags,
            XSObject parent) {

        return traverseSeqChoice(seqDecl, schemaDoc, grammar, allContextFlags, false, parent);
    }

    /**
     * Traverse the Choice declaration
     *
     * <choice
     *   id = ID
     *   maxOccurs = string
     *   minOccurs = nonNegativeInteger>
     *   Content: (annotation? , (element | group | choice | sequence | any)*)
     * </choice>
     *
     * @param choiceDecl
     * @param schemaDoc
     * @param grammar
     * @return
     */
    XSParticleDecl traverseChoice(Element choiceDecl,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar,
            int allContextFlags,
            XSObject parent) {

        return traverseSeqChoice (choiceDecl, schemaDoc, grammar, allContextFlags, true, parent);
    }

    /**
     * Common traversal for <choice> and <sequence>
     *
     * @param decl
     * @param schemaDoc
     * @param grammar
     * @param choice    If traversing <choice> this parameter is true.
     * @return
     */
    private XSParticleDecl traverseSeqChoice(Element decl,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar,
            int allContextFlags,
            boolean choice,
            XSObject parent) {

        Object[] attrValues = fAttrChecker.checkAttributes(decl, false, schemaDoc);

        Element child = DOMUtil.getFirstChildElement(decl);
        XSAnnotationImpl annotation = null;
        if (child !=null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
            annotation = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
            child = DOMUtil.getNextSiblingElement(child);
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(decl);
            if (text != null) {
                annotation = traverseSyntheticAnnotation(decl, text, attrValues, false, schemaDoc);
            }
        }

        String childName = null;
        XSParticleDecl particle;
        fPArray.pushContext();

        for (;child != null;child = DOMUtil.getNextSiblingElement(child)) {

            particle = null;

            childName = DOMUtil.getLocalName(child);
            if (childName.equals(SchemaSymbols.ELT_ELEMENT)) {
                particle = fSchemaHandler.fElementTraverser.traverseLocal(child, schemaDoc, grammar, NOT_ALL_CONTEXT, parent);
            }
            else if (childName.equals(SchemaSymbols.ELT_GROUP)) {
                particle = fSchemaHandler.fGroupTraverser.traverseLocal(child, schemaDoc, grammar);

                if (hasAllContent(particle)) {
                    particle = null;
                    reportSchemaError("cos-all-limited.1.2", null, child);
                }

            }
            else if (childName.equals(SchemaSymbols.ELT_CHOICE)) {
                particle = traverseChoice(child, schemaDoc, grammar, NOT_ALL_CONTEXT, parent);
            }
            else if (childName.equals(SchemaSymbols.ELT_SEQUENCE)) {
                particle = traverseSequence(child, schemaDoc, grammar, NOT_ALL_CONTEXT, parent);
            }
            else if (childName.equals(SchemaSymbols.ELT_ANY)) {
                particle = fSchemaHandler.fWildCardTraverser.traverseAny(child, schemaDoc, grammar);
            }
            else {
                Object [] args;
                if (choice) {
                    args = new Object[]{"choice", "(annotation?, (element | group | choice | sequence | any)*)", DOMUtil.getLocalName(child)};
                }
                else {
                    args = new Object[]{"sequence", "(annotation?, (element | group | choice | sequence | any)*)", DOMUtil.getLocalName(child)};
                }
                reportSchemaError("s4s-elt-must-match.1", args, child);
            }

            if (particle != null)
                fPArray.addParticle(particle);
        }

        particle = null;

        XInt minAtt = (XInt)attrValues[XSAttributeChecker.ATTIDX_MINOCCURS];
        XInt maxAtt = (XInt)attrValues[XSAttributeChecker.ATTIDX_MAXOCCURS];
        Long defaultVals = (Long)attrValues[XSAttributeChecker.ATTIDX_FROMDEFAULT];

        XSModelGroupImpl group = new XSModelGroupImpl();
        group.fCompositor = choice ? XSModelGroupImpl.MODELGROUP_CHOICE : XSModelGroupImpl.MODELGROUP_SEQUENCE;
        group.fParticleCount = fPArray.getParticleCount();
        group.fParticles = fPArray.popContext();
        XSObjectList annotations;
        if (annotation != null) {
            annotations = new XSObjectListImpl();
            ((XSObjectListImpl)annotations).addXSObject (annotation);
        } else {
            annotations = XSObjectListImpl.EMPTY_LIST;
        }
        group.fAnnotations = annotations;
        particle = new XSParticleDecl();
        particle.fType = XSParticleDecl.PARTICLE_MODELGROUP;
        particle.fMinOccurs = minAtt.intValue();
        particle.fMaxOccurs = maxAtt.intValue();
        particle.fValue = group;
        particle.fAnnotations = annotations;

        particle = checkOccurrences(particle,
                choice ? SchemaSymbols.ELT_CHOICE : SchemaSymbols.ELT_SEQUENCE,
                        (Element)decl.getParentNode(),
                        allContextFlags,
                        defaultVals.longValue());
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);

        return particle;
    }

    protected boolean hasAllContent(XSParticleDecl particle) {
        if (particle != null && particle.fType == XSParticleDecl.PARTICLE_MODELGROUP) {
            return ((XSModelGroupImpl)particle.fValue).fCompositor == XSModelGroupImpl.MODELGROUP_ALL;
        }

        return false;
    }

    protected static class ParticleArray {
        XSParticleDecl[] fParticles = new XSParticleDecl[10];
        int[] fPos = new int[5];
        int fContextCount = 0;

        void pushContext() {
            fContextCount++;
            if (fContextCount == fPos.length) {
                int newSize = fContextCount * 2;
                int[] newArray = new int[newSize];
                System.arraycopy(fPos, 0, newArray, 0, fContextCount);
                fPos = newArray;
            }
            fPos[fContextCount] = fPos[fContextCount-1];
        }

        int getParticleCount() {
            return fPos[fContextCount] - fPos[fContextCount-1];
        }

        void addParticle(XSParticleDecl particle) {
            if (fPos[fContextCount] == fParticles.length) {
                int newSize = fPos[fContextCount] * 2;
                XSParticleDecl[] newArray = new XSParticleDecl[newSize];
                System.arraycopy(fParticles, 0, newArray, 0, fPos[fContextCount]);
                fParticles = newArray;
            }
            fParticles[fPos[fContextCount]++] = particle;
        }

        XSParticleDecl[] popContext() {
            int count = fPos[fContextCount] - fPos[fContextCount-1];
            XSParticleDecl[] array = null;
            if (count != 0) {
                array = new XSParticleDecl[count];
                System.arraycopy(fParticles, fPos[fContextCount-1], array, 0, count);
                for (int i = fPos[fContextCount-1]; i < fPos[fContextCount]; i++)
                    fParticles[i] = null;
            }
            fContextCount--;
            return array;
        }

    }

    ParticleArray fPArray = new ParticleArray();
}
