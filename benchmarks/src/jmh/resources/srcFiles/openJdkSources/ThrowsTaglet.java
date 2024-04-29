/*
 * Copyright (c) 2001, 2023, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.formats.html.taglets;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.InheritDocTree;
import com.sun.source.doctree.ThrowsTree;

import jdk.javadoc.doclet.Taglet;
import jdk.javadoc.internal.doclets.formats.html.Contents;
import jdk.javadoc.internal.doclets.formats.html.HtmlConfiguration;
import jdk.javadoc.internal.doclets.formats.html.HtmlLinkInfo;
import jdk.javadoc.internal.doclets.formats.html.markup.ContentBuilder;
import jdk.javadoc.internal.doclets.formats.html.markup.HtmlTree;
import jdk.javadoc.internal.doclets.formats.html.Content;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;
import jdk.javadoc.internal.doclets.toolkit.util.VisibleMemberTable;

/**
 * A taglet that processes {@link ThrowsTree}, which represents {@code @throws}
 * and {@code @exception} tags, collectively referred to as exception tags.
 */
public class ThrowsTaglet extends BaseTaglet implements InheritableTaglet {

    /*
     * Relevant bits from JLS
     * ======================
     *
     * This list is _incomplete_ because some parts cannot be summarized here
     * and require careful reading of JLS.
     *
     * 11.1.1 The Kinds of Exceptions
     *
     *   Throwable and all its subclasses are, collectively, the exception
     *   classes.
     *
     * 8.4.6 Method Throws
     *
     *   Throws:
     *     throws ExceptionTypeList
     *
     *   ExceptionTypeList:
     *     ExceptionType {, ExceptionType}
     *
     *   ExceptionType:
     *     ClassType
     *     TypeVariable
     *
     *   It is a compile-time error if an ExceptionType mentioned in a throws
     *   clause is not a subtype (4.10) of Throwable.
     *
     *   Type variables are allowed in a throws clause even though they are
     *   not allowed in a catch clause (14.20).
     *
     *   It is permitted but not required to mention unchecked exception
     *   classes (11.1.1) in a throws clause.
     *
     * 8.1.2 Generic Classes and Type Parameters
     *
     *   It is a compile-time error if a generic class is a direct or indirect
     *   subclass of Throwable.
     *
     * 8.8.5. Constructor Throws
     *
     *   The throws clause for a constructor is identical in structure and
     *   behavior to the throws clause for a method (8.4.6).
     *
     * 8.8. Constructor Declarations
     *
     *   Constructor declarations are ... never inherited and therefore are not
     *   subject to hiding or overriding.
     *
     * 8.4.4. Generic Methods
     *
     *   A method is generic if it declares one or more type variables (4.4).
     *   These type variables are known as the type parameters of the method.
     *
     *   ...
     *
     *   Two methods or constructors M and N have the same type parameters if
     *   both of the following are true:
     *
     *      - M and N have same number of type parameters (possibly zero).
     *      ...
     *
     * 8.4.2. Method Signature
     *
     *   Two methods or constructors, M and N, have the same signature if they
     *   have ... the same type parameters (if any) (8.4.4) ...
     *   ...
     *   The signature of a method m1 is a subsignature of the signature of
     *   a method m2 if either:
     *
     *     - m2 has the same signature as m1, or
     *     - the signature of m1 is the same as the erasure (4.6) of the
     *       signature of m2.
     *
     *   Two method signatures m1 and m2 are override-equivalent iff either
     *   m1 is a subsignature of m2 or m2 is a subsignature of m1.
     *
     * 8.4.8.1. Overriding (by Instance Methods)
     *
     *   An instance method mC declared in or inherited by class C, overrides
     *   from C another method mA declared in class A, iff all of the following
     *   are true:
     *
     *     ...
     *     - The signature of mC is a subsignature (8.4.2) of the signature of
     *       mA as a member of the supertype of C that names A.
     */

    private final HtmlConfiguration config;
    private final Contents contents;

    ThrowsTaglet(HtmlConfiguration config) {
        super(config, DocTree.Kind.THROWS, false, EnumSet.of(Taglet.Location.CONSTRUCTOR, Taglet.Location.METHOD));
        this.config = config;
        contents = config.contents;
    }

    @Override
    public Output inherit(Element dst, Element src, DocTree tag, boolean isFirstSentence) {
        throw newAssertionError(dst, tag, isFirstSentence);
    }

