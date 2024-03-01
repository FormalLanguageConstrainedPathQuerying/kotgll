/*
 * Copyright (c) 1999, 2023, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.lang.model.element.ElementKind;
import javax.tools.JavaFileObject;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Lint.LintCategory;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.Types.FunctionDescriptorLookupError;
import com.sun.tools.javac.comp.ArgumentAttr.LocalCacheContext;
import com.sun.tools.javac.comp.Check.CheckContext;
import com.sun.tools.javac.comp.DeferredAttr.AttrMode;
import com.sun.tools.javac.comp.MatchBindingsComputer.MatchBindings;
import com.sun.tools.javac.jvm.*;

import static com.sun.tools.javac.resources.CompilerProperties.Fragments.Diamond;
import static com.sun.tools.javac.resources.CompilerProperties.Fragments.DiamondInvalidArg;
import static com.sun.tools.javac.resources.CompilerProperties.Fragments.DiamondInvalidArgs;

import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Fragments;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.JCTree.JCPolyExpression.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.JCDiagnostic.Error;
import com.sun.tools.javac.util.JCDiagnostic.Fragment;
import com.sun.tools.javac.util.JCDiagnostic.Warning;
import com.sun.tools.javac.util.List;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Flags.ANNOTATION;
import static com.sun.tools.javac.code.Flags.BLOCK;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.code.TypeTag.*;
import static com.sun.tools.javac.code.TypeTag.WILDCARD;
import static com.sun.tools.javac.tree.JCTree.Tag.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag;

/** This is the main context-dependent analysis phase in GJC. It
 *  encompasses name resolution, type checking and constant folding as
 *  subtasks. Some subtasks involve auxiliary classes.
 *  @see Check
 *  @see Resolve
 *  @see ConstFold
 *  @see Infer
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Attr extends JCTree.Visitor {
    protected static final Context.Key<Attr> attrKey = new Context.Key<>();

    final Names names;
    final Log log;
    final Symtab syms;
    final Resolve rs;
    final Operators operators;
    final Infer infer;
    final Analyzer analyzer;
    final DeferredAttr deferredAttr;
    final Check chk;
    final Flow flow;
    final MemberEnter memberEnter;
    final TypeEnter typeEnter;
    final TreeMaker make;
    final ConstFold cfolder;
    final Enter enter;
    final Target target;
    final Types types;
    final Preview preview;
    final JCDiagnostic.Factory diags;
    final TypeAnnotations typeAnnotations;
    final DeferredLintHandler deferredLintHandler;
    final TypeEnvs typeEnvs;
    final Dependencies dependencies;
    final Annotate annotate;
    final ArgumentAttr argumentAttr;
    final MatchBindingsComputer matchBindingsComputer;
    final AttrRecover attrRecover;

    public static Attr instance(Context context) {
        Attr instance = context.get(attrKey);
        if (instance == null)
            instance = new Attr(context);
        return instance;
    }

    @SuppressWarnings("this-escape")
    protected Attr(Context context) {
        context.put(attrKey, this);

        names = Names.instance(context);
        log = Log.instance(context);
        syms = Symtab.instance(context);
        rs = Resolve.instance(context);
        operators = Operators.instance(context);
        chk = Check.instance(context);
        flow = Flow.instance(context);
        memberEnter = MemberEnter.instance(context);
        typeEnter = TypeEnter.instance(context);
        make = TreeMaker.instance(context);
        enter = Enter.instance(context);
        infer = Infer.instance(context);
        analyzer = Analyzer.instance(context);
        deferredAttr = DeferredAttr.instance(context);
        cfolder = ConstFold.instance(context);
        target = Target.instance(context);
        types = Types.instance(context);
        preview = Preview.instance(context);
        diags = JCDiagnostic.Factory.instance(context);
        annotate = Annotate.instance(context);
        typeAnnotations = TypeAnnotations.instance(context);
        deferredLintHandler = DeferredLintHandler.instance(context);
        typeEnvs = TypeEnvs.instance(context);
        dependencies = Dependencies.instance(context);
        argumentAttr = ArgumentAttr.instance(context);
        matchBindingsComputer = MatchBindingsComputer.instance(context);
        attrRecover = AttrRecover.instance(context);

        Options options = Options.instance(context);

        Source source = Source.instance(context);
        allowReifiableTypesInInstanceof = Feature.REIFIABLE_TYPES_INSTANCEOF.allowedInSource(source);
        allowRecords = Feature.RECORDS.allowedInSource(source);
        allowPatternSwitch = (preview.isEnabled() || !preview.isPreview(Feature.PATTERN_SWITCH)) &&
                             Feature.PATTERN_SWITCH.allowedInSource(source);
        allowUnconditionalPatternsInstanceOf =
                             Feature.UNCONDITIONAL_PATTERN_IN_INSTANCEOF.allowedInSource(source);
        sourceName = source.name;
        useBeforeDeclarationWarning = options.isSet("useBeforeDeclarationWarning");

        statInfo = new ResultInfo(KindSelector.NIL, Type.noType);
        varAssignmentInfo = new ResultInfo(KindSelector.ASG, Type.noType);
        unknownExprInfo = new ResultInfo(KindSelector.VAL, Type.noType);
        methodAttrInfo = new MethodAttrInfo();
        unknownTypeInfo = new ResultInfo(KindSelector.TYP, Type.noType);
        unknownTypeExprInfo = new ResultInfo(KindSelector.VAL_TYP, Type.noType);
        recoveryInfo = new RecoveryInfo(deferredAttr.emptyDeferredAttrContext);
    }

    /** Switch: reifiable types in instanceof enabled?
     */
    boolean allowReifiableTypesInInstanceof;

    /** Are records allowed
     */
    private final boolean allowRecords;

    /** Are patterns in switch allowed
     */
    private final boolean allowPatternSwitch;

    /** Are unconditional patterns in instanceof allowed
     */
    private final boolean allowUnconditionalPatternsInstanceOf;

    /**
     * Switch: warn about use of variable before declaration?
     * RFE: 6425594
     */
    boolean useBeforeDeclarationWarning;

    /**
     * Switch: name of source level; used for error reporting.
     */
    String sourceName;

    /** Check kind and type of given tree against protokind and prototype.
     *  If check succeeds, store type in tree and return it.
     *  If check fails, store errType in tree and return it.
     *  No checks are performed if the prototype is a method type.
     *  It is not necessary in this case since we know that kind and type
     *  are correct.
     *
     *  @param tree     The tree whose kind and type is checked
     *  @param found    The computed type of the tree
     *  @param ownkind  The computed kind of the tree
     *  @param resultInfo  The expected result of the tree
     */
    Type check(final JCTree tree,
               final Type found,
               final KindSelector ownkind,
               final ResultInfo resultInfo) {
        InferenceContext inferenceContext = resultInfo.checkContext.inferenceContext();
        Type owntype;
        boolean shouldCheck = !found.hasTag(ERROR) &&
                !resultInfo.pt.hasTag(METHOD) &&
                !resultInfo.pt.hasTag(FORALL);
        if (shouldCheck && !ownkind.subset(resultInfo.pkind)) {
            log.error(tree.pos(),
                      Errors.UnexpectedType(resultInfo.pkind.kindNames(),
                                            ownkind.kindNames()));
            owntype = types.createErrorType(found);
        } else if (inferenceContext.free(found)) {
            owntype = shouldCheck ? resultInfo.pt : found;
            if (resultInfo.checkMode.installPostInferenceHook()) {
                inferenceContext.addFreeTypeListener(List.of(found),
                        instantiatedContext -> {
                            ResultInfo pendingResult =
                                    resultInfo.dup(inferenceContext.asInstType(resultInfo.pt));
                            check(tree, inferenceContext.asInstType(found), ownkind, pendingResult);
                        });
            }
        } else {
            owntype = shouldCheck ?
            resultInfo.check(tree, found) :
            found;
        }
        if (resultInfo.checkMode.updateTreeType()) {
            tree.type = owntype;
        }
        return owntype;
    }

    /** Is given blank final variable assignable, i.e. in a scope where it
     *  may be assigned to even though it is final?
     *  @param v      The blank final variable.
     *  @param env    The current environment.
     */
    boolean isAssignableAsBlankFinal(VarSymbol v, Env<AttrContext> env) {
        Symbol owner = env.info.scope.owner;
        boolean isAssignable =
            v.owner == owner
            ||
            ((owner.name == names.init ||    
              owner.kind == VAR ||           
              (owner.flags() & BLOCK) != 0)  
             &&
             v.owner == owner.owner
             &&
             ((v.flags() & STATIC) != 0) == Resolve.isStatic(env));
        boolean insideCompactConstructor = env.enclMethod != null && TreeInfo.isCompactConstructor(env.enclMethod);
        return isAssignable & !insideCompactConstructor;
    }

    /** Check that variable can be assigned to.
     *  @param pos    The current source code position.
     *  @param v      The assigned variable
     *  @param base   If the variable is referred to in a Select, the part
     *                to the left of the `.', null otherwise.
     *  @param env    The current environment.
     */
    void checkAssignable(DiagnosticPosition pos, VarSymbol v, JCTree base, Env<AttrContext> env) {
        if (v.name == names._this) {
            log.error(pos, Errors.CantAssignValToThis);
        } else if ((v.flags() & FINAL) != 0 &&
            ((v.flags() & HASINIT) != 0
             ||
             !((base == null ||
               TreeInfo.isThisQualifier(base)) &&
               isAssignableAsBlankFinal(v, env)))) {
            if (v.isResourceVariable()) { 
                log.error(pos, Errors.TryResourceMayNotBeAssigned(v));
            } else {
                log.error(pos, Errors.CantAssignValToVar(Flags.toSource(v.flags() & (STATIC | FINAL)), v));
            }
        }
    }

    /** Does tree represent a static reference to an identifier?
     *  It is assumed that tree is either a SELECT or an IDENT.
     *  We have to weed out selects from non-type names here.
     *  @param tree    The candidate tree.
     */
    boolean isStaticReference(JCTree tree) {
        if (tree.hasTag(SELECT)) {
            Symbol lsym = TreeInfo.symbol(((JCFieldAccess) tree).selected);
            if (lsym == null || lsym.kind != TYP) {
                return false;
            }
        }
        return true;
    }

    /** Is this symbol a type?
     */
    static boolean isType(Symbol sym) {
        return sym != null && sym.kind == TYP;
    }

    /** The current `this' symbol.
     *  @param env    The current environment.
     */
    Symbol thisSym(DiagnosticPosition pos, Env<AttrContext> env) {
        return rs.resolveSelf(pos, env, env.enclClass.sym, names._this);
    }

    /** Attribute a parsed identifier.
     * @param tree Parsed identifier name
     * @param topLevel The toplevel to use
     */
    public Symbol attribIdent(JCTree tree, JCCompilationUnit topLevel) {
        Env<AttrContext> localEnv = enter.topLevelEnv(topLevel);
        localEnv.enclClass = make.ClassDef(make.Modifiers(0),
                                           syms.errSymbol.name,
                                           null, null, null, null);
        localEnv.enclClass.sym = syms.errSymbol;
        return attribIdent(tree, localEnv);
    }

    /** Attribute a parsed identifier.
     * @param tree Parsed identifier name
     * @param env The env to use
     */
    public Symbol attribIdent(JCTree tree, Env<AttrContext> env) {
        return tree.accept(identAttributer, env);
    }
        private TreeVisitor<Symbol,Env<AttrContext>> identAttributer = new IdentAttributer();
        private class IdentAttributer extends SimpleTreeVisitor<Symbol,Env<AttrContext>> {
            @Override @DefinedBy(Api.COMPILER_TREE)
            public Symbol visitMemberSelect(MemberSelectTree node, Env<AttrContext> env) {
                Symbol site = visit(node.getExpression(), env);
                if (site.kind == ERR || site.kind == ABSENT_TYP || site.kind == HIDDEN)
                    return site;
                Name name = (Name)node.getIdentifier();
                if (site.kind == PCK) {
                    env.toplevel.packge = (PackageSymbol)site;
                    return rs.findIdentInPackage(null, env, (TypeSymbol)site, name,
                            KindSelector.TYP_PCK);
                } else {
                    env.enclClass.sym = (ClassSymbol)site;
                    return rs.findMemberType(env, site.asType(), name, (TypeSymbol)site);
                }
            }

            @Override @DefinedBy(Api.COMPILER_TREE)
            public Symbol visitIdentifier(IdentifierTree node, Env<AttrContext> env) {
                return rs.findIdent(null, env, (Name)node.getName(), KindSelector.TYP_PCK);
            }
        }

    public Type coerce(Type etype, Type ttype) {
        return cfolder.coerce(etype, ttype);
    }

    public Type attribType(JCTree node, TypeSymbol sym) {
        Env<AttrContext> env = typeEnvs.get(sym);
        Env<AttrContext> localEnv = env.dup(node, env.info.dup());
        return attribTree(node, localEnv, unknownTypeInfo);
    }

    public Type attribImportQualifier(JCImport tree, Env<AttrContext> env) {
        JCFieldAccess s = tree.qualid;
        return attribTree(s.selected, env,
                          new ResultInfo(tree.staticImport ?
                                         KindSelector.TYP : KindSelector.TYP_PCK,
                       Type.noType));
    }

    public Env<AttrContext> attribExprToTree(JCTree expr, Env<AttrContext> env, JCTree tree) {
        return attribToTree(expr, env, tree, unknownExprInfo);
    }

    public Env<AttrContext> attribStatToTree(JCTree stmt, Env<AttrContext> env, JCTree tree) {
        return attribToTree(stmt, env, tree, statInfo);
    }

    private Env<AttrContext> attribToTree(JCTree root, Env<AttrContext> env, JCTree tree, ResultInfo resultInfo) {
        breakTree = tree;
        JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
        try {
            deferredAttr.attribSpeculative(root, env, resultInfo,
                    null, DeferredAttr.AttributionMode.ATTRIB_TO_TREE,
                    argumentAttr.withLocalCacheContext());
            attrRecover.doRecovery();
        } catch (BreakAttr b) {
            return b.env;
        } catch (AssertionError ae) {
            if (ae.getCause() instanceof BreakAttr breakAttr) {
                return breakAttr.env;
            } else {
                throw ae;
            }
        } finally {
            breakTree = null;
            log.useSource(prev);
        }
        return env;
    }

    private JCTree breakTree = null;

    private static class BreakAttr extends RuntimeException {
        static final long serialVersionUID = -6924771130405446405L;
        private transient Env<AttrContext> env;
        private BreakAttr(Env<AttrContext> env) {
            this.env = env;
        }
    }

    /**
     * Mode controlling behavior of Attr.Check
     */
    enum CheckMode {

        NORMAL,

        /**
         * Mode signalling 'fake check' - skip tree update. A side-effect of this mode is
         * that the captured var cache in {@code InferenceContext} will be used in read-only
         * mode when performing inference checks.
         */
        NO_TREE_UPDATE {
            @Override
            public boolean updateTreeType() {
                return false;
            }
        },
        /**
         * Mode signalling that caller will manage free types in tree decorations.
         */
        NO_INFERENCE_HOOK {
            @Override
            public boolean installPostInferenceHook() {
                return false;
            }
        };

        public boolean updateTreeType() {
            return true;
        }
        public boolean installPostInferenceHook() {
            return true;
        }
    }


    class ResultInfo {
        final KindSelector pkind;
        final Type pt;
        final CheckContext checkContext;
        final CheckMode checkMode;

        ResultInfo(KindSelector pkind, Type pt) {
            this(pkind, pt, chk.basicHandler, CheckMode.NORMAL);
        }

        ResultInfo(KindSelector pkind, Type pt, CheckMode checkMode) {
            this(pkind, pt, chk.basicHandler, checkMode);
        }

        protected ResultInfo(KindSelector pkind,
                             Type pt, CheckContext checkContext) {
            this(pkind, pt, checkContext, CheckMode.NORMAL);
        }

        protected ResultInfo(KindSelector pkind,
                             Type pt, CheckContext checkContext, CheckMode checkMode) {
            this.pkind = pkind;
            this.pt = pt;
            this.checkContext = checkContext;
            this.checkMode = checkMode;
        }

        /**
         * Should {@link Attr#attribTree} use the {@code ArgumentAttr} visitor instead of this one?
         * @param tree The tree to be type-checked.
         * @return true if {@code ArgumentAttr} should be used.
         */
        protected boolean needsArgumentAttr(JCTree tree) { return false; }

        protected Type check(final DiagnosticPosition pos, final Type found) {
            return chk.checkType(pos, found, pt, checkContext);
        }

        protected ResultInfo dup(Type newPt) {
            return new ResultInfo(pkind, newPt, checkContext, checkMode);
        }

        protected ResultInfo dup(CheckContext newContext) {
            return new ResultInfo(pkind, pt, newContext, checkMode);
        }

        protected ResultInfo dup(Type newPt, CheckContext newContext) {
            return new ResultInfo(pkind, newPt, newContext, checkMode);
        }

        protected ResultInfo dup(Type newPt, CheckContext newContext, CheckMode newMode) {
            return new ResultInfo(pkind, newPt, newContext, newMode);
        }

        protected ResultInfo dup(CheckMode newMode) {
            return new ResultInfo(pkind, pt, checkContext, newMode);
        }

        @Override
        public String toString() {
            if (pt != null) {
                return pt.toString();
            } else {
                return "";
            }
        }
    }

    class MethodAttrInfo extends ResultInfo {
        public MethodAttrInfo() {
            this(chk.basicHandler);
        }

        public MethodAttrInfo(CheckContext checkContext) {
            super(KindSelector.VAL, Infer.anyPoly, checkContext);
        }

        @Override
        protected boolean needsArgumentAttr(JCTree tree) {
            return true;
        }

        protected ResultInfo dup(Type newPt) {
            throw new IllegalStateException();
        }

        protected ResultInfo dup(CheckContext newContext) {
            return new MethodAttrInfo(newContext);
        }

        protected ResultInfo dup(Type newPt, CheckContext newContext) {
            throw new IllegalStateException();
        }

        protected ResultInfo dup(Type newPt, CheckContext newContext, CheckMode newMode) {
            throw new IllegalStateException();
        }

        protected ResultInfo dup(CheckMode newMode) {
            throw new IllegalStateException();
        }
    }

    class RecoveryInfo extends ResultInfo {

        public RecoveryInfo(final DeferredAttr.DeferredAttrContext deferredAttrContext) {
            this(deferredAttrContext, Type.recoveryType);
        }

        public RecoveryInfo(final DeferredAttr.DeferredAttrContext deferredAttrContext, Type pt) {
            super(KindSelector.VAL, pt, new Check.NestedCheckContext(chk.basicHandler) {
                @Override
                public DeferredAttr.DeferredAttrContext deferredAttrContext() {
                    return deferredAttrContext;
                }
                @Override
                public boolean compatible(Type found, Type req, Warner warn) {
                    return true;
                }
                @Override
                public void report(DiagnosticPosition pos, JCDiagnostic details) {
                    boolean needsReport = pt == Type.recoveryType ||
                            (details.getDiagnosticPosition() != null &&
                            details.getDiagnosticPosition().getTree().hasTag(LAMBDA));
                    if (needsReport) {
                        chk.basicHandler.report(pos, details);
                    }
                }
            });
        }
    }

    final ResultInfo statInfo;
    final ResultInfo varAssignmentInfo;
    final ResultInfo methodAttrInfo;
    final ResultInfo unknownExprInfo;
    final ResultInfo unknownTypeInfo;
    final ResultInfo unknownTypeExprInfo;
    final ResultInfo recoveryInfo;

    Type pt() {
        return resultInfo.pt;
    }

    KindSelector pkind() {
        return resultInfo.pkind;
    }

