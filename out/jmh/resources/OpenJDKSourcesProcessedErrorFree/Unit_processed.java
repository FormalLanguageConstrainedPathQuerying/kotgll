/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jshell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import jdk.jshell.ClassTracker.ClassInfo;
import jdk.jshell.Snippet.Kind;
import jdk.jshell.Snippet.Status;
import jdk.jshell.Snippet.SubKind;
import jdk.jshell.TaskFactory.AnalyzeTask;
import jdk.jshell.TaskFactory.CompileTask;
import jdk.jshell.spi.ExecutionControl.ClassBytecodes;
import jdk.jshell.spi.ExecutionControl.ClassInstallException;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;
import static java.util.stream.Collectors.toSet;
import static jdk.internal.jshell.debug.InternalDebugControl.DBG_EVNT;
import static jdk.internal.jshell.debug.InternalDebugControl.DBG_GEN;
import static jdk.internal.jshell.debug.InternalDebugControl.DBG_WRAP;
import static jdk.jshell.Snippet.Status.OVERWRITTEN;
import static jdk.jshell.Snippet.Status.RECOVERABLE_DEFINED;
import static jdk.jshell.Snippet.Status.RECOVERABLE_NOT_DEFINED;
import static jdk.jshell.Snippet.Status.REJECTED;
import static jdk.jshell.Snippet.Status.VALID;
import static jdk.jshell.Util.PARSED_LOCALE;
import static jdk.jshell.Util.expunge;

/**
 * Tracks the compilation and load of a new or updated snippet.
 * @author Robert Field
 */
final class Unit {

    private final JShell state;
    private final Snippet si;
    private final Snippet siOld;
    private final boolean isDependency;
    private final boolean isNew;
    private final Snippet causalSnippet;
    private final DiagList generatedDiagnostics;

    private int seq;
    private String classNameInitial;
    private Wrap activeGuts;
    private Status status;
    private Status prevStatus;
    private boolean signatureChanged;
    private DiagList compilationDiagnostics;
    private DiagList recompilationDiagnostics = null;
    private List<String> unresolved;
    private SnippetEvent replaceOldEvent;
    private List<SnippetEvent> secondaryEvents;
    private boolean isAttemptingCorral;
    private List<ClassInfo> toRedefine;
    private boolean dependenciesNeeded;

    Unit(JShell state, Snippet si, Snippet causalSnippet,
            DiagList generatedDiagnostics) {
        this.state = state;
        this.si = si;
        this.isDependency = causalSnippet != null;
        this.siOld = isDependency
                ? si
                : state.maps.getSnippet(si.key());
        this.isNew = siOld == null;
        this.causalSnippet = causalSnippet;
        this.generatedDiagnostics = generatedDiagnostics;

        this.seq = isNew? 0 : siOld.sequenceNumber();
        this.classNameInitial = isNew? "<none>" : siOld.className();
        this.prevStatus = (isNew || isDependency)
                ? si.status()
                : siOld.status();
        si.setSequenceNumber(seq);
    }

    Unit(JShell state, Snippet si) {
        this.state = state;
        this.si = si;
        this.siOld = null;
        this.isDependency = false;
        this.isNew = false;
        this.causalSnippet = null;
        this.generatedDiagnostics = new DiagList();
        this.prevStatus = si.status();
        si.setDropped();
        this.status = si.status();
    }

