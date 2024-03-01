/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * file, and Oracle licenses the original version of this file under the BSD
 * license:
 */
/*
   Copyright 2009-2013 Attila Szegedi

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of the copyright holder nor the names of
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
   IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDER
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
   OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
   ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package jdk.dynalink.beans;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.NamedOperation;
import jdk.dynalink.Namespace;
import jdk.dynalink.NamespaceOperation;
import jdk.dynalink.Operation;
import jdk.dynalink.StandardNamespace;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.beans.GuardedInvocationComponent.ValidationType;
import jdk.dynalink.internal.InternalTypeUtilities;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.support.Guards;
import jdk.dynalink.linker.support.Lookup;
import jdk.internal.reflect.CallerSensitive;

/**
 * A base class for both {@link StaticClassLinker} and {@link BeanLinker}. Deals with common aspects of property
 * exposure and method calls for both static and instance facets of a class.
 */
abstract class AbstractJavaLinker implements GuardingDynamicLinker {

    final Class<?> clazz;
    private final MethodHandle classGuard;
    private final MethodHandle assignableGuard;
    private final Map<String, AnnotatedDynamicMethod> propertyGetters = new HashMap<>();
    private final Map<String, DynamicMethod> propertySetters = new HashMap<>();
    private final Map<String, DynamicMethod> methods = new HashMap<>();

    AbstractJavaLinker(final Class<?> clazz, final MethodHandle classGuard) {
        this(clazz, classGuard, classGuard);
    }

    AbstractJavaLinker(final Class<?> clazz, final MethodHandle classGuard, final MethodHandle assignableGuard) {
        this.clazz = clazz;
        this.classGuard = classGuard;
        this.assignableGuard = assignableGuard;

        final FacetIntrospector introspector = createFacetIntrospector();
        for (final Method rcg: introspector.getRecordComponentGetters()) {
            setPropertyGetter(rcg, 0);
        }
        for(final Method method: introspector.getMethods()) {
            final String name = method.getName();
            addMember(name, method, methods);
            if(name.startsWith("get") && name.length() > 3 && method.getParameterTypes().length == 0) {
                setPropertyGetter(method, 3);
            } else if(name.startsWith("is") && name.length() > 2 && method.getParameterTypes().length == 0 &&
                    method.getReturnType() == boolean.class) {
                setPropertyGetter(method, 2);
            } else if(name.startsWith("set") && name.length() > 3 && method.getParameterTypes().length == 1) {
                addMember(decapitalize(name.substring(3)), method, propertySetters);
            }
        }

        for(final Field field: introspector.getFields()) {
            final String name = field.getName();
            setPropertyGetter(name, introspector.unreflectGetter(field), ValidationType.EXACT_CLASS);
            if(!(Modifier.isFinal(field.getModifiers()) || propertySetters.containsKey(name))) {
                addMember(name, new SimpleDynamicMethod(introspector.unreflectSetter(field), clazz, name),
                        propertySetters);
            }
        }

        for(final Map.Entry<String, MethodHandle> innerClassSpec: introspector.getInnerClassGetters().entrySet()) {
            setPropertyGetter(innerClassSpec.getKey(), innerClassSpec.getValue(), ValidationType.EXACT_CLASS);
        }
    }

    private static String decapitalize(final String str) {
        assert str != null;
        if(str.isEmpty()) {
            return str;
        }

        final char c0 = str.charAt(0);
        if(Character.isLowerCase(c0)) {
            return str;
        }

        if(str.length() > 1 && Character.isUpperCase(str.charAt(1))) {
            return str;
        }

        final char[] c = str.toCharArray();
        c[0] = Character.toLowerCase(c0);
        return new String(c);
    }

    abstract FacetIntrospector createFacetIntrospector();

    Set<String> getReadablePropertyNames() {
        return getUnmodifiableKeys(propertyGetters);
    }

    Set<String> getWritablePropertyNames() {
        return getUnmodifiableKeys(propertySetters);
    }

    Set<String> getMethodNames() {
        return getUnmodifiableKeys(methods);
    }

    private static Set<String> getUnmodifiableKeys(final Map<String, ?> m) {
        return Collections.unmodifiableSet(m.keySet());
    }

