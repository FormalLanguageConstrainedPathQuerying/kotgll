/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.xsltc.compiler;

import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC;
import com.sun.org.apache.bcel.internal.generic.INVOKEVIRTUAL;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ClassGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ErrorMsg;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.MethodGenerator;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.StringType;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.TypeCheckError;
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Util;
import jdk.xml.internal.JdkXmlFeatures;

/**
 * @author Morten Jorgensen
 */
final class TransletOutput extends Instruction {

    private Expression _filename;
    private boolean _append;

    /**
     * Displays the contents of this <xsltc:output> element.
     */
    public void display(int indent) {
        indent(indent);
        Util.println("TransletOutput: " + _filename);
    }

    /**
     * Parse the contents of this <xsltc:output> element. The only attribute
     * we recognise is the 'file' attribute that contains teh output filename.
     */
    public void parseContents(Parser parser) {
        String filename = getAttribute("file");

        String append   = getAttribute("append");

        if ((filename == null) || (filename.equals(EMPTYSTRING))) {
            reportError(this, parser, ErrorMsg.REQUIRED_ATTR_ERR, "file");
        }

        _filename = AttributeValue.create(this, filename, parser);

        if (append != null && (append.toLowerCase().equals("yes") ||
            append.toLowerCase().equals("true"))) {
          _append = true;
        }
        else
          _append = false;

        parseChildren(parser);
    }

    /**
     * Type checks the 'file' attribute (must be able to convert it to a str).
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
        final Type type = _filename.typeCheck(stable);
        if (type instanceof StringType == false) {
            _filename = new CastExpr(_filename, Type.String);
        }
        typeCheckContents(stable);
        return Type.Void;
    }

    /**
     * Compile code that opens the give file for output, dumps the contents of
     * the element to the file, then closes the file.
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
        final ConstantPoolGen cpg = classGen.getConstantPool();
        final InstructionList il = methodGen.getInstructionList();
        final boolean isSecureProcessing = classGen.getParser().getXSLTC()
                                           .isSecureProcessing();
        final boolean isExtensionFunctionEnabled = classGen.getParser().getXSLTC()
                .getFeature(JdkXmlFeatures.XmlFeature.ENABLE_EXTENSION_FUNCTION);

        if (isSecureProcessing && !isExtensionFunctionEnabled) {
            int index = cpg.addMethodref(BASIS_LIBRARY_CLASS,
                                         "unallowed_extension_elementF",
                                         "(Ljava/lang/String;)V");
            il.append(new PUSH(cpg, "redirect"));
            il.append(new INVOKESTATIC(index));
            return;
        }

        il.append(methodGen.loadHandler());

        final int open =  cpg.addMethodref(TRANSLET_CLASS,
                                           "openOutputHandler",
                                           "(" + STRING_SIG + "Z)" +
                                           TRANSLET_OUTPUT_SIG);

        final int close =  cpg.addMethodref(TRANSLET_CLASS,
                                            "closeOutputHandler",
                                            "("+TRANSLET_OUTPUT_SIG+")V");

        il.append(classGen.loadTranslet());
        _filename.translate(classGen, methodGen);
        il.append(new PUSH(cpg, _append));
        il.append(new INVOKEVIRTUAL(open));

        il.append(methodGen.storeHandler());

        translateContents(classGen, methodGen);

        il.append(classGen.loadTranslet());
        il.append(methodGen.loadHandler());
        il.append(new INVOKEVIRTUAL(close));

        il.append(methodGen.storeHandler());
    }
}