    @Override
    public int hashCode() {
        return si.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Unit)
                ? si.equals(((Unit) o).si)
                : false;
    }

    Snippet snippet() {
        return si;
    }

    boolean isDependency() {
        return isDependency;
    }

    void initialize() {
        isAttemptingCorral = false;
        dependenciesNeeded = false;
        toRedefine = null; 
        activeGuts = si.guts();
        markOldDeclarationOverwritten();
    }

    void setWrap(Collection<Unit> exceptUnit, Collection<Unit> plusUnfiltered) {
        if (isImport()) {
            si.setOuterWrap(state.outerMap.wrapImport(activeGuts, si));
        } else {
            List<Unit> units;
            if (snippet().kind() == Kind.METHOD) {
                String name = ((MethodSnippet) snippet()).name();
                units = plusUnfiltered.stream()
                        .filter(u -> u.snippet().kind() == Kind.METHOD &&
                                 ((MethodSnippet) u.snippet()).name().equals(name))
                        .toList();
            } else {
                units = Collections.singletonList(this);
            }
            Set<Key> except = exceptUnit.stream()
                    .map(u -> u.snippet().key())
                    .collect(toSet());
            Collection<Snippet> plus = plusUnfiltered.stream()
                    .filter(u -> !units.contains(u))
                    .map(Unit::snippet)
                    .toList();
            List<Snippet> snippets = units.stream()
                    .map(Unit::snippet)
                    .toList();
            List<Wrap> wraps = units.stream()
                    .map(u -> u.activeGuts)
                    .toList();
            si.setOuterWrap(state.outerMap.wrapInClass(except, plus, snippets, wraps));
            state.debug(DBG_WRAP, "++setWrap() %s\n%s\n",
                    si, si.outerWrap().wrapped());
        }
    }

    void setDiagnostics(AnalyzeTask ct) {
        setDiagnostics(ct.getDiagnostics().ofUnit(this));
    }

    void setDiagnostics(DiagList diags) {
        compilationDiagnostics = diags;
        UnresolvedExtractor ue = new UnresolvedExtractor(diags);
        unresolved = ue.unresolved();
        state.debug(DBG_GEN, "++setCompilationInfo() %s\n%s\n-- diags: %s\n",
                si, si.outerWrap().wrapped(), diags);
    }

    private boolean isRecoverable() {
        return compilationDiagnostics.hasErrors()
                && si instanceof DeclarationSnippet
                && (isDependency()
                    || (si.subKind() != SubKind.VAR_DECLARATION_WITH_INITIALIZER_SUBKIND
                        && compilationDiagnostics.hasResolutionErrorsAndNoOthers()));
    }

    /**
     * If it meets the conditions for corralling, install the corralled wrap
     * @return true is the corralled wrap was installed
     */
    boolean corralIfNeeded(Collection<Unit> working) {
        if (isRecoverable()
                && si.corralled() != null) {
            activeGuts = si.corralled();
            setWrap(working, working);
            return isAttemptingCorral = true;
        }
        return isAttemptingCorral = false;
    }

    void setCorralledDiagnostics(AnalyzeTask cct) {
        recompilationDiagnostics = cct.getDiagnostics().ofUnit(this);
        state.debug(DBG_GEN, "++recomp %s\n%s\n-- diags: %s\n",
                si, si.outerWrap().wrapped(), recompilationDiagnostics);
    }

    boolean smashingErrorDiagnostics(CompileTask ct) {
        if (isDefined()) {
            DiagList dl = ct.getDiagnostics().ofUnit(this);
            if (dl.hasErrors()) {
                setDiagnostics(dl);
                status = RECOVERABLE_NOT_DEFINED;
                state.debug(DBG_GEN, "++smashingErrorDiagnostics %s\n%s\n-- diags: %s\n",
                        si, si.outerWrap().wrapped(), dl);
                return true;
            }
        }
        return false;
    }

    void setStatus(AnalyzeTask at) {
        if (!compilationDiagnostics.hasErrors()) {
            status = VALID;
        } else if (isRecoverable()) {
            if (isAttemptingCorral && !recompilationDiagnostics.hasErrors()) {
                status = RECOVERABLE_DEFINED;
            } else {
                status = RECOVERABLE_NOT_DEFINED;
            }
        } else {
            status = REJECTED;
        }
        checkForOverwrite(at);

        state.debug(DBG_GEN, "setStatus() %s - status: %s\n",
                si, status);
    }

    boolean isDefined() {
        return status.isDefined();
    }

    /**
     * Process the class information from the last compile. Requires loading of
     * returned list.
     *
     * @return the list of classes to load
     */
    Stream<ClassBytecodes> classesToLoad(List<String> classnames) {
        toRedefine = new ArrayList<>();
        List<ClassBytecodes> toLoad = new ArrayList<>();
        if (status.isDefined() && !isImport()) {
            for (String cn : classnames) {
                ClassInfo ci = state.classTracker.get(cn);
                if (ci.isLoaded()) {
                    if (ci.isCurrent()) {
                    } else {
                        toRedefine.add(ci);
                    }
                } else {
                    toLoad.add(ci.toClassBytecodes());
                    dependenciesNeeded = true;
                }
            }
        }
        return toLoad.stream();
    }

    /**
     * Redefine classes needing redefine. classesToLoad() must be called first.
     *
     * @return true if all redefines succeeded (can be vacuously true)
     */
    boolean doRedefines() {
        if (toRedefine.isEmpty()) {
            return true;
        }
        ClassBytecodes[] cbcs = toRedefine.stream()
                .map(ClassInfo::toClassBytecodes)
                .toArray(ClassBytecodes[]::new);
        try {
            state.executionControl().redefine(cbcs);
            state.classTracker.markLoaded(cbcs);
            return true;
        } catch (ClassInstallException ex) {
            state.classTracker.markLoaded(cbcs, ex.installed());
            return false;
        } catch (EngineTerminationException ex) {
            state.closeDown();
            return false;
        } catch (NotImplementedException ex) {
            return false;
        }
    }

    void markForReplacement() {
        si.setSequenceNumber(++seq);
    }

    private boolean isImport() {
        return si.kind() == Kind.IMPORT;
    }

    private boolean sigChanged() {
        return (status.isDefined() != prevStatus.isDefined())
                || (status.isDefined() && !si.className().equals(classNameInitial))
                || signatureChanged;
    }

    Stream<Unit> effectedDependents() {
        return sigChanged() || dependenciesNeeded || status == RECOVERABLE_NOT_DEFINED
                ? dependents()
                : Stream.empty();
    }

    Stream<Unit> dependents() {
        return state.maps.getDependents(si)
                    .stream()
                    .filter(xsi -> xsi != si && xsi.status().isActive())
                    .map(xsi -> new Unit(state, xsi, si, new DiagList()));
    }

    void finish() {
        recordCompilation();
        state.maps.installSnippet(si);
    }

    private void markOldDeclarationOverwritten() {
        if (si != siOld && siOld != null && siOld.status().isActive()) {
            replaceOldEvent = new SnippetEvent(siOld,
                    siOld.status(), OVERWRITTEN,
                    false, si, null, null);
            siOld.setOverwritten();
        }
    }

    private DiagList computeDiagnostics() {
        DiagList diagnostics = new DiagList();
        DiagList diags = compilationDiagnostics;
        if (status == RECOVERABLE_DEFINED || status == RECOVERABLE_NOT_DEFINED) {
            UnresolvedExtractor ue = new UnresolvedExtractor(diags);
            diagnostics.addAll(ue.otherAll());
        } else {
            unresolved = Collections.emptyList();
            diagnostics.addAll(diags);
        }
        diagnostics.addAll(generatedDiagnostics);
        return diagnostics;
    }

    private void recordCompilation() {
        state.maps.mapDependencies(si);
        DiagList diags = computeDiagnostics();
        si.setCompilationStatus(status, unresolved, diags);
        state.debug(DBG_GEN, "recordCompilation: %s -- status %s, unresolved %s\n",
                si, status, unresolved);
    }

    private void checkForOverwrite(AnalyzeTask at) {
        secondaryEvents = new ArrayList<>();
        if (replaceOldEvent != null) secondaryEvents.add(replaceOldEvent);

        if (si.kind() == Kind.METHOD && status.isDefined()) {
            MethodSnippet msi = (MethodSnippet) si;
            msi.setQualifiedParameterTypes(
                    computeQualifiedParameterTypes(at, msi));
            Status overwrittenStatus = overwriteMatchingMethod(msi);
            if (overwrittenStatus != null) {
                prevStatus = overwrittenStatus;
                signatureChanged = true;
            }
        }
    }

    private Status overwriteMatchingMethod(MethodSnippet msi) {
        String qpt = msi.qualifiedParameterTypes();
        List<MethodSnippet> matching = state.methods()
                .filter(sn ->
                           sn != null
                        && sn != msi
                        && sn.status().isActive()
                        && sn.name().equals(msi.name())
                        && qpt.equals(sn.qualifiedParameterTypes()))
                .toList();

        Status overwrittenStatus = null;
        for (MethodSnippet sn : matching) {
            overwrittenStatus = sn.status();
            SnippetEvent se = new SnippetEvent(
                    sn, overwrittenStatus, OVERWRITTEN,
                    false, msi, null, null);
            sn.setOverwritten();
            secondaryEvents.add(se);
            state.debug(DBG_EVNT,
                    "Overwrite event #%d -- key: %s before: %s status: %s sig: %b cause: %s\n",
                    secondaryEvents.size(), se.snippet(), se.previousStatus(),
                    se.status(), se.isSignatureChange(), se.causeSnippet());
        }
        return overwrittenStatus;
    }

    private String computeQualifiedParameterTypes(AnalyzeTask at, MethodSnippet msi) {
        String rawSig = TreeDissector.createBySnippet(at, msi).typeOfMethod(msi);
        String signature = expunge(rawSig);
        int paren = signature.lastIndexOf(')');

        return paren >= 0
                ? signature.substring(0, paren + 1)
                : msi.parameterTypes();
    }

    SnippetEvent event(String value, JShellException exception) {
        boolean wasSignatureChanged = sigChanged();
        state.debug(DBG_EVNT, "Snippet: %s id: %s before: %s status: %s sig: %b cause: %s\n",
                si, si.id(), prevStatus, si.status(), wasSignatureChanged, causalSnippet);
        return new SnippetEvent(si, prevStatus, si.status(),
                wasSignatureChanged, causalSnippet, value, exception);
    }

    List<SnippetEvent> secondaryEvents() {
        return secondaryEvents==null
                ? Collections.emptyList()
                : secondaryEvents;
    }

    @Override
    public String toString() {
        return "Unit(" + si.name() + ")";
    }

    /**
     * Separate out the unresolvedDependencies errors from both the other
     * corralling errors and the overall errors.
     */
    private static class UnresolvedExtractor {

        private static final String RESOLVE_ERROR_SYMBOL = "symbol:";
        private static final String RESOLVE_ERROR_LOCATION = "location:";

        private final Set<String> unresolved = new LinkedHashSet<>();
        private final DiagList otherErrors = new DiagList();
        private final DiagList otherAll = new DiagList();

        UnresolvedExtractor(DiagList diags) {
            for (Diag diag : diags) {
                if (diag.isError()) {
                    if (diag.isResolutionError()) {
                        String m = diag.getMessage(PARSED_LOCALE);
                        int symPos = m.indexOf(RESOLVE_ERROR_SYMBOL);
                        if (symPos >= 0) {
                            m = m.substring(symPos + RESOLVE_ERROR_SYMBOL.length());
                            int symLoc = m.indexOf(RESOLVE_ERROR_LOCATION);
                            if (symLoc >= 0) {
                                m = m.substring(0, symLoc);
                            }
                            m = m.trim();
                            unresolved.add(m);
                            continue;
                        }
                    }
                    otherErrors.add(diag);
                }
                otherAll.add(diag);
            }
        }

        DiagList otherCorralledErrors() {
            return otherErrors;
        }

        DiagList otherAll() {
            return otherAll;
        }

        List<String> unresolved() {
            return new ArrayList<>(unresolved);
        }
    }
}