/* ************************************************************************
 * Visitor methods
 *************************************************************************/

    /** Visitor argument: the current environment.
     */
    Env<AttrContext> env;

    /** Visitor argument: the currently expected attribution result.
     */
    ResultInfo resultInfo;

    /** Visitor result: the computed type.
     */
    Type result;

    MatchBindings matchBindings = MatchBindingsComputer.EMPTY;

    /** Visitor method: attribute a tree, catching any completion failure
     *  exceptions. Return the tree's type.
     *
     *  @param tree    The tree to be visited.
     *  @param env     The environment visitor argument.
     *  @param resultInfo   The result info visitor argument.
     */
    Type attribTree(JCTree tree, Env<AttrContext> env, ResultInfo resultInfo) {
        Env<AttrContext> prevEnv = this.env;
        ResultInfo prevResult = this.resultInfo;
        try {
            this.env = env;
            this.resultInfo = resultInfo;
            if (resultInfo.needsArgumentAttr(tree)) {
                result = argumentAttr.attribArg(tree, env);
            } else {
                tree.accept(this);
            }
            matchBindings = matchBindingsComputer.finishBindings(tree,
                                                                 matchBindings);
            if (tree == breakTree &&
                    resultInfo.checkContext.deferredAttrContext().mode == AttrMode.CHECK) {
                breakTreeFound(copyEnv(env));
            }
            return result;
        } catch (CompletionFailure ex) {
            tree.type = syms.errType;
            return chk.completionError(tree.pos(), ex);
        } finally {
            this.env = prevEnv;
            this.resultInfo = prevResult;
        }
    }

    protected void breakTreeFound(Env<AttrContext> env) {
        throw new BreakAttr(env);
    }

    Env<AttrContext> copyEnv(Env<AttrContext> env) {
        Env<AttrContext> newEnv =
                env.dup(env.tree, env.info.dup(copyScope(env.info.scope)));
        if (newEnv.outer != null) {
            newEnv.outer = copyEnv(newEnv.outer);
        }
        return newEnv;
    }

    WriteableScope copyScope(WriteableScope sc) {
        WriteableScope newScope = WriteableScope.create(sc.owner);
        List<Symbol> elemsList = List.nil();
        for (Symbol sym : sc.getSymbols()) {
            elemsList = elemsList.prepend(sym);
        }
        for (Symbol s : elemsList) {
            newScope.enter(s);
        }
        return newScope;
    }

    /** Derived visitor method: attribute an expression tree.
     */
    public Type attribExpr(JCTree tree, Env<AttrContext> env, Type pt) {
        return attribTree(tree, env, new ResultInfo(KindSelector.VAL, !pt.hasTag(ERROR) ? pt : Type.noType));
    }

    /** Derived visitor method: attribute an expression tree with
     *  no constraints on the computed type.
     */
    public Type attribExpr(JCTree tree, Env<AttrContext> env) {
        return attribTree(tree, env, unknownExprInfo);
    }

    /** Derived visitor method: attribute a type tree.
     */
    public Type attribType(JCTree tree, Env<AttrContext> env) {
        Type result = attribType(tree, env, Type.noType);
        return result;
    }

    /** Derived visitor method: attribute a type tree.
     */
    Type attribType(JCTree tree, Env<AttrContext> env, Type pt) {
        Type result = attribTree(tree, env, new ResultInfo(KindSelector.TYP, pt));
        return result;
    }

    /** Derived visitor method: attribute a statement or definition tree.
     */
    public Type attribStat(JCTree tree, Env<AttrContext> env) {
        Env<AttrContext> analyzeEnv = analyzer.copyEnvIfNeeded(tree, env);
        Type result = attribTree(tree, env, statInfo);
        analyzer.analyzeIfNeeded(tree, analyzeEnv);
        attrRecover.doRecovery();
        return result;
    }

    /** Attribute a list of expressions, returning a list of types.
     */
    List<Type> attribExprs(List<JCExpression> trees, Env<AttrContext> env, Type pt) {
        ListBuffer<Type> ts = new ListBuffer<>();
        for (List<JCExpression> l = trees; l.nonEmpty(); l = l.tail)
            ts.append(attribExpr(l.head, env, pt));
        return ts.toList();
    }

    /** Attribute a list of statements, returning nothing.
     */
    <T extends JCTree> void attribStats(List<T> trees, Env<AttrContext> env) {
        for (List<T> l = trees; l.nonEmpty(); l = l.tail)
            attribStat(l.head, env);
    }

    /** Attribute the arguments in a method call, returning the method kind.
     */
    KindSelector attribArgs(KindSelector initialKind, List<JCExpression> trees, Env<AttrContext> env, ListBuffer<Type> argtypes) {
        KindSelector kind = initialKind;
        for (JCExpression arg : trees) {
            Type argtype = chk.checkNonVoid(arg, attribTree(arg, env, methodAttrInfo));
            if (argtype.hasTag(DEFERRED)) {
                kind = KindSelector.of(KindSelector.POLY, kind);
            }
            argtypes.append(argtype);
        }
        return kind;
    }

    /** Attribute a type argument list, returning a list of types.
     *  Caller is responsible for calling checkRefTypes.
     */
    List<Type> attribAnyTypes(List<JCExpression> trees, Env<AttrContext> env) {
        ListBuffer<Type> argtypes = new ListBuffer<>();
        for (List<JCExpression> l = trees; l.nonEmpty(); l = l.tail)
            argtypes.append(attribType(l.head, env));
        return argtypes.toList();
    }

    /** Attribute a type argument list, returning a list of types.
     *  Check that all the types are references.
     */
    List<Type> attribTypes(List<JCExpression> trees, Env<AttrContext> env) {
        List<Type> types = attribAnyTypes(trees, env);
        return chk.checkRefTypes(trees, types);
    }

    /**
     * Attribute type variables (of generic classes or methods).
     * Compound types are attributed later in attribBounds.
     * @param typarams the type variables to enter
     * @param env      the current environment
     */
    void attribTypeVariables(List<JCTypeParameter> typarams, Env<AttrContext> env, boolean checkCyclic) {
        for (JCTypeParameter tvar : typarams) {
            TypeVar a = (TypeVar)tvar.type;
            a.tsym.flags_field |= UNATTRIBUTED;
            a.setUpperBound(Type.noType);
            if (!tvar.bounds.isEmpty()) {
                List<Type> bounds = List.of(attribType(tvar.bounds.head, env));
                for (JCExpression bound : tvar.bounds.tail)
                    bounds = bounds.prepend(attribType(bound, env));
                types.setBounds(a, bounds.reverse());
            } else {
                types.setBounds(a, List.of(syms.objectType));
            }
            a.tsym.flags_field &= ~UNATTRIBUTED;
        }
        if (checkCyclic) {
            for (JCTypeParameter tvar : typarams) {
                chk.checkNonCyclic(tvar.pos(), (TypeVar)tvar.type);
            }
        }
    }

    /**
     * Attribute the type references in a list of annotations.
     */
    void attribAnnotationTypes(List<JCAnnotation> annotations,
                               Env<AttrContext> env) {
        for (List<JCAnnotation> al = annotations; al.nonEmpty(); al = al.tail) {
            JCAnnotation a = al.head;
            attribType(a.annotationType, env);
        }
    }

    /**
     * Attribute a "lazy constant value".
     *  @param env         The env for the const value
     *  @param variable    The initializer for the const value
     *  @param type        The expected type, or null
     *  @see VarSymbol#setLazyConstValue
     */
    public Object attribLazyConstantValue(Env<AttrContext> env,
                                      JCVariableDecl variable,
                                      Type type) {

        DiagnosticPosition prevLintPos
                = deferredLintHandler.setPos(variable.pos());

        final JavaFileObject prevSource = log.useSource(env.toplevel.sourcefile);
        try {
            Type itype = attribExpr(variable.init, env, type);
            if (variable.isImplicitlyTyped()) {
                type = variable.type = variable.sym.type = chk.checkLocalVarType(variable, itype, variable.name);
            }
            if (itype.constValue() != null) {
                return coerce(itype, type).constValue();
            } else {
                return null;
            }
        } finally {
            log.useSource(prevSource);
            deferredLintHandler.setPos(prevLintPos);
        }
    }

    /** Attribute type reference in an `extends' or `implements' clause.
     *  Supertypes of anonymous inner classes are usually already attributed.
     *
     *  @param tree              The tree making up the type reference.
     *  @param env               The environment current at the reference.
     *  @param classExpected     true if only a class is expected here.
     *  @param interfaceExpected true if only an interface is expected here.
     */
    Type attribBase(JCTree tree,
                    Env<AttrContext> env,
                    boolean classExpected,
                    boolean interfaceExpected,
                    boolean checkExtensible) {
        Type t = tree.type != null ?
            tree.type :
            attribType(tree, env);
        try {
            return checkBase(t, tree, env, classExpected, interfaceExpected, checkExtensible);
        } catch (CompletionFailure ex) {
            chk.completionError(tree.pos(), ex);
            return t;
        }
    }
    Type checkBase(Type t,
                   JCTree tree,
                   Env<AttrContext> env,
                   boolean classExpected,
                   boolean interfaceExpected,
                   boolean checkExtensible) {
        final DiagnosticPosition pos = tree.hasTag(TYPEAPPLY) ?
                (((JCTypeApply) tree).clazz).pos() : tree.pos();
        if (t.tsym.isAnonymous()) {
            log.error(pos, Errors.CantInheritFromAnon);
            return types.createErrorType(t);
        }
        if (t.isErroneous())
            return t;
        if (t.hasTag(TYPEVAR) && !classExpected && !interfaceExpected) {
            if (t.getUpperBound() == null) {
                log.error(pos, Errors.IllegalForwardRef);
                return types.createErrorType(t);
            }
        } else {
            t = chk.checkClassType(pos, t, checkExtensible);
        }
        if (interfaceExpected && (t.tsym.flags() & INTERFACE) == 0) {
            log.error(pos, Errors.IntfExpectedHere);
            return types.createErrorType(t);
        } else if (checkExtensible &&
                   classExpected &&
                   (t.tsym.flags() & INTERFACE) != 0) {
            log.error(pos, Errors.NoIntfExpectedHere);
            return types.createErrorType(t);
        }
        if (checkExtensible &&
            ((t.tsym.flags() & FINAL) != 0)) {
            log.error(pos,
                      Errors.CantInheritFromFinal(t.tsym));
        }
        chk.checkNonCyclic(pos, t);
        return t;
    }

    Type attribIdentAsEnumType(Env<AttrContext> env, JCIdent id) {
        Assert.check((env.enclClass.sym.flags() & ENUM) != 0);
        id.type = env.info.scope.owner.enclClass().type;
        id.sym = env.info.scope.owner.enclClass();
        return id.type;
    }

    public void visitClassDef(JCClassDecl tree) {
        Optional<ArgumentAttr.LocalCacheContext> localCacheContext =
                Optional.ofNullable(env.info.attributionMode.isSpeculative ?
                        argumentAttr.withLocalCacheContext() : null);
        boolean ctorProloguePrev = env.info.ctorPrologue;
        env.info.ctorPrologue = false;
        try {
            if (env.info.scope.owner.kind.matches(KindSelector.VAL_MTH)) {
                enter.classEnter(tree, env);
            } else {
                if (env.tree.hasTag(NEWCLASS) && TreeInfo.isInAnnotation(env, tree))
                    enter.classEnter(tree, env);
            }

            ClassSymbol c = tree.sym;
            if (c == null) {
                result = null;
            } else {
                c.complete();

                if (ctorProloguePrev && env.tree.hasTag(NEWCLASS)) {
                    c.flags_field |= NOOUTERTHIS;
                }
                attribClass(tree.pos(), c);
                result = tree.type = c.type;
            }
        } finally {
            localCacheContext.ifPresent(LocalCacheContext::leave);
            env.info.ctorPrologue = ctorProloguePrev;
        }
    }

    public void visitMethodDef(JCMethodDecl tree) {
        MethodSymbol m = tree.sym;
        boolean isDefaultMethod = (m.flags() & DEFAULT) != 0;

        Lint lint = env.info.lint.augment(m);
        Lint prevLint = chk.setLint(lint);
        boolean ctorProloguePrev = env.info.ctorPrologue;
        env.info.ctorPrologue = false;
        MethodSymbol prevMethod = chk.setMethod(m);
        try {
            deferredLintHandler.flush(tree.pos());
            chk.checkDeprecatedAnnotation(tree.pos(), m);


            Env<AttrContext> localEnv = memberEnter.methodEnv(tree, env);
            localEnv.info.lint = lint;

            attribStats(tree.typarams, localEnv);

            if (m.isStatic()) {
                chk.checkHideClashes(tree.pos(), env.enclClass.type, m);
            } else {
                chk.checkOverrideClashes(tree.pos(), env.enclClass.type, m);
            }
            chk.checkOverride(env, tree, m);

            if (isDefaultMethod && types.overridesObjectMethod(m.enclClass(), m)) {
                log.error(tree, Errors.DefaultOverridesObjectMember(m.name, Kinds.kindName(m.location()), m.location()));
            }

            for (List<JCTypeParameter> l = tree.typarams; l.nonEmpty(); l = l.tail)
                localEnv.info.scope.enterIfAbsent(l.head.type.tsym);

            ClassSymbol owner = env.enclClass.sym;
            if ((owner.flags() & ANNOTATION) != 0 &&
                    (tree.params.nonEmpty() ||
                    tree.recvparam != null))
                log.error(tree.params.nonEmpty() ?
                        tree.params.head.pos() :
                        tree.recvparam.pos(),
                        Errors.IntfAnnotationMembersCantHaveParams);

            for (List<JCVariableDecl> l = tree.params; l.nonEmpty(); l = l.tail) {
                attribStat(l.head, localEnv);
            }

            chk.checkVarargsMethodDecl(localEnv, tree);

            chk.validate(tree.typarams, localEnv);

            if (tree.restype != null && !tree.restype.type.hasTag(VOID))
                chk.validate(tree.restype, localEnv);

            if (tree.recvparam != null) {
                Env<AttrContext> newEnv = memberEnter.methodEnv(tree, env);
                attribType(tree.recvparam, newEnv);
                chk.validate(tree.recvparam, newEnv);
            }

            boolean isConstructor = TreeInfo.isConstructor(tree);

            if (env.enclClass.sym.isRecord() && tree.sym.owner.kind == TYP) {
                Optional<? extends RecordComponent> recordComponent = env.enclClass.sym.getRecordComponents().stream()
                        .filter(rc -> rc.accessor == tree.sym && (rc.accessor.flags_field & GENERATED_MEMBER) == 0).findFirst();
                if (recordComponent.isPresent()) {
                    if (!tree.sym.isPublic()) {
                        log.error(tree, Errors.InvalidAccessorMethodInRecord(env.enclClass.sym, Fragments.MethodMustBePublic));
                    }
                    if (!types.isSameType(tree.sym.type.getReturnType(), recordComponent.get().type)) {
                        log.error(tree, Errors.InvalidAccessorMethodInRecord(env.enclClass.sym,
                                Fragments.AccessorReturnTypeDoesntMatch(tree.sym, recordComponent.get())));
                    }
                    if (tree.sym.type.asMethodType().thrown != null && !tree.sym.type.asMethodType().thrown.isEmpty()) {
                        log.error(tree,
                                Errors.InvalidAccessorMethodInRecord(env.enclClass.sym, Fragments.AccessorMethodCantThrowException));
                    }
                    if (!tree.typarams.isEmpty()) {
                        log.error(tree,
                                Errors.InvalidAccessorMethodInRecord(env.enclClass.sym, Fragments.AccessorMethodMustNotBeGeneric));
                    }
                    if (tree.sym.isStatic()) {
                        log.error(tree,
                                Errors.InvalidAccessorMethodInRecord(env.enclClass.sym, Fragments.AccessorMethodMustNotBeStatic));
                    }
                }

                if (isConstructor) {
                    if ((tree.sym.flags_field & RECORD) == 0) {
                        if (!TreeInfo.hasConstructorCall(tree, names._this)) {
                            log.error(tree, Errors.NonCanonicalConstructorInvokeAnotherConstructor(env.enclClass.sym));
                        }
                    } else {

                        /* if user generated, then it shouldn't:
                         *     - have an accessibility stricter than that of the record type
                         *     - explicitly invoke any other constructor
                         */
                        if ((tree.sym.flags_field & GENERATEDCONSTR) == 0) {
                            if (Check.protection(m.flags()) > Check.protection(env.enclClass.sym.flags())) {
                                log.error(tree,
                                        (env.enclClass.sym.flags() & AccessFlags) == 0 ?
                                            Errors.InvalidCanonicalConstructorInRecord(
                                                Fragments.Canonical,
                                                env.enclClass.sym.name,
                                                Fragments.CanonicalMustNotHaveStrongerAccess("package")
                                            ) :
                                            Errors.InvalidCanonicalConstructorInRecord(
                                                    Fragments.Canonical,
                                                    env.enclClass.sym.name,
                                                    Fragments.CanonicalMustNotHaveStrongerAccess(asFlagSet(env.enclClass.sym.flags() & AccessFlags))
                                            )
                                );
                            }

                            if (TreeInfo.hasAnyConstructorCall(tree)) {
                                log.error(tree, Errors.InvalidCanonicalConstructorInRecord(
                                        Fragments.Canonical, env.enclClass.sym.name,
                                        Fragments.CanonicalMustNotContainExplicitConstructorInvocation));
                            }
                        }

                        if (!tree.typarams.isEmpty()) {
                            log.error(tree, Errors.InvalidCanonicalConstructorInRecord(
                                    Fragments.Canonical, env.enclClass.sym.name, Fragments.CanonicalMustNotDeclareTypeVariables));
                        }

                        /* and now we need to check that the constructor's arguments are exactly the same as those of the
                         * record components
                         */
                        List<? extends RecordComponent> recordComponents = env.enclClass.sym.getRecordComponents();
                        List<Type> recordFieldTypes = TreeInfo.recordFields(env.enclClass).map(vd -> vd.sym.type);
                        for (JCVariableDecl param: tree.params) {
                            boolean paramIsVarArgs = (param.sym.flags_field & VARARGS) != 0;
                            if (!types.isSameType(param.type, recordFieldTypes.head) ||
                                    (recordComponents.head.isVarargs() != paramIsVarArgs)) {
                                log.error(param, Errors.InvalidCanonicalConstructorInRecord(
                                        Fragments.Canonical, env.enclClass.sym.name,
                                        Fragments.TypeMustBeIdenticalToCorrespondingRecordComponentType));
                            }
                            recordComponents = recordComponents.tail;
                            recordFieldTypes = recordFieldTypes.tail;
                        }
                    }
                }
            }

            if ((owner.flags() & ANNOTATION) != 0) {
                if (tree.thrown.nonEmpty()) {
                    log.error(tree.thrown.head.pos(),
                              Errors.ThrowsNotAllowedInIntfAnnotation);
                }
                if (tree.typarams.nonEmpty()) {
                    log.error(tree.typarams.head.pos(),
                              Errors.IntfAnnotationMembersCantHaveTypeParams);
                }
                chk.validateAnnotationType(tree.restype);
                chk.validateAnnotationMethod(tree.pos(), m);
            }

            for (List<JCExpression> l = tree.thrown; l.nonEmpty(); l = l.tail)
                chk.checkType(l.head.pos(), l.head.type, syms.throwableType);

            if (tree.body == null) {
                if (tree.defaultValue != null) {
                    if ((owner.flags() & ANNOTATION) == 0)
                        log.error(tree.pos(),
                                  Errors.DefaultAllowedInIntfAnnotationMember);
                }
                if (isDefaultMethod || (tree.sym.flags() & (ABSTRACT | NATIVE)) == 0)
                    log.error(tree.pos(), Errors.MissingMethBodyOrDeclAbstract);
            } else {
                if ((tree.sym.flags() & (ABSTRACT|DEFAULT|PRIVATE)) == ABSTRACT) {
                    if ((owner.flags() & INTERFACE) != 0) {
                        log.error(tree.body.pos(), Errors.IntfMethCantHaveBody);
                    } else {
                        log.error(tree.pos(), Errors.AbstractMethCantHaveBody);
                    }
                } else if ((tree.mods.flags & NATIVE) != 0) {
                    log.error(tree.pos(), Errors.NativeMethCantHaveBody);
                }
                if (isConstructor && owner.type != syms.objectType) {
                    if (!TreeInfo.hasAnyConstructorCall(tree)) {
                        JCStatement supCall = make.at(tree.body.pos).Exec(make.Apply(List.nil(),
                                make.Ident(names._super), make.Idents(List.nil())));
                        tree.body.stats = tree.body.stats.prepend(supCall);
                    } else if ((env.enclClass.sym.flags() & ENUM) != 0 &&
                            (tree.mods.flags & GENERATEDCONSTR) == 0 &&
                            TreeInfo.hasConstructorCall(tree, names._super)) {
                        log.error(tree.body.stats.head.pos(),
                                  Errors.CallToSuperNotAllowedInEnumCtor(env.enclClass.sym));
                    }
                    if (env.enclClass.sym.isRecord() && (tree.sym.flags_field & RECORD) != 0) { 
                        List<Name> recordComponentNames = TreeInfo.recordFields(env.enclClass).map(vd -> vd.sym.name);
                        List<Name> initParamNames = tree.sym.params.map(p -> p.name);
                        if (!initParamNames.equals(recordComponentNames)) {
                            log.error(tree, Errors.InvalidCanonicalConstructorInRecord(
                                    Fragments.Canonical, env.enclClass.sym.name, Fragments.CanonicalWithNameMismatch));
                        }
                        if (tree.sym.type.asMethodType().thrown != null && !tree.sym.type.asMethodType().thrown.isEmpty()) {
                            log.error(tree,
                                    Errors.InvalidCanonicalConstructorInRecord(
                                            TreeInfo.isCompactConstructor(tree) ? Fragments.Compact : Fragments.Canonical,
                                            env.enclClass.sym.name,
                                            Fragments.ThrowsClauseNotAllowedForCanonicalConstructor(
                                                    TreeInfo.isCompactConstructor(tree) ? Fragments.Compact : Fragments.Canonical)));
                        }
                    }
                }

                annotate.queueScanTreeAndTypeAnnotate(tree.body, localEnv, m, null);
                annotate.flush();

                localEnv.info.ctorPrologue = isConstructor;

                attribStat(tree.body, localEnv);
            }

            localEnv.info.scope.leave();
            result = tree.type = m.type;
        } finally {
            chk.setLint(prevLint);
            chk.setMethod(prevMethod);
            env.info.ctorPrologue = ctorProloguePrev;
        }
    }

    public void visitVarDef(JCVariableDecl tree) {
        if (env.info.scope.owner.kind == MTH || env.info.scope.owner.kind == VAR) {
            if (tree.sym != null) {
                env.info.scope.enter(tree.sym);
            } else {
                if (tree.isImplicitlyTyped() && (tree.getModifiers().flags & PARAMETER) == 0) {
                    if (tree.init == null) {
                        log.error(tree, Errors.CantInferLocalVarType(tree.name, Fragments.LocalMissingInit));
                        tree.vartype = make.Erroneous();
                    } else {
                        Fragment msg = canInferLocalVarType(tree);
                        if (msg != null) {
                            log.error(tree, Errors.CantInferLocalVarType(tree.name, msg));
                            tree.vartype = make.Erroneous();
                        }
                    }
                }
                try {
                    annotate.blockAnnotations();
                    memberEnter.memberEnter(tree, env);
                } finally {
                    annotate.unblockAnnotations();
                }
            }
        } else {
            if (tree.init != null) {
                annotate.queueScanTreeAndTypeAnnotate(tree.init, env, tree.sym, tree.pos());
                annotate.flush();
            }
        }

        VarSymbol v = tree.sym;
        Lint lint = env.info.lint.augment(v);
        Lint prevLint = chk.setLint(lint);

        boolean isImplicitLambdaParameter = env.tree.hasTag(LAMBDA) &&
                ((JCLambda)env.tree).paramKind == JCLambda.ParameterKind.IMPLICIT &&
                (tree.sym.flags() & PARAMETER) != 0;
        chk.validate(tree.vartype, env, !isImplicitLambdaParameter && !tree.isImplicitlyTyped());

        try {
            v.getConstValue(); 
            deferredLintHandler.flush(tree.pos());
            chk.checkDeprecatedAnnotation(tree.pos(), v);

            if (tree.init != null) {
                if ((v.flags_field & FINAL) == 0 ||
                    !memberEnter.needsLazyConstValue(tree.init)) {
                    Env<AttrContext> initEnv = memberEnter.initEnv(tree, env);
                    initEnv.info.lint = lint;
                    initEnv.info.enclVar = v;
                    attribExpr(tree.init, initEnv, v.type);
                    if (tree.isImplicitlyTyped()) {
                        v.type = chk.checkLocalVarType(tree, tree.init.type, tree.name);
                    }
                }
                if (tree.isImplicitlyTyped()) {
                    setSyntheticVariableType(tree, v.type);
                }
            }
            result = tree.type = v.type;
            if (env.enclClass.sym.isRecord() && tree.sym.owner.kind == TYP && !v.isStatic()) {
                if (isNonArgsMethodInObject(v.name)) {
                    log.error(tree, Errors.IllegalRecordComponentName(v));
                }
            }
        }
        finally {
            chk.setLint(prevLint);
        }
    }

    private boolean isNonArgsMethodInObject(Name name) {
        for (Symbol s : syms.objectType.tsym.members().getSymbolsByName(name, s -> s.kind == MTH)) {
            if (s.type.getParameterTypes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    Fragment canInferLocalVarType(JCVariableDecl tree) {
        LocalInitScanner lis = new LocalInitScanner();
        lis.scan(tree.init);
        return lis.badInferenceMsg;
    }

    static class LocalInitScanner extends TreeScanner {
        Fragment badInferenceMsg = null;
        boolean needsTarget = true;

        @Override
        public void visitNewArray(JCNewArray tree) {
            if (tree.elemtype == null && needsTarget) {
                badInferenceMsg = Fragments.LocalArrayMissingTarget;
            }
        }

        @Override
        public void visitLambda(JCLambda tree) {
            if (needsTarget) {
                badInferenceMsg = Fragments.LocalLambdaMissingTarget;
            }
        }

        @Override
        public void visitTypeCast(JCTypeCast tree) {
            boolean prevNeedsTarget = needsTarget;
            try {
                needsTarget = false;
                super.visitTypeCast(tree);
            } finally {
                needsTarget = prevNeedsTarget;
            }
        }

        @Override
        public void visitReference(JCMemberReference tree) {
            if (needsTarget) {
                badInferenceMsg = Fragments.LocalMrefMissingTarget;
            }
        }

        @Override
        public void visitNewClass(JCNewClass tree) {
            boolean prevNeedsTarget = needsTarget;
            try {
                needsTarget = false;
                super.visitNewClass(tree);
            } finally {
                needsTarget = prevNeedsTarget;
            }
        }

        @Override
        public void visitApply(JCMethodInvocation tree) {
            boolean prevNeedsTarget = needsTarget;
            try {
                needsTarget = false;
                super.visitApply(tree);
            } finally {
                needsTarget = prevNeedsTarget;
            }
        }
    }

    public void visitSkip(JCSkip tree) {
        result = null;
    }

    public void visitBlock(JCBlock tree) {
        if (env.info.scope.owner.kind == TYP || env.info.scope.owner.kind == ERR) {
            Symbol fakeOwner =
                new MethodSymbol(tree.flags | BLOCK |
                    env.info.scope.owner.flags() & STRICTFP, names.empty, null,
                    env.info.scope.owner);
            final Env<AttrContext> localEnv =
                env.dup(tree, env.info.dup(env.info.scope.dupUnshared(fakeOwner)));

            if ((tree.flags & STATIC) != 0) localEnv.info.staticLevel++;
            annotate.queueScanTreeAndTypeAnnotate(tree, localEnv, localEnv.info.scope.owner, null);
            annotate.flush();
            attribStats(tree.stats, localEnv);

            {
                ClassSymbol cs = (ClassSymbol)env.info.scope.owner;
                List<Attribute.TypeCompound> tas = localEnv.info.scope.owner.getRawTypeAttributes();
                if ((tree.flags & STATIC) != 0) {
                    cs.appendClassInitTypeAttributes(tas);
                } else {
                    cs.appendInitTypeAttributes(tas);
                }
            }
        } else {
            Env<AttrContext> localEnv =
                env.dup(tree, env.info.dup(env.info.scope.dup()));
            try {
                attribStats(tree.stats, localEnv);
            } finally {
                localEnv.info.scope.leave();
            }
        }
        result = null;
    }

    public void visitDoLoop(JCDoWhileLoop tree) {
        attribStat(tree.body, env.dup(tree));
        attribExpr(tree.cond, env, syms.booleanType);
        handleLoopConditionBindings(matchBindings, tree, tree.body);
        result = null;
    }

    public void visitWhileLoop(JCWhileLoop tree) {
        attribExpr(tree.cond, env, syms.booleanType);
        MatchBindings condBindings = matchBindings;
        Env<AttrContext> whileEnv = bindingEnv(env, condBindings.bindingsWhenTrue);
        try {
            attribStat(tree.body, whileEnv.dup(tree));
        } finally {
            whileEnv.info.scope.leave();
        }
        handleLoopConditionBindings(condBindings, tree, tree.body);
        result = null;
    }

    public void visitForLoop(JCForLoop tree) {
        Env<AttrContext> loopEnv =
            env.dup(env.tree, env.info.dup(env.info.scope.dup()));
        MatchBindings condBindings = MatchBindingsComputer.EMPTY;
        try {
            attribStats(tree.init, loopEnv);
            if (tree.cond != null) {
                attribExpr(tree.cond, loopEnv, syms.booleanType);
                condBindings = matchBindings;
            }
            Env<AttrContext> bodyEnv = bindingEnv(loopEnv, condBindings.bindingsWhenTrue);
            try {
                bodyEnv.tree = tree; 
                attribStats(tree.step, bodyEnv);
                attribStat(tree.body, bodyEnv);
            } finally {
                bodyEnv.info.scope.leave();
            }
            result = null;
        }
        finally {
            loopEnv.info.scope.leave();
        }
        handleLoopConditionBindings(condBindings, tree, tree.body);
    }

    /**
     * Include condition's bindings when false after the loop, if cannot get out of the loop
     */
    private void handleLoopConditionBindings(MatchBindings condBindings,
                                             JCStatement loop,
                                             JCStatement loopBody) {
        if (condBindings.bindingsWhenFalse.nonEmpty() &&
            !breaksTo(env, loop, loopBody)) {
            addBindings2Scope(loop, condBindings.bindingsWhenFalse);
        }
    }

    private boolean breaksTo(Env<AttrContext> env, JCTree loop, JCTree body) {
        preFlow(body);
        return flow.breaksToTree(env, loop, body, make);
    }

    /**
     * Add given bindings to the current scope, unless there's a break to
     * an immediately enclosing labeled statement.
     */
    private void addBindings2Scope(JCStatement introducingStatement,
                                   List<BindingSymbol> bindings) {
        if (bindings.isEmpty()) {
            return ;
        }

        var searchEnv = env;
        while (searchEnv.tree instanceof JCLabeledStatement labeled &&
               labeled.body == introducingStatement) {
            if (breaksTo(env, labeled, labeled.body)) {
                return ;
            }
            searchEnv = searchEnv.next;
            introducingStatement = labeled;
        }

        bindings.forEach(env.info.scope::enter);
        bindings.forEach(BindingSymbol::preserveBinding);
    }

    public void visitForeachLoop(JCEnhancedForLoop tree) {
        Env<AttrContext> loopEnv =
            env.dup(env.tree, env.info.dup(env.info.scope.dup()));
        try {
            Type exprType = types.cvarUpperBound(attribExpr(tree.expr, loopEnv));
            chk.checkNonVoid(tree.pos(), exprType);
            Type elemtype = types.elemtype(exprType); 
            if (elemtype == null) {
                Type base = types.asSuper(exprType, syms.iterableType.tsym);
                if (base == null) {
                    log.error(tree.expr.pos(),
                              Errors.ForeachNotApplicableToType(exprType,
                                                                Fragments.TypeReqArrayOrIterable));
                    elemtype = types.createErrorType(exprType);
                } else {
                    List<Type> iterableParams = base.allparams();
                    elemtype = iterableParams.isEmpty()
                        ? syms.objectType
                        : types.wildUpperBound(iterableParams.head);

                    Symbol iterSymbol = rs.resolveInternalMethod(tree.pos(),
                            loopEnv, types.skipTypeVars(exprType, false), names.iterator, List.nil(), List.nil());
                    if (types.asSuper(iterSymbol.type.getReturnType(), syms.iteratorType.tsym) == null) {
                        log.error(tree.pos(),
                                Errors.ForeachNotApplicableToType(exprType, Fragments.TypeReqArrayOrIterable));
                    }
                }
            }
            if (tree.var.isImplicitlyTyped()) {
                Type inferredType = chk.checkLocalVarType(tree.var, elemtype, tree.var.name);
                setSyntheticVariableType(tree.var, inferredType);
            }
            attribStat(tree.var, loopEnv);
            chk.checkType(tree.expr.pos(), elemtype, tree.var.sym.type);
            loopEnv.tree = tree; 
            attribStat(tree.body, loopEnv);
            result = null;
        }
        finally {
            loopEnv.info.scope.leave();
        }
    }

    public void visitLabelled(JCLabeledStatement tree) {
        Env<AttrContext> env1 = env;
        while (env1 != null && !env1.tree.hasTag(CLASSDEF)) {
            if (env1.tree.hasTag(LABELLED) &&
                ((JCLabeledStatement) env1.tree).label == tree.label) {
                log.error(tree.pos(),
                          Errors.LabelAlreadyInUse(tree.label));
                break;
            }
            env1 = env1.next;
        }

        attribStat(tree.body, env.dup(tree));
        result = null;
    }

    public void visitSwitch(JCSwitch tree) {
        handleSwitch(tree, tree.selector, tree.cases, (c, caseEnv) -> {
            attribStats(c.stats, caseEnv);
        });
        result = null;
    }

    public void visitSwitchExpression(JCSwitchExpression tree) {
        tree.polyKind = (pt().hasTag(NONE) && pt() != Type.recoveryType && pt() != Infer.anyPoly) ?
                PolyKind.STANDALONE : PolyKind.POLY;

        if (tree.polyKind == PolyKind.POLY && resultInfo.pt.hasTag(VOID)) {
            resultInfo.checkContext.report(tree, diags.fragment(Fragments.SwitchExpressionTargetCantBeVoid));
            result = tree.type = types.createErrorType(resultInfo.pt);
            return;
        }

        ResultInfo condInfo = tree.polyKind == PolyKind.STANDALONE ?
                unknownExprInfo :
                resultInfo.dup(switchExpressionContext(resultInfo.checkContext));

        ListBuffer<DiagnosticPosition> caseTypePositions = new ListBuffer<>();
        ListBuffer<Type> caseTypes = new ListBuffer<>();

        handleSwitch(tree, tree.selector, tree.cases, (c, caseEnv) -> {
            caseEnv.info.yieldResult = condInfo;
            attribStats(c.stats, caseEnv);
            new TreeScanner() {
                @Override
                public void visitYield(JCYield brk) {
                    if (brk.target == tree) {
                        caseTypePositions.append(brk.value != null ? brk.value.pos() : brk.pos());
                        caseTypes.append(brk.value != null ? brk.value.type : syms.errType);
                    }
                    super.visitYield(brk);
                }

                @Override public void visitClassDef(JCClassDecl tree) {}
                @Override public void visitLambda(JCLambda tree) {}
            }.scan(c.stats);
        });

        if (tree.cases.isEmpty()) {
            log.error(tree.pos(),
                      Errors.SwitchExpressionEmpty);
        } else if (caseTypes.isEmpty()) {
            log.error(tree.pos(),
                      Errors.SwitchExpressionNoResultExpressions);
        }

        Type owntype = (tree.polyKind == PolyKind.STANDALONE) ? condType(caseTypePositions.toList(), caseTypes.toList()) : pt();

        result = tree.type = check(tree, owntype, KindSelector.VAL, resultInfo);
    }
        CheckContext switchExpressionContext(CheckContext checkContext) {
            return new Check.NestedCheckContext(checkContext) {
                @Override
                public void report(DiagnosticPosition pos, JCDiagnostic details) {
                    enclosingContext.report(pos, diags.fragment(Fragments.IncompatibleTypeInSwitchExpression(details)));
                }
            };
        }

    private void handleSwitch(JCTree switchTree,
                              JCExpression selector,
                              List<JCCase> cases,
                              BiConsumer<JCCase, Env<AttrContext>> attribCase) {
        Type seltype = attribExpr(selector, env);

        Env<AttrContext> switchEnv =
            env.dup(switchTree, env.info.dup(env.info.scope.dup()));

        try {
            boolean enumSwitch = (seltype.tsym.flags() & Flags.ENUM) != 0;
            boolean stringSwitch = types.isSameType(seltype, syms.stringType);
            boolean errorEnumSwitch = TreeInfo.isErrorEnumSwitch(selector, cases);
            boolean intSwitch = types.isAssignable(seltype, syms.intType);
            boolean errorPrimitiveSwitch = seltype.isPrimitive() && !intSwitch;
            boolean patternSwitch;
            if (!enumSwitch && !stringSwitch && !errorEnumSwitch &&
                !intSwitch && !errorPrimitiveSwitch) {
                preview.checkSourceLevel(selector.pos(), Feature.PATTERN_SWITCH);
                patternSwitch = true;
            } else {
                if (errorPrimitiveSwitch) {
                    log.error(selector.pos(), Errors.SelectorTypeNotAllowed(seltype));
                }
                patternSwitch = cases.stream()
                                     .flatMap(c -> c.labels.stream())
                                     .anyMatch(l -> l.hasTag(PATTERNCASELABEL) ||
                                                    TreeInfo.isNullCaseLabel(l));
            }

            Set<Object> constants = new HashSet<>(); 
            boolean hasDefault = false;           
            boolean hasUnconditionalPattern = false; 
            boolean lastPatternErroneous = false; 
            boolean hasNullPattern = false;       
            CaseTree.CaseKind caseKind = null;
            boolean wasError = false;
            for (List<JCCase> l = cases; l.nonEmpty(); l = l.tail) {
                JCCase c = l.head;
                if (caseKind == null) {
                    caseKind = c.caseKind;
                } else if (caseKind != c.caseKind && !wasError) {
                    log.error(c.pos(),
                              Errors.SwitchMixingCaseTypes);
                    wasError = true;
                }
                MatchBindings currentBindings = null;
                MatchBindings guardBindings = null;
                for (List<JCCaseLabel> labels = c.labels; labels.nonEmpty(); labels = labels.tail) {
                    JCCaseLabel label = labels.head;
                    if (label instanceof JCConstantCaseLabel constLabel) {
                        JCExpression expr = constLabel.expr;
                        if (TreeInfo.isNull(expr)) {
                            preview.checkSourceLevel(expr.pos(), Feature.CASE_NULL);
                            if (hasNullPattern) {
                                log.error(label.pos(), Errors.DuplicateCaseLabel);
                            }
                            hasNullPattern = true;
                            attribExpr(expr, switchEnv, seltype);
                            matchBindings = new MatchBindings(matchBindings.bindingsWhenTrue, matchBindings.bindingsWhenFalse, true);
                        } else if (enumSwitch) {
                            Symbol sym = enumConstant(expr, seltype);
                            if (sym == null) {
                                if (allowPatternSwitch) {
                                    attribTree(expr, switchEnv, caseLabelResultInfo(seltype));
                                    Symbol enumSym = TreeInfo.symbol(expr);
                                    if (enumSym == null || !enumSym.isEnum() || enumSym.kind != VAR) {
                                        log.error(expr.pos(), Errors.EnumLabelMustBeEnumConstant);
                                    } else if (!constants.add(enumSym)) {
                                        log.error(label.pos(), Errors.DuplicateCaseLabel);
                                    }
                                } else {
                                    log.error(expr.pos(), Errors.EnumLabelMustBeUnqualifiedEnum);
                                }
                            } else if (!constants.add(sym)) {
                                log.error(label.pos(), Errors.DuplicateCaseLabel);
                            }
                        } else if (errorEnumSwitch) {
                            var prevResolveHelper = rs.basicLogResolveHelper;
                            try {
                                rs.basicLogResolveHelper = rs.silentLogResolveHelper;
                                attribExpr(expr, switchEnv, seltype);
                            } finally {
                                rs.basicLogResolveHelper = prevResolveHelper;
                            }
                        } else {
                            Type pattype = attribTree(expr, switchEnv, caseLabelResultInfo(seltype));
                            if (!pattype.hasTag(ERROR)) {
                                if (pattype.constValue() == null) {
                                    Symbol s = TreeInfo.symbol(expr);
                                    if (s != null && s.kind == TYP) {
                                        log.error(expr.pos(),
                                                  Errors.PatternExpected);
                                    } else if (s == null || !s.isEnum()) {
                                        log.error(expr.pos(),
                                                  (stringSwitch ? Errors.StringConstReq
                                                                : intSwitch ? Errors.ConstExprReq
                                                                            : Errors.PatternOrEnumReq));
                                    } else if (!constants.add(s)) {
                                        log.error(label.pos(), Errors.DuplicateCaseLabel);
                                    }
                                } else if (!stringSwitch && !intSwitch && !errorPrimitiveSwitch) {
                                    log.error(label.pos(), Errors.ConstantLabelNotCompatible(pattype, seltype));
                                } else if (!constants.add(pattype.constValue())) {
                                    log.error(c.pos(), Errors.DuplicateCaseLabel);
                                }
                            }
                        }
                    } else if (label instanceof JCDefaultCaseLabel def) {
                        if (hasDefault) {
                            log.error(label.pos(), Errors.DuplicateDefaultLabel);
                        } else if (hasUnconditionalPattern) {
                            log.error(label.pos(), Errors.UnconditionalPatternAndDefault);
                        }
                        hasDefault = true;
                        matchBindings = MatchBindingsComputer.EMPTY;
                    } else if (label instanceof JCPatternCaseLabel patternlabel) {
                        JCPattern pat = patternlabel.pat;
                        attribExpr(pat, switchEnv, seltype);
                        Type primaryType = TreeInfo.primaryPatternType(pat);
                        if (!primaryType.hasTag(TYPEVAR)) {
                            primaryType = chk.checkClassOrArrayType(pat.pos(), primaryType);
                        }
                        if (!errorPrimitiveSwitch) {
                            checkCastablePattern(pat.pos(), seltype, primaryType);
                        }
                        Type patternType = types.erasure(primaryType);
                        JCExpression guard = c.guard;
                        if (guardBindings == null && guard != null) {
                            MatchBindings afterPattern = matchBindings;
                            Env<AttrContext> bodyEnv = bindingEnv(switchEnv, matchBindings.bindingsWhenTrue);
                            try {
                                attribExpr(guard, bodyEnv, syms.booleanType);
                            } finally {
                                bodyEnv.info.scope.leave();
                            }

                            guardBindings = matchBindings;
                            matchBindings = afterPattern;

                            if (TreeInfo.isBooleanWithValue(guard, 0)) {
                                log.error(guard.pos(), Errors.GuardHasConstantExpressionFalse);
                            }
                        }
                        boolean unguarded = TreeInfo.unguardedCase(c) && !pat.hasTag(RECORDPATTERN);
                        boolean unconditional =
                                unguarded &&
                                !patternType.isErroneous() &&
                                types.isSubtype(types.boxedTypeOrType(types.erasure(seltype)),
                                                patternType);
                        if (unconditional) {
                            if (hasUnconditionalPattern) {
                                log.error(pat.pos(), Errors.DuplicateUnconditionalPattern);
                            } else if (hasDefault) {
                                log.error(pat.pos(), Errors.UnconditionalPatternAndDefault);
                            }
                            hasUnconditionalPattern = true;
                        }
                        lastPatternErroneous = patternType.isErroneous();
                    } else {
                        Assert.error();
                    }
                    currentBindings = matchBindingsComputer.switchCase(label, currentBindings, matchBindings);
                }

                if (guardBindings != null) {
                    currentBindings = matchBindingsComputer.caseGuard(c, currentBindings, guardBindings);
                }

                Env<AttrContext> caseEnv =
                        bindingEnv(switchEnv, c, currentBindings.bindingsWhenTrue);
                try {
                    attribCase.accept(c, caseEnv);
                } finally {
                    caseEnv.info.scope.leave();
                }
                addVars(c.stats, switchEnv.info.scope);

                preFlow(c);
                c.completesNormally = flow.aliveAfter(caseEnv, c, make);
            }
            if (patternSwitch) {
                chk.checkSwitchCaseStructure(cases);
                chk.checkSwitchCaseLabelDominated(cases);
            }
            if (switchTree.hasTag(SWITCH)) {
                ((JCSwitch) switchTree).hasUnconditionalPattern =
                        hasDefault || hasUnconditionalPattern || lastPatternErroneous;
                ((JCSwitch) switchTree).patternSwitch = patternSwitch;
            } else if (switchTree.hasTag(SWITCH_EXPRESSION)) {
                ((JCSwitchExpression) switchTree).hasUnconditionalPattern =
                        hasDefault || hasUnconditionalPattern || lastPatternErroneous;
                ((JCSwitchExpression) switchTree).patternSwitch = patternSwitch;
            } else {
                Assert.error(switchTree.getTag().name());
            }
        } finally {
            switchEnv.info.scope.leave();
        }
    }
        private ResultInfo caseLabelResultInfo(Type seltype) {
            return new ResultInfo(KindSelector.VAL_TYP,
                                  !seltype.hasTag(ERROR) ? seltype
                                                         : Type.noType);
        }
        /** Add any variables defined in stats to the switch scope. */
        private static void addVars(List<JCStatement> stats, WriteableScope switchScope) {
            for (;stats.nonEmpty(); stats = stats.tail) {
                JCTree stat = stats.head;
                if (stat.hasTag(VARDEF))
                    switchScope.enter(((JCVariableDecl) stat).sym);
            }
        }
    /** Return the selected enumeration constant symbol, or null. */
    private Symbol enumConstant(JCTree tree, Type enumType) {
        if (tree.hasTag(IDENT)) {
            JCIdent ident = (JCIdent)tree;
            Name name = ident.name;
            for (Symbol sym : enumType.tsym.members().getSymbolsByName(name)) {
                if (sym.kind == VAR) {
                    Symbol s = ident.sym = sym;
                    ((VarSymbol)s).getConstValue(); 
                    ident.type = s.type;
                    return ((s.flags_field & Flags.ENUM) == 0)
                        ? null : s;
                }
            }
        }
        return null;
    }

    public void visitSynchronized(JCSynchronized tree) {
        chk.checkRefType(tree.pos(), attribExpr(tree.lock, env));
        if (env.info.lint.isEnabled(LintCategory.SYNCHRONIZATION) && isValueBased(tree.lock.type)) {
            log.warning(LintCategory.SYNCHRONIZATION, tree.pos(), Warnings.AttemptToSynchronizeOnInstanceOfValueBasedClass);
        }
        attribStat(tree.body, env);
        result = null;
    }
        private boolean isValueBased(Type t) {
            return t != null && t.tsym != null && (t.tsym.flags() & VALUE_BASED) != 0;
        }


    public void visitTry(JCTry tree) {
        Env<AttrContext> localEnv = env.dup(tree, env.info.dup(env.info.scope.dup()));
        try {
            boolean isTryWithResource = tree.resources.nonEmpty();
            Env<AttrContext> tryEnv = isTryWithResource ?
                env.dup(tree, localEnv.info.dup(localEnv.info.scope.dup())) :
                localEnv;
            try {
                for (JCTree resource : tree.resources) {
                    CheckContext twrContext = new Check.NestedCheckContext(resultInfo.checkContext) {
                        @Override
                        public void report(DiagnosticPosition pos, JCDiagnostic details) {
                            chk.basicHandler.report(pos, diags.fragment(Fragments.TryNotApplicableToType(details)));
                        }
                    };
                    ResultInfo twrResult =
                        new ResultInfo(KindSelector.VAR,
                                       syms.autoCloseableType,
                                       twrContext);
                    if (resource.hasTag(VARDEF)) {
                        attribStat(resource, tryEnv);
                        twrResult.check(resource, resource.type);

                        checkAutoCloseable(resource.pos(), localEnv, resource.type);

                        VarSymbol var = ((JCVariableDecl) resource).sym;

                        var.flags_field |= Flags.FINAL;
                        var.setData(ElementKind.RESOURCE_VARIABLE);
                    } else {
                        attribTree(resource, tryEnv, twrResult);
                    }
                }
                attribStat(tree.body, tryEnv);
            } finally {
                if (isTryWithResource)
                    tryEnv.info.scope.leave();
            }

            for (List<JCCatch> l = tree.catchers; l.nonEmpty(); l = l.tail) {
                JCCatch c = l.head;
                Env<AttrContext> catchEnv =
                    localEnv.dup(c, localEnv.info.dup(localEnv.info.scope.dup()));
                try {
                    Type ctype = attribStat(c.param, catchEnv);
                    if (TreeInfo.isMultiCatch(c)) {
                        c.param.sym.flags_field |= FINAL | UNION;
                    }
                    if (c.param.sym.kind == VAR) {
                        c.param.sym.setData(ElementKind.EXCEPTION_PARAMETER);
                    }
                    chk.checkType(c.param.vartype.pos(),
                                  chk.checkClassType(c.param.vartype.pos(), ctype),
                                  syms.throwableType);
                    attribStat(c.body, catchEnv);
                } finally {
                    catchEnv.info.scope.leave();
                }
            }

            if (tree.finalizer != null) attribStat(tree.finalizer, localEnv);
            result = null;
        }
        finally {
            localEnv.info.scope.leave();
        }
    }

    void checkAutoCloseable(DiagnosticPosition pos, Env<AttrContext> env, Type resource) {
        if (!resource.isErroneous() &&
            types.asSuper(resource, syms.autoCloseableType.tsym) != null &&
            !types.isSameType(resource, syms.autoCloseableType)) { 
            Symbol close = syms.noSymbol;
            Log.DiagnosticHandler discardHandler = new Log.DiscardDiagnosticHandler(log);
            try {
                close = rs.resolveQualifiedMethod(pos,
                        env,
                        types.skipTypeVars(resource, false),
                        names.close,
                        List.nil(),
                        List.nil());
            }
            finally {
                log.popDiagnosticHandler(discardHandler);
            }
            if (close.kind == MTH &&
                    close.overrides(syms.autoCloseableClose, resource.tsym, types, true) &&
                    chk.isHandled(syms.interruptedExceptionType, types.memberType(resource, close).getThrownTypes()) &&
                    env.info.lint.isEnabled(LintCategory.TRY)) {
                log.warning(LintCategory.TRY, pos, Warnings.TryResourceThrowsInterruptedExc(resource));
            }
        }
    }

    public void visitConditional(JCConditional tree) {
        Type condtype = attribExpr(tree.cond, env, syms.booleanType);
        MatchBindings condBindings = matchBindings;

        tree.polyKind = (pt().hasTag(NONE) && pt() != Type.recoveryType && pt() != Infer.anyPoly ||
                isBooleanOrNumeric(env, tree)) ?
                PolyKind.STANDALONE : PolyKind.POLY;

        if (tree.polyKind == PolyKind.POLY && resultInfo.pt.hasTag(VOID)) {
            resultInfo.checkContext.report(tree, diags.fragment(Fragments.ConditionalTargetCantBeVoid));
            result = tree.type = types.createErrorType(resultInfo.pt);
            return;
        }

        ResultInfo condInfo = tree.polyKind == PolyKind.STANDALONE ?
                unknownExprInfo :
                resultInfo.dup(conditionalContext(resultInfo.checkContext));



        Type truetype;
        Env<AttrContext> trueEnv = bindingEnv(env, condBindings.bindingsWhenTrue);
        try {
            truetype = attribTree(tree.truepart, trueEnv, condInfo);
        } finally {
            trueEnv.info.scope.leave();
        }

        MatchBindings trueBindings = matchBindings;

        Type falsetype;
        Env<AttrContext> falseEnv = bindingEnv(env, condBindings.bindingsWhenFalse);
        try {
            falsetype = attribTree(tree.falsepart, falseEnv, condInfo);
        } finally {
            falseEnv.info.scope.leave();
        }

        MatchBindings falseBindings = matchBindings;

        Type owntype = (tree.polyKind == PolyKind.STANDALONE) ?
                condType(List.of(tree.truepart.pos(), tree.falsepart.pos()),
                         List.of(truetype, falsetype)) : pt();
        if (condtype.constValue() != null &&
                truetype.constValue() != null &&
                falsetype.constValue() != null &&
                !owntype.hasTag(NONE)) {
            owntype = cfolder.coerce(condtype.isTrue() ? truetype : falsetype, owntype);
        }
        result = check(tree, owntype, KindSelector.VAL, resultInfo);
        matchBindings = matchBindingsComputer.conditional(tree, condBindings, trueBindings, falseBindings);
    }
        private boolean isBooleanOrNumeric(Env<AttrContext> env, JCExpression tree) {
            switch (tree.getTag()) {
                case LITERAL: return ((JCLiteral)tree).typetag.isSubRangeOf(DOUBLE) ||
                              ((JCLiteral)tree).typetag == BOOLEAN ||
                              ((JCLiteral)tree).typetag == BOT;
                case LAMBDA: case REFERENCE: return false;
                case PARENS: return isBooleanOrNumeric(env, ((JCParens)tree).expr);
                case CONDEXPR:
                    JCConditional condTree = (JCConditional)tree;
                    return isBooleanOrNumeric(env, condTree.truepart) &&
                            isBooleanOrNumeric(env, condTree.falsepart);
                case APPLY:
                    JCMethodInvocation speculativeMethodTree =
                            (JCMethodInvocation)deferredAttr.attribSpeculative(
                                    tree, env, unknownExprInfo,
                                    argumentAttr.withLocalCacheContext());
                    Symbol msym = TreeInfo.symbol(speculativeMethodTree.meth);
                    Type receiverType = speculativeMethodTree.meth.hasTag(IDENT) ?
                            env.enclClass.type :
                            ((JCFieldAccess)speculativeMethodTree.meth).selected.type;
                    Type owntype = types.memberType(receiverType, msym).getReturnType();
                    return primitiveOrBoxed(owntype);
                case NEWCLASS:
                    JCExpression className =
                            removeClassParams.translate(((JCNewClass)tree).clazz);
                    JCExpression speculativeNewClassTree =
                            (JCExpression)deferredAttr.attribSpeculative(
                                    className, env, unknownTypeInfo,
                                    argumentAttr.withLocalCacheContext());
                    return primitiveOrBoxed(speculativeNewClassTree.type);
                default:
                    Type speculativeType = deferredAttr.attribSpeculative(tree, env, unknownExprInfo,
                            argumentAttr.withLocalCacheContext()).type;
                    return primitiveOrBoxed(speculativeType);
            }
        }
            boolean primitiveOrBoxed(Type t) {
                return (!t.hasTag(TYPEVAR) && !t.isErroneous() && types.unboxedTypeOrType(t).isPrimitive());
            }

            TreeTranslator removeClassParams = new TreeTranslator() {
                @Override
                public void visitTypeApply(JCTypeApply tree) {
                    result = translate(tree.clazz);
                }
            };

        CheckContext conditionalContext(CheckContext checkContext) {
            return new Check.NestedCheckContext(checkContext) {
                @Override
                public void report(DiagnosticPosition pos, JCDiagnostic details) {
                    enclosingContext.report(pos, diags.fragment(Fragments.IncompatibleTypeInConditional(details)));
                }
            };
        }

        /** Compute the type of a conditional expression, after
         *  checking that it exists.  See JLS 15.25. Does not take into
         *  account the special case where condition and both arms
         *  are constants.
         *
         *  @param pos      The source position to be used for error
         *                  diagnostics.
         *  @param thentype The type of the expression's then-part.
         *  @param elsetype The type of the expression's else-part.
         */
        Type condType(List<DiagnosticPosition> positions, List<Type> condTypes) {
            if (condTypes.isEmpty()) {
                return syms.objectType; 
            }
            Type first = condTypes.head;
            if (condTypes.tail.stream().allMatch(t -> types.isSameType(first, t)))
                return first.baseType();

            List<Type> unboxedTypes = condTypes.stream()
                                               .map(t -> t.isPrimitive() ? t : types.unboxedType(t))
                                               .collect(List.collector());

            if (unboxedTypes.stream().allMatch(t -> t.isPrimitive())) {
                for (Type type : unboxedTypes) {
                    if (!type.getTag().isStrictSubRangeOf(INT)) {
                        continue;
                    }
                    if (unboxedTypes.stream().filter(t -> t != type).allMatch(t -> t.hasTag(INT) && types.isAssignable(t, type)))
                        return type.baseType();
                }

                for (TypeTag tag : primitiveTags) {
                    Type candidate = syms.typeOfTag[tag.ordinal()];
                    if (unboxedTypes.stream().allMatch(t -> types.isSubtype(t, candidate))) {
                        return candidate;
                    }
                }
            }

            condTypes = condTypes.stream()
                                 .map(t -> t.isPrimitive() ? types.boxedClass(t).type : t)
                                 .collect(List.collector());

            for (Type type : condTypes) {
                if (condTypes.stream().filter(t -> t != type).allMatch(t -> types.isAssignable(t, type)))
                    return type.baseType();
            }

            Iterator<DiagnosticPosition> posIt = positions.iterator();

            condTypes = condTypes.stream()
                                 .map(t -> chk.checkNonVoid(posIt.next(), t))
                                 .collect(List.collector());

            return types.lub(condTypes.stream()
                        .map(t -> t.baseType())
                        .filter(t -> !t.hasTag(BOT))
                        .collect(List.collector()));
        }

    static final TypeTag[] primitiveTags = new TypeTag[]{
        BYTE,
        CHAR,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        BOOLEAN,
    };

    Env<AttrContext> bindingEnv(Env<AttrContext> env, List<BindingSymbol> bindings) {
        return bindingEnv(env, env.tree, bindings);
    }

    Env<AttrContext> bindingEnv(Env<AttrContext> env, JCTree newTree, List<BindingSymbol> bindings) {
        Env<AttrContext> env1 = env.dup(newTree, env.info.dup(env.info.scope.dup()));
        bindings.forEach(env1.info.scope::enter);
        return env1;
    }

    public void visitIf(JCIf tree) {
        attribExpr(tree.cond, env, syms.booleanType);


        MatchBindings condBindings = matchBindings;
        Env<AttrContext> thenEnv = bindingEnv(env, condBindings.bindingsWhenTrue);

        try {
            attribStat(tree.thenpart, thenEnv);
        } finally {
            thenEnv.info.scope.leave();
        }

        preFlow(tree.thenpart);
        boolean aliveAfterThen = flow.aliveAfter(env, tree.thenpart, make);
        boolean aliveAfterElse;

        if (tree.elsepart != null) {
            Env<AttrContext> elseEnv = bindingEnv(env, condBindings.bindingsWhenFalse);
            try {
                attribStat(tree.elsepart, elseEnv);
            } finally {
                elseEnv.info.scope.leave();
            }
            preFlow(tree.elsepart);
            aliveAfterElse = flow.aliveAfter(env, tree.elsepart, make);
        } else {
            aliveAfterElse = true;
        }

        chk.checkEmptyIf(tree);

        List<BindingSymbol> afterIfBindings = List.nil();

        if (aliveAfterThen && !aliveAfterElse) {
            afterIfBindings = condBindings.bindingsWhenTrue;
        } else if (aliveAfterElse && !aliveAfterThen) {
            afterIfBindings = condBindings.bindingsWhenFalse;
        }

        addBindings2Scope(tree, afterIfBindings);

        result = null;
    }

        void preFlow(JCTree tree) {
            attrRecover.doRecovery();
            new PostAttrAnalyzer() {
                @Override
                public void scan(JCTree tree) {
                    if (tree == null ||
                            (tree.type != null &&
                            tree.type == Type.stuckType)) {
                        return;
                    }
                    super.scan(tree);
                }

                @Override
                public void visitClassDef(JCClassDecl that) {
                    if (that.sym != null) {
                        super.visitClassDef(that);
                    }
                }

                @Override
                public void visitLambda(JCLambda that) {
                    if (that.type != null) {
                        super.visitLambda(that);
                    }
                }
            }.scan(tree);
        }

    public void visitExec(JCExpressionStatement tree) {
        Env<AttrContext> localEnv = env.dup(tree);
        attribExpr(tree.expr, localEnv);
        result = null;
    }

    public void visitBreak(JCBreak tree) {
        tree.target = findJumpTarget(tree.pos(), tree.getTag(), tree.label, env);
        result = null;
    }

    public void visitYield(JCYield tree) {
        if (env.info.yieldResult != null) {
            attribTree(tree.value, env, env.info.yieldResult);
            tree.target = findJumpTarget(tree.pos(), tree.getTag(), names.empty, env);
        } else {
            log.error(tree.pos(), tree.value.hasTag(PARENS)
                    ? Errors.NoSwitchExpressionQualify
                    : Errors.NoSwitchExpression);
            attribTree(tree.value, env, unknownExprInfo);
        }
        result = null;
    }

    public void visitContinue(JCContinue tree) {
        tree.target = findJumpTarget(tree.pos(), tree.getTag(), tree.label, env);
        result = null;
    }
        /** Return the target of a break, continue or yield statement,
         *  if it exists, report an error if not.
         *  Note: The target of a labelled break or continue is the
         *  (non-labelled) statement tree referred to by the label,
         *  not the tree representing the labelled statement itself.
         *
         *  @param pos     The position to be used for error diagnostics
         *  @param tag     The tag of the jump statement. This is either
         *                 Tree.BREAK or Tree.CONTINUE.
         *  @param label   The label of the jump statement, or null if no
         *                 label is given.
         *  @param env     The environment current at the jump statement.
         */
        private JCTree findJumpTarget(DiagnosticPosition pos,
                                                   JCTree.Tag tag,
                                                   Name label,
                                                   Env<AttrContext> env) {
            Pair<JCTree, Error> jumpTarget = findJumpTargetNoError(tag, label, env);

            if (jumpTarget.snd != null) {
                log.error(pos, jumpTarget.snd);
            }

            return jumpTarget.fst;
        }
        /** Return the target of a break or continue statement, if it exists,
         *  report an error if not.
         *  Note: The target of a labelled break or continue is the
         *  (non-labelled) statement tree referred to by the label,
         *  not the tree representing the labelled statement itself.
         *
         *  @param tag     The tag of the jump statement. This is either
         *                 Tree.BREAK or Tree.CONTINUE.
         *  @param label   The label of the jump statement, or null if no
         *                 label is given.
         *  @param env     The environment current at the jump statement.
         */
        private Pair<JCTree, JCDiagnostic.Error> findJumpTargetNoError(JCTree.Tag tag,
                                                                       Name label,
                                                                       Env<AttrContext> env) {
            Env<AttrContext> env1 = env;
            JCDiagnostic.Error pendingError = null;
            LOOP:
            while (env1 != null) {
                switch (env1.tree.getTag()) {
                    case LABELLED:
                        JCLabeledStatement labelled = (JCLabeledStatement)env1.tree;
                        if (label == labelled.label) {
                            if (tag == CONTINUE) {
                                if (!labelled.body.hasTag(DOLOOP) &&
                                        !labelled.body.hasTag(WHILELOOP) &&
                                        !labelled.body.hasTag(FORLOOP) &&
                                        !labelled.body.hasTag(FOREACHLOOP)) {
                                    pendingError = Errors.NotLoopLabel(label);
                                }
                                return Pair.of(TreeInfo.referencedStatement(labelled), pendingError);
                            } else {
                                return Pair.of(labelled, pendingError);
                            }
                        }
                        break;
                    case DOLOOP:
                    case WHILELOOP:
                    case FORLOOP:
                    case FOREACHLOOP:
                        if (label == null) return Pair.of(env1.tree, pendingError);
                        break;
                    case SWITCH:
                        if (label == null && tag == BREAK) return Pair.of(env1.tree, null);
                        break;
                    case SWITCH_EXPRESSION:
                        if (tag == YIELD) {
                            return Pair.of(env1.tree, null);
                        } else if (tag == BREAK) {
                            pendingError = Errors.BreakOutsideSwitchExpression;
                        } else {
                            pendingError = Errors.ContinueOutsideSwitchExpression;
                        }
                        break;
                    case LAMBDA:
                    case METHODDEF:
                    case CLASSDEF:
                        break LOOP;
                    default:
                }
                env1 = env1.next;
            }
            if (label != null)
                return Pair.of(null, Errors.UndefLabel(label));
            else if (pendingError != null)
                return Pair.of(null, pendingError);
            else if (tag == CONTINUE)
                return Pair.of(null, Errors.ContOutsideLoop);
            else
                return Pair.of(null, Errors.BreakOutsideSwitchLoop);
        }

    public void visitReturn(JCReturn tree) {
        if (env.info.returnResult == null) {
            log.error(tree.pos(), Errors.RetOutsideMeth);
        } else if (env.info.yieldResult != null) {
            log.error(tree.pos(), Errors.ReturnOutsideSwitchExpression);
        } else if (!env.info.isLambda &&
                !env.info.isNewClass &&
                env.enclMethod != null &&
                TreeInfo.isCompactConstructor(env.enclMethod)) {
            log.error(env.enclMethod,
                    Errors.InvalidCanonicalConstructorInRecord(Fragments.Compact, env.enclMethod.sym.name, Fragments.CanonicalCantHaveReturnStatement));
        } else {
            if (tree.expr != null) {
                if (env.info.returnResult.pt.hasTag(VOID)) {
                    env.info.returnResult.checkContext.report(tree.expr.pos(),
                              diags.fragment(Fragments.UnexpectedRetVal));
                }
                attribTree(tree.expr, env, env.info.returnResult);
            } else if (!env.info.returnResult.pt.hasTag(VOID) &&
                    !env.info.returnResult.pt.hasTag(NONE)) {
                env.info.returnResult.checkContext.report(tree.pos(),
                              diags.fragment(Fragments.MissingRetVal(env.info.returnResult.pt)));
            }
        }
        result = null;
    }

    public void visitThrow(JCThrow tree) {
        Type owntype = attribExpr(tree.expr, env, Type.noType);
        chk.checkType(tree, owntype, syms.throwableType);
        result = null;
    }

    public void visitAssert(JCAssert tree) {
        attribExpr(tree.cond, env, syms.booleanType);
        if (tree.detail != null) {
            chk.checkNonVoid(tree.detail.pos(), attribExpr(tree.detail, env));
        }
        result = null;
    }

     /** Visitor method for method invocations.
     *  NOTE: The method part of an application will have in its type field
     *        the return type of the method, not the method's type itself!
     */
    public void visitApply(JCMethodInvocation tree) {
        Env<AttrContext> localEnv = env.dup(tree, env.info.dup());

        List<Type> argtypes;

        List<Type> typeargtypes = null;

        Name methName = TreeInfo.name(tree.meth);

        boolean isConstructorCall =
            methName == names._this || methName == names._super;

        ListBuffer<Type> argtypesBuf = new ListBuffer<>();
        if (isConstructorCall) {

            KindSelector kind = attribArgs(KindSelector.MTH, tree.args, localEnv, argtypesBuf);
            argtypes = argtypesBuf.toList();
            typeargtypes = attribTypes(tree.typeargs, localEnv);

            env.info.ctorPrologue = false;

            Type site = env.enclClass.sym.type;
            if (methName == names._super) {
                if (site == syms.objectType) {
                    log.error(tree.meth.pos(), Errors.NoSuperclass(site));
                    site = types.createErrorType(syms.objectType);
                } else {
                    site = types.supertype(site);
                }
            }

            if (site.hasTag(CLASS)) {
                Type encl = site.getEnclosingType();
                while (encl != null && encl.hasTag(TYPEVAR))
                    encl = encl.getUpperBound();
                if (encl.hasTag(CLASS)) {

                    if (tree.meth.hasTag(SELECT)) {
                        JCTree qualifier = ((JCFieldAccess) tree.meth).selected;

                        chk.checkRefType(qualifier.pos(),
                                         attribExpr(qualifier, localEnv,
                                                    encl));
                    } else if (methName == names._super) {
                        rs.resolveImplicitThis(tree.meth.pos(),
                                               localEnv, site, true);
                    }
                } else if (tree.meth.hasTag(SELECT)) {
                    log.error(tree.meth.pos(),
                              Errors.IllegalQualNotIcls(site.tsym));
                    attribExpr(((JCFieldAccess) tree.meth).selected, localEnv, site);
                }

                if (site.tsym == syms.enumSym)
                    argtypes = argtypes.prepend(syms.intType).prepend(syms.stringType);

                boolean selectSuperPrev = localEnv.info.selectSuper;
                localEnv.info.selectSuper = true;
                localEnv.info.pendingResolutionPhase = null;
                Symbol sym = rs.resolveConstructor(
                    tree.meth.pos(), localEnv, site, argtypes, typeargtypes);
                localEnv.info.selectSuper = selectSuperPrev;

                TreeInfo.setSymbol(tree.meth, sym);

                Type mpt = newMethodTemplate(resultInfo.pt, argtypes, typeargtypes);
                checkId(tree.meth, site, sym, localEnv,
                        new ResultInfo(kind, mpt));
            } else if (site.hasTag(ERROR) && tree.meth.hasTag(SELECT)) {
                attribExpr(((JCFieldAccess) tree.meth).selected, localEnv, site);
            }
            result = tree.type = syms.voidType;
        } else {
            KindSelector kind = attribArgs(KindSelector.VAL, tree.args, localEnv, argtypesBuf);
            argtypes = argtypesBuf.toList();
            typeargtypes = attribAnyTypes(tree.typeargs, localEnv);

            Type mpt = newMethodTemplate(resultInfo.pt, argtypes, typeargtypes);
            localEnv.info.pendingResolutionPhase = null;
            Type mtype = attribTree(tree.meth, localEnv, new ResultInfo(kind, mpt, resultInfo.checkContext));

            Type restype = mtype.getReturnType();
            if (restype.hasTag(WILDCARD))
                throw new AssertionError(mtype);

            Type qualifier = (tree.meth.hasTag(SELECT))
                    ? ((JCFieldAccess) tree.meth).selected.type
                    : env.enclClass.sym.type;
            Symbol msym = TreeInfo.symbol(tree.meth);
            restype = adjustMethodReturnType(msym, qualifier, methName, argtypes, restype);

            chk.checkRefTypes(tree.typeargs, typeargtypes);

            Type capturedRes = resultInfo.checkContext.inferenceContext().cachedCapture(tree, restype, true);
            result = check(tree, capturedRes, KindSelector.VAL, resultInfo);
        }
        chk.validate(tree.typeargs, localEnv);
    }
        Type adjustMethodReturnType(Symbol msym, Type qualifierType, Name methodName, List<Type> argtypes, Type restype) {
            if (msym != null &&
                    (msym.owner == syms.objectType.tsym || msym.owner.isInterface()) &&
                    methodName == names.getClass &&
                    argtypes.isEmpty()) {
                return new ClassType(restype.getEnclosingType(),
                        List.of(new WildcardType(types.erasure(qualifierType.baseType()),
                                BoundKind.EXTENDS,
                                syms.boundClass)),
                        restype.tsym,
                        restype.getMetadata());
            } else if (msym != null &&
                    msym.owner == syms.arrayClass &&
                    methodName == names.clone &&
                    types.isArray(qualifierType)) {
                return qualifierType;
            } else {
                return restype;
            }
        }

        /** Obtain a method type with given argument types.
         */
        Type newMethodTemplate(Type restype, List<Type> argtypes, List<Type> typeargtypes) {
            MethodType mt = new MethodType(argtypes, restype, List.nil(), syms.methodClass);
            return (typeargtypes == null) ? mt : (Type)new ForAll(typeargtypes, mt);
        }

    public void visitNewClass(final JCNewClass tree) {
        Type owntype = types.createErrorType(tree.type);

        Env<AttrContext> localEnv = env.dup(tree, env.info.dup());

        JCClassDecl cdef = tree.def;

        JCExpression clazz = tree.clazz; 
        JCExpression clazzid;            
        JCAnnotatedType annoclazzid;     
        annoclazzid = null;

        if (clazz.hasTag(TYPEAPPLY)) {
            clazzid = ((JCTypeApply) clazz).clazz;
            if (clazzid.hasTag(ANNOTATED_TYPE)) {
                annoclazzid = (JCAnnotatedType) clazzid;
                clazzid = annoclazzid.underlyingType;
            }
        } else {
            if (clazz.hasTag(ANNOTATED_TYPE)) {
                annoclazzid = (JCAnnotatedType) clazz;
                clazzid = annoclazzid.underlyingType;
            } else {
                clazzid = clazz;
            }
        }

        JCExpression clazzid1 = clazzid; 

        if (tree.encl != null) {
            Type encltype = chk.checkRefType(tree.encl.pos(),
                                             attribExpr(tree.encl, env));
            clazzid1 = make.at(clazz.pos).Select(make.Type(encltype),
                                                 ((JCIdent) clazzid).name);

            EndPosTable endPosTable = this.env.toplevel.endPositions;
            endPosTable.storeEnd(clazzid1, clazzid.getEndPosition(endPosTable));
            if (clazz.hasTag(ANNOTATED_TYPE)) {
                JCAnnotatedType annoType = (JCAnnotatedType) clazz;
                List<JCAnnotation> annos = annoType.annotations;

                if (annoType.underlyingType.hasTag(TYPEAPPLY)) {
                    clazzid1 = make.at(tree.pos).
                        TypeApply(clazzid1,
                                  ((JCTypeApply) clazz).arguments);
                }

                clazzid1 = make.at(tree.pos).
                    AnnotatedType(annos, clazzid1);
            } else if (clazz.hasTag(TYPEAPPLY)) {
                clazzid1 = make.at(tree.pos).
                    TypeApply(clazzid1,
                              ((JCTypeApply) clazz).arguments);
            }

            clazz = clazzid1;
        }

        Type clazztype;

        try {
            env.info.isNewClass = true;
            clazztype = TreeInfo.isEnumInit(env.tree) ?
                attribIdentAsEnumType(env, (JCIdent)clazz) :
                attribType(clazz, env);
        } finally {
            env.info.isNewClass = false;
        }

        clazztype = chk.checkDiamond(tree, clazztype);
        chk.validate(clazz, localEnv);
        if (tree.encl != null) {
            tree.clazz.type = clazztype;
            TreeInfo.setSymbol(clazzid, TreeInfo.symbol(clazzid1));
            clazzid.type = ((JCIdent) clazzid).sym.type;
            if (annoclazzid != null) {
                annoclazzid.type = clazzid.type;
            }
            if (!clazztype.isErroneous()) {
                if (cdef != null && clazztype.tsym.isInterface()) {
                    log.error(tree.encl.pos(), Errors.AnonClassImplIntfNoQualForNew);
                } else if (clazztype.tsym.isStatic()) {
                    log.error(tree.encl.pos(), Errors.QualifiedNewOfStaticClass(clazztype.tsym));
                }
            }
        } else if (!clazztype.tsym.isInterface() &&
                   clazztype.getEnclosingType().hasTag(CLASS)) {
            rs.resolveImplicitThis(tree.pos(), env, clazztype);
        }

        ListBuffer<Type> argtypesBuf = new ListBuffer<>();
        final KindSelector pkind =
            attribArgs(KindSelector.VAL, tree.args, localEnv, argtypesBuf);
        List<Type> argtypes = argtypesBuf.toList();
        List<Type> typeargtypes = attribTypes(tree.typeargs, localEnv);

        if (clazztype.hasTag(CLASS) || clazztype.hasTag(ERROR)) {
            if ((clazztype.tsym.flags_field & Flags.ENUM) != 0 &&
                (!env.tree.hasTag(VARDEF) ||
                 (((JCVariableDecl) env.tree).mods.flags & Flags.ENUM) == 0 ||
                 ((JCVariableDecl) env.tree).init != tree))
                log.error(tree.pos(), Errors.EnumCantBeInstantiated);

            boolean isSpeculativeDiamondInferenceRound = TreeInfo.isDiamond(tree) &&
                    resultInfo.checkContext.deferredAttrContext().mode == DeferredAttr.AttrMode.SPECULATIVE;
            boolean skipNonDiamondPath = false;
            if (cdef == null && !isSpeculativeDiamondInferenceRound && 
                (clazztype.tsym.flags() & (ABSTRACT | INTERFACE)) != 0) {
                log.error(tree.pos(),
                          Errors.AbstractCantBeInstantiated(clazztype.tsym));
                skipNonDiamondPath = true;
            } else if (cdef != null && clazztype.tsym.isInterface()) {
                if (!argtypes.isEmpty())
                    log.error(tree.args.head.pos(), Errors.AnonClassImplIntfNoArgs);

                if (!typeargtypes.isEmpty())
                    log.error(tree.typeargs.head.pos(), Errors.AnonClassImplIntfNoTypeargs);

                argtypes = List.nil();
                typeargtypes = List.nil();
                skipNonDiamondPath = true;
            }
            if (TreeInfo.isDiamond(tree)) {
                ClassType site = new ClassType(clazztype.getEnclosingType(),
                            clazztype.tsym.type.getTypeArguments(),
                                               clazztype.tsym,
                                               clazztype.getMetadata());

                Env<AttrContext> diamondEnv = localEnv.dup(tree);
                diamondEnv.info.selectSuper = cdef != null || tree.classDeclRemoved();
                diamondEnv.info.pendingResolutionPhase = null;

                Symbol constructor = rs.resolveDiamond(tree.pos(),
                            diamondEnv,
                            site,
                            argtypes,
                            typeargtypes);
                tree.constructor = constructor.baseSymbol();

                final TypeSymbol csym = clazztype.tsym;
                ResultInfo diamondResult = new ResultInfo(pkind, newMethodTemplate(resultInfo.pt, argtypes, typeargtypes),
                        diamondContext(tree, csym, resultInfo.checkContext), CheckMode.NO_TREE_UPDATE);
                Type constructorType = tree.constructorType = types.createErrorType(clazztype);
                constructorType = checkId(tree, site,
                        constructor,
                        diamondEnv,
                        diamondResult);

                tree.clazz.type = types.createErrorType(clazztype);
                if (!constructorType.isErroneous()) {
                    tree.clazz.type = clazz.type = constructorType.getReturnType();
                    tree.constructorType = types.createMethodTypeWithReturn(constructorType, syms.voidType);
                }
                clazztype = chk.checkClassType(tree.clazz, tree.clazz.type, true);
            }

            else if (!skipNonDiamondPath) {
                Env<AttrContext> rsEnv = localEnv.dup(tree);
                rsEnv.info.selectSuper = cdef != null;
                rsEnv.info.pendingResolutionPhase = null;
                tree.constructor = rs.resolveConstructor(
                    tree.pos(), rsEnv, clazztype, argtypes, typeargtypes);
                if (cdef == null) { 
                    tree.constructorType = checkId(tree,
                            clazztype,
                            tree.constructor,
                            rsEnv,
                            new ResultInfo(pkind, newMethodTemplate(syms.voidType, argtypes, typeargtypes), CheckMode.NO_TREE_UPDATE));
                    if (rsEnv.info.lastResolveVarargs())
                        Assert.check(tree.constructorType.isErroneous() || tree.varargsElement != null);
                }
            }

            if (cdef != null) {
                visitAnonymousClassDefinition(tree, clazz, clazztype, cdef, localEnv, argtypes, typeargtypes, pkind);
                return;
            }

            if (tree.constructor != null && tree.constructor.kind == MTH)
                owntype = clazztype;
        }
        result = check(tree, owntype, KindSelector.VAL, resultInfo);
        InferenceContext inferenceContext = resultInfo.checkContext.inferenceContext();
        if (tree.constructorType != null && inferenceContext.free(tree.constructorType)) {
            inferenceContext.addFreeTypeListener(List.of(tree.constructorType),
                    instantiatedContext -> {
                        tree.constructorType = instantiatedContext.asInstType(tree.constructorType);
                    });
        }
        chk.validate(tree.typeargs, localEnv);
    }

        private void visitAnonymousClassDefinition(JCNewClass tree, JCExpression clazz, Type clazztype,
                                                   JCClassDecl cdef, Env<AttrContext> localEnv,
                                                   List<Type> argtypes, List<Type> typeargtypes,
                                                   KindSelector pkind) {
            InferenceContext inferenceContext = resultInfo.checkContext.inferenceContext();
            Type enclType = clazztype.getEnclosingType();
            if (enclType != null &&
                    enclType.hasTag(CLASS) &&
                    !chk.checkDenotable((ClassType)enclType)) {
                log.error(tree.encl, Errors.EnclosingClassTypeNonDenotable(enclType));
            }
            final boolean isDiamond = TreeInfo.isDiamond(tree);
            if (isDiamond
                    && ((tree.constructorType != null && inferenceContext.free(tree.constructorType))
                    || (tree.clazz.type != null && inferenceContext.free(tree.clazz.type)))) {
                final ResultInfo resultInfoForClassDefinition = this.resultInfo;
                Env<AttrContext> dupLocalEnv = copyEnv(localEnv);
                inferenceContext.addFreeTypeListener(List.of(tree.constructorType, tree.clazz.type),
                        instantiatedContext -> {
                            tree.constructorType = instantiatedContext.asInstType(tree.constructorType);
                            tree.clazz.type = clazz.type = instantiatedContext.asInstType(clazz.type);
                            ResultInfo prevResult = this.resultInfo;
                            try {
                                this.resultInfo = resultInfoForClassDefinition;
                                visitAnonymousClassDefinition(tree, clazz, clazz.type, cdef,
                                        dupLocalEnv, argtypes, typeargtypes, pkind);
                            } finally {
                                this.resultInfo = prevResult;
                            }
                        });
            } else {
                if (isDiamond && clazztype.hasTag(CLASS)) {
                    List<Type> invalidDiamondArgs = chk.checkDiamondDenotable((ClassType)clazztype);
                    if (!clazztype.isErroneous() && invalidDiamondArgs.nonEmpty()) {
                        Fragment fragment = Diamond(clazztype.tsym);
                        log.error(tree.clazz.pos(),
                                Errors.CantApplyDiamond1(
                                        fragment,
                                        invalidDiamondArgs.size() > 1 ?
                                                DiamondInvalidArgs(invalidDiamondArgs, fragment) :
                                                DiamondInvalidArg(invalidDiamondArgs, fragment)));
                    }
                    for (Type t : clazztype.getTypeArguments()) {
                        rs.checkAccessibleType(env, t);
                    }
                }

                boolean implementing = clazztype.tsym.isInterface() ||
                        clazztype.isErroneous() && !clazztype.getOriginalType().hasTag(NONE) &&
                        clazztype.getOriginalType().tsym.isInterface();

                if (implementing) {
                    cdef.implementing = List.of(clazz);
                } else {
                    cdef.extending = clazz;
                }

                if (resultInfo.checkContext.deferredAttrContext().mode == DeferredAttr.AttrMode.CHECK &&
                    rs.isSerializable(clazztype)) {
                    localEnv.info.isSerializable = true;
                }

                attribStat(cdef, localEnv);

                List<Type> finalargtypes;
                if (tree.encl != null && !clazztype.tsym.isInterface()) {
                    finalargtypes = argtypes.prepend(tree.encl.type);
                } else {
                    finalargtypes = argtypes;
                }

                if (isDiamond && pkind.contains(KindSelector.POLY)) {
                    finalargtypes = finalargtypes.map(deferredAttr.deferredCopier);
                }

                clazztype = clazztype.hasTag(ERROR) ? types.createErrorType(cdef.sym.type)
                                                    : cdef.sym.type;
                Symbol sym = tree.constructor = rs.resolveConstructor(
                        tree.pos(), localEnv, clazztype, finalargtypes, typeargtypes);
                Assert.check(!sym.kind.isResolutionError());
                tree.constructor = sym;
                tree.constructorType = checkId(tree,
                        clazztype,
                        tree.constructor,
                        localEnv,
                        new ResultInfo(pkind, newMethodTemplate(syms.voidType, finalargtypes, typeargtypes), CheckMode.NO_TREE_UPDATE));
            }
            Type owntype = (tree.constructor != null && tree.constructor.kind == MTH) ?
                                clazztype : types.createErrorType(tree.type);
            result = check(tree, owntype, KindSelector.VAL, resultInfo.dup(CheckMode.NO_INFERENCE_HOOK));
            chk.validate(tree.typeargs, localEnv);
        }

        CheckContext diamondContext(JCNewClass clazz, TypeSymbol tsym, CheckContext checkContext) {
            return new Check.NestedCheckContext(checkContext) {
                @Override
                public void report(DiagnosticPosition _unused, JCDiagnostic details) {
                    enclosingContext.report(clazz.clazz,
                            diags.fragment(Fragments.CantApplyDiamond1(Fragments.Diamond(tsym), details)));
                }
            };
        }

    /** Make an attributed null check tree.
     */
    public JCExpression makeNullCheck(JCExpression arg) {
        if (arg.getTag() == NEWCLASS)
            return arg;
        Name name = TreeInfo.name(arg);
        if (name == names._this || name == names._super) return arg;

        JCTree.Tag optag = NULLCHK;
        JCUnary tree = make.at(arg.pos).Unary(optag, arg);
        tree.operator = operators.resolveUnary(arg, optag, arg.type);
        tree.type = arg.type;
        return tree;
    }

    public void visitNewArray(JCNewArray tree) {
        Type owntype = types.createErrorType(tree.type);
        Env<AttrContext> localEnv = env.dup(tree);
        Type elemtype;
        if (tree.elemtype != null) {
            elemtype = attribType(tree.elemtype, localEnv);
            chk.validate(tree.elemtype, localEnv);
            owntype = elemtype;
            for (List<JCExpression> l = tree.dims; l.nonEmpty(); l = l.tail) {
                attribExpr(l.head, localEnv, syms.intType);
                owntype = new ArrayType(owntype, syms.arrayClass);
            }
        } else {
            if (pt().hasTag(ARRAY)) {
                elemtype = types.elemtype(pt());
            } else {
                if (!pt().hasTag(ERROR) &&
                        (env.info.enclVar == null || !env.info.enclVar.type.isErroneous())) {
                    log.error(tree.pos(),
                              Errors.IllegalInitializerForType(pt()));
                }
                elemtype = types.createErrorType(pt());
            }
        }
        if (tree.elems != null) {
            attribExprs(tree.elems, localEnv, elemtype);
            owntype = new ArrayType(elemtype, syms.arrayClass);
        }
        if (!types.isReifiable(elemtype))
            log.error(tree.pos(), Errors.GenericArrayCreation);
        result = check(tree, owntype, KindSelector.VAL, resultInfo);
    }

    /*
     * A lambda expression can only be attributed when a target-type is available.
     * In addition, if the target-type is that of a functional interface whose
     * descriptor contains inference variables in argument position the lambda expression
     * is 'stuck' (see DeferredAttr).
     */
    @Override
    public void visitLambda(final JCLambda that) {
        boolean wrongContext = false;
        if (pt().isErroneous() || (pt().hasTag(NONE) && pt() != Type.recoveryType)) {
            if (pt().hasTag(NONE) && (env.info.enclVar == null || !env.info.enclVar.type.isErroneous())) {
                log.error(that.pos(), Errors.UnexpectedLambda);
            }
            resultInfo = recoveryInfo;
            wrongContext = true;
        }
        final Env<AttrContext> localEnv = lambdaEnv(that, env);
        boolean needsRecovery =
                resultInfo.checkContext.deferredAttrContext().mode == DeferredAttr.AttrMode.CHECK;
        try {
            if (needsRecovery && rs.isSerializable(pt())) {
                localEnv.info.isSerializable = true;
                localEnv.info.isSerializableLambda = true;
            }
            List<Type> explicitParamTypes = null;
            if (that.paramKind == JCLambda.ParameterKind.EXPLICIT) {
                attribStats(that.params, localEnv);
                explicitParamTypes = TreeInfo.types(that.params);
            }

            TargetInfo targetInfo = getTargetInfo(that, resultInfo, explicitParamTypes);
            Type currentTarget = targetInfo.target;
            Type lambdaType = targetInfo.descriptor;

            if (currentTarget.isErroneous()) {
                result = that.type = currentTarget;
                return;
            }

            setFunctionalInfo(localEnv, that, pt(), lambdaType, currentTarget, resultInfo.checkContext);

            if (lambdaType.hasTag(FORALL)) {
                Fragment msg = Fragments.InvalidGenericLambdaTarget(lambdaType,
                                                                    kindName(currentTarget.tsym),
                                                                    currentTarget.tsym);
                resultInfo.checkContext.report(that, diags.fragment(msg));
                result = that.type = types.createErrorType(pt());
                return;
            }

            if (that.paramKind == JCLambda.ParameterKind.IMPLICIT) {
                List<Type> actuals = lambdaType.getParameterTypes();
                List<JCVariableDecl> params = that.params;

                boolean arityMismatch = false;

                while (params.nonEmpty()) {
                    if (actuals.isEmpty()) {
                        arityMismatch = true;
                    }
                    Type argType = arityMismatch ?
                            syms.errType :
                            actuals.head;
                    if (params.head.isImplicitlyTyped()) {
                        setSyntheticVariableType(params.head, argType);
                    }
                    params.head.sym = null;
                    actuals = actuals.isEmpty() ?
                            actuals :
                            actuals.tail;
                    params = params.tail;
                }

                attribStats(that.params, localEnv);

                if (arityMismatch) {
                    resultInfo.checkContext.report(that, diags.fragment(Fragments.IncompatibleArgTypesInLambda));
                        result = that.type = types.createErrorType(currentTarget);
                        return;
                }
            }

            needsRecovery = false;

            ResultInfo bodyResultInfo = localEnv.info.returnResult =
                    lambdaBodyResult(that, lambdaType, resultInfo);

            if (that.getBodyKind() == JCLambda.BodyKind.EXPRESSION) {
                attribTree(that.getBody(), localEnv, bodyResultInfo);
            } else {
                JCBlock body = (JCBlock)that.body;
                if (body == breakTree &&
                        resultInfo.checkContext.deferredAttrContext().mode == AttrMode.CHECK) {
                    breakTreeFound(copyEnv(localEnv));
                }
                attribStats(body.stats, localEnv);
            }

            result = check(that, currentTarget, KindSelector.VAL, resultInfo);

            boolean isSpeculativeRound =
                    resultInfo.checkContext.deferredAttrContext().mode == DeferredAttr.AttrMode.SPECULATIVE;

            preFlow(that);
            flow.analyzeLambda(env, that, make, isSpeculativeRound);

            that.type = currentTarget; 
            checkLambdaCompatible(that, lambdaType, resultInfo.checkContext);

            if (!isSpeculativeRound) {
                if (resultInfo.checkContext.inferenceContext().free(lambdaType.getThrownTypes())) {
                    List<Type> inferredThrownTypes = flow.analyzeLambdaThrownTypes(env, that, make);
                    if(!checkExConstraints(inferredThrownTypes, lambdaType.getThrownTypes(), resultInfo.checkContext.inferenceContext())) {
                        log.error(that, Errors.IncompatibleThrownTypesInMref(lambdaType.getThrownTypes()));
                    }
                }

                checkAccessibleTypes(that, localEnv, resultInfo.checkContext.inferenceContext(), lambdaType, currentTarget);
            }
            result = wrongContext ? that.type = types.createErrorType(pt())
                                  : check(that, currentTarget, KindSelector.VAL, resultInfo);
        } catch (Types.FunctionDescriptorLookupError ex) {
            JCDiagnostic cause = ex.getDiagnostic();
            resultInfo.checkContext.report(that, cause);
            result = that.type = types.createErrorType(pt());
            return;
        } catch (CompletionFailure cf) {
            chk.completionError(that.pos(), cf);
        } catch (Throwable t) {
            needsRecovery = false;
            throw t;
        } finally {
            localEnv.info.scope.leave();
            if (needsRecovery) {
                Type prevResult = result;
                try {
                    attribTree(that, env, recoveryInfo);
                } finally {
                    if (result == Type.recoveryType) {
                        result = prevResult;
                    }
                }
            }
        }
    }
        class TargetInfo {
            Type target;
            Type descriptor;

            public TargetInfo(Type target, Type descriptor) {
                this.target = target;
                this.descriptor = descriptor;
            }
        }

        TargetInfo getTargetInfo(JCPolyExpression that, ResultInfo resultInfo, List<Type> explicitParamTypes) {
            Type lambdaType;
            Type currentTarget = resultInfo.pt;
            if (resultInfo.pt != Type.recoveryType) {
                /* We need to adjust the target. If the target is an
                 * intersection type, for example: SAM & I1 & I2 ...
                 * the target will be updated to SAM
                 */
                currentTarget = targetChecker.visit(currentTarget, that);
                if (!currentTarget.isIntersection()) {
                    if (explicitParamTypes != null) {
                        currentTarget = infer.instantiateFunctionalInterface(that,
                                currentTarget, explicitParamTypes, resultInfo.checkContext);
                    }
                    currentTarget = types.removeWildcards(currentTarget);
                    lambdaType = types.findDescriptorType(currentTarget);
                } else {
                    IntersectionClassType ict = (IntersectionClassType)currentTarget;
                    ListBuffer<Type> components = new ListBuffer<>();
                    for (Type bound : ict.getExplicitComponents()) {
                        if (explicitParamTypes != null) {
                            try {
                                bound = infer.instantiateFunctionalInterface(that,
                                        bound, explicitParamTypes, resultInfo.checkContext);
                            } catch (FunctionDescriptorLookupError t) {
                            }
                        }
                        bound = types.removeWildcards(bound);
                        components.add(bound);
                    }
                    currentTarget = types.makeIntersectionType(components.toList());
                    currentTarget.tsym.flags_field |= INTERFACE;
                    lambdaType = types.findDescriptorType(currentTarget);
                }

            } else {
                currentTarget = Type.recoveryType;
                lambdaType = fallbackDescriptorType(that);
            }
            if (that.hasTag(LAMBDA) && lambdaType.hasTag(FORALL)) {
                Fragment msg = Fragments.InvalidGenericLambdaTarget(lambdaType,
                                                                    kindName(currentTarget.tsym),
                                                                    currentTarget.tsym);
                resultInfo.checkContext.report(that, diags.fragment(msg));
                currentTarget = types.createErrorType(pt());
            }
            return new TargetInfo(currentTarget, lambdaType);
        }

        void preFlow(JCLambda tree) {
            attrRecover.doRecovery();
            new PostAttrAnalyzer() {
                @Override
                public void scan(JCTree tree) {
                    if (tree == null ||
                            (tree.type != null &&
                            tree.type == Type.stuckType)) {
                        return;
                    }
                    super.scan(tree);
                }

                @Override
                public void visitClassDef(JCClassDecl that) {
                }

                public void visitLambda(JCLambda that) {
                }
            }.scan(tree.body);
        }

        Types.MapVisitor<DiagnosticPosition> targetChecker = new Types.MapVisitor<DiagnosticPosition>() {

            @Override
            public Type visitClassType(ClassType t, DiagnosticPosition pos) {
                return t.isIntersection() ?
                        visitIntersectionClassType((IntersectionClassType)t, pos) : t;
            }

            public Type visitIntersectionClassType(IntersectionClassType ict, DiagnosticPosition pos) {
                types.findDescriptorSymbol(makeNotionalInterface(ict, pos));
                return ict;
            }

            private TypeSymbol makeNotionalInterface(IntersectionClassType ict, DiagnosticPosition pos) {
                ListBuffer<Type> targs = new ListBuffer<>();
                ListBuffer<Type> supertypes = new ListBuffer<>();
                for (Type i : ict.interfaces_field) {
                    if (i.isParameterized()) {
                        targs.appendList(i.tsym.type.allparams());
                    }
                    supertypes.append(i.tsym.type);
                }
                IntersectionClassType notionalIntf = types.makeIntersectionType(supertypes.toList());
                notionalIntf.allparams_field = targs.toList();
                notionalIntf.tsym.flags_field |= INTERFACE;
                return notionalIntf.tsym;
            }
        };

        private Type fallbackDescriptorType(JCExpression tree) {
            switch (tree.getTag()) {
                case LAMBDA:
                    JCLambda lambda = (JCLambda)tree;
                    List<Type> argtypes = List.nil();
                    for (JCVariableDecl param : lambda.params) {
                        argtypes = param.vartype != null && param.vartype.type != null ?
                                argtypes.append(param.vartype.type) :
                                argtypes.append(syms.errType);
                    }
                    return new MethodType(argtypes, Type.recoveryType,
                            List.of(syms.throwableType), syms.methodClass);
                case REFERENCE:
                    return new MethodType(List.nil(), Type.recoveryType,
                            List.of(syms.throwableType), syms.methodClass);
                default:
                    Assert.error("Cannot get here!");
            }
            return null;
        }

        private void checkAccessibleTypes(final DiagnosticPosition pos, final Env<AttrContext> env,
                final InferenceContext inferenceContext, final Type... ts) {
            checkAccessibleTypes(pos, env, inferenceContext, List.from(ts));
        }

        private void checkAccessibleTypes(final DiagnosticPosition pos, final Env<AttrContext> env,
                final InferenceContext inferenceContext, final List<Type> ts) {
            if (inferenceContext.free(ts)) {
                inferenceContext.addFreeTypeListener(ts,
                        solvedContext -> checkAccessibleTypes(pos, env, solvedContext, solvedContext.asInstTypes(ts)));
            } else {
                for (Type t : ts) {
                    rs.checkAccessibleType(env, t);
                }
            }
        }

        /**
         * Lambda/method reference have a special check context that ensures
         * that i.e. a lambda return type is compatible with the expected
         * type according to both the inherited context and the assignment
         * context.
         */
        class FunctionalReturnContext extends Check.NestedCheckContext {

            FunctionalReturnContext(CheckContext enclosingContext) {
                super(enclosingContext);
            }

            @Override
            public boolean compatible(Type found, Type req, Warner warn) {
                return chk.basicHandler.compatible(inferenceContext().asUndetVar(found), inferenceContext().asUndetVar(req), warn);
            }

            @Override
            public void report(DiagnosticPosition pos, JCDiagnostic details) {
                enclosingContext.report(pos, diags.fragment(Fragments.IncompatibleRetTypeInLambda(details)));
            }
        }

        class ExpressionLambdaReturnContext extends FunctionalReturnContext {

            JCExpression expr;
            boolean expStmtExpected;

            ExpressionLambdaReturnContext(JCExpression expr, CheckContext enclosingContext) {
                super(enclosingContext);
                this.expr = expr;
            }

            @Override
            public void report(DiagnosticPosition pos, JCDiagnostic details) {
                if (expStmtExpected) {
                    enclosingContext.report(pos, diags.fragment(Fragments.StatExprExpected));
                } else {
                    super.report(pos, details);
                }
            }

            @Override
            public boolean compatible(Type found, Type req, Warner warn) {
                if (req.hasTag(VOID)) {
                    expStmtExpected = true;
                    return TreeInfo.isExpressionStatement(expr);
                } else {
                    return super.compatible(found, req, warn);
                }
            }
        }

        ResultInfo lambdaBodyResult(JCLambda that, Type descriptor, ResultInfo resultInfo) {
            FunctionalReturnContext funcContext = that.getBodyKind() == JCLambda.BodyKind.EXPRESSION ?
                    new ExpressionLambdaReturnContext((JCExpression)that.getBody(), resultInfo.checkContext) :
                    new FunctionalReturnContext(resultInfo.checkContext);

            return descriptor.getReturnType() == Type.recoveryType ?
                    recoveryInfo :
                    new ResultInfo(KindSelector.VAL,
                            descriptor.getReturnType(), funcContext);
        }

        /**
        * Lambda compatibility. Check that given return types, thrown types, parameter types
        * are compatible with the expected functional interface descriptor. This means that:
        * (i) parameter types must be identical to those of the target descriptor; (ii) return
        * types must be compatible with the return type of the expected descriptor.
        */
        void checkLambdaCompatible(JCLambda tree, Type descriptor, CheckContext checkContext) {
            Type returnType = checkContext.inferenceContext().asUndetVar(descriptor.getReturnType());

            if (tree.getBodyKind() == JCLambda.BodyKind.STATEMENT && tree.canCompleteNormally &&
                    !returnType.hasTag(VOID) && returnType != Type.recoveryType) {
                Fragment msg =
                        Fragments.IncompatibleRetTypeInLambda(Fragments.MissingRetVal(returnType));
                checkContext.report(tree,
                                    diags.fragment(msg));
            }

            List<Type> argTypes = checkContext.inferenceContext().asUndetVars(descriptor.getParameterTypes());
            if (!types.isSameTypes(argTypes, TreeInfo.types(tree.params))) {
                checkContext.report(tree, diags.fragment(Fragments.IncompatibleArgTypesInLambda));
            }
        }

        /* Map to hold 'fake' clinit methods. If a lambda is used to initialize a
         * static field and that lambda has type annotations, these annotations will
         * also be stored at these fake clinit methods.
         *
         * LambdaToMethod also use fake clinit methods so they can be reused.
         * Also as LTM is a phase subsequent to attribution, the methods from
         * clinits can be safely removed by LTM to save memory.
         */
        private Map<ClassSymbol, MethodSymbol> clinits = new HashMap<>();

        public MethodSymbol removeClinit(ClassSymbol sym) {
            return clinits.remove(sym);
        }

        /* This method returns an environment to be used to attribute a lambda
         * expression.
         *
         * The owner of this environment is a method symbol. If the current owner
         * is not a method, for example if the lambda is used to initialize
         * a field, then if the field is:
         *
         * - an instance field, we use the first constructor.
         * - a static field, we create a fake clinit method.
         */
        public Env<AttrContext> lambdaEnv(JCLambda that, Env<AttrContext> env) {
            Env<AttrContext> lambdaEnv;
            Symbol owner = env.info.scope.owner;
            if (owner.kind == VAR && owner.owner.kind == TYP) {
                ClassSymbol enclClass = owner.enclClass();
                Symbol newScopeOwner = env.info.scope.owner;
                /* if the field isn't static, then we can get the first constructor
                 * and use it as the owner of the environment. This is what
                 * LTM code is doing to look for type annotations so we are fine.
                 */
                if ((owner.flags() & STATIC) == 0) {
                    for (Symbol s : enclClass.members_field.getSymbolsByName(names.init)) {
                        newScopeOwner = s;
                        break;
                    }
                } else {
                    /* if the field is static then we need to create a fake clinit
                     * method, this method can later be reused by LTM.
                     */
                    MethodSymbol clinit = clinits.get(enclClass);
                    if (clinit == null) {
                        Type clinitType = new MethodType(List.nil(),
                                syms.voidType, List.nil(), syms.methodClass);
                        clinit = new MethodSymbol(STATIC | SYNTHETIC | PRIVATE,
                                names.clinit, clinitType, enclClass);
                        clinit.params = List.nil();
                        clinits.put(enclClass, clinit);
                    }
                    newScopeOwner = clinit;
                }
                lambdaEnv = env.dup(that, env.info.dup(env.info.scope.dupUnshared(newScopeOwner)));
            } else {
                lambdaEnv = env.dup(that, env.info.dup(env.info.scope.dup()));
            }
            lambdaEnv.info.yieldResult = null;
            lambdaEnv.info.isLambda = true;
            return lambdaEnv;
        }

    @Override
    public void visitReference(final JCMemberReference that) {
        if (pt().isErroneous() || (pt().hasTag(NONE) && pt() != Type.recoveryType)) {
            if (pt().hasTag(NONE) && (env.info.enclVar == null || !env.info.enclVar.type.isErroneous())) {
                log.error(that.pos(), Errors.UnexpectedMref);
            }
            result = that.type = types.createErrorType(pt());
            return;
        }
        final Env<AttrContext> localEnv = env.dup(that);
        try {
            Type exprType = attribTree(that.expr, env, memberReferenceQualifierResult(that));

            if (that.getMode() == JCMemberReference.ReferenceMode.NEW) {
                exprType = chk.checkConstructorRefType(that.expr, exprType);
                if (!exprType.isErroneous() &&
                    exprType.isRaw() &&
                    that.typeargs != null) {
                    log.error(that.expr.pos(),
                              Errors.InvalidMref(Kinds.kindName(that.getMode()),
                                                 Fragments.MrefInferAndExplicitParams));
                    exprType = types.createErrorType(exprType);
                }
            }

            if (exprType.isErroneous()) {
                result = that.type = exprType;
                return;
            }

            if (TreeInfo.isStaticSelector(that.expr, names)) {
                chk.validate(that.expr, env, false);
            } else {
                Symbol lhsSym = TreeInfo.symbol(that.expr);
                localEnv.info.selectSuper = lhsSym != null && lhsSym.name == names._super;
            }
            List<Type> typeargtypes = List.nil();
            if (that.typeargs != null) {
                typeargtypes = attribTypes(that.typeargs, localEnv);
            }

            boolean isTargetSerializable =
                    resultInfo.checkContext.deferredAttrContext().mode == DeferredAttr.AttrMode.CHECK &&
                    rs.isSerializable(pt());
            TargetInfo targetInfo = getTargetInfo(that, resultInfo, null);
            Type currentTarget = targetInfo.target;
            Type desc = targetInfo.descriptor;

            setFunctionalInfo(localEnv, that, pt(), desc, currentTarget, resultInfo.checkContext);
            List<Type> argtypes = desc.getParameterTypes();
            Resolve.MethodCheck referenceCheck = rs.resolveMethodCheck;

            if (resultInfo.checkContext.inferenceContext().free(argtypes)) {
                referenceCheck = rs.new MethodReferenceCheck(resultInfo.checkContext.inferenceContext());
            }

            Pair<Symbol, Resolve.ReferenceLookupHelper> refResult = null;
            List<Type> saved_undet = resultInfo.checkContext.inferenceContext().save();
            try {
                refResult = rs.resolveMemberReference(localEnv, that, that.expr.type,
                        that.name, argtypes, typeargtypes, targetInfo.descriptor, referenceCheck,
                        resultInfo.checkContext.inferenceContext(), rs.basicReferenceChooser);
            } finally {
                resultInfo.checkContext.inferenceContext().rollback(saved_undet);
            }

            Symbol refSym = refResult.fst;
            Resolve.ReferenceLookupHelper lookupHelper = refResult.snd;

            /** this switch will need to go away and be replaced by the new RESOLUTION_TARGET testing
             *  JDK-8075541
             */
            if (refSym.kind != MTH) {
                boolean targetError;
                switch (refSym.kind) {
                    case ABSENT_MTH:
                    case MISSING_ENCL:
                        targetError = false;
                        break;
                    case WRONG_MTH:
                    case WRONG_MTHS:
                    case AMBIGUOUS:
                    case HIDDEN:
                    case STATICERR:
                        targetError = true;
                        break;
                    default:
                        Assert.error("unexpected result kind " + refSym.kind);
                        targetError = false;
                }

                JCDiagnostic detailsDiag = ((Resolve.ResolveError)refSym.baseSymbol())
                        .getDiagnostic(JCDiagnostic.DiagnosticType.FRAGMENT,
                                that, exprType.tsym, exprType, that.name, argtypes, typeargtypes);

                JCDiagnostic diag = diags.create(log.currentSource(), that,
                        targetError ?
                            Fragments.InvalidMref(Kinds.kindName(that.getMode()), detailsDiag) :
                            Errors.InvalidMref(Kinds.kindName(that.getMode()), detailsDiag));

                if (targetError && currentTarget == Type.recoveryType) {
                    result = that.type = currentTarget;
                    return;
                } else {
                    if (targetError) {
                        resultInfo.checkContext.report(that, diag);
                    } else {
                        log.report(diag);
                    }
                    result = that.type = types.createErrorType(currentTarget);
                    return;
                }
            }

            that.sym = refSym.isConstructor() ? refSym.baseSymbol() : refSym;
            that.kind = lookupHelper.referenceKind(that.sym);
            that.ownerAccessible = rs.isAccessible(localEnv, that.sym.enclClass());

            if (desc.getReturnType() == Type.recoveryType) {
                result = that.type = currentTarget;
                return;
            }

            if (!env.info.attributionMode.isSpeculative && that.getMode() == JCMemberReference.ReferenceMode.NEW) {
                Type enclosingType = exprType.getEnclosingType();
                if (enclosingType != null && enclosingType.hasTag(CLASS)) {
                    rs.resolveImplicitThis(that.pos(), env, exprType);
                }
            }

            if (resultInfo.checkContext.deferredAttrContext().mode == AttrMode.CHECK) {

                if (that.getMode() == ReferenceMode.INVOKE &&
                        TreeInfo.isStaticSelector(that.expr, names) &&
                        that.kind.isUnbound() &&
                        lookupHelper.site.isRaw()) {
                    chk.checkRaw(that.expr, localEnv);
                }

                if (that.sym.isStatic() && TreeInfo.isStaticSelector(that.expr, names) &&
                        exprType.getTypeArguments().nonEmpty()) {
                    log.error(that.expr.pos(),
                              Errors.InvalidMref(Kinds.kindName(that.getMode()),
                                                 Fragments.StaticMrefWithTargs));
                    result = that.type = types.createErrorType(currentTarget);
                    return;
                }

                if (!refSym.isStatic() && that.kind == JCMemberReference.ReferenceKind.SUPER) {
                    rs.checkNonAbstract(that.pos(), that.sym);
                }

                if (isTargetSerializable) {
                    chk.checkAccessFromSerializableElement(that, true);
                }
            }

            ResultInfo checkInfo =
                    resultInfo.dup(newMethodTemplate(
                        desc.getReturnType().hasTag(VOID) ? Type.noType : desc.getReturnType(),
                        that.kind.isUnbound() ? argtypes.tail : argtypes, typeargtypes),
                        new FunctionalReturnContext(resultInfo.checkContext), CheckMode.NO_TREE_UPDATE);

            Type refType = checkId(that, lookupHelper.site, refSym, localEnv, checkInfo);

            if (that.kind.isUnbound() &&
                    resultInfo.checkContext.inferenceContext().free(argtypes.head)) {
                if (!types.isSubtype(resultInfo.checkContext.inferenceContext().asUndetVar(argtypes.head), exprType)) {
                    Assert.error("Can't get here");
                }
            }

            if (!refType.isErroneous()) {
                refType = types.createMethodTypeWithReturn(refType,
                        adjustMethodReturnType(refSym, lookupHelper.site, that.name, checkInfo.pt.getParameterTypes(), refType.getReturnType()));
            }

            boolean isSpeculativeRound =
                    resultInfo.checkContext.deferredAttrContext().mode == DeferredAttr.AttrMode.SPECULATIVE;

            that.type = currentTarget; 
            checkReferenceCompatible(that, desc, refType, resultInfo.checkContext, isSpeculativeRound);
            if (!isSpeculativeRound) {
                checkAccessibleTypes(that, localEnv, resultInfo.checkContext.inferenceContext(), desc, currentTarget);
            }
            result = check(that, currentTarget, KindSelector.VAL, resultInfo);
        } catch (Types.FunctionDescriptorLookupError ex) {
            JCDiagnostic cause = ex.getDiagnostic();
            resultInfo.checkContext.report(that, cause);
            result = that.type = types.createErrorType(pt());
            return;
        }
    }
        ResultInfo memberReferenceQualifierResult(JCMemberReference tree) {
            return new ResultInfo(tree.getMode() == ReferenceMode.INVOKE ?
                                  KindSelector.VAL_TYP : KindSelector.TYP,
                                  Type.noType);
        }


    @SuppressWarnings("fallthrough")
    void checkReferenceCompatible(JCMemberReference tree, Type descriptor, Type refType, CheckContext checkContext, boolean speculativeAttr) {
        InferenceContext inferenceContext = checkContext.inferenceContext();
        Type returnType = inferenceContext.asUndetVar(descriptor.getReturnType());

        Type resType;
        switch (tree.getMode()) {
            case NEW:
                if (!tree.expr.type.isRaw()) {
                    resType = tree.expr.type;
                    break;
                }
            default:
                resType = refType.getReturnType();
        }

        Type incompatibleReturnType = resType;

        if (returnType.hasTag(VOID)) {
            incompatibleReturnType = null;
        }

        if (!returnType.hasTag(VOID) && !resType.hasTag(VOID)) {
            if (resType.isErroneous() ||
                    new FunctionalReturnContext(checkContext).compatible(resType, returnType,
                            checkContext.checkWarner(tree, resType, returnType))) {
                incompatibleReturnType = null;
            }
        }

        if (incompatibleReturnType != null) {
            Fragment msg =
                    Fragments.IncompatibleRetTypeInMref(Fragments.InconvertibleTypes(resType, descriptor.getReturnType()));
            checkContext.report(tree, diags.fragment(msg));
        } else {
            if (inferenceContext.free(refType)) {
                inferenceContext.addFreeTypeListener(List.of(refType),
                        instantiatedContext -> {
                            tree.referentType = instantiatedContext.asInstType(refType);
                        });
            } else {
                tree.referentType = refType;
            }
        }

        if (!speculativeAttr) {
            if (!checkExConstraints(refType.getThrownTypes(), descriptor.getThrownTypes(), inferenceContext)) {
                log.error(tree, Errors.IncompatibleThrownTypesInMref(refType.getThrownTypes()));
            }
        }
    }

    boolean checkExConstraints(
            List<Type> thrownByFuncExpr,
            List<Type> thrownAtFuncType,
            InferenceContext inferenceContext) {
        /** 18.2.5: Otherwise, let E1, ..., En be the types in the function type's throws clause that
         *  are not proper types
         */
        List<Type> nonProperList = thrownAtFuncType.stream()
                .filter(e -> inferenceContext.free(e)).collect(List.collector());
        List<Type> properList = thrownAtFuncType.diff(nonProperList);

        /** Let X1,...,Xm be the checked exception types that the lambda body can throw or
         *  in the throws clause of the invocation type of the method reference's compile-time
         *  declaration
         */
        List<Type> checkedList = thrownByFuncExpr.stream()
                .filter(e -> chk.isChecked(e)).collect(List.collector());

        /** If n = 0 (the function type's throws clause consists only of proper types), then
         *  if there exists some i (1 <= i <= m) such that Xi is not a subtype of any proper type
         *  in the throws clause, the constraint reduces to false; otherwise, the constraint
         *  reduces to true
         */
        ListBuffer<Type> uncaughtByProperTypes = new ListBuffer<>();
        for (Type checked : checkedList) {
            boolean isSubtype = false;
            for (Type proper : properList) {
                if (types.isSubtype(checked, proper)) {
                    isSubtype = true;
                    break;
                }
            }
            if (!isSubtype) {
                uncaughtByProperTypes.add(checked);
            }
        }

        if (nonProperList.isEmpty() && !uncaughtByProperTypes.isEmpty()) {
            return false;
        }

        /** If n > 0, the constraint reduces to a set of subtyping constraints:
         *  for all i (1 <= i <= m), if Xi is not a subtype of any proper type in the
         *  throws clause, then the constraints include, for all j (1 <= j <= n), <Xi <: Ej>
         */
        List<Type> nonProperAsUndet = inferenceContext.asUndetVars(nonProperList);
        uncaughtByProperTypes.forEach(checkedEx -> {
            nonProperAsUndet.forEach(nonProper -> {
                types.isSubtype(checkedEx, nonProper);
            });
        });

        /** In addition, for all j (1 <= j <= n), the constraint reduces to the bound throws Ej
         */
        nonProperAsUndet.stream()
                .filter(t -> t.hasTag(UNDETVAR))
                .forEach(t -> ((UndetVar)t).setThrow());
        return true;
    }

    /**
     * Set functional type info on the underlying AST. Note: as the target descriptor
     * might contain inference variables, we might need to register an hook in the
     * current inference context.
     */
    private void setFunctionalInfo(final Env<AttrContext> env, final JCFunctionalExpression fExpr,
            final Type pt, final Type descriptorType, final Type primaryTarget, final CheckContext checkContext) {
        if (checkContext.inferenceContext().free(descriptorType)) {
            checkContext.inferenceContext().addFreeTypeListener(List.of(pt, descriptorType),
                    inferenceContext -> setFunctionalInfo(env, fExpr, pt, inferenceContext.asInstType(descriptorType),
                    inferenceContext.asInstType(primaryTarget), checkContext));
        } else {
            if (pt.hasTag(CLASS)) {
                fExpr.target = primaryTarget;
            }
            if (checkContext.deferredAttrContext().mode == DeferredAttr.AttrMode.CHECK &&
                    pt != Type.recoveryType) {
                try {
                    /* Types.makeFunctionalInterfaceClass() may throw an exception
                     * when it's executed post-inference. See the listener code
                     * above.
                     */
                    ClassSymbol csym = types.makeFunctionalInterfaceClass(env,
                            names.empty, fExpr.target, ABSTRACT);
                    if (csym != null) {
                        chk.checkImplementations(env.tree, csym, csym);
                        try {
                            csym.flags_field |= INTERFACE;
                            types.findDescriptorType(csym.type);
                        } catch (FunctionDescriptorLookupError err) {
                            resultInfo.checkContext.report(fExpr,
                                    diags.fragment(Fragments.NoSuitableFunctionalIntfInst(fExpr.target)));
                        }
                    }
                } catch (Types.FunctionDescriptorLookupError ex) {
                    JCDiagnostic cause = ex.getDiagnostic();
                    resultInfo.checkContext.report(env.tree, cause);
                }
            }
        }
    }

    public void visitParens(JCParens tree) {
        Type owntype = attribTree(tree.expr, env, resultInfo);
        result = check(tree, owntype, pkind(), resultInfo);
        Symbol sym = TreeInfo.symbol(tree);
        if (sym != null && sym.kind.matches(KindSelector.TYP_PCK) && sym.kind != Kind.ERR)
            log.error(tree.pos(), Errors.IllegalParenthesizedExpression);
    }

    public void visitAssign(JCAssign tree) {
        Type owntype = attribTree(tree.lhs, env.dup(tree), varAssignmentInfo);
        Type capturedType = capture(owntype);
        attribExpr(tree.rhs, env, owntype);
        result = check(tree, capturedType, KindSelector.VAL, resultInfo);
    }

    public void visitAssignop(JCAssignOp tree) {
        Type owntype = attribTree(tree.lhs, env, varAssignmentInfo);
        Type operand = attribExpr(tree.rhs, env);
        Symbol operator = tree.operator = operators.resolveBinary(tree, tree.getTag().noAssignOp(), owntype, operand);
        if (operator != operators.noOpSymbol &&
                !owntype.isErroneous() &&
                !operand.isErroneous()) {
            chk.checkDivZero(tree.rhs.pos(), operator, operand);
            chk.checkCastable(tree.rhs.pos(),
                              operator.type.getReturnType(),
                              owntype);
            chk.checkLossOfPrecision(tree.rhs.pos(), operand, owntype);
        }
        result = check(tree, owntype, KindSelector.VAL, resultInfo);
    }

    public void visitUnary(JCUnary tree) {
        Type argtype = (tree.getTag().isIncOrDecUnaryOp())
            ? attribTree(tree.arg, env, varAssignmentInfo)
            : chk.checkNonVoid(tree.arg.pos(), attribExpr(tree.arg, env));

        OperatorSymbol operator = tree.operator = operators.resolveUnary(tree, tree.getTag(), argtype);
        Type owntype = types.createErrorType(tree.type);
        if (operator != operators.noOpSymbol &&
                !argtype.isErroneous()) {
            owntype = (tree.getTag().isIncOrDecUnaryOp())
                ? tree.arg.type
                : operator.type.getReturnType();
            int opc = operator.opcode;

            if (argtype.constValue() != null) {
                Type ctype = cfolder.fold1(opc, argtype);
                if (ctype != null) {
                    owntype = cfolder.coerce(ctype, owntype);
                }
            }
        }
        result = check(tree, owntype, KindSelector.VAL, resultInfo);
        matchBindings = matchBindingsComputer.unary(tree, matchBindings);
    }

    public void visitBinary(JCBinary tree) {
        Type left = chk.checkNonVoid(tree.lhs.pos(), attribExpr(tree.lhs, env));


        MatchBindings lhsBindings = matchBindings;
        List<BindingSymbol> propagatedBindings;
        switch (tree.getTag()) {
            case AND:
                propagatedBindings = lhsBindings.bindingsWhenTrue;
                break;
            case OR:
                propagatedBindings = lhsBindings.bindingsWhenFalse;
                break;
            default:
                propagatedBindings = List.nil();
                break;
        }
        Env<AttrContext> rhsEnv = bindingEnv(env, propagatedBindings);
        Type right;
        try {
            right = chk.checkNonVoid(tree.rhs.pos(), attribExpr(tree.rhs, rhsEnv));
        } finally {
            rhsEnv.info.scope.leave();
        }

        matchBindings = matchBindingsComputer.binary(tree, lhsBindings, matchBindings);

        OperatorSymbol operator = tree.operator = operators.resolveBinary(tree, tree.getTag(), left, right);
        Type owntype = types.createErrorType(tree.type);
        if (operator != operators.noOpSymbol &&
                !left.isErroneous() &&
                !right.isErroneous()) {
            owntype = operator.type.getReturnType();
            int opc = operator.opcode;
            if (left.constValue() != null && right.constValue() != null) {
                Type ctype = cfolder.fold2(opc, left, right);
                if (ctype != null) {
                    owntype = cfolder.coerce(ctype, owntype);
                }
            }

            if ((opc == ByteCodes.if_acmpeq || opc == ByteCodes.if_acmpne)) {
                if (!types.isCastable(left, right, new Warner(tree.pos()))) {
                    log.error(tree.pos(), Errors.IncomparableTypes(left, right));
                }
            }

            chk.checkDivZero(tree.rhs.pos(), operator, right);
        }
        result = check(tree, owntype, KindSelector.VAL, resultInfo);
    }

    public void visitTypeCast(final JCTypeCast tree) {
        Type clazztype = attribType(tree.clazz, env);
        chk.validate(tree.clazz, env, false);
        Env<AttrContext> localEnv = env.dup(tree);
        final ResultInfo castInfo;
        JCExpression expr = TreeInfo.skipParens(tree.expr);
        boolean isPoly = (expr.hasTag(LAMBDA) || expr.hasTag(REFERENCE));
        if (isPoly) {
            castInfo = new ResultInfo(KindSelector.VAL, clazztype,
                                      new Check.NestedCheckContext(resultInfo.checkContext) {
                @Override
                public boolean compatible(Type found, Type req, Warner warn) {
                    return types.isCastable(found, req, warn);
                }
            });
        } else {
            castInfo = unknownExprInfo;
        }
        Type exprtype = attribTree(tree.expr, localEnv, castInfo);
        Type owntype = isPoly ? clazztype : chk.checkCastable(tree.expr.pos(), exprtype, clazztype);
        if (exprtype.constValue() != null)
            owntype = cfolder.coerce(exprtype, owntype);
        result = check(tree, capture(owntype), KindSelector.VAL, resultInfo);
        if (!isPoly)
            chk.checkRedundantCast(localEnv, tree);
    }

    public void visitTypeTest(JCInstanceOf tree) {
        Type exprtype = chk.checkNullOrRefType(
                tree.expr.pos(), attribExpr(tree.expr, env));
        Type clazztype;
        JCTree typeTree;
        if (tree.pattern.getTag() == BINDINGPATTERN ||
            tree.pattern.getTag() == RECORDPATTERN) {
            attribExpr(tree.pattern, env, exprtype);
            clazztype = tree.pattern.type;
            if (types.isSubtype(exprtype, clazztype) &&
                !exprtype.isErroneous() && !clazztype.isErroneous() &&
                tree.pattern.getTag() != RECORDPATTERN) {
                if (!allowUnconditionalPatternsInstanceOf) {
                    log.error(DiagnosticFlag.SOURCE_LEVEL, tree.pos(),
                              Feature.UNCONDITIONAL_PATTERN_IN_INSTANCEOF.error(this.sourceName));
                }
            }
            typeTree = TreeInfo.primaryPatternTypeTree((JCPattern) tree.pattern);
        } else {
            clazztype = attribType(tree.pattern, env);
            typeTree = tree.pattern;
            chk.validate(typeTree, env, false);
        }
        if (!clazztype.hasTag(TYPEVAR)) {
            clazztype = chk.checkClassOrArrayType(typeTree.pos(), clazztype);
        }
        if (!clazztype.isErroneous() && !types.isReifiable(clazztype)) {
            boolean valid = false;
            if (allowReifiableTypesInInstanceof) {
                valid = checkCastablePattern(tree.expr.pos(), exprtype, clazztype);
            } else {
                log.error(DiagnosticFlag.SOURCE_LEVEL, tree.pos(),
                          Feature.REIFIABLE_TYPES_INSTANCEOF.error(this.sourceName));
                allowReifiableTypesInInstanceof = true;
            }
            if (!valid) {
                clazztype = types.createErrorType(clazztype);
            }
        }
        chk.checkCastable(tree.expr.pos(), exprtype, clazztype);
        result = check(tree, syms.booleanType, KindSelector.VAL, resultInfo);
    }

    private boolean checkCastablePattern(DiagnosticPosition pos,
                                         Type exprType,
                                         Type pattType) {
        Warner warner = new Warner();
        if (exprType.isErroneous() || pattType.isErroneous()) {
            return false;
        }
        if (!types.isCastable(exprType, pattType, warner)) {
            chk.basicHandler.report(pos,
                    diags.fragment(Fragments.InconvertibleTypes(exprType, pattType)));
            return false;
        } else if ((exprType.isPrimitive() || pattType.isPrimitive()) &&
                   (!exprType.isPrimitive() ||
                    !pattType.isPrimitive() ||
                    !types.isSameType(exprType, pattType))) {
            chk.basicHandler.report(pos,
                    diags.fragment(Fragments.NotApplicableTypes(exprType, pattType)));
            return false;
        } else if (warner.hasLint(LintCategory.UNCHECKED)) {
            log.error(pos,
                    Errors.InstanceofReifiableNotSafe(exprType, pattType));
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void visitAnyPattern(JCAnyPattern tree) {
        result = tree.type = resultInfo.pt;
    }

    public void visitBindingPattern(JCBindingPattern tree) {
        Type type;
        if (tree.var.vartype != null) {
            type = attribType(tree.var.vartype, env);
        } else {
            type = resultInfo.pt;
        }
        tree.type = tree.var.type = type;
        BindingSymbol v = new BindingSymbol(tree.var.mods.flags, tree.var.name, type, env.info.scope.owner);
        v.pos = tree.pos;
        tree.var.sym = v;
        if (chk.checkUnique(tree.var.pos(), v, env.info.scope)) {
            chk.checkTransparentVar(tree.var.pos(), v, env.info.scope);
        }
        annotate.annotateLater(tree.var.mods.annotations, env, v, tree.pos());
        if (!tree.var.isImplicitlyTyped()) {
            annotate.queueScanTreeAndTypeAnnotate(tree.var.vartype, env, v, tree.var.pos());
        }
        annotate.flush();
        chk.validate(tree.var.vartype, env, true);
        result = tree.type;
        if (v.isUnnamedVariable()) {
            matchBindings = MatchBindingsComputer.EMPTY;
        } else {
            matchBindings = new MatchBindings(List.of(v), List.nil());
        }
    }

    @Override
    public void visitRecordPattern(JCRecordPattern tree) {
        Type site;

        if (tree.deconstructor == null) {
            log.error(tree.pos(), Errors.DeconstructionPatternVarNotAllowed);
            tree.record = syms.errSymbol;
            site = tree.type = types.createErrorType(tree.record.type);
        } else {
            Type type = attribType(tree.deconstructor, env);
            if (type.isRaw() && type.tsym.getTypeParameters().nonEmpty()) {
                Type inferred = infer.instantiatePatternType(resultInfo.pt, type.tsym);
                if (inferred == null) {
                    log.error(tree.pos(), Errors.PatternTypeCannotInfer);
                } else {
                    type = inferred;
                }
            }
            tree.type = tree.deconstructor.type = type;
            site = types.capture(tree.type);
        }

        List<Type> expectedRecordTypes;
        if (site.tsym.kind == Kind.TYP && ((ClassSymbol) site.tsym).isRecord()) {
            ClassSymbol record = (ClassSymbol) site.tsym;
            expectedRecordTypes = record.getRecordComponents()
                                        .stream()
                                        .map(rc -> types.memberType(site, rc))
                                        .map(t -> types.upward(t, types.captures(t)).baseType())
                                        .collect(List.collector());
            tree.record = record;
        } else {
            log.error(tree.pos(), Errors.DeconstructionPatternOnlyRecords(site.tsym));
            expectedRecordTypes = Stream.generate(() -> types.createErrorType(tree.type))
                                .limit(tree.nested.size())
                                .collect(List.collector());
            tree.record = syms.errSymbol;
        }
        ListBuffer<BindingSymbol> outBindings = new ListBuffer<>();
        List<Type> recordTypes = expectedRecordTypes;
        List<JCPattern> nestedPatterns = tree.nested;
        Env<AttrContext> localEnv = env.dup(tree, env.info.dup(env.info.scope.dup()));
        try {
            while (recordTypes.nonEmpty() && nestedPatterns.nonEmpty()) {
                attribExpr(nestedPatterns.head, localEnv, recordTypes.head);
                checkCastablePattern(nestedPatterns.head.pos(), recordTypes.head, nestedPatterns.head.type);
                outBindings.addAll(matchBindings.bindingsWhenTrue);
                matchBindings.bindingsWhenTrue.forEach(localEnv.info.scope::enter);
                nestedPatterns = nestedPatterns.tail;
                recordTypes = recordTypes.tail;
            }
            if (recordTypes.nonEmpty() || nestedPatterns.nonEmpty()) {
                while (nestedPatterns.nonEmpty()) {
                    attribExpr(nestedPatterns.head, localEnv, Type.noType);
                    nestedPatterns = nestedPatterns.tail;
                }
                List<Type> nestedTypes =
                        tree.nested.stream().map(p -> p.type).collect(List.collector());
                log.error(tree.pos(),
                          Errors.IncorrectNumberOfNestedPatterns(expectedRecordTypes,
                                                                 nestedTypes));
            }
        } finally {
            localEnv.info.scope.leave();
        }
        chk.validate(tree.deconstructor, env, true);
        result = tree.type;
        matchBindings = new MatchBindings(outBindings.toList(), List.nil());
    }

    public void visitIndexed(JCArrayAccess tree) {
        Type owntype = types.createErrorType(tree.type);
        Type atype = attribExpr(tree.indexed, env);
        attribExpr(tree.index, env, syms.intType);
        if (types.isArray(atype))
            owntype = types.elemtype(atype);
        else if (!atype.hasTag(ERROR))
            log.error(tree.pos(), Errors.ArrayReqButFound(atype));
        if (!pkind().contains(KindSelector.VAL))
            owntype = capture(owntype);
        result = check(tree, owntype, KindSelector.VAR, resultInfo);
    }

    public void visitIdent(JCIdent tree) {
        Symbol sym;

        if (pt().hasTag(METHOD) || pt().hasTag(FORALL)) {
            env.info.pendingResolutionPhase = null;
            sym = rs.resolveMethod(tree.pos(), env, tree.name, pt().getParameterTypes(), pt().getTypeArguments());
        } else if (tree.sym != null && tree.sym.kind != VAR) {
            sym = tree.sym;
        } else {
            sym = rs.resolveIdent(tree.pos(), env, tree.name, pkind());
        }
        tree.sym = sym;

        Env<AttrContext> symEnv = env;
        if (env.enclClass.sym.owner.kind != PCK && 
            sym.kind.matches(KindSelector.VAL_MTH) &&
            sym.owner.kind == TYP &&
            tree.name != names._this && tree.name != names._super) {

            while (symEnv.outer != null &&
                   !sym.isMemberOf(symEnv.enclClass.sym, types)) {
                symEnv = symEnv.outer;
            }
        }

        if (sym.kind == VAR) {
            VarSymbol v = (VarSymbol)sym;

            checkInit(tree, env, v, false);

            if (KindSelector.ASG.subset(pkind()))
                checkAssignable(tree.pos(), v, null, env);
        }

        Env<AttrContext> env1 = env;
        if (sym.kind != ERR && sym.kind != TYP &&
            sym.owner != null && sym.owner != env1.enclClass.sym) {
            while (env1.outer != null && !rs.isAccessible(env, env1.enclClass.sym.type, sym))
                env1 = env1.outer;
        }

        if (env.info.isSerializable) {
            chk.checkAccessFromSerializableElement(tree, env.info.isSerializableLambda);
        }

        result = checkId(tree, env1.enclClass.sym.type, sym, env, resultInfo);
    }

    public void visitSelect(JCFieldAccess tree) {
        KindSelector skind = KindSelector.NIL;
        if (tree.name == names._this || tree.name == names._super ||
                tree.name == names._class)
        {
            skind = KindSelector.TYP;
        } else {
            if (pkind().contains(KindSelector.PCK))
                skind = KindSelector.of(skind, KindSelector.PCK);
            if (pkind().contains(KindSelector.TYP))
                skind = KindSelector.of(skind, KindSelector.TYP, KindSelector.PCK);
            if (pkind().contains(KindSelector.VAL_MTH))
                skind = KindSelector.of(skind, KindSelector.VAL, KindSelector.TYP);
        }

        Type site = attribTree(tree.selected, env, new ResultInfo(skind, Type.noType));
        if (!pkind().contains(KindSelector.TYP_PCK))
            site = capture(site); 

        if (skind == KindSelector.TYP) {
            Type elt = site;
            while (elt.hasTag(ARRAY))
                elt = ((ArrayType)elt).elemtype;
            if (elt.hasTag(TYPEVAR)) {
                log.error(tree.pos(), Errors.TypeVarCantBeDeref);
                result = tree.type = types.createErrorType(tree.name, site.tsym, site);
                tree.sym = tree.type.tsym;
                return ;
            }
        }

        Symbol sitesym = TreeInfo.symbol(tree.selected);
        boolean selectSuperPrev = env.info.selectSuper;
        env.info.selectSuper =
            sitesym != null &&
            sitesym.name == names._super;

        env.info.pendingResolutionPhase = null;
        Symbol sym = selectSym(tree, sitesym, site, env, resultInfo);
        if (sym.kind == VAR && sym.name != names._super && env.info.defaultSuperCallSite != null) {
            log.error(tree.selected.pos(), Errors.NotEnclClass(site.tsym));
            sym = syms.errSymbol;
        }
        if (sym.exists() && !isType(sym) && pkind().contains(KindSelector.TYP_PCK)) {
            site = capture(site);
            sym = selectSym(tree, sitesym, site, env, resultInfo);
        }
        boolean varArgs = env.info.lastResolveVarargs();
        tree.sym = sym;

        if (site.hasTag(TYPEVAR) && !isType(sym) && sym.kind != ERR) {
            site = types.skipTypeVars(site, true);
        }

        if (sym.kind == VAR) {
            VarSymbol v = (VarSymbol)sym;

            checkInit(tree, env, v, true);

            if (KindSelector.ASG.subset(pkind()))
                checkAssignable(tree.pos(), v, tree.selected, env);
        }

        if (sitesym != null &&
                sitesym.kind == VAR &&
                ((VarSymbol)sitesym).isResourceVariable() &&
                sym.kind == MTH &&
                sym.name.equals(names.close) &&
                sym.overrides(syms.autoCloseableClose, sitesym.type.tsym, types, true) &&
                env.info.lint.isEnabled(LintCategory.TRY)) {
            log.warning(LintCategory.TRY, tree, Warnings.TryExplicitCloseCall);
        }

        if (isType(sym) && (sitesym == null || !sitesym.kind.matches(KindSelector.TYP_PCK))) {
            tree.type = check(tree.selected, pt(),
                              sitesym == null ?
                                      KindSelector.VAL : sitesym.kind.toSelector(),
                              new ResultInfo(KindSelector.TYP_PCK, pt()));
        }

        if (isType(sitesym)) {
            if (sym.name != names._this && sym.name != names._super) {
                if ((sym.flags() & STATIC) == 0 &&
                    sym.name != names._super &&
                    (sym.kind == VAR || sym.kind == MTH)) {
                    rs.accessBase(rs.new StaticError(sym),
                              tree.pos(), site, sym.name, true);
                }
            }
        } else if (sym.kind != ERR &&
                   (sym.flags() & STATIC) != 0 &&
                   sym.name != names._class) {
            if (!sym.owner.isAnonymous()) {
                chk.warnStatic(tree, Warnings.StaticNotQualifiedByType(sym.kind.kindName(), sym.owner));
            } else {
                chk.warnStatic(tree, Warnings.StaticNotQualifiedByType2(sym.kind.kindName()));
            }
        }

        if (env.info.selectSuper && (sym.flags() & STATIC) == 0) {

            rs.checkNonAbstract(tree.pos(), sym);

            if (site.isRaw()) {
                Type site1 = types.asSuper(env.enclClass.sym.type, site.tsym);
                if (site1 != null) site = site1;
            }
        }

        if (env.info.isSerializable) {
            chk.checkAccessFromSerializableElement(tree, env.info.isSerializableLambda);
        }

        env.info.selectSuper = selectSuperPrev;
        result = checkId(tree, site, sym, env, resultInfo);
    }
        /** Determine symbol referenced by a Select expression,
         *
         *  @param tree   The select tree.
         *  @param site   The type of the selected expression,
         *  @param env    The current environment.
         *  @param resultInfo The current result.
         */
        private Symbol selectSym(JCFieldAccess tree,
                                 Symbol location,
                                 Type site,
                                 Env<AttrContext> env,
                                 ResultInfo resultInfo) {
            DiagnosticPosition pos = tree.pos();
            Name name = tree.name;
            switch (site.getTag()) {
            case PACKAGE:
                return rs.accessBase(
                    rs.findIdentInPackage(pos, env, site.tsym, name, resultInfo.pkind),
                    pos, location, site, name, true);
            case ARRAY:
            case CLASS:
                if (resultInfo.pt.hasTag(METHOD) || resultInfo.pt.hasTag(FORALL)) {
                    return rs.resolveQualifiedMethod(
                        pos, env, location, site, name, resultInfo.pt.getParameterTypes(), resultInfo.pt.getTypeArguments());
                } else if (name == names._this || name == names._super) {
                    return rs.resolveSelf(pos, env, site.tsym, name);
                } else if (name == names._class) {
                    return syms.getClassField(site, types);
                } else {
                    Symbol sym = rs.findIdentInType(pos, env, site, name, resultInfo.pkind);
                        sym = rs.accessBase(sym, pos, location, site, name, true);
                    return sym;
                }
            case WILDCARD:
                throw new AssertionError(tree);
            case TYPEVAR:
                Symbol sym = (site.getUpperBound() != null)
                    ? selectSym(tree, location, capture(site.getUpperBound()), env, resultInfo)
                    : null;
                if (sym == null) {
                    log.error(pos, Errors.TypeVarCantBeDeref);
                    return syms.errSymbol;
                } else {
                    Symbol sym2 = (sym.flags() & Flags.PRIVATE) != 0 ?
                        rs.new AccessError(env, site, sym) :
                                sym;
                    rs.accessBase(sym2, pos, location, site, name, true);
                    return sym;
                }
            case ERROR:
                return types.createErrorType(name, site.tsym, site).tsym;
            default:
                if (name == names._class) {
                    return syms.getClassField(site, types);
                } else {
                    log.error(pos, Errors.CantDeref(site));
                    return syms.errSymbol;
                }
            }
        }

        /** Determine type of identifier or select expression and check that
         *  (1) the referenced symbol is not deprecated
         *  (2) the symbol's type is safe (@see checkSafe)
         *  (3) if symbol is a variable, check that its type and kind are
         *      compatible with the prototype and protokind.
         *  (4) if symbol is an instance field of a raw type,
         *      which is being assigned to, issue an unchecked warning if its
         *      type changes under erasure.
         *  (5) if symbol is an instance method of a raw type, issue an
         *      unchecked warning if its argument types change under erasure.
         *  If checks succeed:
         *    If symbol is a constant, return its constant type
         *    else if symbol is a method, return its result type
         *    otherwise return its type.
         *  Otherwise return errType.
         *
         *  @param tree       The syntax tree representing the identifier
         *  @param site       If this is a select, the type of the selected
         *                    expression, otherwise the type of the current class.
         *  @param sym        The symbol representing the identifier.
         *  @param env        The current environment.
         *  @param resultInfo    The expected result
         */
        Type checkId(JCTree tree,
                     Type site,
                     Symbol sym,
                     Env<AttrContext> env,
                     ResultInfo resultInfo) {
            return (resultInfo.pt.hasTag(FORALL) || resultInfo.pt.hasTag(METHOD)) ?
                    checkMethodIdInternal(tree, site, sym, env, resultInfo) :
                    checkIdInternal(tree, site, sym, resultInfo.pt, env, resultInfo);
        }

        Type checkMethodIdInternal(JCTree tree,
                     Type site,
                     Symbol sym,
                     Env<AttrContext> env,
                     ResultInfo resultInfo) {
            if (resultInfo.pkind.contains(KindSelector.POLY)) {
                return attrRecover.recoverMethodInvocation(tree, site, sym, env, resultInfo);
            } else {
                return checkIdInternal(tree, site, sym, resultInfo.pt, env, resultInfo);
            }
        }

        Type checkIdInternal(JCTree tree,
                     Type site,
                     Symbol sym,
                     Type pt,
                     Env<AttrContext> env,
                     ResultInfo resultInfo) {
            if (pt.isErroneous()) {
                return types.createErrorType(site);
            }
            Type owntype; 
            switch (sym.kind) {
            case TYP:
                owntype = sym.type;
                if (owntype.hasTag(CLASS)) {
                    chk.checkForBadAuxiliaryClassAccess(tree.pos(), env, (ClassSymbol)sym);
                    Type ownOuter = owntype.getEnclosingType();

                    if (owntype.tsym.type.getTypeArguments().nonEmpty()) {
                        owntype = types.erasure(owntype);
                    }

                    else if (ownOuter.hasTag(CLASS) && site != ownOuter) {
                        Type normOuter = site;
                        if (normOuter.hasTag(CLASS)) {
                            normOuter = types.asEnclosingSuper(site, ownOuter.tsym);
                        }
                        if (normOuter == null) 
                            normOuter = types.erasure(ownOuter);
                        if (normOuter != ownOuter)
                            owntype = new ClassType(
                                normOuter, List.nil(), owntype.tsym,
                                owntype.getMetadata());
                    }
                }
                break;
            case VAR:
                VarSymbol v = (VarSymbol)sym;

                if (env.info.enclVar != null
                        && v.type.hasTag(NONE)) {
                    log.error(TreeInfo.positionFor(v, env.enclClass), Errors.CantInferLocalVarType(v.name, Fragments.LocalSelfRef));
                    return tree.type = v.type = types.createErrorType(v.type);
                }

                if (KindSelector.ASG.subset(pkind()) &&
                    v.owner.kind == TYP &&
                    (v.flags() & STATIC) == 0 &&
                    (site.hasTag(CLASS) || site.hasTag(TYPEVAR))) {
                    Type s = types.asOuterSuper(site, v.owner);
                    if (s != null &&
                        s.isRaw() &&
                        !types.isSameType(v.type, v.erasure(types))) {
                        chk.warnUnchecked(tree.pos(), Warnings.UncheckedAssignToVar(v, s));
                    }
                }
                owntype = (sym.owner.kind == TYP &&
                           sym.name != names._this && sym.name != names._super)
                    ? types.memberType(site, sym)
                    : sym.type;

                if (v.getConstValue() != null && isStaticReference(tree))
                    owntype = owntype.constType(v.getConstValue());

                if (resultInfo.pkind == KindSelector.VAL) {
                    owntype = capture(owntype); 
                }
                break;
            case MTH: {
                owntype = checkMethod(site, sym,
                        new ResultInfo(resultInfo.pkind, resultInfo.pt.getReturnType(), resultInfo.checkContext, resultInfo.checkMode),
                        env, TreeInfo.args(env.tree), resultInfo.pt.getParameterTypes(),
                        resultInfo.pt.getTypeArguments());
                chk.checkRestricted(tree.pos(), sym);
                break;
            }
            case PCK: case ERR:
                owntype = sym.type;
                break;
            default:
                throw new AssertionError("unexpected kind: " + sym.kind +
                                         " in tree " + tree);
            }


            if (sym.name != names.init || tree.hasTag(REFERENCE)) {
                chk.checkDeprecated(tree.pos(), env.info.scope.owner, sym);
                chk.checkSunAPI(tree.pos(), sym);
                chk.checkProfile(tree.pos(), sym);
                chk.checkPreview(tree.pos(), env.info.scope.owner, sym);
            }

            return check(tree, owntype, sym.kind.toSelector(), resultInfo);
        }

        /** Check that variable is initialized and evaluate the variable's
         *  initializer, if not yet done. Also check that variable is not
         *  referenced before it is defined.
         *  @param tree    The tree making up the variable reference.
         *  @param env     The current environment.
         *  @param v       The variable's symbol.
         */
        private void checkInit(JCTree tree,
                               Env<AttrContext> env,
                               VarSymbol v,
                               boolean onlyWarning) {
            Env<AttrContext> initEnv = enclosingInitEnv(env);
            if (initEnv != null &&
                (initEnv.info.enclVar == v || v.pos > tree.pos) &&
                v.owner.kind == TYP &&
                v.owner == env.info.scope.owner.enclClass() &&
                ((v.flags() & STATIC) != 0) == Resolve.isStatic(env) &&
                (!env.tree.hasTag(ASSIGN) ||
                 TreeInfo.skipParens(((JCAssign) env.tree).lhs) != tree)) {
                if (!onlyWarning || isStaticEnumField(v)) {
                    Error errkey = (initEnv.info.enclVar == v) ?
                                Errors.IllegalSelfRef : Errors.IllegalForwardRef;
                    log.error(tree.pos(), errkey);
                } else if (useBeforeDeclarationWarning) {
                    Warning warnkey = (initEnv.info.enclVar == v) ?
                                Warnings.SelfRef(v) : Warnings.ForwardRef(v);
                    log.warning(tree.pos(), warnkey);
                }
            }

            v.getConstValue(); 

            checkEnumInitializer(tree, env, v);
        }

        /**
         * Returns the enclosing init environment associated with this env (if any). An init env
         * can be either a field declaration env or a static/instance initializer env.
         */
        Env<AttrContext> enclosingInitEnv(Env<AttrContext> env) {
            while (true) {
                switch (env.tree.getTag()) {
                    case VARDEF:
                        JCVariableDecl vdecl = (JCVariableDecl)env.tree;
                        if (vdecl.sym.owner.kind == TYP) {
                            return env;
                        }
                        break;
                    case BLOCK:
                        if (env.next.tree.hasTag(CLASSDEF)) {
                            return env;
                        }
                        break;
                    case METHODDEF:
                    case CLASSDEF:
                    case TOPLEVEL:
                        return null;
                }
                Assert.checkNonNull(env.next);
                env = env.next;
            }
        }

        /**
         * Check for illegal references to static members of enum.  In
         * an enum type, constructors and initializers may not
         * reference its static members unless they are constant.
         *
         * @param tree    The tree making up the variable reference.
         * @param env     The current environment.
         * @param v       The variable's symbol.
         * @jls 8.9 Enum Types
         */
        private void checkEnumInitializer(JCTree tree, Env<AttrContext> env, VarSymbol v) {
            if (isStaticEnumField(v)) {
                ClassSymbol enclClass = env.info.scope.owner.enclClass();

                if (enclClass == null || enclClass.owner == null)
                    return;

                if (v.owner != enclClass && !types.isSubtype(enclClass.type, v.owner.type))
                    return;

                if (!Resolve.isInitializer(env))
                    return;

                log.error(tree.pos(), Errors.IllegalEnumStaticRef);
            }
        }

        /** Is the given symbol a static, non-constant field of an Enum?
         *  Note: enum literals should not be regarded as such
         */
        private boolean isStaticEnumField(VarSymbol v) {
            return Flags.isEnum(v.owner) &&
                   Flags.isStatic(v) &&
                   !Flags.isConstant(v) &&
                   v.name != names._class;
        }

    /**
     * Check that method arguments conform to its instantiation.
     **/
    public Type checkMethod(Type site,
                            final Symbol sym,
                            ResultInfo resultInfo,
                            Env<AttrContext> env,
                            final List<JCExpression> argtrees,
                            List<Type> argtypes,
                            List<Type> typeargtypes) {
        if ((sym.flags() & STATIC) == 0 &&
            (site.hasTag(CLASS) || site.hasTag(TYPEVAR))) {
            Type s = types.asOuterSuper(site, sym.owner);
            if (s != null && s.isRaw() &&
                !types.isSameTypes(sym.type.getParameterTypes(),
                                   sym.erasure(types).getParameterTypes())) {
                chk.warnUnchecked(env.tree.pos(), Warnings.UncheckedCallMbrOfRawType(sym, s));
            }
        }

        if (env.info.defaultSuperCallSite != null) {
            for (Type sup : types.interfaces(env.enclClass.type).prepend(types.supertype((env.enclClass.type)))) {
                if (!sup.tsym.isSubClass(sym.enclClass(), types) ||
                        types.isSameType(sup, env.info.defaultSuperCallSite)) continue;
                List<MethodSymbol> icand_sup =
                        types.interfaceCandidates(sup, (MethodSymbol)sym);
                if (icand_sup.nonEmpty() &&
                        icand_sup.head != sym &&
                        icand_sup.head.overrides(sym, icand_sup.head.enclClass(), types, true)) {
                    log.error(env.tree.pos(),
                              Errors.IllegalDefaultSuperCall(env.info.defaultSuperCallSite, Fragments.OverriddenDefault(sym, sup)));
                    break;
                }
            }
            env.info.defaultSuperCallSite = null;
        }

        if (sym.isStatic() && site.isInterface() && env.tree.hasTag(APPLY)) {
            JCMethodInvocation app = (JCMethodInvocation)env.tree;
            if (app.meth.hasTag(SELECT) &&
                    !TreeInfo.isStaticSelector(((JCFieldAccess)app.meth).selected, names)) {
                log.error(env.tree.pos(), Errors.IllegalStaticIntfMethCall(site));
            }
        }

        Warner noteWarner = new Warner();
        try {
            Type owntype = rs.checkMethod(
                    env,
                    site,
                    sym,
                    resultInfo,
                    argtypes,
                    typeargtypes,
                    noteWarner);

            DeferredAttr.DeferredTypeMap<Void> checkDeferredMap =
                deferredAttr.new DeferredTypeMap<>(DeferredAttr.AttrMode.CHECK, sym, env.info.pendingResolutionPhase);

            argtypes = argtypes.map(checkDeferredMap);

            if (noteWarner.hasNonSilentLint(LintCategory.UNCHECKED)) {
                chk.warnUnchecked(env.tree.pos(), Warnings.UncheckedMethInvocationApplied(kindName(sym),
                        sym.name,
                        rs.methodArguments(sym.type.getParameterTypes()),
                        rs.methodArguments(argtypes.map(checkDeferredMap)),
                        kindName(sym.location()),
                        sym.location()));
                if (resultInfo.pt != Infer.anyPoly ||
                        !owntype.hasTag(METHOD) ||
                        !owntype.isPartial()) {
                    owntype = new MethodType(owntype.getParameterTypes(),
                            types.erasure(owntype.getReturnType()),
                            types.erasure(owntype.getThrownTypes()),
                            syms.methodClass);
                }
            }

            PolyKind pkind = (sym.type.hasTag(FORALL) &&
                 sym.type.getReturnType().containsAny(((ForAll)sym.type).tvars)) ?
                 PolyKind.POLY : PolyKind.STANDALONE;
            TreeInfo.setPolyKind(env.tree, pkind);

            return (resultInfo.pt == Infer.anyPoly) ?
                    owntype :
                    chk.checkMethod(owntype, sym, env, argtrees, argtypes, env.info.lastResolveVarargs(),
                            resultInfo.checkContext.inferenceContext());
        } catch (Infer.InferenceException ex) {
            resultInfo.checkContext.report(env.tree.pos(), ex.getDiagnostic());
            return types.createErrorType(site);
        } catch (Resolve.InapplicableMethodException ex) {
            final JCDiagnostic diag = ex.getDiagnostic();
            Resolve.InapplicableSymbolError errSym = rs.new InapplicableSymbolError(null) {
                @Override
                protected Pair<Symbol, JCDiagnostic> errCandidate() {
                    return new Pair<>(sym, diag);
                }
            };
            List<Type> argtypes2 = argtypes.map(
                    rs.new ResolveDeferredRecoveryMap(AttrMode.CHECK, sym, env.info.pendingResolutionPhase));
            JCDiagnostic errDiag = errSym.getDiagnostic(JCDiagnostic.DiagnosticType.ERROR,
                    env.tree, sym, site, sym.name, argtypes2, typeargtypes);
            log.report(errDiag);
            return types.createErrorType(site);
        }
    }

    public void visitLiteral(JCLiteral tree) {
        result = check(tree, litType(tree.typetag).constType(tree.value),
                KindSelector.VAL, resultInfo);
    }
    /** Return the type of a literal with given type tag.
     */
    Type litType(TypeTag tag) {
        return (tag == CLASS) ? syms.stringType : syms.typeOfTag[tag.ordinal()];
    }

    public void visitStringTemplate(JCStringTemplate tree) {
        JCExpression processor = tree.processor;
        Type processorType = attribTree(processor, env, new ResultInfo(KindSelector.VAL, Type.noType));
        chk.checkProcessorType(processor, processorType, env);
        Type processMethodType = getProcessMethodType(tree, processorType);
        tree.processMethodType = processMethodType;
        Type resultType = processMethodType.getReturnType();

        Env<AttrContext> localEnv = env.dup(tree, env.info.dup());

        for (JCExpression arg : tree.expressions) {
            chk.checkNonVoid(arg.pos(), attribExpr(arg, localEnv));
        }

        tree.type = resultType;
        result = resultType;
        check(tree, resultType, KindSelector.VAL, resultInfo);
    }

    private Type getProcessMethodType(JCStringTemplate tree, Type processorType) {
        MethodSymbol processSymbol = rs.resolveInternalMethod(tree.pos(),
                env, types.skipTypeVars(processorType, false),
                names.process, List.of(syms.stringTemplateType), List.nil());
        return types.memberType(processorType, processSymbol);
    }

    public void visitTypeIdent(JCPrimitiveTypeTree tree) {
        result = check(tree, syms.typeOfTag[tree.typetag.ordinal()], KindSelector.TYP, resultInfo);
    }

    public void visitTypeArray(JCArrayTypeTree tree) {
        Type etype = attribType(tree.elemtype, env);
        Type type = new ArrayType(etype, syms.arrayClass);
        result = check(tree, type, KindSelector.TYP, resultInfo);
    }

    /** Visitor method for parameterized types.
     *  Bound checking is left until later, since types are attributed
     *  before supertype structure is completely known
     */
    public void visitTypeApply(JCTypeApply tree) {
        Type owntype = types.createErrorType(tree.type);

        Type clazztype = chk.checkClassType(tree.clazz.pos(), attribType(tree.clazz, env));

        List<Type> actuals = attribTypes(tree.arguments, env);

        if (clazztype.hasTag(CLASS)) {
            List<Type> formals = clazztype.tsym.type.getTypeArguments();
            if (actuals.isEmpty()) 
                actuals = formals;

            if (actuals.length() == formals.length()) {
                List<Type> a = actuals;
                List<Type> f = formals;
                while (a.nonEmpty()) {
                    a.head = a.head.withTypeVar(f.head);
                    a = a.tail;
                    f = f.tail;
                }
                Type clazzOuter = clazztype.getEnclosingType();
                if (clazzOuter.hasTag(CLASS)) {
                    Type site;
                    JCExpression clazz = TreeInfo.typeIn(tree.clazz);
                    if (clazz.hasTag(IDENT)) {
                        site = env.enclClass.sym.type;
                    } else if (clazz.hasTag(SELECT)) {
                        site = ((JCFieldAccess) clazz).selected.type;
                    } else throw new AssertionError(""+tree);
                    if (clazzOuter.hasTag(CLASS) && site != clazzOuter) {
                        if (site.hasTag(CLASS))
                            site = types.asOuterSuper(site, clazzOuter.tsym);
                        if (site == null)
                            site = types.erasure(clazzOuter);
                        clazzOuter = site;
                    }
                }
                owntype = new ClassType(clazzOuter, actuals, clazztype.tsym,
                                        clazztype.getMetadata());
            } else {
                if (formals.length() != 0) {
                    log.error(tree.pos(),
                              Errors.WrongNumberTypeArgs(Integer.toString(formals.length())));
                } else {
                    log.error(tree.pos(), Errors.TypeDoesntTakeParams(clazztype.tsym));
                }
                owntype = types.createErrorType(tree.type);
            }
        }
        result = check(tree, owntype, KindSelector.TYP, resultInfo);
    }

    public void visitTypeUnion(JCTypeUnion tree) {
        ListBuffer<Type> multicatchTypes = new ListBuffer<>();
        ListBuffer<Type> all_multicatchTypes = null; 
        for (JCExpression typeTree : tree.alternatives) {
            Type ctype = attribType(typeTree, env);
            ctype = chk.checkType(typeTree.pos(),
                          chk.checkClassType(typeTree.pos(), ctype),
                          syms.throwableType);
            if (!ctype.isErroneous()) {
                if (chk.intersects(ctype,  multicatchTypes.toList())) {
                    for (Type t : multicatchTypes) {
                        boolean sub = types.isSubtype(ctype, t);
                        boolean sup = types.isSubtype(t, ctype);
                        if (sub || sup) {
                            Type a = sub ? ctype : t;
                            Type b = sub ? t : ctype;
                            log.error(typeTree.pos(), Errors.MulticatchTypesMustBeDisjoint(a, b));
                        }
                    }
                }
                multicatchTypes.append(ctype);
                if (all_multicatchTypes != null)
                    all_multicatchTypes.append(ctype);
            } else {
                if (all_multicatchTypes == null) {
                    all_multicatchTypes = new ListBuffer<>();
                    all_multicatchTypes.appendList(multicatchTypes);
                }
                all_multicatchTypes.append(ctype);
            }
        }
        Type t = check(tree, types.lub(multicatchTypes.toList()),
                KindSelector.TYP, resultInfo.dup(CheckMode.NO_TREE_UPDATE));
        if (t.hasTag(CLASS)) {
            List<Type> alternatives =
                ((all_multicatchTypes == null) ? multicatchTypes : all_multicatchTypes).toList();
            t = new UnionClassType((ClassType) t, alternatives);
        }
        tree.type = result = t;
    }

    public void visitTypeIntersection(JCTypeIntersection tree) {
        attribTypes(tree.bounds, env);
        tree.type = result = checkIntersection(tree, tree.bounds);
    }

    public void visitTypeParameter(JCTypeParameter tree) {
        TypeVar typeVar = (TypeVar) tree.type;

        if (tree.annotations != null && tree.annotations.nonEmpty()) {
            annotate.annotateTypeParameterSecondStage(tree, tree.annotations);
        }

        if (!typeVar.getUpperBound().isErroneous()) {
            typeVar.setUpperBound(checkIntersection(tree, tree.bounds));
        }
    }

    Type checkIntersection(JCTree tree, List<JCExpression> bounds) {
        Set<Symbol> boundSet = new HashSet<>();
        if (bounds.nonEmpty()) {
            bounds.head.type = checkBase(bounds.head.type, bounds.head, env, false, false, false);
            boundSet.add(types.erasure(bounds.head.type).tsym);
            if (bounds.head.type.isErroneous()) {
                return bounds.head.type;
            }
            else if (bounds.head.type.hasTag(TYPEVAR)) {
                if (bounds.tail.nonEmpty()) {
                    log.error(bounds.tail.head.pos(),
                              Errors.TypeVarMayNotBeFollowedByOtherBounds);
                    return bounds.head.type;
                }
            } else {
                for (JCExpression bound : bounds.tail) {
                    bound.type = checkBase(bound.type, bound, env, false, true, false);
                    if (bound.type.isErroneous()) {
                        bounds = List.of(bound);
                    }
                    else if (bound.type.hasTag(CLASS)) {
                        chk.checkNotRepeated(bound.pos(), types.erasure(bound.type), boundSet);
                    }
                }
            }
        }

        if (bounds.length() == 0) {
            return syms.objectType;
        } else if (bounds.length() == 1) {
            return bounds.head.type;
        } else {
            Type owntype = types.makeIntersectionType(TreeInfo.types(bounds));
            JCExpression extending;
            List<JCExpression> implementing;
            if (!bounds.head.type.isInterface()) {
                extending = bounds.head;
                implementing = bounds.tail;
            } else {
                extending = null;
                implementing = bounds;
            }
            JCClassDecl cd = make.at(tree).ClassDef(
                make.Modifiers(PUBLIC | ABSTRACT),
                names.empty, List.nil(),
                extending, implementing, List.nil());

            ClassSymbol c = (ClassSymbol)owntype.tsym;
            Assert.check((c.flags() & COMPOUND) != 0);
            cd.sym = c;
            c.sourcefile = env.toplevel.sourcefile;

            c.flags_field |= UNATTRIBUTED;
            Env<AttrContext> cenv = enter.classEnv(cd, env);
            typeEnvs.put(c, cenv);
            attribClass(c);
            return owntype;
        }
    }

    public void visitWildcard(JCWildcard tree) {
        Type type = (tree.kind.kind == BoundKind.UNBOUND)
            ? syms.objectType
            : attribType(tree.inner, env);
        result = check(tree, new WildcardType(chk.checkRefType(tree.pos(), type),
                                              tree.kind.kind,
                                              syms.boundClass),
                KindSelector.TYP, resultInfo);
    }

    public void visitAnnotation(JCAnnotation tree) {
        Assert.error("should be handled in annotate");
    }

    @Override
    public void visitModifiers(JCModifiers tree) {
        Assert.check(resultInfo.pkind == KindSelector.ERR);

        attribAnnotationTypes(tree.annotations, env);
    }

    public void visitAnnotatedType(JCAnnotatedType tree) {
        attribAnnotationTypes(tree.annotations, env);
        Type underlyingType = attribType(tree.underlyingType, env);
        Type annotatedType = underlyingType.preannotatedType();

        if (!env.info.isNewClass)
            annotate.annotateTypeSecondStage(tree, tree.annotations, annotatedType);
        result = tree.type = annotatedType;
    }

    public void visitErroneous(JCErroneous tree) {
        if (tree.errs != null) {
            Env<AttrContext> errEnv = env.dup(env.tree, env.info.dup());
            errEnv.info.returnResult = unknownExprInfo;
            for (JCTree err : tree.errs)
                attribTree(err, errEnv, new ResultInfo(KindSelector.ERR, pt()));
        }
        result = tree.type = syms.errType;
    }

    /** Default visitor method for all other trees.
     */
    public void visitTree(JCTree tree) {
        throw new AssertionError();
    }

    /**
     * Attribute an env for either a top level tree or class or module declaration.
     */
    public void attrib(Env<AttrContext> env) {
        switch (env.tree.getTag()) {
            case MODULEDEF:
                attribModule(env.tree.pos(), ((JCModuleDecl)env.tree).sym);
                break;
            case PACKAGEDEF:
                attribPackage(env.tree.pos(), ((JCPackageDecl) env.tree).packge);
                break;
            default:
                attribClass(env.tree.pos(), env.enclClass.sym);
        }
    }

    public void attribPackage(DiagnosticPosition pos, PackageSymbol p) {
        try {
            annotate.flush();
            attribPackage(p);
        } catch (CompletionFailure ex) {
            chk.completionError(pos, ex);
        }
    }

    void attribPackage(PackageSymbol p) {
        attribWithLint(p,
                       env -> chk.checkDeprecatedAnnotation(((JCPackageDecl) env.tree).pid.pos(), p));
    }

    public void attribModule(DiagnosticPosition pos, ModuleSymbol m) {
        try {
            annotate.flush();
            attribModule(m);
        } catch (CompletionFailure ex) {
            chk.completionError(pos, ex);
        }
    }

    void attribModule(ModuleSymbol m) {
        attribWithLint(m, env -> attribStat(env.tree, env));
    }

    private void attribWithLint(TypeSymbol sym, Consumer<Env<AttrContext>> attrib) {
        Env<AttrContext> env = typeEnvs.get(sym);

        Env<AttrContext> lintEnv = env;
        while (lintEnv.info.lint == null)
            lintEnv = lintEnv.next;

        Lint lint = lintEnv.info.lint.augment(sym);

        Lint prevLint = chk.setLint(lint);
        JavaFileObject prev = log.useSource(env.toplevel.sourcefile);

        try {
            deferredLintHandler.flush(env.tree.pos());
            attrib.accept(env);
        } finally {
            log.useSource(prev);
            chk.setLint(prevLint);
        }
    }

    /** Main method: attribute class definition associated with given class symbol.
     *  reporting completion failures at the given position.
     *  @param pos The source position at which completion errors are to be
     *             reported.
     *  @param c   The class symbol whose definition will be attributed.
     */
    public void attribClass(DiagnosticPosition pos, ClassSymbol c) {
        try {
            annotate.flush();
            attribClass(c);
        } catch (CompletionFailure ex) {
            chk.completionError(pos, ex);
        }
    }

    /** Attribute class definition associated with given class symbol.
     *  @param c   The class symbol whose definition will be attributed.
     */
    void attribClass(ClassSymbol c) throws CompletionFailure {
        if (c.type.hasTag(ERROR)) return;

        chk.checkNonCyclic(null, c.type);

        Type st = types.supertype(c.type);
        if ((c.flags_field & Flags.COMPOUND) == 0 &&
            (c.flags_field & Flags.SUPER_OWNER_ATTRIBUTED) == 0) {
            if (st.hasTag(CLASS))
                attribClass((ClassSymbol)st.tsym);

            if (c.owner.kind == TYP && c.owner.type.hasTag(CLASS))
                attribClass((ClassSymbol)c.owner);

            c.flags_field |= Flags.SUPER_OWNER_ATTRIBUTED;
        }

        if ((c.flags_field & UNATTRIBUTED) != 0) {
            c.flags_field &= ~UNATTRIBUTED;

            Env<AttrContext> env = typeEnvs.get(c);

            Env<AttrContext> lintEnv = env;
            while (lintEnv.info.lint == null)
                lintEnv = lintEnv.next;

            env.info.lint = lintEnv.info.lint.augment(c);

            Lint prevLint = chk.setLint(env.info.lint);
            JavaFileObject prev = log.useSource(c.sourcefile);
            ResultInfo prevReturnRes = env.info.returnResult;

            try {
                if (c.isSealed() &&
                        !c.isEnum() &&
                        !c.isPermittedExplicit &&
                        c.getPermittedSubclasses().isEmpty()) {
                    log.error(TreeInfo.diagnosticPositionFor(c, env.tree), Errors.SealedClassMustHaveSubclasses);
                }

                if (c.isSealed()) {
                    Set<Symbol> permittedTypes = new HashSet<>();
                    boolean sealedInUnnamed = c.packge().modle == syms.unnamedModule || c.packge().modle == syms.noModule;
                    for (Type subType : c.getPermittedSubclasses()) {
                        boolean isTypeVar = false;
                        if (subType.getTag() == TYPEVAR) {
                            isTypeVar = true; 
                            log.error(TreeInfo.diagnosticPositionFor(subType.tsym, env.tree),
                                    Errors.InvalidPermitsClause(Fragments.IsATypeVariable(subType)));
                        }
                        if (subType.tsym.isAnonymous() && !c.isEnum()) {
                            log.error(TreeInfo.diagnosticPositionFor(subType.tsym, env.tree),  Errors.LocalClassesCantExtendSealed(Fragments.Anonymous));
                        }
                        if (permittedTypes.contains(subType.tsym)) {
                            DiagnosticPosition pos =
                                    env.enclClass.permitting.stream()
                                            .filter(permittedExpr -> TreeInfo.diagnosticPositionFor(subType.tsym, permittedExpr, true) != null)
                                            .limit(2).collect(List.collector()).get(1);
                            log.error(pos, Errors.InvalidPermitsClause(Fragments.IsDuplicated(subType)));
                        } else {
                            permittedTypes.add(subType.tsym);
                        }
                        if (sealedInUnnamed) {
                            if (subType.tsym.packge() != c.packge()) {
                                log.error(TreeInfo.diagnosticPositionFor(subType.tsym, env.tree),
                                        Errors.ClassInUnnamedModuleCantExtendSealedInDiffPackage(c)
                                );
                            }
                        } else if (subType.tsym.packge().modle != c.packge().modle) {
                            log.error(TreeInfo.diagnosticPositionFor(subType.tsym, env.tree),
                                    Errors.ClassInModuleCantExtendSealedInDiffModule(c, c.packge().modle)
                            );
                        }
                        if (subType.tsym == c.type.tsym || types.isSuperType(subType, c.type)) {
                            log.error(TreeInfo.diagnosticPositionFor(subType.tsym, ((JCClassDecl)env.tree).permitting),
                                    Errors.InvalidPermitsClause(
                                            subType.tsym == c.type.tsym ?
                                                    Fragments.MustNotBeSameClass :
                                                    Fragments.MustNotBeSupertype(subType)
                                    )
                            );
                        } else if (!isTypeVar) {
                            boolean thisIsASuper = types.directSupertypes(subType)
                                                        .stream()
                                                        .anyMatch(d -> d.tsym == c);
                            if (!thisIsASuper) {
                                log.error(TreeInfo.diagnosticPositionFor(subType.tsym, env.tree),
                                        Errors.InvalidPermitsClause(Fragments.DoesntExtendSealed(subType)));
                            }
                        }
                    }
                }

                List<ClassSymbol> sealedSupers = types.directSupertypes(c.type)
                                                      .stream()
                                                      .filter(s -> s.tsym.isSealed())
                                                      .map(s -> (ClassSymbol) s.tsym)
                                                      .collect(List.collector());

                if (sealedSupers.isEmpty()) {
                    if ((c.flags_field & Flags.NON_SEALED) != 0) {
                        boolean hasErrorSuper = false;

                        hasErrorSuper |= types.directSupertypes(c.type)
                                              .stream()
                                              .anyMatch(s -> s.tsym.kind == Kind.ERR);

                        ClassType ct = (ClassType) c.type;

                        hasErrorSuper |= !ct.isCompound() && ct.interfaces_field != ct.all_interfaces_field;

                        if (!hasErrorSuper) {
                            log.error(TreeInfo.diagnosticPositionFor(c, env.tree), Errors.NonSealedWithNoSealedSupertype(c));
                        }
                    }
                } else {
                    if (c.isDirectlyOrIndirectlyLocal() && !c.isEnum()) {
                        log.error(TreeInfo.diagnosticPositionFor(c, env.tree), Errors.LocalClassesCantExtendSealed(c.isAnonymous() ? Fragments.Anonymous : Fragments.Local));
                    }

                    if (!c.type.isCompound()) {
                        for (ClassSymbol supertypeSym : sealedSupers) {
                            if (!supertypeSym.isPermittedSubclass(c.type.tsym)) {
                                log.error(TreeInfo.diagnosticPositionFor(c.type.tsym, env.tree), Errors.CantInheritFromSealed(supertypeSym));
                            }
                        }
                        if (!c.isNonSealed() && !c.isFinal() && !c.isSealed()) {
                            log.error(TreeInfo.diagnosticPositionFor(c, env.tree),
                                    c.isInterface() ?
                                            Errors.NonSealedOrSealedExpected :
                                            Errors.NonSealedSealedOrFinalExpected);
                        }
                    }
                }

                deferredLintHandler.flush(env.tree);
                env.info.returnResult = null;
                if (st.tsym == syms.enumSym &&
                    ((c.flags_field & (Flags.ENUM|Flags.COMPOUND)) == 0))
                    log.error(env.tree.pos(), Errors.EnumNoSubclassing);

                if (st.tsym != null &&
                    ((st.tsym.flags_field & Flags.ENUM) != 0) &&
                    ((c.flags_field & (Flags.ENUM | Flags.COMPOUND)) == 0)) {
                    log.error(env.tree.pos(), Errors.EnumTypesNotExtensible);
                }

                if (rs.isSerializable(c.type)) {
                    env.info.isSerializable = true;
                }

                attribClassBody(env, c);

                chk.checkDeprecatedAnnotation(env.tree.pos(), c);
                chk.checkClassOverrideEqualsAndHashIfNeeded(env.tree.pos(), c);
                chk.checkFunctionalInterface((JCClassDecl) env.tree, c);
                chk.checkLeaksNotAccessible(env, (JCClassDecl) env.tree);

                if (c.isImplicit()) {
                    chk.checkHasMain(env.tree.pos(), c);
                }
            } finally {
                env.info.returnResult = prevReturnRes;
                log.useSource(prev);
                chk.setLint(prevLint);
            }

        }
    }

    public void visitImport(JCImport tree) {
    }

    public void visitModuleDef(JCModuleDecl tree) {
        tree.sym.completeUsesProvides();
        ModuleSymbol msym = tree.sym;
        Lint lint = env.outer.info.lint = env.outer.info.lint.augment(msym);
        Lint prevLint = chk.setLint(lint);
        chk.checkModuleName(tree);
        chk.checkDeprecatedAnnotation(tree, msym);

        try {
            deferredLintHandler.flush(tree.pos());
        } finally {
            chk.setLint(prevLint);
        }
    }

    /** Finish the attribution of a class. */
    private void attribClassBody(Env<AttrContext> env, ClassSymbol c) {
        JCClassDecl tree = (JCClassDecl)env.tree;
        Assert.check(c == tree.sym);

        attribStats(tree.typarams, env);
        if (!c.isAnonymous()) {
            chk.validate(tree.typarams, env);
            chk.validate(tree.extending, env);
            chk.validate(tree.implementing, env);
        }

        c.markAbstractIfNeeded(types);

        if ((c.flags() & (ABSTRACT | INTERFACE)) == 0) {
            chk.checkAllDefined(tree.pos(), c);
        }

        if ((c.flags() & ANNOTATION) != 0) {
            if (tree.implementing.nonEmpty())
                log.error(tree.implementing.head.pos(),
                          Errors.CantExtendIntfAnnotation);
            if (tree.typarams.nonEmpty()) {
                log.error(tree.typarams.head.pos(),
                          Errors.IntfAnnotationCantHaveTypeParams(c));
            }

            Attribute.Compound repeatable = c.getAnnotationTypeMetadata().getRepeatable();
            if (repeatable != null) {
                DiagnosticPosition cbPos = getDiagnosticPosition(tree, repeatable.type);
                Assert.checkNonNull(cbPos);

                chk.validateRepeatable(c, repeatable, cbPos);
            }
        } else {
            chk.checkCompatibleSupertypes(tree.pos(), c.type);
            chk.checkDefaultMethodClashes(tree.pos(), c.type);
            chk.checkPotentiallyAmbiguousOverloads(tree, c.type);
        }

        chk.checkClassBounds(tree.pos(), c.type);

        tree.type = c.type;

        for (List<JCTypeParameter> l = tree.typarams;
             l.nonEmpty(); l = l.tail) {
             Assert.checkNonNull(env.info.scope.findFirst(l.head.name));
        }

        if (!c.type.allparams().isEmpty() && types.isSubtype(c.type, syms.throwableType))
            log.error(tree.extending.pos(), Errors.GenericThrowable);

        chk.checkImplementations(tree);

        checkAutoCloseable(tree.pos(), env, c.type);

        for (List<JCTree> l = tree.defs; l.nonEmpty(); l = l.tail) {
            attribStat(l.head, env);
            if (!allowRecords &&
                    c.owner.kind != PCK &&
                    ((c.flags() & STATIC) == 0 || c.name == names.empty) &&
                    (TreeInfo.flags(l.head) & (STATIC | INTERFACE)) != 0) {
                VarSymbol sym = null;
                if (l.head.hasTag(VARDEF)) sym = ((JCVariableDecl) l.head).sym;
                if (sym == null ||
                        sym.kind != VAR ||
                        sym.getConstValue() == null)
                    log.error(l.head.pos(), Errors.IclsCantHaveStaticDecl(c));
            }
        }

        chk.checkSuperInitCalls(tree);

        chk.checkCyclicConstructors(tree);

        chk.checkNonCyclicElements(tree);

        if (env.info.lint.isEnabled(LintCategory.SERIAL)
                && rs.isSerializable(c.type)
                && !c.isAnonymous()) {
            chk.checkSerialStructure(tree, c);
        }
        typeAnnotations.organizeTypeAnnotationsBodies(tree);

        validateTypeAnnotations(tree, false);
    }
        /** get a diagnostic position for an attribute of Type t, or null if attribute missing */
        private DiagnosticPosition getDiagnosticPosition(JCClassDecl tree, Type t) {
            for(List<JCAnnotation> al = tree.mods.annotations; !al.isEmpty(); al = al.tail) {
                if (types.isSameType(al.head.annotationType.type, t))
                    return al.head.pos();
            }

            return null;
        }

    private Type capture(Type type) {
        return types.capture(type);
    }

    private void setSyntheticVariableType(JCVariableDecl tree, Type type) {
        if (type.isErroneous()) {
            tree.vartype = make.at(Position.NOPOS).Erroneous();
        } else {
            tree.vartype = make.at(Position.NOPOS).Type(type);
        }
    }

    public void validateTypeAnnotations(JCTree tree, boolean sigOnly) {
        tree.accept(new TypeAnnotationsValidator(sigOnly));
    }
    private final class TypeAnnotationsValidator extends TreeScanner {

        private final boolean sigOnly;
        public TypeAnnotationsValidator(boolean sigOnly) {
            this.sigOnly = sigOnly;
        }

        public void visitAnnotation(JCAnnotation tree) {
            chk.validateTypeAnnotation(tree, null, false);
            super.visitAnnotation(tree);
        }
        public void visitAnnotatedType(JCAnnotatedType tree) {
            if (!tree.underlyingType.type.isErroneous()) {
                super.visitAnnotatedType(tree);
            }
        }
        public void visitTypeParameter(JCTypeParameter tree) {
            chk.validateTypeAnnotations(tree.annotations, tree.type.tsym, true);
            scan(tree.bounds);
        }
        public void visitMethodDef(JCMethodDecl tree) {
            if (tree.recvparam != null &&
                    !tree.recvparam.vartype.type.isErroneous()) {
                checkForDeclarationAnnotations(tree.recvparam.mods.annotations, tree.recvparam.sym);
            }
            if (tree.restype != null && tree.restype.type != null) {
                validateAnnotatedType(tree.restype, tree.restype.type);
            }
            if (sigOnly) {
                scan(tree.mods);
                scan(tree.restype);
                scan(tree.typarams);
                scan(tree.recvparam);
                scan(tree.params);
                scan(tree.thrown);
            } else {
                scan(tree.defaultValue);
                scan(tree.body);
            }
        }
        public void visitVarDef(final JCVariableDecl tree) {
            if (tree.sym != null && tree.sym.type != null && !tree.isImplicitlyTyped())
                validateAnnotatedType(tree.vartype, tree.sym.type);
            scan(tree.mods);
            scan(tree.vartype);
            if (!sigOnly) {
                scan(tree.init);
            }
        }
        public void visitTypeCast(JCTypeCast tree) {
            if (tree.clazz != null && tree.clazz.type != null)
                validateAnnotatedType(tree.clazz, tree.clazz.type);
            super.visitTypeCast(tree);
        }
        public void visitTypeTest(JCInstanceOf tree) {
            if (tree.pattern != null && !(tree.pattern instanceof JCPattern) && tree.pattern.type != null)
                validateAnnotatedType(tree.pattern, tree.pattern.type);
            super.visitTypeTest(tree);
        }
        public void visitNewClass(JCNewClass tree) {
            if (tree.clazz != null && tree.clazz.type != null) {
                if (tree.clazz.hasTag(ANNOTATED_TYPE)) {
                    checkForDeclarationAnnotations(((JCAnnotatedType) tree.clazz).annotations,
                            tree.clazz.type.tsym);
                }
                if (tree.def != null) {
                    checkForDeclarationAnnotations(tree.def.mods.annotations, tree.clazz.type.tsym);
                }

                validateAnnotatedType(tree.clazz, tree.clazz.type);
            }
            super.visitNewClass(tree);
        }
        public void visitNewArray(JCNewArray tree) {
            if (tree.elemtype != null && tree.elemtype.type != null) {
                if (tree.elemtype.hasTag(ANNOTATED_TYPE)) {
                    checkForDeclarationAnnotations(((JCAnnotatedType) tree.elemtype).annotations,
                            tree.elemtype.type.tsym);
                }
                validateAnnotatedType(tree.elemtype, tree.elemtype.type);
            }
            super.visitNewArray(tree);
        }
        public void visitClassDef(JCClassDecl tree) {
            if (sigOnly) {
                scan(tree.mods);
                scan(tree.typarams);
                scan(tree.extending);
                scan(tree.implementing);
            }
            for (JCTree member : tree.defs) {
                if (member.hasTag(Tag.CLASSDEF)) {
                    continue;
                }
                scan(member);
            }
        }
        public void visitBlock(JCBlock tree) {
            if (!sigOnly) {
                scan(tree.stats);
            }
        }

        /* I would want to model this after
         * com.sun.tools.javac.comp.Check.Validator.visitSelectInternal(JCFieldAccess)
         * and override visitSelect and visitTypeApply.
         * However, we only set the annotated type in the top-level type
         * of the symbol.
         * Therefore, we need to override each individual location where a type
         * can occur.
         */
        private void validateAnnotatedType(final JCTree errtree, final Type type) {

            if (type.isPrimitiveOrVoid()) {
                return;
            }

            JCTree enclTr = errtree;
            Type enclTy = type;

            boolean repeat = true;
            while (repeat) {
                if (enclTr.hasTag(TYPEAPPLY)) {
                    List<Type> tyargs = enclTy.getTypeArguments();
                    List<JCExpression> trargs = ((JCTypeApply)enclTr).getTypeArguments();
                    if (trargs.length() > 0) {
                        if (tyargs.length() == trargs.length()) {
                            for (int i = 0; i < tyargs.length(); ++i) {
                                validateAnnotatedType(trargs.get(i), tyargs.get(i));
                            }
                        }
                    }

                    enclTr = ((JCTree.JCTypeApply)enclTr).clazz;
                }

                if (enclTr.hasTag(SELECT)) {
                    enclTr = ((JCTree.JCFieldAccess)enclTr).getExpression();
                    if (enclTy != null &&
                            !enclTy.hasTag(NONE)) {
                        enclTy = enclTy.getEnclosingType();
                    }
                } else if (enclTr.hasTag(ANNOTATED_TYPE)) {
                    JCAnnotatedType at = (JCTree.JCAnnotatedType) enclTr;
                    if (enclTy == null || enclTy.hasTag(NONE)) {
                        if (at.getAnnotations().size() == 1) {
                            log.error(at.underlyingType.pos(), Errors.CantTypeAnnotateScoping1(at.getAnnotations().head.attribute));
                        } else {
                            ListBuffer<Attribute.Compound> comps = new ListBuffer<>();
                            for (JCAnnotation an : at.getAnnotations()) {
                                comps.add(an.attribute);
                            }
                            log.error(at.underlyingType.pos(), Errors.CantTypeAnnotateScoping(comps.toList()));
                        }
                        repeat = false;
                    }
                    enclTr = at.underlyingType;
                } else if (enclTr.hasTag(IDENT)) {
                    repeat = false;
                } else if (enclTr.hasTag(JCTree.Tag.WILDCARD)) {
                    JCWildcard wc = (JCWildcard) enclTr;
                    if (wc.getKind() == JCTree.Kind.EXTENDS_WILDCARD ||
                            wc.getKind() == JCTree.Kind.SUPER_WILDCARD) {
                        validateAnnotatedType(wc.getBound(), wc.getBound().type);
                    } else {
                    }
                    repeat = false;
                } else if (enclTr.hasTag(TYPEARRAY)) {
                    JCArrayTypeTree art = (JCArrayTypeTree) enclTr;
                    validateAnnotatedType(art.getType(), art.elemtype.type);
                    repeat = false;
                } else if (enclTr.hasTag(TYPEUNION)) {
                    JCTypeUnion ut = (JCTypeUnion) enclTr;
                    for (JCTree t : ut.getTypeAlternatives()) {
                        validateAnnotatedType(t, t.type);
                    }
                    repeat = false;
                } else if (enclTr.hasTag(TYPEINTERSECTION)) {
                    JCTypeIntersection it = (JCTypeIntersection) enclTr;
                    for (JCTree t : it.getBounds()) {
                        validateAnnotatedType(t, t.type);
                    }
                    repeat = false;
                } else if (enclTr.getKind() == JCTree.Kind.PRIMITIVE_TYPE ||
                           enclTr.getKind() == JCTree.Kind.ERRONEOUS) {
                    repeat = false;
                } else {
                    Assert.error("Unexpected tree: " + enclTr + " with kind: " + enclTr.getKind() +
                            " within: "+ errtree + " with kind: " + errtree.getKind());
                }
            }
        }

        private void checkForDeclarationAnnotations(List<? extends JCAnnotation> annotations,
                Symbol sym) {
            for (JCAnnotation ai : annotations) {
                if (!ai.type.isErroneous() &&
                        typeAnnotations.annotationTargetType(ai, ai.attribute, sym) == TypeAnnotations.AnnotationType.DECLARATION) {
                    log.error(ai.pos(), Errors.AnnotationTypeNotApplicableToType(ai.type));
                }
            }
        }
    }


    /**
     * Handle missing types/symbols in an AST. This routine is useful when
     * the compiler has encountered some errors (which might have ended up
     * terminating attribution abruptly); if the compiler is used in fail-over
     * mode (e.g. by an IDE) and the AST contains semantic errors, this routine
     * prevents NPE to be propagated during subsequent compilation steps.
     */
    public void postAttr(JCTree tree) {
        new PostAttrAnalyzer().scan(tree);
    }

    class PostAttrAnalyzer extends TreeScanner {

        private void initTypeIfNeeded(JCTree that) {
            if (that.type == null) {
                if (that.hasTag(METHODDEF)) {
                    that.type = dummyMethodType((JCMethodDecl)that);
                } else {
                    that.type = syms.unknownType;
                }
            }
        }

        /* Construct a dummy method type. If we have a method declaration,
         * and the declared return type is void, then use that return type
         * instead of UNKNOWN to avoid spurious error messages in lambda
         * bodies (see:JDK-8041704).
         */
        private Type dummyMethodType(JCMethodDecl md) {
            Type restype = syms.unknownType;
            if (md != null && md.restype != null && md.restype.hasTag(TYPEIDENT)) {
                JCPrimitiveTypeTree prim = (JCPrimitiveTypeTree)md.restype;
                if (prim.typetag == VOID)
                    restype = syms.voidType;
            }
            return new MethodType(List.nil(), restype,
                                  List.nil(), syms.methodClass);
        }
        private Type dummyMethodType() {
            return dummyMethodType(null);
        }

        @Override
        public void scan(JCTree tree) {
            if (tree == null) return;
            if (tree instanceof JCExpression) {
                initTypeIfNeeded(tree);
            }
            super.scan(tree);
        }

        @Override
        public void visitIdent(JCIdent that) {
            if (that.sym == null) {
                that.sym = syms.unknownSymbol;
            }
        }

        @Override
        public void visitSelect(JCFieldAccess that) {
            if (that.sym == null) {
                that.sym = syms.unknownSymbol;
            }
            super.visitSelect(that);
        }

        @Override
        public void visitClassDef(JCClassDecl that) {
            initTypeIfNeeded(that);
            if (that.sym == null) {
                that.sym = new ClassSymbol(0, that.name, that.type, syms.noSymbol);
            }
            super.visitClassDef(that);
        }

        @Override
        public void visitMethodDef(JCMethodDecl that) {
            initTypeIfNeeded(that);
            if (that.sym == null) {
                that.sym = new MethodSymbol(0, that.name, that.type, syms.noSymbol);
            }
            super.visitMethodDef(that);
        }

        @Override
        public void visitVarDef(JCVariableDecl that) {
            initTypeIfNeeded(that);
            if (that.sym == null) {
                that.sym = new VarSymbol(0, that.name, that.type, syms.noSymbol);
                that.sym.adr = 0;
            }
            if (that.vartype == null) {
                that.vartype = make.at(Position.NOPOS).Erroneous();
            }
            super.visitVarDef(that);
        }

        @Override
        public void visitBindingPattern(JCBindingPattern that) {
            initTypeIfNeeded(that);
            initTypeIfNeeded(that.var);
            if (that.var.sym == null) {
                that.var.sym = new BindingSymbol(0, that.var.name, that.var.type, syms.noSymbol);
                that.var.sym.adr = 0;
            }
            super.visitBindingPattern(that);
        }

        @Override
        public void visitNewClass(JCNewClass that) {
            if (that.constructor == null) {
                that.constructor = new MethodSymbol(0, names.init,
                        dummyMethodType(), syms.noSymbol);
            }
            if (that.constructorType == null) {
                that.constructorType = syms.unknownType;
            }
            super.visitNewClass(that);
        }

        @Override
        public void visitAssignop(JCAssignOp that) {
            if (that.operator == null) {
                that.operator = new OperatorSymbol(names.empty, dummyMethodType(),
                        -1, syms.noSymbol);
            }
            super.visitAssignop(that);
        }

        @Override
        public void visitBinary(JCBinary that) {
            if (that.operator == null) {
                that.operator = new OperatorSymbol(names.empty, dummyMethodType(),
                        -1, syms.noSymbol);
            }
            super.visitBinary(that);
        }

        @Override
        public void visitUnary(JCUnary that) {
            if (that.operator == null) {
                that.operator = new OperatorSymbol(names.empty, dummyMethodType(),
                        -1, syms.noSymbol);
            }
            super.visitUnary(that);
        }

        @Override
        public void visitReference(JCMemberReference that) {
            super.visitReference(that);
            if (that.sym == null) {
                that.sym = new MethodSymbol(0, names.empty, dummyMethodType(),
                        syms.noSymbol);
            }
        }
    }

    public void setPackageSymbols(JCExpression pid, Symbol pkg) {
        new TreeScanner() {
            Symbol packge = pkg;
            @Override
            public void visitIdent(JCIdent that) {
                that.sym = packge;
            }

            @Override
            public void visitSelect(JCFieldAccess that) {
                that.sym = packge;
                packge = packge.owner;
                super.visitSelect(that);
            }
        }.scan(pid);
    }

}