    @Override
    public Content getAllBlockTagOutput(Element holder, TagletWriter tagletWriter) {
        this.tagletWriter = tagletWriter;
        try {
            return getAllBlockTagOutput0(holder);
        } catch (Failure f) {
            var ch = utils.getCommentHelper(f.holder());
            if (f instanceof Failure.ExceptionTypeNotFound e) {
                var path = ch.getDocTreePath(e.tag().getExceptionName());
                messages.warning(path, "doclet.throws.reference_not_found");
            } else if (f instanceof Failure.NotExceptionType e) {
                var path = ch.getDocTreePath(e.tag().getExceptionName());
                messages.warning(path, "doclet.throws.reference_bad_type", diagnosticDescriptionOf(e.type()));
            } else if (f instanceof Failure.Invalid e) {
                messages.error(ch.getDocTreePath(e.tag()), "doclet.inheritDocWithinInappropriateTag");
            } else if (f instanceof Failure.UnsupportedTypeParameter e) {
                var path = ch.getDocTreePath(e.tag().getExceptionName());
                messages.warning(path, "doclet.throwsInheritDocUnsupported");
            } else if (f instanceof Failure.NoOverrideFound e) {
                var path = ch.getDocTreePath(e.inheritDoc);
                messages.error(path, "doclet.inheritDocBadSupertype");
            } else if (f instanceof Failure.Undocumented e) {
                messages.warning(ch.getDocTreePath(e.tag()), "doclet.inheritDocNoDoc", diagnosticDescriptionOf(e.exceptionElement));
            } else {
                throw newAssertionError(f);
            }
        } catch (DocFinder.NoOverriddenMethodFound e) {
            String signature = utils.getSimpleName(holder)
                    + utils.flatSignature((ExecutableElement) holder, tagletWriter.getCurrentPageElement());
            messages.warning(holder, "doclet.noInheritedDoc", signature);
        }
        return tagletWriter.getOutputInstance(); 
    }

    private Content getAllBlockTagOutput0(Element holder)
            throws Failure.ExceptionTypeNotFound,
                   Failure.NotExceptionType,
                   Failure.Invalid,
                   Failure.Undocumented,
                   Failure.UnsupportedTypeParameter,
                   Failure.NoOverrideFound,
            DocFinder.NoOverriddenMethodFound
    {
        ElementKind kind = holder.getKind();
        if (kind != ElementKind.METHOD && kind != ElementKind.CONSTRUCTOR) {
            throw newAssertionError(holder, kind);
        }
        var executable = (ExecutableElement) holder;
        ExecutableType instantiatedType = utils.asInstantiatedMethodType(
                tagletWriter.getCurrentPageElement(), executable);
        List<? extends TypeMirror> substitutedExceptionTypes = instantiatedType.getThrownTypes();
        List<? extends TypeMirror> originalExceptionTypes = executable.getThrownTypes();
        Map<TypeMirror, TypeMirror> typeSubstitutions = getSubstitutedThrownTypes(
                utils.typeUtils,
                originalExceptionTypes,
                substitutedExceptionTypes);
        var exceptionSection = new ExceptionSectionBuilder(tagletWriter, this);
        Set<TypeMirror> alreadyDocumentedExceptions = new HashSet<>();
        List<ThrowsTree> exceptionTags = utils.getThrowsTrees(executable);
        for (ThrowsTree t : exceptionTags) {
            Element exceptionElement = getExceptionType(t, executable);
            outputAnExceptionTagDeeply(exceptionSection, exceptionElement, t, executable, alreadyDocumentedExceptions, typeSubstitutions);
        }
        if (executable.getKind() == ElementKind.METHOD) {
            for (TypeMirror exceptionType : substitutedExceptionTypes) {
                Element exceptionElement = utils.typeUtils.asElement(exceptionType);
                Map<ThrowsTree, ExecutableElement> r;
                try {
                    r = expandShallowly(exceptionElement, executable, Optional.empty());
                } catch (Failure | DocFinder.NoOverriddenMethodFound e) {
                    continue;
                }
                if (r.isEmpty()) {
                    continue;
                }
                if (!alreadyDocumentedExceptions.add(exceptionType)) {
                    continue;
                }
                for (Map.Entry<ThrowsTree, ExecutableElement> e : r.entrySet()) {
                    outputAnExceptionTagDeeply(exceptionSection, exceptionElement, e.getKey(), e.getValue(), alreadyDocumentedExceptions, typeSubstitutions);
                }
            }
        }
        for (TypeMirror e : substitutedExceptionTypes) {
            if (!alreadyDocumentedExceptions.add(e)) {
                continue;
            }
            exceptionSection.beginEntry(e);
            exceptionSection.endEntry();
        }
        assert alreadyDocumentedExceptions.containsAll(substitutedExceptionTypes);
        return exceptionSection.build();
    }

