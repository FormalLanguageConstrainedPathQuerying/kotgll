/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.debugger.win32.coff;

import java.util.NoSuchElementException;

/** <p> Provides iteration-style access to the types in the
    sstGlobalTypes subsection of the VC++ 5.0 debug
    information. Clients should walk down these platform-dependent
    types and transform them into the platform-independent interfaces
    described in the package sun.jvm.hotspot.debugger.csym. </p>

    <p> This iterator is a "two-dimensional" iterator; it iterates not
    only over all of the types in the type table, but also iterates
    over the leaf types in the current type string. This structure was
    chosen to avoid constructing a new type iterator for each type in
    the type table because of the expected large number of types. </p>
*/

public interface DebugVC50TypeIterator {

  /** Indicates whether the iteration through the type table is
      complete. */
  public boolean done();

  /** Go to the next type in the type table. NOTE that the iterator is
      pointing at the first type initially, so one should use a while
      (!iter.done()) { ...  iter.next(); } construct.

      @throw NoSuchElementException if the iterator is already done
      and next() is called. */
  public void next() throws NoSuchElementException;

  /** Gets the length, in bytes, of the current type record. */
  public short getLength();

  /** Gets the type index of the current type. This number is
      compatible with type references in symbols and type records. */
  public int getTypeIndex();

  /** Debugging support only */
  public int getNumTypes();


  /** Indicates whether iteration through the current type string is
      complete. */
  public boolean typeStringDone();

  /** Goes to the next element in the current type string. NOTE that
      the iterator is pointing at the first type initially, so one
      should use a while (!iter.typeStringDone()) { ...
      iter.typeStringNext(); } construct.

      @throw NoSuchElementException if the iterator is already done
      and typeStringNext() is called. */
  public void typeStringNext() throws NoSuchElementException;

  /** Return the leaf index (see {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeLeafIndices})
      for the current element of the current type string. */
  public int typeStringLeaf();

  /** For debugging: returns the file offset of the current type
      string leaf. */
  public int typeStringOffset();




  /** Type index of the modified type. */
  public int getModifierIndex();

  /** Attributes specified in MODIFIER_ enums in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public short getModifierAttribute();


  /** Type index of object pointed to. */
  public int getPointerType();

  /** Pointer attributes. Consists of seven bit fields whose
      enumerants are in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}:
      PTRTYPE, PTRMODE, ISFLAT32, VOLATILE, CONST, UNALIGNED, and
      RESTRICT. */
  public int getPointerAttributes();

  /** Only valid if the pointer type is BASED_ON_TYPE; retrieves index
      of type. */
  public int getPointerBasedOnTypeIndex();

  /** Only valid if the pointer type is BASED_ON_TYPE; retrieves name
      of type. */
  public String getPointerBasedOnTypeName();

  /** Only valid if the pointer mode is either PTR_TO_DATA_MEMBER or
      PTR_TO_METHOD; retrieves the type index of the containing
      class. */
  public int getPointerToMemberClass();

  /** Only valid if the pointer mode is either PTR_TO_DATA_MEMBER or
      PTR_TO_METHOD; retrieves the data format of the pointer in
      memory. See the PTR_FORMAT enum in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public short getPointerToMemberFormat();


  /** Type index of each array element. */
  public int getArrayElementType();

  /** Type index of indexing variable. */
  public int getArrayIndexType();

  /** Length of the array in bytes. */
  public int getArrayLength() throws DebugVC50WrongNumericTypeException;

  /** Length-prefixed name of array. */
  public String getArrayName();


  /** Number of elements in the class or structure. This count
      includes direct, virtual, and indirect virtual bases, and
      methods including overloads, data members, static data members,
      friends, and so on. */
  public short getClassCount();

  /** Property bit field; see PROPERTY_ enumeration in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public short getClassProperty();

  /** Type index of the field list for this class. */
  public int getClassFieldList();

  /** Get new iterator pointing at the field list of this class. */
  public DebugVC50TypeIterator getClassFieldListIterator();

  /** Type index of the derivation list. This is output by the
      compiler as 0x0000 and is filled in by the CVPACK utility to a
      LF_DERIVED record containing the type indices of those classes
      which immediately inherit the current class. A zero index
      indicates that no derivation information is available. A LF_NULL
      index indicates that the class is not inherited by other
      classes. */
  public int getClassDerivationList();

