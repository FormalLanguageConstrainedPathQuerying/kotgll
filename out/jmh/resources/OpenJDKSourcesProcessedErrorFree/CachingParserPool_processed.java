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

package com.sun.org.apache.xerces.internal.parsers;

import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.util.XMLGrammarPoolImpl;

import com.sun.org.apache.xerces.internal.util.ShadowedSymbolTable;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.SynchronizedSymbolTable;

/**
 * A parser pool that enables caching of grammars. The caching parser
 * pool is constructed with a specific symbol table and grammar pool
 * that has already been populated with the grammars used by the
 * application.
 * <p>
 * Once the caching parser pool is constructed, specific parser
 * instances are created by calling the appropriate factory method
 * on the parser pool.
 * <p>
 * <strong>Note:</strong> There is a performance penalty for using
 * a caching parser pool due to thread safety. Access to the symbol
 * table and grammar pool must be synchronized to ensure the safe
 * operation of the symbol table and grammar pool.
 * <p>
 * <strong>Note:</strong> If performance is critical, then another
 * mechanism needs to be used instead of the caching parser pool.
 * One approach would be to create parser instances that do not
 * share these structures. Instead, each instance would get its
 * own copy to use while parsing. This avoids the synchronization
 * overhead at the expense of more memory and the time required
 * to copy the structures for each new parser instance. And even
 * when a parser instance is re-used, there is a potential for a
 * memory leak due to new symbols being added to the symbol table
 * over time. In other words, always take caution to make sure
 * that your application is thread-safe and avoids leaking memory.
 *
 * @author Andy Clark, IBM
 *
 */
public class CachingParserPool {


    /** Default shadow symbol table (false). */
    public static final boolean DEFAULT_SHADOW_SYMBOL_TABLE = false;

    /** Default shadow grammar pool (false). */
    public static final boolean DEFAULT_SHADOW_GRAMMAR_POOL = false;


    /**
     * Symbol table. The symbol table that the caching parser pool is
     * constructed with is automatically wrapped in a synchronized
     * version for thread-safety.
     */
    protected SymbolTable fSynchronizedSymbolTable;

    /**
     * Grammar pool. The grammar pool that the caching parser pool is
     * constructed with is automatically wrapped in a synchronized
     * version for thread-safety.
     */
    protected XMLGrammarPool fSynchronizedGrammarPool;

    /**
     * Shadow the symbol table for new parser instances. If true,
     * new parser instances use shadow copies of the main symbol
     * table and are not allowed to add new symbols to the main
     * symbol table. New symbols are added to the shadow symbol
     * table and are local to the parser instance.
     */
    protected boolean fShadowSymbolTable = DEFAULT_SHADOW_SYMBOL_TABLE;

    /**
     * Shadow the grammar pool for new parser instances. If true,
     * new parser instances use shadow copies of the main grammar
     * pool and are not allowed to add new grammars to the main
     * grammar pool. New grammars are added to the shadow grammar
     * pool and are local to the parser instance.
     */
    protected boolean fShadowGrammarPool = DEFAULT_SHADOW_GRAMMAR_POOL;


    /** Default constructor. */
    public CachingParserPool() {
        this(new SymbolTable(), new XMLGrammarPoolImpl());
    } 

    /**
     * Constructs a caching parser pool with the specified symbol table
     * and grammar pool.
     *
     * @param symbolTable The symbol table.
     * @param grammarPool The grammar pool.
     */
    public CachingParserPool(SymbolTable symbolTable, XMLGrammarPool grammarPool) {
        fSynchronizedSymbolTable = new SynchronizedSymbolTable(symbolTable);
        fSynchronizedGrammarPool = new SynchronizedGrammarPool(grammarPool);
    } 


    /** Returns the symbol table. */
    public SymbolTable getSymbolTable() {
        return fSynchronizedSymbolTable;
    } 

    /** Returns the grammar pool. */
    public XMLGrammarPool getXMLGrammarPool() {
        return fSynchronizedGrammarPool;
    } 


    /**
     * Sets whether new parser instance receive shadow copies of the
     * main symbol table.
     *
     * @param shadow If true, new parser instances use shadow copies
     *               of the main symbol table and are not allowed to
     *               add new symbols to the main symbol table. New
     *               symbols are added to the shadow symbol table and
     *               are local to the parser instance. If false, new
     *               parser instances are allowed to add new symbols
     *               to the main symbol table.
     */
    public void setShadowSymbolTable(boolean shadow) {
        fShadowSymbolTable = shadow;
    } 


    /** Creates a new DOM parser. */
    public DOMParser createDOMParser() {
        SymbolTable symbolTable = fShadowSymbolTable
                                ? new ShadowedSymbolTable(fSynchronizedSymbolTable)
                                : fSynchronizedSymbolTable;
        XMLGrammarPool grammarPool = fShadowGrammarPool
                                ? new ShadowedGrammarPool(fSynchronizedGrammarPool)
                                : fSynchronizedGrammarPool;
        return new DOMParser(symbolTable, grammarPool);
    } 

    /** Creates a new SAX parser. */
    public SAXParser createSAXParser() {
        SymbolTable symbolTable = fShadowSymbolTable
                                ? new ShadowedSymbolTable(fSynchronizedSymbolTable)
                                : fSynchronizedSymbolTable;
        XMLGrammarPool grammarPool = fShadowGrammarPool
                                ? new ShadowedGrammarPool(fSynchronizedGrammarPool)
                                : fSynchronizedGrammarPool;
        return new SAXParser(symbolTable, grammarPool);
    } 