    /**
     * Returns the header for the {@code @throws} tag.
     *
     * @return the header for the throws tag
     */
    private Content getThrowsHeader() {
        return HtmlTree.DT(contents.throws_);
    }

    /**
     * Returns the output for a default {@code @throws} tag.
     *
     * @param throwsType the type that is thrown
     * @param content    the optional content to add as a description
     *
     * @return the output
     */
    private Content throwsTagOutput(TypeMirror throwsType, Optional<Content> content) {
        var htmlWriter = tagletWriter.htmlWriter;
        var linkInfo = new HtmlLinkInfo(config, HtmlLinkInfo.Kind.PLAIN, throwsType);
        var link = htmlWriter.getLink(linkInfo);
        var concat = new ContentBuilder(HtmlTree.CODE(link));
        if (content.isPresent()) {
            concat.add(" - ");
            concat.add(content.get());
        }
        return HtmlTree.DD(concat);
    }

    private void outputAnExceptionTagDeeply(ExceptionSectionBuilder exceptionSection,
                                            Element originalExceptionElement,
                                            ThrowsTree tag,
                                            ExecutableElement holder,
                                            Set<TypeMirror> alreadyDocumentedExceptions,
                                            Map<TypeMirror, TypeMirror> typeSubstitutions)
            throws Failure.ExceptionTypeNotFound,
                   Failure.NotExceptionType,
                   Failure.Invalid,
                   Failure.Undocumented,
                   Failure.UnsupportedTypeParameter,
                   Failure.NoOverrideFound,
            DocFinder.NoOverriddenMethodFound
    {
        outputAnExceptionTagDeeply(exceptionSection, originalExceptionElement, tag, holder, true, alreadyDocumentedExceptions, typeSubstitutions);
    }