    /**
     * Sets the specified dynamic method to be the property getter for the specified property. Note that you can only
     * use this when you're certain that the method handle does not belong to a caller-sensitive method. For properties
     * that are caller-sensitive, you must use {@link #setPropertyGetter(String, SingleDynamicMethod, ValidationType)}
     * instead.
     * @param name name of the property
     * @param handle the method handle that implements the property getter
     * @param validationType the validation type for the property
     */
    private void setPropertyGetter(final String name, final SingleDynamicMethod handle, final ValidationType validationType) {
        if (!propertyGetters.containsKey(name)) {
            propertyGetters.put(name, new AnnotatedDynamicMethod(handle, validationType));
        }
    }

    /**
     * Sets the specified reflective method to be the property getter for the specified property.
     * @param getter the getter method
     * @param prefixLen the getter prefix in the method name; should be 3 for getter names starting with "get" and 2 for
     * names starting with "is".
     */
    private void setPropertyGetter(final Method getter, final int prefixLen) {
        setPropertyGetter(decapitalize(getter.getName().substring(prefixLen)), createDynamicMethod(
                getMostGenericGetter(getter)), ValidationType.INSTANCE_OF);
    }

    /**
     * Sets the specified method handle to be the property getter for the specified property. Note that you can only
     * use this when you're certain that the method handle does not belong to a caller-sensitive method. For properties
     * that are caller-sensitive, you must use {@link #setPropertyGetter(String, SingleDynamicMethod, ValidationType)}
     * instead.
     * @param name name of the property
     * @param handle the method handle that implements the property getter
     * @param validationType the validation type for the property
     */
    void setPropertyGetter(final String name, final MethodHandle handle, final ValidationType validationType) {
        setPropertyGetter(name, new SimpleDynamicMethod(handle, clazz, name), validationType);
    }

    private void addMember(final String name, final Executable m, final Map<String, DynamicMethod> methodMap) {
        addMember(name, createDynamicMethod(m), methodMap);
    }

    private void addMember(final String name, final SingleDynamicMethod method, final Map<String, DynamicMethod> methodMap) {
        final DynamicMethod existingMethod = methodMap.get(name);
        final DynamicMethod newMethod = mergeMethods(method, existingMethod, clazz, name);
        if(newMethod != existingMethod) {
            methodMap.put(name, newMethod);
        }
    }

    /**
     * Given one or more reflective methods or constructors, creates a dynamic method that represents them all. The
     * methods should represent all overloads of the same name (or all constructors of the class).
     * @param members the reflective members
     * @param clazz the class declaring the reflective members
     * @param name the common name of the reflective members.
     * @return a dynamic method representing all the specified reflective members.
     */
    static DynamicMethod createDynamicMethod(final Iterable<? extends Executable> members, final Class<?> clazz, final String name) {
        DynamicMethod dynMethod = null;
        for(final Executable method: members) {
            dynMethod = mergeMethods(createDynamicMethod(method), dynMethod, clazz, name);
        }
        return dynMethod;
    }

    /**
     * Given a reflective method or a constructor, creates a dynamic method that represents it. This method will
     * distinguish between caller sensitive and ordinary methods/constructors, and create appropriate caller sensitive
     * dynamic method when needed.
     * @param m the reflective member
     * @return the single dynamic method representing the reflective member
     */
    private static SingleDynamicMethod createDynamicMethod(final Executable m) {
        if (m.isAnnotationPresent(CallerSensitive.class)) {
            return new CallerSensitiveDynamicMethod(m);
        }
        final MethodHandle mh;
        try {
            mh = unreflectSafely(m);
        } catch (final IllegalAccessError e) {
            return new CallerSensitiveDynamicMethod(m);
        }
        return new SimpleDynamicMethod(mh, m.getDeclaringClass(), m.getName(), m instanceof Constructor);
    }

