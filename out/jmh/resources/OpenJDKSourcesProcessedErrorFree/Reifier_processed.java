/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.generics.visitor;


import java.lang.reflect.Type;
import java.util.List;
import java.util.Iterator;
import sun.reflect.generics.tree.*;
import sun.reflect.generics.factory.*;



/**
 * Visitor that converts AST to reified types.
 */
public class Reifier implements TypeTreeVisitor<Type> {
    private Type resultType;
    private final GenericsFactory factory;

    private Reifier(GenericsFactory f){
        factory = f;
    }

    private GenericsFactory getFactory(){ return factory;}

    /**
     * Factory method. The resulting visitor will convert an AST
     * representing generic signatures into corresponding reflective
     * objects, using the provided factory, {@code f}.
     * @param f - a factory that can be used to manufacture reflective
     * objects returned by this visitor
     * @return A visitor that can be used to reify ASTs representing
     * generic type information into reflective objects
     */
    public static Reifier make(GenericsFactory f){
        return new Reifier(f);
    }

    private Type[] reifyTypeArguments(TypeArgument[] tas) {
        Type[] ts = new Type[tas.length];
        for (int i = 0; i < tas.length; i++) {
            tas[i].accept(this);
            ts[i] = resultType;
        }
        return ts;
    }


    /**
     * Accessor for the result of the last visit by this visitor,
     * @return The type computed by this visitor based on its last
     * visit
     */
    public Type getResult() { assert resultType != null;return resultType;}

    public void visitFormalTypeParameter(FormalTypeParameter ftp){
        resultType = getFactory().makeTypeVariable(ftp.getName(),
                                                   ftp.getBounds());
    }


    public void visitClassTypeSignature(ClassTypeSignature ct){

        List<SimpleClassTypeSignature> scts = ct.getPath();
        assert(!scts.isEmpty());
        Iterator<SimpleClassTypeSignature> iter = scts.iterator();
        SimpleClassTypeSignature sc = iter.next();
        StringBuilder n = new StringBuilder(sc.getName());
        boolean dollar = sc.getDollar();

        while (iter.hasNext() && sc.getTypeArguments().length == 0) {
            sc = iter.next();
            dollar = sc.getDollar();
            n.append(dollar?"$":".").append(sc.getName());
        }

        assert(!(iter.hasNext()) || (sc.getTypeArguments().length > 0));
        Type c = getFactory().makeNamedType(n.toString());
        if (sc.getTypeArguments().length == 0) {
            assert(!iter.hasNext());
            resultType = c; 
        } else {
            assert(sc.getTypeArguments().length > 0);
            Type[] pts = reifyTypeArguments(sc.getTypeArguments());

            Type owner = getFactory().makeParameterizedType(c, pts, null);
            dollar =false;
            while (iter.hasNext()) {
                sc = iter.next();
                dollar = sc.getDollar();
                n.append(dollar?"$":".").append(sc.getName()); 
                c = getFactory().makeNamedType(n.toString()); 
                pts = reifyTypeArguments(sc.getTypeArguments());
                owner = getFactory().makeParameterizedType(c, pts, owner);
            }
            resultType = owner;
        }
    }

    public void visitArrayTypeSignature(ArrayTypeSignature a){
        a.getComponentType().accept(this);
        Type ct = resultType;
        resultType = getFactory().makeArrayType(ct);
    }

    public void visitTypeVariableSignature(TypeVariableSignature tv){
        resultType = getFactory().findTypeVariable(tv.getIdentifier());
    }

    public void visitWildcard(Wildcard w){
        resultType = getFactory().makeWildcard(w.getUpperBounds(),
                                               w.getLowerBounds());
    }

    public void visitSimpleClassTypeSignature(SimpleClassTypeSignature sct){
        resultType = getFactory().makeNamedType(sct.getName());
    }

    public void visitBottomSignature(BottomSignature b){

    }

    public void visitByteSignature(ByteSignature b){
        resultType = getFactory().makeByte();
    }

    public void visitBooleanSignature(BooleanSignature b){
        resultType = getFactory().makeBool();
    }

    public void visitShortSignature(ShortSignature s){
        resultType = getFactory().makeShort();
    }

    public void visitCharSignature(CharSignature c){
        resultType = getFactory().makeChar();
    }

    public void visitIntSignature(IntSignature i){
        resultType = getFactory().makeInt();
    }

    public void visitLongSignature(LongSignature l){
        resultType = getFactory().makeLong();
    }

    public void visitFloatSignature(FloatSignature f){
        resultType = getFactory().makeFloat();
    }

    public void visitDoubleSignature(DoubleSignature d){
        resultType = getFactory().makeDouble();
    }

    public void visitVoidDescriptor(VoidDescriptor v){
        resultType = getFactory().makeVoid();
    }


}