    private void outputAnExceptionTagDeeply(ExceptionSectionBuilder exceptionSection,
                                            Element originalExceptionElement,
                                            ThrowsTree tag,
                                            ExecutableElement holder,
                                            boolean beginNewEntry,
                                            Set<TypeMirror> alreadyDocumentedExceptions,
                                            Map<TypeMirror, TypeMirror> typeSubstitutions)
            throws Failure.ExceptionTypeNotFound,
                   Failure.NotExceptionType,
                   Failure.Invalid,
                   Failure.Undocumented,
                   Failure.UnsupportedTypeParameter,
                   Failure.NoOverrideFound,
            DocFinder.NoOverriddenMethodFound {
        var originalExceptionType = originalExceptionElement.asType();
        var exceptionType = typeSubstitutions.getOrDefault(originalExceptionType, originalExceptionType); 
        alreadyDocumentedExceptions.add(exceptionType);
        var description = tag.getDescription();
        int i = indexOfInheritDoc(tag, holder);
        if (i == -1) {

            assert exceptionSection.debugEntryBegun() || beginNewEntry;
            if (beginNewEntry) { 
                exceptionSection.beginEntry(exceptionType);
            }
            exceptionSection.continueEntry(tagletWriter.commentTagsToOutput(holder, description));
            if (beginNewEntry) { 
                exceptionSection.endEntry();
            }
        } else { 
            assert holder.getKind() == ElementKind.METHOD : holder.getKind(); 
            boolean loneInheritDoc = description.size() == 1;
            assert !loneInheritDoc || i == 0 : i;
            boolean add = !loneInheritDoc && beginNewEntry;
            if (add) {
                exceptionSection.beginEntry(exceptionType);
            }
            if (i > 0) {
                assert exceptionSection.debugEntryBegun();
                Content beforeInheritDoc = tagletWriter.commentTagsToOutput(holder, description.subList(0, i));
                exceptionSection.continueEntry(beforeInheritDoc);
            }

            var inheritDoc = (InheritDocTree) tag.getDescription().get(i);
            var ch = utils.getCommentHelper(holder);
            ExecutableElement src = null;
            if (inheritDoc.getSupertype() != null) {
                var supertype = (TypeElement) ch.getReferencedElement(inheritDoc.getSupertype());
                if (supertype == null) {
                    throw new Failure.NoOverrideFound(tag, holder, inheritDoc);
                }
                VisibleMemberTable visibleMemberTable = config.getVisibleMemberTable(supertype);
                List<Element> methods = visibleMemberTable.getAllVisibleMembers(VisibleMemberTable.Kind.METHODS);
                for (Element e : methods) {
                    ExecutableElement m = (ExecutableElement) e;
                    if (utils.elementUtils.overrides(holder, m, (TypeElement) holder.getEnclosingElement())) {
                        assert !holder.equals(m) : Utils.diagnosticDescriptionOf(holder);
                        src = m;
                        break;
                    }
                }
                if (src == null) {
                    throw new Failure.NoOverrideFound(tag, holder, inheritDoc);
                }
            }

            Map<ThrowsTree, ExecutableElement> tags;
            try {
                tags = expandShallowly(originalExceptionElement, holder, Optional.ofNullable(src));
            } catch (Failure.UnsupportedTypeParameter e) {
                throw new Failure.UnsupportedTypeParameter(e.element, tag, holder);
            }
            if (tags.isEmpty()) {
                throw new Failure.Undocumented(tag, holder, originalExceptionElement);
            }
            boolean addNewEntryRecursively = beginNewEntry && !add;
            if (!addNewEntryRecursively && tags.size() > 1) {
                throw new Failure.Invalid(tag, holder);
            }
            for (Map.Entry<ThrowsTree, ExecutableElement> e : tags.entrySet()) {
                outputAnExceptionTagDeeply(exceptionSection, originalExceptionElement, e.getKey(), e.getValue(), addNewEntryRecursively, alreadyDocumentedExceptions, typeSubstitutions);
            }
            if (!loneInheritDoc) {
                Content afterInheritDoc = tagletWriter.commentTagsToOutput(holder, description.subList(i + 1, description.size()));
                exceptionSection.continueEntry(afterInheritDoc);
            }
            if (add) {
                exceptionSection.endEntry();
            }
        }
    }

    private static int indexOfInheritDoc(ThrowsTree tag, ExecutableElement holder)
            throws Failure.Invalid
    {
        var description = tag.getDescription();
        int i = -1;
        for (var iterator = description.listIterator(); iterator.hasNext(); ) {
            DocTree t = iterator.next();
            if (t.getKind() == DocTree.Kind.INHERIT_DOC) {
                if (i != -1) {
                    throw new Failure.Invalid(t, holder);
                }
                i = iterator.previousIndex();
            }
        }
        return i;
    }

    private Element getExceptionType(ThrowsTree tag, ExecutableElement holder)
            throws Failure.ExceptionTypeNotFound, Failure.NotExceptionType
    {
        Element e = utils.getCommentHelper(holder).getException(tag);
        if (e == null) {
            throw new Failure.ExceptionTypeNotFound(tag, holder);
        }
        var t = e.asType();
        var subtypeTestInapplicable = switch (t.getKind()) {
            case EXECUTABLE, PACKAGE, MODULE -> true;
            default -> false;
        };
        if (subtypeTestInapplicable || !utils.typeUtils.isSubtype(t, utils.getThrowableType())) {
            throw new Failure.NotExceptionType(tag, holder, e);
        }
        var k = e.getKind();
        assert k == ElementKind.CLASS || k == ElementKind.TYPE_PARAMETER : k; 
        return e;
    }

    @SuppressWarnings("serial")
    private static sealed class Failure extends Exception {

        private final DocTree tag;
        private final ExecutableElement holder;

        Failure(DocTree tag, ExecutableElement holder) {
            super();
            this.tag = tag;
            this.holder = holder;
        }

        DocTree tag() { return tag; }

        ExecutableElement holder() { return holder; }

        static final class ExceptionTypeNotFound extends Failure {

            ExceptionTypeNotFound(ThrowsTree tag, ExecutableElement holder) {
                super(tag, holder);
            }

            @Override ThrowsTree tag() { return (ThrowsTree) super.tag(); }
        }

        static final class NotExceptionType extends Failure {

            private final Element type;