  /** Type index of the virtual function table shape descriptor. */
  public int getClassVShape();

  /** Numeric leaf specifying size in bytes of the structure. */
  public int getClassSize() throws DebugVC50WrongNumericTypeException;

  /** Length-prefixed name of this type. */
  public String getClassName();


  /** Number of fields in the union. */
  public short getUnionCount();

  /** Property bit field. */
  public short getUnionProperty();

  /** Type index of field list. */
  public int getUnionFieldList();

  /** Get new iterator pointing at the field list of this union. */
  public DebugVC50TypeIterator getUnionFieldListIterator();

  /** Numeric leaf specifying size in bytes of the union. */
  public int getUnionSize() throws DebugVC50WrongNumericTypeException;

  /** Length-prefixed name of union. */
  public String getUnionName();


  /** Number of enumerates. */
  public short getEnumCount();

  /** Property bit field. */
  public short getEnumProperty();

  /** Index of underlying type of enum. */
  public int getEnumType();

  /** Type index of field list. */
  public int getEnumFieldList();

  /** Get new iterator pointing at the field list of this enum. */
  public DebugVC50TypeIterator getEnumFieldListIterator();

  /** Length-prefixed name of enum. */
  public String getEnumName();


  /** Type index of the value returned by the procedure. */
  public int getProcedureReturnType();

  /** Calling convention of the procedure; see CALLCONV_ enumeration
      in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public byte getProcedureCallingConvention();

  /** Number of parameters. */
  public short getProcedureNumberOfParameters();

  /** Type index of argument list type record. */
  public int getProcedureArgumentList();

  /** Get new iterator pointing at the argument list of this procedure. */
  public DebugVC50TypeIterator getProcedureArgumentListIterator();


  /** Type index of the value returned by the procedure. */
  public int getMFunctionReturnType();

  /** Type index of the containing class of the function. */
  public int getMFunctionContainingClass();

  /** Type index of the <b>this</b> parameter of the member function.
      A type of void indicates that the member function is static and
      has no <b>this</b> parameter. */
  public int getMFunctionThis();

  /** Calling convention of the procedure; see CALLCONV_ enumeration
      in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public byte getMFunctionCallingConvention();

  /** Number of parameters. This count does not include the
      <b>this</b> parameter. */
  public short getMFunctionNumberOfParameters();

  /** List of parameter specifiers. This list does not include the
      <b>this</b> parameter. */
  public int getMFunctionArgumentList();

  /** Get new iterator pointing at the argument list of this member function. */
  public DebugVC50TypeIterator getMFunctionArgumentListIterator();

  /** Logical <b>this</b> adjustor for the method. Whenever a class
      element is referenced via the <b>this</b> pointer, thisadjust
      will be added to the resultant offset before referencing the
      element. */
  public int getMFunctionThisAdjust();



  /** Number of descriptors. */
  public short getVTShapeCount();

  /** Fetch the <i>i</i>th descriptor (0..getVTShapeCount() - 1). Each
      descriptor is a 4-bit (half-byte) value described by the
      VTENTRY_ enumeration in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public int getVTShapeDescriptor(int i);



  /** Type of each element of the array. */
  public int getBasicArrayType();


  /** Addressing mode of the label, described by LABEL_ADDR_MODE_ enum
      in {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums}. */
  public short getLabelAddressMode();



  /** Underlying type of the array. */
  public int getDimArrayType();

  /** Index of the type record containing the dimension information. */
  public int getDimArrayDimInfo();

  /** Length-prefixed name of the array. */
  public String getDimArrayName();


  /** Count of number of bases in the path to the virtual function
      table. */
  public int getVFTPathCount();

  /** Type indices of the base classes in the path
      (0..getVFTPathCount() - 1). */
  public int getVFTPathBase(int i);






  /** In processing $$TYPES, the index counter is advanced to index
      count, skipping all intermediate indices. This is the next valid
      index. */
  public int getSkipIndex();


  /** Count of number of indices in list. */
  public int getArgListCount();

  /** List of type indices (0..getArgListCount() - 1) for describing
      the formal parameters to a function or method. */
  public int getArgListType(int i);


