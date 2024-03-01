/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package jdk.test.lib.hprof.model;

import java.lang.ref.SoftReference;
import java.util.*;

import jdk.test.lib.hprof.parser.ReadBuffer;
import jdk.test.lib.hprof.util.Misc;

/**
 *
 * @author      Bill Foote
 */

/**
 * Represents a snapshot of the Java objects in the VM at one instant.
 * This is the top-level "model" object read out of a single .hprof or .bod
 * file.
 */

public class Snapshot implements AutoCloseable {

    public static final long SMALL_ID_MASK = 0x0FFFFFFFFL;
    public static final JavaThing[] EMPTY_JAVATHING_ARRAY = new JavaThing[0];

    private static final JavaField[] EMPTY_FIELD_ARRAY = new JavaField[0];
    private static final JavaStatic[] EMPTY_STATIC_ARRAY = new JavaStatic[0];

    private Hashtable<Number, JavaHeapObject> heapObjects =
                 new Hashtable<Number, JavaHeapObject>();

    private Hashtable<Number, JavaClass> fakeClasses =
                 new Hashtable<Number, JavaClass>();

    private Vector<Root> roots = new Vector<Root>();

    private Map<String, JavaClass> classes =
                 new TreeMap<String, JavaClass>();

    private volatile Map<JavaHeapObject, Boolean> newObjects;

    private volatile Map<JavaHeapObject, StackTrace> siteTraces;

    private Map<JavaHeapObject, Root> rootsMap =
                 new HashMap<JavaHeapObject, Root>();

    private SoftReference<Vector<?>> finalizablesCache;

    private ArrayList<ThreadObject> threads = new ArrayList<>();

    private JavaThing nullThing;

    private JavaClass weakReferenceClass;
    private int referentFieldIndex;

    private JavaClass javaLangClass;
    private JavaClass javaLangString;
    private JavaClass javaLangClassLoader;

    private volatile JavaClass otherArrayType;
    private ReachableExcludes reachableExcludes;
    private ReadBuffer readBuf;

    private boolean hasNewSet;
    private boolean unresolvedObjectsOK;

    private boolean newStyleArrayClass;

    private int identifierSize = 4;

    private int minimumObjectSize;

    public Snapshot(ReadBuffer buf) {
        nullThing = new HackJavaValue("<null>", 0);
        readBuf = buf;
    }

    public void setSiteTrace(JavaHeapObject obj, StackTrace trace) {
        if (trace != null && trace.getFrames().length != 0) {
            initSiteTraces();
            siteTraces.put(obj, trace);
        }
    }

    public StackTrace getSiteTrace(JavaHeapObject obj) {
        if (siteTraces != null) {
            return siteTraces.get(obj);
        } else {
            return null;
        }
    }

    public void setNewStyleArrayClass(boolean value) {
        newStyleArrayClass = value;
    }

    public boolean isNewStyleArrayClass() {
        return newStyleArrayClass;
    }

    public void setIdentifierSize(int size) {
        identifierSize = size;
        minimumObjectSize = 2 * size;
    }

    public int getIdentifierSize() {
        return identifierSize;
    }

    public int getMinimumObjectSize() {
        return minimumObjectSize;
    }

    public void addHeapObject(long id, JavaHeapObject ho) {
        heapObjects.put(makeId(id), ho);
    }

    public void addRoot(Root r) {
        r.setIndex(roots.size());
        roots.addElement(r);
    }

    public void addClass(long id, JavaClass c) {
        addHeapObject(id, c);
        putInClassesMap(c);
    }

    public void addThreadObject(ThreadObject thread) {
        threads.add(thread);
    }