    /**
     * Unreflects a method handle from a Method or a Constructor using safe (zero-privilege) unreflection. Should be
     * only used for methods and constructors that are not caller sensitive. If a caller sensitive method were
     * unreflected through this mechanism, it would not be a security issue, but would be bound to the zero-privilege
     * unreflector as its caller, and thus completely useless.
     * @param m the method or constructor
     * @return the method handle
     */
    private static MethodHandle unreflectSafely(final Executable m) {
        if(m instanceof Method) {
            final Method reflMethod = (Method)m;
            final MethodHandle handle = Lookup.PUBLIC.unreflect(reflMethod);
            if(Modifier.isStatic(reflMethod.getModifiers())) {
                return StaticClassIntrospector.editStaticMethodHandle(handle);
            }
            return handle;
        }
        return StaticClassIntrospector.editConstructorMethodHandle(Lookup.PUBLIC.unreflectConstructor((Constructor<?>)m));
    }

    private static DynamicMethod mergeMethods(final SingleDynamicMethod method, final DynamicMethod existing, final Class<?> clazz, final String name) {
        if(existing == null) {
            return method;
        } else if(existing.contains(method)) {
            return existing;
        } else if(existing instanceof SingleDynamicMethod) {
            final OverloadedDynamicMethod odm = new OverloadedDynamicMethod(clazz, name);
            odm.addMethod(((SingleDynamicMethod)existing));
            odm.addMethod(method);
            return odm;
        } else if(existing instanceof OverloadedDynamicMethod) {
            ((OverloadedDynamicMethod)existing).addMethod(method);
            return existing;
        }
        throw new AssertionError();
    }

    @Override
    public GuardedInvocation getGuardedInvocation(final LinkRequest request, final LinkerServices linkerServices)
            throws Exception {
        final MissingMemberHandlerFactory missingMemberHandlerFactory;
        final LinkerServices directLinkerServices;
        if (linkerServices instanceof LinkerServicesWithMissingMemberHandlerFactory) {
            final LinkerServicesWithMissingMemberHandlerFactory lswmmhf = ((LinkerServicesWithMissingMemberHandlerFactory)linkerServices);
            missingMemberHandlerFactory = lswmmhf.missingMemberHandlerFactory;
            directLinkerServices = lswmmhf.linkerServices;
        } else {
            missingMemberHandlerFactory = null;
            directLinkerServices = linkerServices;
        }

        final GuardedInvocationComponent gic = getGuardedInvocationComponent(
                new ComponentLinkRequest(request, directLinkerServices,
                        missingMemberHandlerFactory));
        return gic != null ? gic.getGuardedInvocation() : null;
    }

    static final class ComponentLinkRequest {
        final LinkRequest linkRequest;
        final LinkerServices linkerServices;
        final MissingMemberHandlerFactory missingMemberHandlerFactory;
        final Operation baseOperation;
        final List<Namespace> namespaces;
        final Object name;

        ComponentLinkRequest(final LinkRequest linkRequest,
                final LinkerServices linkerServices,
                final MissingMemberHandlerFactory missingMemberHandlerFactory) {
            this.linkRequest = linkRequest;
            this.linkerServices = linkerServices;
            this.missingMemberHandlerFactory = missingMemberHandlerFactory;
            final Operation namedOp = linkRequest.getCallSiteDescriptor().getOperation();
            this.name = NamedOperation.getName(namedOp);
            final Operation namespaceOp = NamedOperation.getBaseOperation(namedOp);
            this.baseOperation = NamespaceOperation.getBaseOperation(namespaceOp);
            this.namespaces = Arrays.asList(NamespaceOperation.getNamespaces(namespaceOp));
        }

        private ComponentLinkRequest(final LinkRequest linkRequest,
                final LinkerServices linkerServices,
                final MissingMemberHandlerFactory missingMemberHandlerFactory,
                final Operation baseOperation, final List<Namespace> namespaces, final Object name) {
            this.linkRequest = linkRequest;
            this.linkerServices = linkerServices;
            this.missingMemberHandlerFactory = missingMemberHandlerFactory;
            this.baseOperation = baseOperation;
            this.namespaces = namespaces;
            this.name = name;
        }

        CallSiteDescriptor getDescriptor() {
            return linkRequest.getCallSiteDescriptor();
        }