    /**
     * Synchronized grammar pool.
     *
     * @author Andy Clark, IBM
     */
    public static final class SynchronizedGrammarPool
        implements XMLGrammarPool {


        /** Main grammar pool. */
        private XMLGrammarPool fGrammarPool;


        /** Constructs a synchronized grammar pool. */
        public SynchronizedGrammarPool(XMLGrammarPool grammarPool) {
            fGrammarPool = grammarPool;
        } 


        public Grammar [] retrieveInitialGrammarSet(String grammarType ) {
            synchronized (fGrammarPool) {
                return fGrammarPool.retrieveInitialGrammarSet(grammarType);
            }
        } 

        public Grammar retrieveGrammar(XMLGrammarDescription gDesc) {
            synchronized (fGrammarPool) {
                return fGrammarPool.retrieveGrammar(gDesc);
            }
        } 

        public void cacheGrammars(String grammarType, Grammar[] grammars) {
            synchronized (fGrammarPool) {
                fGrammarPool.cacheGrammars(grammarType, grammars);
            }
        } 

        /** lock the grammar pool */
        public void lockPool() {
            synchronized (fGrammarPool) {
                fGrammarPool.lockPool();
            }
        } 

        /** clear the grammar pool */
        public void clear() {
            synchronized (fGrammarPool) {
                fGrammarPool.clear();
            }
        } 

        /** unlock the grammar pool */
        public void unlockPool() {
            synchronized (fGrammarPool) {
                fGrammarPool.unlockPool();
            }
        } 

        /***
         * Methods corresponding to original (pre Xerces2.0.0final)
         * grammarPool have been commented out.
         */
        /**
         * Puts the specified grammar into the grammar pool.
         *
         * @param key Key to associate with grammar.
         * @param grammar Grammar object.
         */
        /******
        public void putGrammar(String key, Grammar grammar) {
            synchronized (fGrammarPool) {
                fGrammarPool.putGrammar(key, grammar);
            }
        } 
        *******/

        /**
         * Returns the grammar associated to the specified key.
         *
         * @param key The key of the grammar.
         */
        /**********
        public Grammar getGrammar(String key) {
            synchronized (fGrammarPool) {
                return fGrammarPool.getGrammar(key);
            }
        } 
        ***********/

        /**
         * Removes the grammar associated to the specified key from the
         * grammar pool and returns the removed grammar.
         *
         * @param key The key of the grammar.
         */
        /**********
        public Grammar removeGrammar(String key) {
            synchronized (fGrammarPool) {
                return fGrammarPool.removeGrammar(key);
            }
        } 
        ******/

        /**
         * Returns true if the grammar pool contains a grammar associated
         * to the specified key.
         *
         * @param key The key of the grammar.
         */
        /**********
        public boolean containsGrammar(String key) {
            synchronized (fGrammarPool) {
                return fGrammarPool.containsGrammar(key);
            }
        } 
        ********/

    } 

    /**
     * Shadowed grammar pool.
     * This class is predicated on the existence of a concrete implementation;
     * so using our own doesn't seem to bad an idea.
     *
     * @author Andy Clark, IBM
     * @author Neil Graham, IBM
     */
    public static final class ShadowedGrammarPool
        extends XMLGrammarPoolImpl {


        /** Main grammar pool. */
        private XMLGrammarPool fGrammarPool;


        /** Constructs a shadowed grammar pool. */
        public ShadowedGrammarPool(XMLGrammarPool grammarPool) {
            fGrammarPool = grammarPool;
        } 


        /**
         * Retrieve the initial set of grammars for the validator to work with.
         * REVISIT:  does this need to be synchronized since it's just reading?
         *
         * @param grammarType Type of the grammars to be retrieved.
         * @return            The initial grammar set the validator may place in its "bucket"
         */
        public Grammar [] retrieveInitialGrammarSet(String grammarType ) {
            Grammar [] grammars = super.retrieveInitialGrammarSet(grammarType);
            if (grammars != null) return grammars;
            return fGrammarPool.retrieveInitialGrammarSet(grammarType);
        } 

        /**
         * Retrieve a particular grammar.
         * REVISIT:  does this need to be synchronized since it's just reading?
         *
         * @param gDesc Description of the grammar to be retrieved
         * @return      Grammar corresponding to gDesc, or null if none exists.
         */
        public Grammar retrieveGrammar(XMLGrammarDescription gDesc) {
            Grammar g = super.retrieveGrammar(gDesc);
            if(g != null) return g;
            return fGrammarPool.retrieveGrammar(gDesc);
        } 

        /**
         * Give the grammarPool the option of caching these grammars.
         * This certainly must be synchronized.
         *
         * @param grammarType The type of the grammars to be cached.
         * @param grammars    The Grammars that may be cached (unordered, Grammars previously
         *                    given to the validator may be included).
         */
        public void cacheGrammars(String grammarType, Grammar[] grammars) {
           super.cacheGrammars(grammarType, grammars);
           fGrammarPool.cacheGrammars(grammarType, grammars);
        } 

        /**
         * Returns the grammar associated to the specified description.
         *
         * @param desc The description of the grammar.
         */
        public Grammar getGrammar(XMLGrammarDescription desc) {

            if (super.containsGrammar(desc)) {
                return super.getGrammar(desc);
            }
            return null;

        } 

        /**
         * Returns true if the grammar pool contains a grammar associated
         * to the specified description.
         *
         * @param desc The description of the grammar.
         */
        public boolean containsGrammar(XMLGrammarDescription desc) {
            return super.containsGrammar(desc);
        } 

    } 

} 