            public NotExceptionType(ThrowsTree tag, ExecutableElement holder, Element type) {
                super(tag, holder);
                this.type = type;
            }

            Element type() { return type; }

            @Override ThrowsTree tag() { return (ThrowsTree) super.tag(); }
        }

        static final class Invalid extends Failure {

            public Invalid(DocTree tag, ExecutableElement holder) {
                super(tag, holder);
            }
        }

        static final class Undocumented extends Failure {

            private final Element exceptionElement;

            public Undocumented(DocTree tag, ExecutableElement holder, Element exceptionElement) {
                super(tag, holder);
                this.exceptionElement = exceptionElement;
            }
        }

        static final class UnsupportedTypeParameter extends Failure {

            private final Element element;

            public UnsupportedTypeParameter(Element element, ThrowsTree tag, ExecutableElement holder) {
                super(tag, holder);
                this.element = element;
            }

            @Override ThrowsTree tag() { return (ThrowsTree) super.tag(); }
        }

        static final class NoOverrideFound extends Failure {

            private final InheritDocTree inheritDoc;

            public NoOverrideFound(DocTree tag, ExecutableElement holder, InheritDocTree inheritDoc) {
                super(tag, holder);
                this.inheritDoc = inheritDoc;
            }
        }
    }

    /*
     * Returns immediately inherited tags that document the provided exception type.
     *
     * A map associates a doc tree with its holder element externally. Such maps
     * have defined iteration order of entries, whose keys and values
     * are non-null.
     */
    private Map<ThrowsTree, ExecutableElement> expandShallowly(Element exceptionType,
                                                               ExecutableElement holder,
                                                               Optional<ExecutableElement> src)
            throws Failure.ExceptionTypeNotFound,
                   Failure.NotExceptionType,
                   Failure.Invalid,
                   Failure.UnsupportedTypeParameter,
            DocFinder.NoOverriddenMethodFound
    {
        ElementKind kind = exceptionType.getKind();
        DocFinder.Criterion<Map<ThrowsTree, ExecutableElement>, Failure> criterion;
        if (kind == ElementKind.CLASS) {
            criterion = method -> {
                var tags = findByTypeElement(exceptionType, method);
                return toResult(exceptionType, method, tags);
            };
        } else {
            criterion = method -> {
                int i = holder.getTypeParameters().indexOf((TypeParameterElement) exceptionType);
                if (i == -1) { 
                    throw new Failure.UnsupportedTypeParameter(exceptionType, null /* don't know if tag-related */, holder);
                }
                assert utils.elementUtils.overrides(holder, method, (TypeElement) holder.getEnclosingElement());
                var typeParameterElement = method.getTypeParameters().get(i);
                var tags = findByTypeElement(typeParameterElement, method);
                return toResult(exceptionType, method, tags);
            };
        }
        DocFinder.Result<Map<ThrowsTree, ExecutableElement>> result;
        try {
            if (src.isPresent()) {
                result = utils.docFinder().search(src.get(), criterion);
            } else {
                result = utils.docFinder().find(holder, criterion);
            }
        } catch (Failure.NotExceptionType
                 | Failure.ExceptionTypeNotFound
                 | Failure.UnsupportedTypeParameter x) {
            throw x;
        } catch (Failure f) {
            throw newAssertionError(f);
        }
        if (result instanceof DocFinder.Result.Conclude<Map<ThrowsTree, ExecutableElement>> c) {
            return c.value();
        }
        return Map.of(); 
    }

    private static DocFinder.Result<Map<ThrowsTree, ExecutableElement>> toResult(Element target,
                                                                                 ExecutableElement holder,
                                                                                 List<ThrowsTree> tags) {
        if (!tags.isEmpty()) {
            return DocFinder.Result.CONCLUDE(toExceptionTags(holder, tags));
        }
        return DocFinder.Result.CONTINUE();
    }

    /*
     * Associates exception tags with their holder.
     *
     * Such a map is used as a data structure to pass around methods that output tags to content.
     */
    private static Map<ThrowsTree, ExecutableElement> toExceptionTags(ExecutableElement holder,
                                                                      List<ThrowsTree> tags)
    {
        var map = new LinkedHashMap<ThrowsTree, ExecutableElement>();
        for (var t : tags) {
            var prev = map.put(t, holder);
            assert prev == null; 
        }
        return map;
    }