        ComponentLinkRequest popNamespace() {
            return new ComponentLinkRequest(linkRequest, linkerServices,
                    missingMemberHandlerFactory, baseOperation,
                namespaces.subList(1, namespaces.size()), name);
        }
    }

    protected GuardedInvocationComponent getGuardedInvocationComponent(final ComponentLinkRequest req)
    throws Exception {
        if (req.namespaces.isEmpty()) {
            return null;
        }
        final Namespace ns = req.namespaces.get(0);
        final Operation op = req.baseOperation;
        if (op == StandardOperation.GET) {
            if (ns == StandardNamespace.PROPERTY) {
                return getPropertyGetter(req.popNamespace());
            } else if (ns == StandardNamespace.METHOD) {
                return getMethodGetter(req.popNamespace());
            }
        } else if (op == StandardOperation.SET && ns == StandardNamespace.PROPERTY) {
            return getPropertySetter(req.popNamespace());
        }
        return getNextComponent(req.popNamespace());
    }

    GuardedInvocationComponent getNextComponent(final ComponentLinkRequest req) throws Exception {
        if (req.namespaces.isEmpty()) {
            return createNoSuchMemberHandler(req.missingMemberHandlerFactory,
                    req.linkRequest, req.linkerServices);
        }
        final GuardedInvocationComponent gic = getGuardedInvocationComponent(req);
        if (gic != null) {
            return gic;
        }
        return getNextComponent(req.popNamespace());
    }

    private GuardedInvocationComponent createNoSuchMemberHandler(
            final MissingMemberHandlerFactory missingMemberHandlerFactory,
            final LinkRequest linkRequest, final LinkerServices linkerServices) throws Exception {
        if (missingMemberHandlerFactory == null) {
            return null;
        }
        final MethodHandle handler = missingMemberHandlerFactory.createMissingMemberHandler(linkRequest, linkerServices);
        if (handler == null) {
            return null;
        }
        final MethodType type = linkRequest.getCallSiteDescriptor().getMethodType();
        assert handler.type().changeReturnType(type.returnType()).equals(type);
        return getClassGuardedInvocationComponent(handler, type);
    }

    MethodHandle getClassGuard(final MethodType type) {
        return Guards.asType(classGuard, type);
    }

    GuardedInvocationComponent getClassGuardedInvocationComponent(final MethodHandle invocation, final MethodType type) {
        return new GuardedInvocationComponent(invocation, getClassGuard(type), clazz, ValidationType.EXACT_CLASS);
    }

    abstract SingleDynamicMethod getConstructorMethod(final String signature);

    private MethodHandle getAssignableGuard(final MethodType type) {
        return Guards.asType(assignableGuard, type);
    }

    private GuardedInvocation createGuardedDynamicMethodInvocation(final CallSiteDescriptor callSiteDescriptor,
            final LinkerServices linkerServices, final String methodName, final Map<String, DynamicMethod> methodMap){
        final MethodHandle inv = getDynamicMethodInvocation(callSiteDescriptor, linkerServices, methodName, methodMap);
        return inv == null ? null : new GuardedInvocation(inv, getClassGuard(callSiteDescriptor.getMethodType()));
    }

    private MethodHandle getDynamicMethodInvocation(final CallSiteDescriptor callSiteDescriptor,
            final LinkerServices linkerServices, final String methodName, final Map<String, DynamicMethod> methodMap) {
        final DynamicMethod dynaMethod = getDynamicMethod(methodName, methodMap);
        return dynaMethod != null ? dynaMethod.getInvocation(callSiteDescriptor, linkerServices) : null;
    }

    private DynamicMethod getDynamicMethod(final String methodName, final Map<String, DynamicMethod> methodMap) {
        final DynamicMethod dynaMethod = methodMap.get(methodName);
        return dynaMethod != null ? dynaMethod : getExplicitSignatureDynamicMethod(methodName, methodMap);
    }