  /** Type index of resulting expression. */
  public int getDefaultArgType();

  /** Length-prefixed string of supplied default expression. */
  public String getDefaultArgExpression();




  /** Number of types in the list. */
  public int getDerivedCount();

  /** Fetch <i>i</i>th derived type (0..getDerivedCount() - 1). */
  public int getDerivedType(int i);



  /** Type index of the field. */
  public int getBitfieldFieldType();

  /** The length in bits of the object. */
  public byte getBitfieldLength();

  /** Starting position (from bit 0) of the object in the word. */
  public byte getBitfieldPosition();



  /** Attribute of the member function; see {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums} and {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50MemberAttributes}. */
  public short getMListAttribute();

  /** Number of types corresponding to this overloaded method. FIXME:
      must verify this can be inferred solely from this record's
      length. */
  public int getMListLength();

  /** Type index of the procedure record for the <i>i</i>th occurrence
      of the function (0..getMListLength() - 1). */
  public int getMListType(int i);

  /** Convenience routine indicating whether this member function is
      introducing virtual. */
  public boolean isMListIntroducingVirtual();

  /** Present only when property attribute is introducing virtual
      (optional). Offset in vtable of the class which contains the
      pointer to the function. (FIXME: is this on a per-method or
      per-method list basis? If the latter, will have to provide an
      iterator for this record.) */
  public int getMListVtabOffset();




  /** Create a new SymbolIterator pointing at the copy of the symbol
      this record contains. */
  public DebugVC50SymbolIterator getRefSym();




  /** Member attribute bit field. */
  public short getBClassAttribute();

  /** Index to type record of the class. The class name can be
      obtained from this record. */
  public int getBClassType();

  /** Offset of subobject that represents the base class within the
      structure. */
  public int getBClassOffset() throws DebugVC50WrongNumericTypeException;



  /** Member attribute bit field. */
  public short getVBClassAttribute();

  /** Index to type record of the direct or indirect virtual base
      class. The class name can be obtained from this record. */
  public int getVBClassBaseClassType();

  /** Type index of the virtual base pointer for this base. */
  public int getVBClassVirtualBaseClassType();

  /** Numeric leaf specifying the offset of the virtual base pointer
      from the address point of the class for this virtual base. */
  public int getVBClassVBPOff() throws DebugVC50WrongNumericTypeException;

  /** Numeric leaf specifying the index into the virtual base
      displacement table of the entry that contains the displacement
      of the virtual base. The displacement is relative to the address
      point of the class plus vbpoff. */
  public int getVBClassVBOff() throws DebugVC50WrongNumericTypeException;



  /** Member attribute bit field. */
  public short getIVBClassAttribute();

  /** Index to type record of the direct or indirect virtual base
      class. The class name can be obtained from this record. */
  public int getIVBClassBType();

  /** Type index of the virtual base pointer for this base. */
  public int getIVBClassVBPType();

  /** Numeric leaf specifying the offset of the virtual base pointer
      from the address point of the class for this virtual base. */
  public int getIVBClassVBPOff() throws DebugVC50WrongNumericTypeException;

  /** Numeric leaf specifying the index into the virtual base
      displacement table of the entry that contains the displacement
      of the virtual base. The displacement is relative to the address
      point of the class plus vbpoff. */
  public int getIVBClassVBOff() throws DebugVC50WrongNumericTypeException;


  /** Member attribute bit field. */
  public short getEnumerateAttribute();

  /** Numeric leaf specifying the value of enumerate. */
  public long getEnumerateValue() throws DebugVC50WrongNumericTypeException;

  /** Length-prefixed name of the member field. */
  public String getEnumerateName();


  /** Index to type record of the friend function. */
  public int getFriendFcnType();

  /** Length prefixed name of friend function. */
  public String getFriendFcnName();


  /** Type index. This field is emitted by the compiler when a complex
      list needs to be split during writing. */
  public int getIndexValue();

  /** Create a new type iterator starting at the above index. */
  public DebugVC50TypeIterator getIndexIterator();


  /** Member attribute bit field. */
  public short getMemberAttribute();

  /** Index to type record for field. */
  public int getMemberType();