    JavaClass addFakeInstanceClass(long classID, int instSize) {
        String name = "unknown-class<@" + Misc.toHex(classID) + ">";

        int numInts = instSize / 4;
        int numBytes = instSize % 4;
        JavaField[] fields = new JavaField[numInts + numBytes];
        int i;
        for (i = 0; i < numInts; i++) {
            fields[i] = new JavaField("unknown-field-" + i, "I");
        }
        for (i = 0; i < numBytes; i++) {
            fields[i + numInts] = new JavaField("unknown-field-" +
                                                i + numInts, "B");
        }

        JavaClass c = new JavaClass(name, 0, 0, 0, 0, fields,
                                 EMPTY_STATIC_ARRAY, instSize);
        addFakeClass(makeId(classID), c);
        return c;
    }


    /**
     * @return true iff it's possible that some JavaThing instances might
     *          isNew set
     *
     * @see JavaThing.isNew()
     */
    public boolean getHasNewSet() {
        return hasNewSet;
    }

    private static class MyVisitor extends AbstractJavaHeapObjectVisitor {
        JavaHeapObject t;
        public void visit(JavaHeapObject other) {
            other.addReferenceFrom(t);
        }
    }

    private static final int DOT_LIMIT = 5000;

    /**
     * Called after reading complete, to initialize the structure
     */
    public void resolve(boolean calculateRefs) {
        System.out.println("Resolving " + heapObjects.size() + " objects...");

        javaLangClass = findClass("java.lang.Class");
        if (javaLangClass == null) {
            System.out.println("WARNING:  hprof file does not include java.lang.Class!");
            javaLangClass = new JavaClass("java.lang.Class", 0, 0, 0, 0,
                                 EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangClass);
        }
        javaLangString = findClass("java.lang.String");
        if (javaLangString == null) {
            System.out.println("WARNING:  hprof file does not include java.lang.String!");
            javaLangString = new JavaClass("java.lang.String", 0, 0, 0, 0,
                                 EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangString);
        }
        javaLangClassLoader = findClass("java.lang.ClassLoader");
        if (javaLangClassLoader == null) {
            System.out.println("WARNING:  hprof file does not include java.lang.ClassLoader!");
            javaLangClassLoader = new JavaClass("java.lang.ClassLoader", 0, 0, 0, 0,
                                 EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangClassLoader);
        }

        for (JavaHeapObject t : heapObjects.values()) {
            if (t instanceof JavaClass) {
                t.resolve(this);
            }
        }

        for (JavaHeapObject t : heapObjects.values()) {
            if (!(t instanceof JavaClass)) {
                t.resolve(this);
            }
        }

        heapObjects.putAll(fakeClasses);
        fakeClasses.clear();

        weakReferenceClass = findClass("java.lang.ref.Reference");
        referentFieldIndex = 0;
        if (weakReferenceClass != null)  {
            JavaField[] fields = weakReferenceClass.getFieldsForInstance();
            for (int i = 0; i < fields.length; i++) {
                if ("referent".equals(fields[i].getName())) {
                    referentFieldIndex = i;
                    break;
                }
            }
        }

        if (calculateRefs) {
            calculateReferencesToObjects();
            System.out.print("Eliminating duplicate references");
            System.out.flush();
        }
        int count = 0;
        for (JavaHeapObject t : heapObjects.values()) {
            t.setupReferrers();
            ++count;
            if (calculateRefs && count % DOT_LIMIT == 0) {
                System.out.print(".");
                System.out.flush();
            }
        }
        if (calculateRefs) {
            System.out.println("");
        }

        classes = Collections.unmodifiableMap(classes);
    }

    private void calculateReferencesToObjects() {
        System.out.print("Chasing references, expect "
                         + (heapObjects.size() / DOT_LIMIT) + " dots");
        System.out.flush();
        int count = 0;
        MyVisitor visitor = new MyVisitor();
        for (JavaHeapObject t : heapObjects.values()) {
            visitor.t = t;
            t.visitReferencedObjects(visitor);
            ++count;
            if (count % DOT_LIMIT == 0) {
                System.out.print(".");
                System.out.flush();
            }
        }
        System.out.println();
        for (Root r : roots) {
            r.resolve(this);
            JavaHeapObject t = findThing(r.getId());
            if (t != null) {
                t.addReferenceFromRoot(r);
            }
        }
    }

