/*
 * Copyright (c) 2008, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.lang.invoke;

import sun.invoke.util.Wrapper;

import java.lang.ref.SoftReference;

import static java.lang.invoke.MethodHandleStatics.newIllegalArgumentException;

/**
 * Shared information for a group of method types, which differ
 * only by reference types, and therefore share a common erasure
 * and wrapping.
 * <p>
 * For an empirical discussion of the structure of method types,
 * see <a href="http:
 * the thread "Avoiding Boxing" on jvm-languages</a>.
 * There are approximately 2000 distinct erased method types in the JDK.
 * There are a little over 10 times that number of unerased types.
 * No more than half of these are likely to be loaded at once.
 * @author John Rose
 */
final class MethodTypeForm {
    final short parameterSlotCount;
    final short primitiveCount;
    final MethodType erasedType;        
    final MethodType basicType;         

    final SoftReference<MethodHandle>[] methodHandles;

    static final int
            MH_BASIC_INV      =  0,  
            MH_NF_INV         =  1,  
            MH_UNINIT_CS      =  2,  
            MH_LIMIT          =  3;

    final SoftReference<LambdaForm>[] lambdaForms;

    static final int
            LF_INVVIRTUAL              =  0,  
            LF_INVSTATIC               =  1,
            LF_INVSPECIAL              =  2,
            LF_NEWINVSPECIAL           =  3,
            LF_INVINTERFACE            =  4,
            LF_INVSTATIC_INIT          =  5,  
            LF_INTERPRET               =  6,  
            LF_REBIND                  =  7,  
            LF_DELEGATE                =  8,  
            LF_DELEGATE_BLOCK_INLINING =  9,  
            LF_EX_LINKER               = 10,  
            LF_EX_INVOKER              = 11,  
            LF_GEN_LINKER              = 12,  
            LF_GEN_INVOKER             = 13,  
            LF_CS_LINKER               = 14,  
            LF_MH_LINKER               = 15,  
            LF_GWC                     = 16,  
            LF_GWT                     = 17,  
            LF_TF                      = 18,  
            LF_LOOP                    = 19,  
            LF_INVSPECIAL_IFC          = 20,  
            LF_INVNATIVE               = 21,  
            LF_VH_EX_INVOKER           = 22,  
            LF_VH_GEN_INVOKER          = 23,  
            LF_VH_GEN_LINKER           = 24,  
            LF_COLLECTOR               = 25,  
            LF_LIMIT                   = 26;

    /** Return the type corresponding uniquely (1-1) to this MT-form.
     *  It might have any primitive returns or arguments, but will have no references except Object.
     */
    public MethodType erasedType() {
        return erasedType;
    }

    /** Return the basic type derived from the erased type of this MT-form.
     *  A basic type is erased (all references Object) and also has all primitive
     *  types (except int, long, float, double, void) normalized to int.
     *  Such basic types correspond to low-level JVM calling sequences.
     */
    public MethodType basicType() {
        return basicType;
    }

    public MethodHandle cachedMethodHandle(int which) {
        SoftReference<MethodHandle> entry = methodHandles[which];
        return (entry != null) ? entry.get() : null;
    }

    public synchronized MethodHandle setCachedMethodHandle(int which, MethodHandle mh) {
        SoftReference<MethodHandle> entry = methodHandles[which];
        if (entry != null) {
            MethodHandle prev = entry.get();
            if (prev != null) {
                return prev;
            }
        }
        methodHandles[which] = new SoftReference<>(mh);
        return mh;
    }

    public LambdaForm cachedLambdaForm(int which) {
        SoftReference<LambdaForm> entry = lambdaForms[which];
        return (entry != null) ? entry.get() : null;
    }

    public synchronized LambdaForm setCachedLambdaForm(int which, LambdaForm form) {
        SoftReference<LambdaForm> entry = lambdaForms[which];
        if (entry != null) {
            LambdaForm prev = entry.get();
            if (prev != null) {
                return prev;
            }
        }
        lambdaForms[which] = new SoftReference<>(form);
        return form;
    }