    private SingleDynamicMethod getExplicitSignatureDynamicMethod(final String fullName,
            final Map<String, DynamicMethod> methodsMap) {

        final int lastChar = fullName.length() - 1;
        if(fullName.charAt(lastChar) != ')') {
            return null;
        }
        final int openBrace = fullName.indexOf('(');
        if(openBrace == -1) {
            return null;
        }

        final String name = fullName.substring(0, openBrace);
        final String signature = fullName.substring(openBrace + 1, lastChar);

        final DynamicMethod simpleNamedMethod = methodsMap.get(name);
        if(simpleNamedMethod == null) {
            if (name.isEmpty()) {
                return getConstructorMethod(signature);
            }

            return null;
        }

        return simpleNamedMethod.getMethodForExactParamTypes(signature);
    }

    private static final MethodHandle IS_METHOD_HANDLE_NOT_NULL = Guards.isNotNull().asType(MethodType.methodType(
            boolean.class, MethodHandle.class));
    private static final MethodHandle CONSTANT_NULL_DROP_METHOD_HANDLE = MethodHandles.dropArguments(
            MethodHandles.constant(Object.class, null), 0, MethodHandle.class);

    private GuardedInvocationComponent getPropertySetter(final ComponentLinkRequest req) throws Exception {
        if (req.name == null) {
            return getUnnamedPropertySetter(req);
        }
        return getNamedPropertySetter(req);
    }

    private GuardedInvocationComponent getUnnamedPropertySetter(final ComponentLinkRequest req) throws Exception {
        final CallSiteDescriptor callSiteDescriptor = req.getDescriptor();
        assertParameterCount(callSiteDescriptor, 3);

        final MethodType origType = callSiteDescriptor.getMethodType();
        final MethodType type = origType.returnType() == void.class ? origType : origType.changeReturnType(Object.class);
        final LinkerServices linkerServices = req.linkerServices;


        final MethodType setterType = type.dropParameterTypes(1, 2);
        final MethodHandle boundGetter = MethodHandles.insertArguments(getPropertySetterHandle, 0,
                callSiteDescriptor.changeMethodType(setterType), linkerServices);

        final MethodHandle typedGetter = linkerServices.asType(boundGetter, type.changeReturnType(
                MethodHandle.class));

        final MethodHandle invokeHandle = MethodHandles.exactInvoker(setterType);
        final MethodHandle invokeHandleFolded = MethodHandles.dropArguments(invokeHandle, 2, type.parameterType(
                1));
        final GuardedInvocationComponent nextComponent = getNextComponent(req);

        final MethodHandle fallbackFolded;
        if (nextComponent == null) {
            fallbackFolded = MethodHandles.dropArguments(CONSTANT_NULL_DROP_METHOD_HANDLE, 1,
                    type.parameterList()).asType(type.insertParameterTypes(0, MethodHandle.class));
        } else {
            fallbackFolded = MethodHandles.dropArguments(nextComponent.getGuardedInvocation().getInvocation(),
                    0, MethodHandle.class);
        }

        final MethodHandle compositeSetter = MethodHandles.foldArguments(MethodHandles.guardWithTest(
                    IS_METHOD_HANDLE_NOT_NULL, invokeHandleFolded, fallbackFolded), typedGetter);
        if(nextComponent == null) {
            return getClassGuardedInvocationComponent(compositeSetter, type);
        }
        return nextComponent.compose(compositeSetter, getClassGuard(type), clazz, ValidationType.EXACT_CLASS);
    }

    private GuardedInvocationComponent getNamedPropertySetter(final ComponentLinkRequest req) throws Exception {
        final CallSiteDescriptor callSiteDescriptor = req.getDescriptor();
        assertParameterCount(callSiteDescriptor, 2);
        final GuardedInvocation gi = createGuardedDynamicMethodInvocation(callSiteDescriptor, req.linkerServices,
                req.name.toString(), propertySetters);
        if(gi != null) {
            return new GuardedInvocationComponent(gi, clazz, ValidationType.EXACT_CLASS);
        }
        return getNextComponent(req);
    }

    private static final Lookup privateLookup = new Lookup(MethodHandles.lookup());