    public void markNewRelativeTo(Snapshot baseline) {
        hasNewSet = true;
        for (JavaHeapObject t : heapObjects.values()) {
            boolean isNew;
            long thingID = t.getId();
            if (thingID == 0L || thingID == -1L) {
                isNew = false;
            } else {
                JavaThing other = baseline.findThing(t.getId());
                if (other == null) {
                    isNew = true;
                } else {
                    isNew = !t.isSameTypeAs(other);
                }
            }
            t.setNew(isNew);
        }
    }

    public Enumeration<JavaHeapObject> getThings() {
        return heapObjects.elements();
    }


    public JavaHeapObject findThing(long id) {
        Number idObj = makeId(id);
        JavaHeapObject jho = heapObjects.get(idObj);
        return jho != null? jho : fakeClasses.get(idObj);
    }

    public JavaHeapObject findThing(String id) {
        return findThing(Misc.parseHex(id));
    }

    public JavaClass findClass(String name) {
        if (name.startsWith("0x")) {
            return (JavaClass) findThing(name);
        } else {
            return classes.get(name);
        }
    }

    /**
     * Return an Iterator of all of the classes in this snapshot.
     **/
    public Iterator<JavaClass> getClasses() {
        return classes.values().iterator();
    }

    public JavaClass[] getClassesArray() {
        JavaClass[] res = new JavaClass[classes.size()];
        classes.values().toArray(res);
        return res;
    }

    public synchronized Enumeration<?> getFinalizerObjects() {
        Vector<?> obj;
        if (finalizablesCache != null &&
            (obj = finalizablesCache.get()) != null) {
            return obj.elements();
        }

        JavaClass clazz = findClass("java.lang.ref.Finalizer");
        JavaObject queue = (JavaObject) clazz.getStaticField("queue");
        JavaThing tmp = queue.getField("head");
        Vector<JavaHeapObject> finalizables = new Vector<JavaHeapObject>();
        if (tmp != getNullThing()) {
            JavaObject head = (JavaObject) tmp;
            while (true) {
                JavaHeapObject referent = (JavaHeapObject) head.getField("referent");
                JavaThing next = head.getField("next");
                if (next == getNullThing() || next.equals(head)) {
                    break;
                }
                head = (JavaObject) next;
                finalizables.add(referent);
            }
        }
        finalizablesCache = new SoftReference<Vector<?>>(finalizables);
        return finalizables.elements();
    }

    public Enumeration<Root> getRoots() {
        return roots.elements();
    }

    public Root[] getRootsArray() {
        Root[] res = new Root[roots.size()];
        roots.toArray(res);
        return res;
    }

    public Root getRootAt(int i) {
        return roots.elementAt(i);
    }

    public List<ThreadObject> getThreads() {
        return Collections.unmodifiableList(threads);
    }

    public ReferenceChain[]
    rootsetReferencesTo(JavaHeapObject target, boolean includeWeak) {
        Vector<ReferenceChain> fifo = new Vector<ReferenceChain>();  
        Hashtable<JavaHeapObject, JavaHeapObject> visited = new Hashtable<JavaHeapObject, JavaHeapObject>();
        Vector<ReferenceChain> result = new Vector<ReferenceChain>();
        visited.put(target, target);
        fifo.addElement(new ReferenceChain(target, null));

        while (fifo.size() > 0) {
            ReferenceChain chain = fifo.elementAt(0);
            fifo.removeElementAt(0);
            JavaHeapObject curr = chain.getObj();
            if (curr.getRoot() != null) {
                result.addElement(chain);
            }
            Enumeration<JavaThing> referrers = curr.getReferrers();
            while (referrers.hasMoreElements()) {
                JavaHeapObject t = (JavaHeapObject)referrers.nextElement();
                if (t != null && !visited.containsKey(t)) {
                    if (includeWeak || !t.refersOnlyWeaklyTo(this, curr)) {
                        visited.put(t, t);
                        fifo.addElement(new ReferenceChain(t, chain));
                    }
                }
            }
        }

        ReferenceChain[] realResult = new ReferenceChain[result.size()];
        for (int i = 0; i < result.size(); i++) {
            realResult[i] =  result.elementAt(i);
        }
        return realResult;
    }