    private List<ThrowsTree> findByTypeElement(Element targetExceptionType,
                                               ExecutableElement executable)
            throws Failure.ExceptionTypeNotFound,
                   Failure.NotExceptionType
    {
        var result = new LinkedList<ThrowsTree>();
        for (ThrowsTree t : utils.getThrowsTrees(executable)) {
            Element candidate = getExceptionType(t, executable);
            if (targetExceptionType.equals(candidate)) {
                result.add(t);
            }
        }
        return List.copyOf(result);
    }

    /*
     * An exception section (that is, the "Throws:" section in the Method
     * or Constructor Details section) builder.
     *
     * The section is being built sequentially from top to bottom.
     *
     * Adapts one-off methods of writer to continuous building.
     */
    private static class ExceptionSectionBuilder {

        private final TagletWriter writer;
        private final ThrowsTaglet taglet;
        private final Content result;
        private Content current;
        private boolean began;
        private boolean headerAdded;
        private TypeMirror exceptionType;

        ExceptionSectionBuilder(TagletWriter writer, ThrowsTaglet taglet) {
            this.writer = writer;
            this.taglet = taglet;
            this.result = writer.getOutputInstance();
        }

        void beginEntry(TypeMirror exceptionType) {
            if (began) {
                throw new IllegalStateException();
            }
            began = true;
            current = writer.getOutputInstance();
            this.exceptionType = exceptionType;
        }

        void continueEntry(Content c) {
            if (!began) {
                throw new IllegalStateException();
            }
            current.add(c);
        }

        public void endEntry() {
            if (!began) {
                throw new IllegalStateException();
            }
            began = false;
            if (!headerAdded) {
                headerAdded = true;
                result.add(taglet.getThrowsHeader());
            }
            result.add(taglet.throwsTagOutput(exceptionType,
                    current.isEmpty() ? Optional.empty() : Optional.of(current)));
            current = null;
        }

        Content build() {
            return result;
        }

        boolean debugEntryBegun() {
            return began;
        }
    }

    /**
     * Returns a map of substitutions for a list of thrown types with the original type-variable
     * as a key and the instantiated type as a value. If no types need to be substituted
     * an empty map is returned.
     * @param declaredThrownTypes the originally declared thrown types.
     * @param instantiatedThrownTypes the thrown types in the context of the current type.
     * @return map of declared to instantiated thrown types or an empty map.
     */
    private Map<TypeMirror, TypeMirror> getSubstitutedThrownTypes(Types types,
                                                                  List<? extends TypeMirror> declaredThrownTypes,
                                                                  List<? extends TypeMirror> instantiatedThrownTypes) {
        Map<TypeMirror, TypeMirror> map = new HashMap<>();
        var i1 = declaredThrownTypes.iterator();
        var i2 = instantiatedThrownTypes.iterator();
        while (i1.hasNext() && i2.hasNext()) {
            TypeMirror t1 = i1.next();
            TypeMirror t2 = i2.next();
            if (!types.isSameType(t1, t2)) {
                map.put(t1, t2);
            }
        }
        assert !i1.hasNext() && !i2.hasNext();
        return Map.copyOf(map);
    }

    private static AssertionError newAssertionError(Object... objects) {
        return new AssertionError(Arrays.toString(objects));
    }

    private static String diagnosticDescriptionOf(Element e) {
        var name = e instanceof QualifiedNameable q ? q.getQualifiedName() : e.getSimpleName();
        return name + " (" + detailedDescriptionOf(e) + ")";
    }

    private static String detailedDescriptionOf(Element e) {
        var lowerCasedKind = e.getKind().toString().toLowerCase(Locale.ROOT);
        var thisElementDescription = lowerCasedKind + " " + switch (e.getKind()) {
            case PACKAGE -> {
                var p = (PackageElement) e;
                yield p.isUnnamed() ? "<unnamed package>" : p.getQualifiedName();
            }
            case MODULE -> {
                var m = (ModuleElement) e;
                yield m.isUnnamed() ? "<unnamed module>" : m.getQualifiedName();
            }
            default -> e.getSimpleName();
        };
        if (e.getEnclosingElement() == null) {
            return thisElementDescription;
        }
        var enclosingElementDescription = detailedDescriptionOf(e.getEnclosingElement());
        return enclosingElementDescription + " " + thisElementDescription;
    }
}
