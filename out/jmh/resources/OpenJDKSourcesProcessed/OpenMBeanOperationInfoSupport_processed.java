/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates. All rights reserved.
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


package javax.management.openmbean;


import java.util.Arrays;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;


/**
 * Describes an operation of an Open MBean.
 *
 *
 * @since 1.5
 */
public class OpenMBeanOperationInfoSupport
    extends MBeanOperationInfo
    implements OpenMBeanOperationInfo {

    /* Serial version */
    static final long serialVersionUID = 4996859732565369366L;

    /**
     * @serial The <i>open type</i> of the values returned by the operation
     *         described by this {@link OpenMBeanOperationInfo} instance
     *
     */
    private OpenType<?> returnOpenType;


    private transient Integer myHashCode = null;
    private transient String  myToString = null;


    /**
     * <p>Constructs an {@code OpenMBeanOperationInfoSupport}
     * instance, which describes the operation of a class of open
     * MBeans, with the specified {@code name}, {@code description},
     * {@code signature}, {@code returnOpenType} and {@code
     * impact}.</p>
     *
     * <p>The {@code signature} array parameter is internally copied,
     * so that subsequent changes to the array referenced by {@code
     * signature} have no effect on this instance.</p>
     *
     * @param name cannot be a null or empty string.
     *
     * @param description cannot be a null or empty string.
     *
     * @param signature can be null or empty if there are no
     * parameters to describe.
     *
     * @param returnOpenType cannot be null: use {@code
     * SimpleType.VOID} for operations that return nothing.
     *
     * @param impact must be one of {@code ACTION}, {@code
     * ACTION_INFO}, {@code INFO}, or {@code UNKNOWN}.
     *
     * @throws IllegalArgumentException if {@code name} or {@code
     * description} are null or empty string, or {@code
     * returnOpenType} is null, or {@code impact} is not one of {@code
     * ACTION}, {@code ACTION_INFO}, {@code INFO}, or {@code UNKNOWN}.
     *
     * @throws ArrayStoreException If {@code signature} is not an
     * array of instances of a subclass of {@code MBeanParameterInfo}.
     */
    public OpenMBeanOperationInfoSupport(String name,
                                         String description,
                                         OpenMBeanParameterInfo[] signature,
                                         OpenType<?> returnOpenType,
                                         int impact) {
        this(name, description, signature, returnOpenType, impact,
             (Descriptor) null);
    }

    /**
     * <p>Constructs an {@code OpenMBeanOperationInfoSupport}
     * instance, which describes the operation of a class of open
     * MBeans, with the specified {@code name}, {@code description},
     * {@code signature}, {@code returnOpenType}, {@code
     * impact}, and {@code descriptor}.</p>
     *
     * <p>The {@code signature} array parameter is internally copied,
     * so that subsequent changes to the array referenced by {@code
     * signature} have no effect on this instance.</p>
     *
     * @param name cannot be a null or empty string.
     *
     * @param description cannot be a null or empty string.
     *
     * @param signature can be null or empty if there are no
     * parameters to describe.
     *
     * @param returnOpenType cannot be null: use {@code
     * SimpleType.VOID} for operations that return nothing.
     *
     * @param impact must be one of {@code ACTION}, {@code
     * ACTION_INFO}, {@code INFO}, or {@code UNKNOWN}.
     *
     * @param descriptor The descriptor for the operation.  This may
     * be null, which is equivalent to an empty descriptor.
     *
     * @throws IllegalArgumentException if {@code name} or {@code
     * description} are null or empty string, or {@code
     * returnOpenType} is null, or {@code impact} is not one of {@code
     * ACTION}, {@code ACTION_INFO}, {@code INFO}, or {@code UNKNOWN}.
     *
     * @throws ArrayStoreException If {@code signature} is not an
     * array of instances of a subclass of {@code MBeanParameterInfo}.
     *
     * @since 1.6
     */
    public OpenMBeanOperationInfoSupport(String name,
                                         String description,
                                         OpenMBeanParameterInfo[] signature,
                                         OpenType<?> returnOpenType,
                                         int impact,
                                         Descriptor descriptor) {
        super(name,
              description,
              arrayCopyCast(signature),
              (returnOpenType == null) ? null : returnOpenType.getClassName(),
              impact,
              ImmutableDescriptor.union(descriptor,
                (returnOpenType==null) ? null :returnOpenType.getDescriptor()));

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument name cannot " +
                                               "be null or empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument description cannot " +
                                               "be null or empty");
        }
        if (returnOpenType == null) {
            throw new IllegalArgumentException("Argument returnOpenType " +
                                               "cannot be null");
        }

        if (impact != ACTION && impact != ACTION_INFO && impact != INFO &&
                impact != UNKNOWN) {
            throw new IllegalArgumentException("Argument impact can only be " +
                                               "one of ACTION, ACTION_INFO, " +
                                               "INFO, or UNKNOWN: " + impact);
        }

        this.returnOpenType = returnOpenType;
    }


    private static MBeanParameterInfo[]
            arrayCopyCast(OpenMBeanParameterInfo[] src) {
        if (src == null)
            return null;

        MBeanParameterInfo[] dst = new MBeanParameterInfo[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    private static OpenMBeanParameterInfo[]
            arrayCopyCast(MBeanParameterInfo[] src) {
        if (src == null)
            return null;

        OpenMBeanParameterInfo[] dst = new OpenMBeanParameterInfo[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }




    /**
     * Returns the <i>open type</i> of the values returned by the
     * operation described by this {@code OpenMBeanOperationInfo}
     * instance.
     */
    public OpenType<?> getReturnOpenType() {

        return returnOpenType;
    }



    /* ***  Commodity methods from java.lang.Object  *** */


    /**
     * <p>Compares the specified {@code obj} parameter with this
     * {@code OpenMBeanOperationInfoSupport} instance for
     * equality.</p>
     *
     * <p>Returns {@code true} if and only if all of the following
     * statements are true:
     *
     * <ul>
     * <li>{@code obj} is non null,</li>
     * <li>{@code obj} also implements the {@code
     * OpenMBeanOperationInfo} interface,</li>
     * <li>their names are equal</li>
     * <li>their signatures are equal</li>
     * <li>their return open types are equal</li>
     * <li>their impacts are equal</li>
     * </ul>
     *
     * This ensures that this {@code equals} method works properly for
     * {@code obj} parameters which are different implementations of
     * the {@code OpenMBeanOperationInfo} interface.
     *
     * @param obj the object to be compared for equality with this
     * {@code OpenMBeanOperationInfoSupport} instance;
     *
     * @return {@code true} if the specified object is equal to this
     * {@code OpenMBeanOperationInfoSupport} instance.
     */
    public boolean equals(Object obj) {

        if (obj == null) {
            return false;
        }

        OpenMBeanOperationInfo other;
        try {
            other = (OpenMBeanOperationInfo) obj;
        } catch (ClassCastException e) {
            return false;
        }


        if ( ! this.getName().equals(other.getName()) ) {
            return false;
        }

        if ( ! Arrays.equals(this.getSignature(), other.getSignature()) ) {
            return false;
        }

        if ( ! this.getReturnOpenType().equals(other.getReturnOpenType()) ) {
            return false;
        }

        if ( this.getImpact() != other.getImpact() ) {
            return false;
        }

        return true;
    }

    /**
     * <p>Returns the hash code value for this {@code
     * OpenMBeanOperationInfoSupport} instance.</p>
     *
     * <p>The hash code of an {@code OpenMBeanOperationInfoSupport}
     * instance is the sum of the hash codes of all elements of
     * information used in {@code equals} comparisons (ie: its name,
     * return open type, impact and signature, where the signature
     * hashCode is calculated by a call to {@code
     * java.util.Arrays.asList(this.getSignature).hashCode()}).</p>
     *
     * <p>This ensures that {@code t1.equals(t2) } implies that {@code
     * t1.hashCode()==t2.hashCode() } for any two {@code
     * OpenMBeanOperationInfoSupport} instances {@code t1} and {@code
     * t2}, as required by the general contract of the method {@link
     * Object#hashCode() Object.hashCode()}.</p>
     *
     * <p>However, note that another instance of a class implementing
     * the {@code OpenMBeanOperationInfo} interface may be equal to
     * this {@code OpenMBeanOperationInfoSupport} instance as defined
     * by {@link #equals(java.lang.Object)}, but may have a different
     * hash code if it is calculated differently.</p>
     *
     * <p>As {@code OpenMBeanOperationInfoSupport} instances are
     * immutable, the hash code for this instance is calculated once,
     * on the first call to {@code hashCode}, and then the same value
     * is returned for subsequent calls.</p>
     *
     * @return the hash code value for this {@code
     * OpenMBeanOperationInfoSupport} instance
     */
    public int hashCode() {

        if (myHashCode == null) {
            int value = 0;
            value += this.getName().hashCode();
            value += Arrays.asList(this.getSignature()).hashCode();
            value += this.getReturnOpenType().hashCode();
            value += this.getImpact();
            myHashCode = Integer.valueOf(value);
        }

        return myHashCode.intValue();
    }

    /**
     * <p>Returns a string representation of this {@code
     * OpenMBeanOperationInfoSupport} instance.</p>
     *
     * <p>The string representation consists of the name of this class
     * (ie {@code
     * javax.management.openmbean.OpenMBeanOperationInfoSupport}), and
     * the name, signature, return open type and impact of the
     * described operation and the string representation of its descriptor.</p>
     *
     * <p>As {@code OpenMBeanOperationInfoSupport} instances are
     * immutable, the string representation for this instance is
     * calculated once, on the first call to {@code toString}, and
     * then the same value is returned for subsequent calls.</p>
     *
     * @return a string representation of this {@code
     * OpenMBeanOperationInfoSupport} instance
     */
    public String toString() {

        if (myToString == null) {
            myToString = new StringBuilder()
                .append(this.getClass().getName())
                .append("(name=")
                .append(this.getName())
                .append(",signature=")
                .append(Arrays.asList(this.getSignature()).toString())
                .append(",return=")
                .append(this.getReturnOpenType().toString())
                .append(",impact=")
                .append(this.getImpact())
                .append(",descriptor=")
                .append(this.getDescriptor())
                .append(")")
                .toString();
        }

        return myToString;
    }

    /**
     * An object serialized in a version of the API before Descriptors were
     * added to this class will have an empty or null Descriptor.
     * For consistency with our
     * behavior in this version, we must replace the object with one
     * where the Descriptors reflect the same value of returned openType.
     **/
    private Object readResolve() {
        if (getDescriptor().getFieldNames().length == 0) {
            return new OpenMBeanOperationInfoSupport(
                    name, description, arrayCopyCast(getSignature()),
                    returnOpenType, getImpact());
        } else
            return this;
    }

}