    private static final MethodHandle IS_ANNOTATED_METHOD_NOT_NULL = Guards.isNotNull().asType(MethodType.methodType(
            boolean.class, AnnotatedDynamicMethod.class));
    private static final MethodHandle CONSTANT_NULL_DROP_ANNOTATED_METHOD = MethodHandles.dropArguments(
            MethodHandles.constant(Object.class, null), 0, AnnotatedDynamicMethod.class);
    private static final MethodHandle GET_ANNOTATED_METHOD = privateLookup.findVirtual(AnnotatedDynamicMethod.class,
            "getTarget", MethodType.methodType(MethodHandle.class, CallSiteDescriptor.class, LinkerServices.class));
    private static final MethodHandle GETTER_INVOKER = MethodHandles.invoker(MethodType.methodType(Object.class, Object.class));

    private GuardedInvocationComponent getPropertyGetter(final ComponentLinkRequest req) throws Exception {
        if (req.name == null) {
            return getUnnamedPropertyGetter(req);
        }
        return getNamedPropertyGetter(req);
    }

    private GuardedInvocationComponent getUnnamedPropertyGetter(final ComponentLinkRequest req) throws Exception {
        final CallSiteDescriptor callSiteDescriptor = req.getDescriptor();
        final MethodType type = callSiteDescriptor.getMethodType().changeReturnType(Object.class);
        assertParameterCount(callSiteDescriptor, 2);


        final LinkerServices linkerServices = req.linkerServices;
        final MethodHandle typedGetter = linkerServices.asType(getPropertyGetterHandle, type.changeReturnType(
                AnnotatedDynamicMethod.class));
        final MethodHandle callSiteBoundMethodGetter = MethodHandles.insertArguments(
                GET_ANNOTATED_METHOD, 1, callSiteDescriptor, linkerServices);
        final MethodHandle callSiteBoundInvoker = MethodHandles.filterArguments(GETTER_INVOKER, 0,
                callSiteBoundMethodGetter);
        final MethodHandle invokeHandleTyped = linkerServices.asType(callSiteBoundInvoker,
                MethodType.methodType(type.returnType(), AnnotatedDynamicMethod.class, type.parameterType(0)));
        final MethodHandle invokeHandleFolded = MethodHandles.dropArguments(invokeHandleTyped, 2,
                type.parameterType(1));
        final GuardedInvocationComponent nextComponent = getNextComponent(req);

        final MethodHandle fallbackFolded;
        if(nextComponent == null) {
            fallbackFolded = MethodHandles.dropArguments(CONSTANT_NULL_DROP_ANNOTATED_METHOD, 1,
                    type.parameterList()).asType(type.insertParameterTypes(0, AnnotatedDynamicMethod.class));
        } else {
            final MethodHandle nextInvocation = nextComponent.getGuardedInvocation().getInvocation();
            final MethodType nextType = nextInvocation.type();
            fallbackFolded = MethodHandles.dropArguments(nextInvocation.asType(
                    nextType.changeReturnType(Object.class)), 0, AnnotatedDynamicMethod.class);
        }

        final MethodHandle compositeGetter = MethodHandles.foldArguments(MethodHandles.guardWithTest(
                    IS_ANNOTATED_METHOD_NOT_NULL, invokeHandleFolded, fallbackFolded), typedGetter);
        if(nextComponent == null) {
            return getClassGuardedInvocationComponent(compositeGetter, type);
        }
        return nextComponent.compose(compositeGetter, getClassGuard(type), clazz, ValidationType.EXACT_CLASS);
    }

    private GuardedInvocationComponent getNamedPropertyGetter(final ComponentLinkRequest req) throws Exception {
        final CallSiteDescriptor callSiteDescriptor = req.getDescriptor();
        assertParameterCount(callSiteDescriptor, 1);
        final AnnotatedDynamicMethod annGetter = propertyGetters.get(req.name.toString());
        if(annGetter == null) {
            return getNextComponent(req);
        }
        final MethodHandle getter = annGetter.getInvocation(req);
        final ValidationType validationType = annGetter.validationType;
        return new GuardedInvocationComponent(getter, getGuard(validationType,
                callSiteDescriptor.getMethodType()), clazz, validationType);
    }

    private MethodHandle getGuard(final ValidationType validationType, final MethodType methodType) {
        switch(validationType) {
            case EXACT_CLASS: {
                return getClassGuard(methodType);
            }
            case INSTANCE_OF: {
                return getAssignableGuard(methodType);
            }
            case IS_ARRAY: {
                return Guards.isArray(0, methodType);
            }
            case NONE: {
                return null;
            }
            default: {
                throw new AssertionError();
            }
        }
    }

