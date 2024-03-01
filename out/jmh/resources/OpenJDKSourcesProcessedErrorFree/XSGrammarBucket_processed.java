/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.xs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class used to hold the internal schema grammar set for the current instance
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 * @LastModified: Nov 2017
 */
public class XSGrammarBucket {


    /**
     * Map that maps between Namespace and a Grammar
     */
    Map<String, SchemaGrammar> fGrammarRegistry = new HashMap<>();
    SchemaGrammar fNoNSGrammar = null;

    /**
     * Get the schema grammar for the specified namespace
     *
     * @param namespace
     * @return SchemaGrammar associated with the namespace
     */
    public SchemaGrammar getGrammar(String namespace) {
        if (namespace == null)
            return fNoNSGrammar;
        return fGrammarRegistry.get(namespace);
    }

    /**
     * Put a schema grammar into the registry
     * This method is for internal use only: it assumes that a grammar with
     * the same target namespace is not already in the bucket.
     *
     * @param grammar   the grammar to put in the registry
     */
    public void putGrammar(SchemaGrammar grammar) {
        if (grammar.getTargetNamespace() == null)
            fNoNSGrammar = grammar;
        else
            fGrammarRegistry.put(grammar.getTargetNamespace(), grammar);
    }

    /**
     * put a schema grammar and any grammars imported by it (directly or
     * inderectly) into the registry. when a grammar with the same target
     * namespace is already in the bucket, and different from the one being
     * added, it's an error, and no grammar will be added into the bucket.
     *
     * @param grammar   the grammar to put in the registry
     * @param deep      whether to add imported grammars
     * @return          whether the process succeeded
     */
    public boolean putGrammar(SchemaGrammar grammar, boolean deep) {
        SchemaGrammar sg = getGrammar(grammar.fTargetNamespace);
        if (sg != null) {
            return sg == grammar;
        }
        if (!deep) {
            putGrammar(grammar);
            return true;
        }

        ArrayList<SchemaGrammar> currGrammars = (ArrayList<SchemaGrammar>)grammar.getImportedGrammars();
        if (currGrammars == null) {
            putGrammar(grammar);
            return true;
        }

        @SuppressWarnings("unchecked")
        List<SchemaGrammar> grammars = ((ArrayList<SchemaGrammar>)currGrammars.clone());
        SchemaGrammar sg1, sg2;
        List<SchemaGrammar> gs;
        for (int i = 0; i < grammars.size(); i++) {
            sg1 = grammars.get(i);
            sg2 = getGrammar(sg1.fTargetNamespace);
            if (sg2 == null) {
                gs = sg1.getImportedGrammars();
                if(gs == null) continue;
                for (int j = gs.size() - 1; j >= 0; j--) {
                    sg2 = gs.get(j);
                    if (!grammars.contains(sg2))
                        grammars.add(sg2);
                }
            }
            else if (sg2 != sg1) {
                return false;
            }
        }

        putGrammar(grammar);
        for (int i = grammars.size() - 1; i >= 0; i--)
            putGrammar(grammars.get(i));

        return true;
    }

    /**
     * put a schema grammar and any grammars imported by it (directly or
     * inderectly) into the registry. when a grammar with the same target
     * namespace is already in the bucket, and different from the one being
     * added, no grammar will be added into the bucket.
     *
     * @param grammar        the grammar to put in the registry
     * @param deep           whether to add imported grammars
     * @param ignoreConflict whether to ignore grammars that already exist in the grammar
     *                       bucket or not - including 'grammar' parameter.
     * @return               whether the process succeeded
     */
    public boolean putGrammar(SchemaGrammar grammar, boolean deep, boolean ignoreConflict) {
        if (!ignoreConflict) {
            return putGrammar(grammar, deep);
        }

        SchemaGrammar sg = getGrammar(grammar.fTargetNamespace);
        if (sg == null) {
            putGrammar(grammar);
        }

        if (!deep) {
            return true;
        }

        ArrayList<SchemaGrammar> currGrammars = (ArrayList<SchemaGrammar>)grammar.getImportedGrammars();
        if (currGrammars == null) {
            return true;
        }

        @SuppressWarnings("unchecked")
        List<SchemaGrammar> grammars = ((ArrayList<SchemaGrammar>)currGrammars.clone());
        SchemaGrammar sg1, sg2;
        List<SchemaGrammar> gs;
        for (int i = 0; i < grammars.size(); i++) {
            sg1 = grammars.get(i);
            sg2 = getGrammar(sg1.fTargetNamespace);
            if (sg2 == null) {
                gs = sg1.getImportedGrammars();
                if(gs == null) continue;
                for (int j = gs.size() - 1; j >= 0; j--) {
                    sg2 = gs.get(j);
                    if (!grammars.contains(sg2))
                        grammars.add(sg2);
                }
            }
            else  {
                grammars.remove(sg1);
            }
        }

        for (int i = grammars.size() - 1; i >= 0; i--) {
            putGrammar(grammars.get(i));
        }

        return true;
    }

    /**
     * get all grammars in the registry
     *
     * @return an array of SchemaGrammars.
     */
    public SchemaGrammar[] getGrammars() {
        int count = fGrammarRegistry.size() + (fNoNSGrammar==null ? 0 : 1);
        SchemaGrammar[] grammars = new SchemaGrammar[count];
        int i = 0;
        for(Map.Entry<String, SchemaGrammar> entry : fGrammarRegistry.entrySet()){
            grammars[i++] = entry.getValue();
        }

        if (fNoNSGrammar != null)
            grammars[count-1] = fNoNSGrammar;
        return grammars;
    }

    /**
     * Clear the registry.
     * REVISIT: update to use another XSGrammarBucket
     */
    public void reset() {
        fNoNSGrammar = null;
        fGrammarRegistry.clear();
    }

} 