  /** Numeric leaf specifying the offset of field in the structure. */
  public int getMemberOffset() throws DebugVC50WrongNumericTypeException;

  /** Length-prefixed name of the member field. */
  public String getMemberName();



  /** Member attribute bit field. */
  public short getStaticAttribute();

  /** Index to type record for field. */
  public int getStaticType();

  /** Length-prefixed name of the member field. */
  public String getStaticName();



  /** Number of occurrences of function within the class. If the
      function is overloaded then there will be multiple entries in
      the method list. */
  public short getMethodCount();

  /** Type index of method list. */
  public int getMethodList();

  /** Length-prefixed name of method. */
  public String getMethodName();


  /** Type index of nested type. */
  public int getNestedType();

  /** Length-prefixed name of type. */
  public String getNestedName();



  /** Index to the pointer record describing the pointer. The pointer
      will in turn have a LF_VTSHAPE type record as the underlying
      type. Note that the offset of the virtual function table pointer
      from the address point of the class is always zero. */
  public int getVFuncTabType();


  /** Index to type record of the friend class. The name of the class
      can be obtained from the referenced record. */
  public int getFriendClsType();


  /** Method attribute; see {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50TypeEnums} and
      {@link
      sun.jvm.hotspot.debugger.win32.coff.DebugVC50MemberAttributes}. */
  public short getOneMethodAttribute();

  /** Type index of method. */
  public int getOneMethodType();

  /** Convenience routine indicating whether this method is
      introducing virtual. */
  public boolean isOneMethodIntroducingVirtual();

  /** Offset in virtual function table if introducing virtual method.
      If the method is not an introducing virtual, then this field is
      not present. */
  public int getOneMethodVBaseOff();

  /** Length prefixed name of method. */
  public String getOneMethodName();



  /** Type index of virtual function table pointer. */
  public int getVFuncOffType();

  /** Offset of virtual function table pointer relative to address
      point of class. */
  public int getVFuncOffOffset();



  /** Nested type attribute (protection fields are valid). */
  public short getNestedExAttribute();

  /** Type index of nested type. */
  public int getNestedExType();

  /** Length-prefixed name of type. */
  public String getNestedExName();


  /** New protection attributes. */
  public short getMemberModifyAttribute();

  /** Type index of base class that introduced the member. */
  public int getMemberModifyType();

  /** Length-prefixed name of member. */
  public String getMemberModifyName();


  /** Fetch the two-byte type (or data, for short integer numeric
      leaves) of the numeric leaf at the given offset, in bytes, from
      the start of the current leaf. */
  public short getNumericTypeAt(int byteOffset);

  /** The size in bytes of the numeric leaf at the given offset, in
      bytes, from the start of the current leaf.

      @throw DebugVC50WrongNumericTypeException if there is no numeric
      leaf at the specified byte offset. */
  public int getNumericLengthAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;

  /** Fetch the value of the integer numeric leaf at the given offset,
      in bytes, from the start of the current leaf.

      @throw DebugVC50WrongNumericTypeException if the specified
      numeric leaf is not of integer type. */
  public int getNumericIntAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;

  /** Fetch the value of the long or integer numeric leaf at the given
      offset, in bytes, from the start of the current leaf.

      @throw DebugVC50WrongNumericTypeException if the specified
      numeric leaf is not of long or integer type. */
  public long getNumericLongAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;

  /** Fetch the value of the single-precision floating-point numeric
      leaf at the given offset, in bytes, from the start of the
      current leaf.

      @throw DebugVC50WrongNumericTypeException if the specified
      numeric leaf is not of 32-bit float type. */
  public float getNumericFloatAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;

  /** Fetch the value of the double-precision floating-point numeric
      leaf at the given offset, in bytes, from the start of the
      current leaf.

      @throw DebugVC50WrongNumericTypeException if the specified
      numeric leaf is not of 64-bit float type. */
  public double getNumericDoubleAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;

  /** Fetch the raw bytes, including LF_ prefix (if any), of the
      numeric leaf at the given offset, in bytes, from the start of
      the current leaf.

      @throw DebugVC50WrongNumericTypeException if there is no numeric
      leaf at the specified byte offset. */
  public byte[] getNumericDataAt(int byteOffset)
    throws DebugVC50WrongNumericTypeException;
}