    private static final MethodHandle IS_DYNAMIC_METHOD = Guards.isInstance(DynamicMethod.class,
            MethodType.methodType(boolean.class, Object.class));
    private static final MethodHandle OBJECT_IDENTITY = MethodHandles.identity(Object.class);

    private GuardedInvocationComponent getMethodGetter(final ComponentLinkRequest req) throws Exception {
        if (req.name == null) {
            return getUnnamedMethodGetter(req);
        }
        return getNamedMethodGetter(req);
    }

    private static MethodType getMethodGetterType(final ComponentLinkRequest req) {
        return req.getDescriptor().getMethodType().changeReturnType(Object.class);
    }

    private GuardedInvocationComponent getUnnamedMethodGetter(final ComponentLinkRequest req) throws Exception {
        assertParameterCount(req.getDescriptor(), 2);
        final GuardedInvocationComponent nextComponent = getNextComponent(req);
        final LinkerServices linkerServices = req.linkerServices;
        final MethodType type = getMethodGetterType(req);
        if(nextComponent == null) {
            return getClassGuardedInvocationComponent(linkerServices.asType(getDynamicMethod, type), type);
        }


        final MethodHandle typedGetter = linkerServices.asType(getDynamicMethod, type);
        final MethodHandle returnMethodHandle = linkerServices.asType(MethodHandles.dropArguments(
                OBJECT_IDENTITY, 1, type.parameterList()), type.insertParameterTypes(0, Object.class));
        final MethodHandle nextComponentInvocation = nextComponent.getGuardedInvocation().getInvocation();
        assert nextComponentInvocation.type().changeReturnType(type.returnType()).equals(type);
        final MethodHandle nextCombinedInvocation = MethodHandles.dropArguments(nextComponentInvocation, 0,
                Object.class);
        final MethodHandle compositeGetter = MethodHandles.foldArguments(MethodHandles.guardWithTest(
                IS_DYNAMIC_METHOD, returnMethodHandle,
                nextCombinedInvocation.asType(nextCombinedInvocation.type().changeReturnType(Object.class))),
                typedGetter);

        return nextComponent.compose(compositeGetter, getClassGuard(type), clazz, ValidationType.EXACT_CLASS);
    }

    private GuardedInvocationComponent getNamedMethodGetter(final ComponentLinkRequest req)
            throws Exception {
        assertParameterCount(req.getDescriptor(), 1);
        final DynamicMethod method = getDynamicMethod(req.name.toString());
        if(method == null) {
            return getNextComponent(req);
        }
        final MethodType type = getMethodGetterType(req);
        return getClassGuardedInvocationComponent(req.linkerServices.asType(MethodHandles.dropArguments(
                MethodHandles.constant(Object.class, method), 0, type.parameterType(0)), type), type);
    }

    static class MethodPair {
        final MethodHandle method1;
        final MethodHandle method2;

        MethodPair(final MethodHandle method1, final MethodHandle method2) {
            this.method1 = method1;
            this.method2 = method2;
        }

        MethodHandle guardWithTest(final MethodHandle test) {
            return MethodHandles.guardWithTest(test, method1, method2);
        }
    }

    static MethodPair matchReturnTypes(final MethodHandle m1, final MethodHandle m2) {
        final MethodType type1 = m1.type();
        final MethodType type2 = m2.type();
        final Class<?> commonRetType = InternalTypeUtilities.getCommonLosslessConversionType(type1.returnType(),
                type2.returnType());
        return new MethodPair(
                m1.asType(type1.changeReturnType(commonRetType)),
                m2.asType(type2.changeReturnType(commonRetType)));
    }

    private static void assertParameterCount(final CallSiteDescriptor descriptor, final int paramCount) {
        if(descriptor.getMethodType().parameterCount() != paramCount) {
            throw new BootstrapMethodError(descriptor.getOperation() + " must have exactly " + paramCount + " parameters.");
        }
    }