    /**
     * Build an MTF for a given type, which must have all references erased to Object.
     * This MTF will stand for that type and all un-erased variations.
     * Eagerly compute some basic properties of the type, common to all variations.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected MethodTypeForm(MethodType erasedType) {
        this.erasedType = erasedType;

        Class<?>[] ptypes = erasedType.ptypes();
        int pslotCount = ptypes.length;

        short primitiveCount = 0, longArgCount = 0;
        Class<?>[] erasedPtypes = ptypes;
        Class<?>[] basicPtypes = erasedPtypes;
        for (int i = 0; i < erasedPtypes.length; i++) {
            Class<?> ptype = erasedPtypes[i];
            if (ptype != Object.class) {
                ++primitiveCount;
                Wrapper w = Wrapper.forPrimitiveType(ptype);
                if (w.isDoubleWord())  ++longArgCount;
                if (w.isSubwordOrInt() && ptype != int.class) {
                    if (basicPtypes == erasedPtypes)
                        basicPtypes = basicPtypes.clone();
                    basicPtypes[i] = int.class;
                }
            }
        }
        pslotCount += longArgCount;                  
        Class<?> returnType = erasedType.returnType();
        Class<?> basicReturnType = returnType;
        if (returnType != Object.class) {
            ++primitiveCount; 
            Wrapper w = Wrapper.forPrimitiveType(returnType);
            if (w.isSubwordOrInt() && returnType != int.class)
                basicReturnType = int.class;
        }
        if (erasedPtypes == basicPtypes && basicReturnType == returnType) {
            this.basicType = erasedType;

            if (pslotCount >= 256)  throw newIllegalArgumentException("too many arguments");

            this.primitiveCount = primitiveCount;
            this.parameterSlotCount = (short)pslotCount;
            this.lambdaForms   = new SoftReference[LF_LIMIT];
            this.methodHandles = new SoftReference[MH_LIMIT];
        } else {
            this.basicType = MethodType.methodType(basicReturnType, basicPtypes, true);
            MethodTypeForm that = this.basicType.form();
            assert(this != that);

            this.parameterSlotCount = that.parameterSlotCount;
            this.primitiveCount = that.primitiveCount;
            this.methodHandles = null;
            this.lambdaForms = null;
        }
    }

    public int parameterCount() {
        return erasedType.parameterCount();
    }
    public int parameterSlotCount() {
        return parameterSlotCount;
    }
    public boolean hasPrimitives() {
        return primitiveCount != 0;
    }

    static MethodTypeForm findForm(MethodType mt) {
        MethodType erased = canonicalize(mt, ERASE);
        if (erased == null) {
            return new MethodTypeForm(mt);
        } else {
            return erased.form();
        }
    }

    /** Codes for {@link #canonicalize(java.lang.Class, int)}.
     * ERASE means change every reference to {@code Object}.
     * WRAP means convert primitives (including {@code void} to their
     * corresponding wrapper types.  UNWRAP means the reverse of WRAP.
     */
    public static final int ERASE = 1, WRAP = 2, UNWRAP = 3;

    /** Canonicalize the types in the given method type.
     * If any types change, intern the new type, and return it.
     * Otherwise return null.
     */
    public static MethodType canonicalize(MethodType mt, int how) {
        Class<?>[] ptypes = mt.ptypes();
        Class<?>[] ptypesCanonical = canonicalizeAll(ptypes, how);
        Class<?> rtype = mt.returnType();
        Class<?> rtypeCanonical = canonicalize(rtype, how);
        if (ptypesCanonical == null && rtypeCanonical == null) {
            return null;
        }
        if (rtypeCanonical == null)  rtypeCanonical = rtype;
        if (ptypesCanonical == null)  ptypesCanonical = ptypes;
        return MethodType.methodType(rtypeCanonical, ptypesCanonical, true);
    }

    /** Canonicalize the given return or param type.
     *  Return null if the type is already canonicalized.
     */
    static Class<?> canonicalize(Class<?> t, int how) {
        if (t == Object.class) {
        } else if (!t.isPrimitive()) {
            switch (how) {
                case UNWRAP:
                    Class<?> ct = Wrapper.asPrimitiveType(t);
                    if (ct != t)  return ct;
                    break;
                case ERASE:
                    return Object.class;
            }
        } else if (how == WRAP) {
            return Wrapper.asWrapperType(t);
        }
        return null;
    }

    /** Canonicalize each param type in the given array.
     *  Return null if all types are already canonicalized.
     */
    static Class<?>[] canonicalizeAll(Class<?>[] ts, int how) {
        Class<?>[] cs = null;
        for (int imax = ts.length, i = 0; i < imax; i++) {
            Class<?> c = canonicalize(ts[i], how);
            if (c != null && c != void.class) {
                if (cs == null)
                    cs = ts.clone();
                cs[i] = c;
            }
        }
        return cs;
    }

    @Override
    public String toString() {
        return "Form"+erasedType;
    }
}
