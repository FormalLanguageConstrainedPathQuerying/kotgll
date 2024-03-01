/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package jdk.internal.org.objectweb.asm.signature;

/**
 * A parser for signature literals, as defined in the Java Virtual Machine Specification (JVMS), to
 * visit them with a SignatureVisitor.
 *
 * @see <a href="https:
 *     4.7.9.1</a>
 * @author Thomas Hallgren
 * @author Eric Bruneton
 */
public class SignatureReader {

    /** The JVMS signature to be read. */
    private final String signatureValue;

    /**
      * Constructs a {@link SignatureReader} for the given signature.
      *
      * @param signature A <i>JavaTypeSignature</i>, <i>ClassSignature</i> or <i>MethodSignature</i>.
      */
    public SignatureReader(final String signature) {
        this.signatureValue = signature;
    }

    /**
      * Makes the given visitor visit the signature of this {@link SignatureReader}. This signature is
      * the one specified in the constructor (see {@link #SignatureReader}). This method is intended to
      * be called on a {@link SignatureReader} that was created using a <i>ClassSignature</i> (such as
      * the <code>signature</code> parameter of the {@link jdk.internal.org.objectweb.asm.ClassVisitor#visit}
      * method) or a <i>MethodSignature</i> (such as the <code>signature</code> parameter of the {@link
      * jdk.internal.org.objectweb.asm.ClassVisitor#visitMethod} method).
      *
      * @param signatureVistor the visitor that must visit this signature.
      */
    public void accept(final SignatureVisitor signatureVistor) {
        String signature = this.signatureValue;
        int length = signature.length();
        int offset; 
        char currentChar; 

        if (signature.charAt(0) == '<') {
            offset = 2;
            do {
                int classBoundStartOffset = signature.indexOf(':', offset);
                signatureVistor.visitFormalTypeParameter(
                        signature.substring(offset - 1, classBoundStartOffset));

                offset = classBoundStartOffset + 1;
                currentChar = signature.charAt(offset);
                if (currentChar == 'L' || currentChar == '[' || currentChar == 'T') {
                    offset = parseType(signature, offset, signatureVistor.visitClassBound());
                }

                while ((currentChar = signature.charAt(offset++)) == ':') {
                    offset = parseType(signature, offset, signatureVistor.visitInterfaceBound());
                }

            } while (currentChar != '>');
        } else {
            offset = 0;
        }

        if (signature.charAt(offset) == '(') {
            offset++;
            while (signature.charAt(offset) != ')') {
                offset = parseType(signature, offset, signatureVistor.visitParameterType());
            }
            offset = parseType(signature, offset + 1, signatureVistor.visitReturnType());
            while (offset < length) {
                offset = parseType(signature, offset + 1, signatureVistor.visitExceptionType());
            }
        } else {
            offset = parseType(signature, offset, signatureVistor.visitSuperclass());
            while (offset < length) {
                offset = parseType(signature, offset, signatureVistor.visitInterface());
            }
        }
    }

    /**
      * Makes the given visitor visit the signature of this {@link SignatureReader}. This signature is
      * the one specified in the constructor (see {@link #SignatureReader}). This method is intended to
      * be called on a {@link SignatureReader} that was created using a <i>JavaTypeSignature</i>, such
      * as the <code>signature</code> parameter of the {@link
      * jdk.internal.org.objectweb.asm.ClassVisitor#visitField} or {@link
      * jdk.internal.org.objectweb.asm.MethodVisitor#visitLocalVariable} methods.
      *
      * @param signatureVisitor the visitor that must visit this signature.
      */
    public void acceptType(final SignatureVisitor signatureVisitor) {
        parseType(signatureValue, 0, signatureVisitor);
    }

    /**
      * Parses a JavaTypeSignature and makes the given visitor visit it.
      *
      * @param signature a string containing the signature that must be parsed.
      * @param startOffset index of the first character of the signature to parsed.
      * @param signatureVisitor the visitor that must visit this signature.
      * @return the index of the first character after the parsed signature.
      */
    private static int parseType(
            final String signature, final int startOffset, final SignatureVisitor signatureVisitor) {
        int offset = startOffset; 
        char currentChar = signature.charAt(offset++); 

        switch (currentChar) {
            case 'Z':
            case 'C':
            case 'B':
            case 'S':
            case 'I':
            case 'F':
            case 'J':
            case 'D':
            case 'V':
                signatureVisitor.visitBaseType(currentChar);
                return offset;

            case '[':
                return parseType(signature, offset, signatureVisitor.visitArrayType());

            case 'T':
                int endOffset = signature.indexOf(';', offset);
                signatureVisitor.visitTypeVariable(signature.substring(offset, endOffset));
                return endOffset + 1;

            case 'L':
                int start = offset; 
                boolean visited = false; 
                boolean inner = false; 
                while (true) {
                    currentChar = signature.charAt(offset++);
                    if (currentChar == '.' || currentChar == ';') {
                        if (!visited) {
                            String name = signature.substring(start, offset - 1);
                            if (inner) {
                                signatureVisitor.visitInnerClassType(name);
                            } else {
                                signatureVisitor.visitClassType(name);
                            }
                        }
                        if (currentChar == ';') {
                            signatureVisitor.visitEnd();
                            break;
                        }
                        start = offset;
                        visited = false;
                        inner = true;
                    } else if (currentChar == '<') {
                        String name = signature.substring(start, offset - 1);
                        if (inner) {
                            signatureVisitor.visitInnerClassType(name);
                        } else {
                            signatureVisitor.visitClassType(name);
                        }
                        visited = true;
                        while ((currentChar = signature.charAt(offset)) != '>') {
                            switch (currentChar) {
                                case '*':
                                    ++offset;
                                    signatureVisitor.visitTypeArgument();
                                    break;
                                case '+':
                                case '-':
                                    offset =
                                            parseType(
                                                    signature, offset + 1, signatureVisitor.visitTypeArgument(currentChar));
                                    break;
                                default:
                                    offset = parseType(signature, offset, signatureVisitor.visitTypeArgument('='));
                                    break;
                            }
                        }
                    }
                }
                return offset;

            default:
                throw new IllegalArgumentException();
        }
    }
}

