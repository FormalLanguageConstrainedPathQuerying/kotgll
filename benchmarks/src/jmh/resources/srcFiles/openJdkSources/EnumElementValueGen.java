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

package com.sun.org.apache.bcel.internal.generic;

import java.io.DataOutputStream;
import java.io.IOException;

import com.sun.org.apache.bcel.internal.classfile.ConstantUtf8;
import com.sun.org.apache.bcel.internal.classfile.ElementValue;
import com.sun.org.apache.bcel.internal.classfile.EnumElementValue;

/**
 * @since 6.0
 */
public class EnumElementValueGen extends ElementValueGen {
    private final int typeIdx;

    private final int valueIdx;

    public EnumElementValueGen(final EnumElementValue value, final ConstantPoolGen cpool, final boolean copyPoolEntries) {
        super(ENUM_CONSTANT, cpool);
        if (copyPoolEntries) {
            typeIdx = cpool.addUtf8(value.getEnumTypeString());
            valueIdx = cpool.addUtf8(value.getEnumValueString()); 
        } else {
            typeIdx = value.getTypeIndex();
            valueIdx = value.getValueIndex();
        }
    }

    /**
     * This ctor assumes the constant pool already contains the right type and value - as indicated by typeIdx and valueIdx.
     * This ctor is used for deserialization
     */
    protected EnumElementValueGen(final int typeIdx, final int valueIdx, final ConstantPoolGen cpool) {
        super(ElementValueGen.ENUM_CONSTANT, cpool);
        if (super.getElementValueType() != ENUM_CONSTANT) {
            throw new IllegalArgumentException("Only element values of type enum can be built with this ctor - type specified: " + super.getElementValueType());
        }
        this.typeIdx = typeIdx;
        this.valueIdx = valueIdx;
    }

    public EnumElementValueGen(final ObjectType t, final String value, final ConstantPoolGen cpool) {
        super(ElementValueGen.ENUM_CONSTANT, cpool);
        typeIdx = cpool.addUtf8(t.getSignature());
        valueIdx = cpool.addUtf8(value);
    }

    @Override
    public void dump(final DataOutputStream dos) throws IOException {
        dos.writeByte(super.getElementValueType()); 
        dos.writeShort(typeIdx); 
        dos.writeShort(valueIdx); 
    }

    /**
     * Return immutable variant of this EnumElementValue
     */
    @Override
    public ElementValue getElementValue() {
        System.err.println("Duplicating value: " + getEnumTypeString() + ":" + getEnumValueString());
        return new EnumElementValue(super.getElementValueType(), typeIdx, valueIdx, getConstantPool().getConstantPool());
    }

    public String getEnumTypeString() {
        return ((ConstantUtf8) getConstantPool().getConstant(typeIdx)).getBytes();
    }

    public String getEnumValueString() {
        return ((ConstantUtf8) getConstantPool().getConstant(valueIdx)).getBytes();
    }

    public int getTypeIndex() {
        return typeIdx;
    }

    public int getValueIndex() {
        return valueIdx;
    }

    @Override
    public String stringifyValue() {
        final ConstantUtf8 cu8 = (ConstantUtf8) getConstantPool().getConstant(valueIdx);
        return cu8.getBytes();
    }
}