    public boolean getUnresolvedObjectsOK() {
        return unresolvedObjectsOK;
    }

    public void setUnresolvedObjectsOK(boolean v) {
        unresolvedObjectsOK = v;
    }

    public JavaClass getWeakReferenceClass() {
        return weakReferenceClass;
    }

    public int getReferentFieldIndex() {
        return referentFieldIndex;
    }

    public JavaThing getNullThing() {
        return nullThing;
    }

    public void setReachableExcludes(ReachableExcludes e) {
        reachableExcludes = e;
    }

    public ReachableExcludes getReachableExcludes() {
        return reachableExcludes;
    }

    void addReferenceFromRoot(Root r, JavaHeapObject obj) {
        Root root = rootsMap.get(obj);
        if (root == null) {
            rootsMap.put(obj, r);
        } else {
            rootsMap.put(obj, root.mostInteresting(r));
        }
    }

    Root getRoot(JavaHeapObject obj) {
        return rootsMap.get(obj);
    }

    JavaClass getJavaLangClass() {
        return javaLangClass;
    }

    JavaClass getJavaLangString() {
        return javaLangString;
    }

    JavaClass getJavaLangClassLoader() {
        return javaLangClassLoader;
    }

    JavaClass getOtherArrayType() {
        if (otherArrayType == null) {
            synchronized(this) {
                if (otherArrayType == null) {
                    addFakeClass(new JavaClass("[<other>", 0, 0, 0, 0,
                                     EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY,
                                     0));
                    otherArrayType = findClass("[<other>");
                }
            }
        }
        return otherArrayType;
    }

    JavaClass getArrayClass(String elementSignature) {
        JavaClass clazz;
        synchronized(classes) {
            clazz = findClass("[" + elementSignature);
            if (clazz == null) {
                clazz = new JavaClass("[" + elementSignature, 0, 0, 0, 0,
                                   EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
                addFakeClass(clazz);
            }
        }
        return clazz;
    }

    ReadBuffer getReadBuffer() {
        return readBuf;
    }

    void setNew(JavaHeapObject obj, boolean isNew) {
        initNewObjects();
        if (isNew) {
            newObjects.put(obj, Boolean.TRUE);
        }
    }

    boolean isNew(JavaHeapObject obj) {
        if (newObjects != null) {
            return newObjects.get(obj) != null;
        } else {
            return false;
        }
    }

    private Number makeId(long id) {
        if (identifierSize == 4) {
            return (int)id;
        } else {
            return id;
        }
    }

    private void putInClassesMap(JavaClass c) {
        String name = c.getName();
        if (classes.containsKey(name)) {
            name += "-" + c.getIdString();
        }
        classes.put(c.getName(), c);
    }

    private void addFakeClass(JavaClass c) {
        putInClassesMap(c);
        c.resolve(this);
    }

    private void addFakeClass(Number id, JavaClass c) {
        fakeClasses.put(id, c);
        addFakeClass(c);
    }

    private synchronized void initNewObjects() {
        if (newObjects == null) {
            synchronized (this) {
                if (newObjects == null) {
                    newObjects = new HashMap<JavaHeapObject, Boolean>();
                }
            }
        }
    }

    private synchronized void initSiteTraces() {
        if (siteTraces == null) {
            synchronized (this) {
                if (siteTraces == null) {
                    siteTraces = new HashMap<JavaHeapObject, StackTrace>();
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        readBuf.close();
    }

}