    private static final MethodHandle GET_PROPERTY_GETTER_HANDLE = MethodHandles.dropArguments(privateLookup.findOwnSpecial(
            "getPropertyGetterHandle", Object.class, Object.class), 1, Object.class);
    private final MethodHandle getPropertyGetterHandle = GET_PROPERTY_GETTER_HANDLE.bindTo(this);

    /**
     * @param id the property ID
     * @return the method handle for retrieving the property, or null if the property does not exist
     */
    @SuppressWarnings("unused")
    private Object getPropertyGetterHandle(final Object id) {
        return propertyGetters.get(String.valueOf(id));
    }

    private static final MethodHandle GET_PROPERTY_SETTER_HANDLE = MethodHandles.dropArguments(MethodHandles.dropArguments(
            privateLookup.findOwnSpecial("getPropertySetterHandle", MethodHandle.class, CallSiteDescriptor.class,
                    LinkerServices.class, Object.class), 3, Object.class), 5, Object.class);
    private final MethodHandle getPropertySetterHandle = GET_PROPERTY_SETTER_HANDLE.bindTo(this);

    @SuppressWarnings("unused")
    private MethodHandle getPropertySetterHandle(final CallSiteDescriptor setterDescriptor, final LinkerServices linkerServices,
            final Object id) {
        return getDynamicMethodInvocation(setterDescriptor, linkerServices, String.valueOf(id), propertySetters);
    }

    private static final MethodHandle GET_DYNAMIC_METHOD = MethodHandles.dropArguments(privateLookup.findOwnSpecial(
            "getDynamicMethod", Object.class, Object.class), 1, Object.class);
    private final MethodHandle getDynamicMethod = GET_DYNAMIC_METHOD.bindTo(this);

    @SuppressWarnings("unused")
    private Object getDynamicMethod(final Object name) {
        return getDynamicMethod(String.valueOf(name), methods);
    }

    /**
     * Returns a dynamic method of the specified name.
     *
     * @param name name of the method
     * @return the dynamic method (either {@link SimpleDynamicMethod} or {@link OverloadedDynamicMethod}, or null if the
     * method with the specified name does not exist.
     */
    DynamicMethod getDynamicMethod(final String name) {
        return getDynamicMethod(name, methods);
    }

    /**
     * Find the most generic superclass that declares this getter. Since getters have zero args (aside from the
     * receiver), they can't be overloaded, so we're free to link with an instanceof guard for the most generic one,
     * creating more stable call sites.
     * @param getter the getter
     * @return getter with same name, declared on the most generic superclass/interface of the declaring class
     */
    private static Method getMostGenericGetter(final Method getter) {
        return getMostGenericGetter(getter.getName(), getter.getDeclaringClass());
    }

    private static Method getMostGenericGetter(final String name, final Class<?> declaringClass) {
        if(declaringClass == null) {
            return null;
        }
        for(final Class<?> itf: declaringClass.getInterfaces()) {
            final Method itfGetter = getMostGenericGetter(name, itf);
            if(itfGetter != null) {
                return itfGetter;
            }
        }
        final Method superGetter = getMostGenericGetter(name, declaringClass.getSuperclass());
        if(superGetter != null) {
            return superGetter;
        }
        if(!CheckRestrictedPackage.isRestrictedClass(declaringClass)) {
            try {
                return declaringClass.getMethod(name);
            } catch(final NoSuchMethodException e) {
            }
        }
        return null;
    }

    private static final class AnnotatedDynamicMethod {
        private final SingleDynamicMethod method;
        /*private*/ final ValidationType validationType;

        AnnotatedDynamicMethod(final SingleDynamicMethod method, final ValidationType validationType) {
            this.method = method;
            this.validationType = validationType;
        }

        MethodHandle getInvocation(final ComponentLinkRequest req) {
            return method.getInvocation(req.getDescriptor(), req.linkerServices);
        }

        @SuppressWarnings("unused")
        MethodHandle getTarget(final CallSiteDescriptor desc, final LinkerServices linkerServices) {
            final MethodHandle inv = linkerServices.filterInternalObjects(method.getTarget(desc));
            assert inv != null;
            return inv;
        }
    }
}
