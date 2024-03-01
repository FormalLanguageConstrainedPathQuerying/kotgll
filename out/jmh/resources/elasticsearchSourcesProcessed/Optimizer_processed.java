/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.sql.optimizer;

import org.elasticsearch.search.aggregations.metrics.PercentilesConfig;
import org.elasticsearch.xpack.ql.expression.Alias;
import org.elasticsearch.xpack.ql.expression.Attribute;
import org.elasticsearch.xpack.ql.expression.AttributeMap;
import org.elasticsearch.xpack.ql.expression.Expression;
import org.elasticsearch.xpack.ql.expression.ExpressionSet;
import org.elasticsearch.xpack.ql.expression.Expressions;
import org.elasticsearch.xpack.ql.expression.FieldAttribute;
import org.elasticsearch.xpack.ql.expression.Literal;
import org.elasticsearch.xpack.ql.expression.NamedExpression;
import org.elasticsearch.xpack.ql.expression.Order;
import org.elasticsearch.xpack.ql.expression.ReferenceAttribute;
import org.elasticsearch.xpack.ql.expression.UnresolvedAttribute;
import org.elasticsearch.xpack.ql.expression.function.Function;
import org.elasticsearch.xpack.ql.expression.function.aggregate.AggregateFunction;
import org.elasticsearch.xpack.ql.expression.function.aggregate.Count;
import org.elasticsearch.xpack.ql.expression.function.aggregate.InnerAggregate;
import org.elasticsearch.xpack.ql.expression.predicate.operator.comparison.Equals;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.BooleanSimplification;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.CombineBinaryComparisons;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.ConstantFolding;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.FoldNull;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.LiteralsOnTheRight;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.OptimizerExpressionRule;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.OptimizerRule;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.PropagateEquals;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.PruneLiteralsInOrderBy;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.ReplaceRegexMatch;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.SetAsOptimized;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.SimplifyComparisonsArithmetics;
import org.elasticsearch.xpack.ql.optimizer.OptimizerRules.TransformDirection;
import org.elasticsearch.xpack.ql.plan.logical.Aggregate;
import org.elasticsearch.xpack.ql.plan.logical.EsRelation;
import org.elasticsearch.xpack.ql.plan.logical.Filter;
import org.elasticsearch.xpack.ql.plan.logical.LeafPlan;
import org.elasticsearch.xpack.ql.plan.logical.Limit;
import org.elasticsearch.xpack.ql.plan.logical.LogicalPlan;
import org.elasticsearch.xpack.ql.plan.logical.OrderBy;
import org.elasticsearch.xpack.ql.plan.logical.Project;
import org.elasticsearch.xpack.ql.plan.logical.UnaryPlan;
import org.elasticsearch.xpack.ql.rule.Rule;
import org.elasticsearch.xpack.ql.rule.RuleExecutor;
import org.elasticsearch.xpack.ql.tree.Source;
import org.elasticsearch.xpack.ql.type.DataType;
import org.elasticsearch.xpack.ql.type.DataTypes;
import org.elasticsearch.xpack.ql.util.Holder;
import org.elasticsearch.xpack.sql.SqlIllegalArgumentException;
import org.elasticsearch.xpack.sql.analysis.analyzer.Analyzer.CleanAliases;
import org.elasticsearch.xpack.sql.expression.function.aggregate.ExtendedStats;
import org.elasticsearch.xpack.sql.expression.function.aggregate.ExtendedStatsEnclosed;
import org.elasticsearch.xpack.sql.expression.function.aggregate.First;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Last;
import org.elasticsearch.xpack.sql.expression.function.aggregate.MatrixStats;
import org.elasticsearch.xpack.sql.expression.function.aggregate.MatrixStatsEnclosed;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Max;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Min;
import org.elasticsearch.xpack.sql.expression.function.aggregate.NumericAggregate;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Percentile;
import org.elasticsearch.xpack.sql.expression.function.aggregate.PercentileRank;
import org.elasticsearch.xpack.sql.expression.function.aggregate.PercentileRanks;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Percentiles;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Stats;
import org.elasticsearch.xpack.sql.expression.function.aggregate.Sum;
import org.elasticsearch.xpack.sql.expression.function.aggregate.TopHits;
import org.elasticsearch.xpack.sql.expression.function.scalar.Cast;
import org.elasticsearch.xpack.sql.expression.predicate.conditional.ArbitraryConditionalFunction;
import org.elasticsearch.xpack.sql.expression.predicate.conditional.Case;
import org.elasticsearch.xpack.sql.expression.predicate.conditional.Coalesce;
import org.elasticsearch.xpack.sql.expression.predicate.conditional.ConditionalFunction;
import org.elasticsearch.xpack.sql.expression.predicate.conditional.IfConditional;
import org.elasticsearch.xpack.sql.expression.predicate.conditional.Iif;
import org.elasticsearch.xpack.sql.expression.predicate.conditional.NullIf;
import org.elasticsearch.xpack.sql.expression.predicate.operator.arithmetic.Mul;
import org.elasticsearch.xpack.sql.expression.predicate.operator.comparison.In;
import org.elasticsearch.xpack.sql.plan.logical.LocalRelation;
import org.elasticsearch.xpack.sql.plan.logical.Pivot;
import org.elasticsearch.xpack.sql.plan.logical.SubQueryAlias;
import org.elasticsearch.xpack.sql.session.EmptyExecutable;
import org.elasticsearch.xpack.sql.session.SingletonExecutable;
import org.elasticsearch.xpack.sql.type.SqlDataTypes;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.elasticsearch.xpack.ql.expression.Expressions.equalsAsAttribute;
import static org.elasticsearch.xpack.ql.optimizer.OptimizerRules.BinaryComparisonSimplification;
import static org.elasticsearch.xpack.ql.optimizer.OptimizerRules.PushDownAndCombineFilters;
import static org.elasticsearch.xpack.ql.type.DataTypes.UNSIGNED_LONG;
import static org.elasticsearch.xpack.ql.util.CollectionUtils.combine;

public class Optimizer extends RuleExecutor<LogicalPlan> {

    public ExecutionInfo debugOptimize(LogicalPlan verified) {
        return verified.optimized() ? null : executeWithInfo(verified);
    }

    public LogicalPlan optimize(LogicalPlan verified) {
        return verified.optimized() ? verified : execute(verified);
    }

    @Override
    protected Iterable<RuleExecutor.Batch<LogicalPlan>> batches() {
        var substitutions = new Batch<>(
            "Substitutions",
            Limiter.ONCE,
            new RewritePivot(),
            new ReplaceRegexMatch(),
            new ReplaceAggregatesWithLiterals()
        );

        var refs = new Batch<>("Replace References", Limiter.ONCE, new ReplaceReferenceAttributeWithSource());

        var operators = new Batch<>(
            "Operator Optimization",
            new CombineProjections(),
            new ReplaceFoldableAttributes(),
            new FoldNull(),
            new ReplaceAggregationsInLocalRelations(),
            new ConstantFolding(),
            new SimplifyConditional(),
            new SimplifyCase(),
            new BooleanSimplification(),
            new LiteralsOnTheRight(),
            new BinaryComparisonSimplification(),
            new PropagateEquals(),
            new PropagateNullable(),
            new CombineBinaryComparisons(),
            new CombineDisjunctionsToIn(),
            new SimplifyComparisonsArithmetics(SqlDataTypes::areCompatible),
            new PruneDuplicatesInGroupBy(),
            new PruneFilters(),
            new PruneOrderByForImplicitGrouping(),
            new PruneLiteralsInOrderBy(),
            new PruneOrderByNestedFields(),
            new PruneCast(),
            new SortAggregateOnOrderBy(),
            new PushDownAndCombineFilters()
        );

        var aggregate = new Batch<>(
            "Aggregation Rewrite",
            new ReplaceMinMaxWithTopHits(),
            new ReplaceAggsWithMatrixStats(),
            new ReplaceAggsWithExtendedStats(),
            new ReplaceAggsWithStats(),
            new ReplaceSumWithStats(),
            new PromoteStatsToExtendedStats(),
            new ReplaceAggsWithPercentiles(),
            new ReplaceAggsWithPercentileRanks()
        );

        var local = new Batch<>(
            "Skip Elasticsearch",
            new SkipQueryOnLimitZero(),
            new SkipQueryForLiteralAggregations(),
            new PushProjectionsIntoLocalRelation(),
            new PruneLiteralsInGroupBy()
        );

        var label = new Batch<>("Set as Optimized", Limiter.ONCE, CleanAliases.INSTANCE, new SetAsOptimized());

        return Arrays.asList(substitutions, refs, operators, aggregate, local, label);
    }

    static class RewritePivot extends OptimizerRule<Pivot> {

        @Override
        protected LogicalPlan rule(Pivot plan) {
            List<Expression> rawValues = new ArrayList<>(plan.values().size());
            for (NamedExpression namedExpression : plan.values()) {
                if (namedExpression instanceof Alias) {
                    rawValues.add(Literal.of(((Alias) namedExpression).child()));
                }
                else {
                    UnresolvedAttribute attr = new UnresolvedAttribute(
                        namedExpression.source(),
                        namedExpression.name(),
                        null,
                        "Unexpected alias"
                    );
                    return new Pivot(plan.source(), plan.child(), plan.column(), singletonList(attr), plan.aggregates());
                }
            }
            Filter filter = new Filter(plan.source(), plan.child(), new In(plan.source(), plan.column(), rawValues));
            return new Pivot(plan.source(), filter, plan.column(), plan.values(), plan.aggregates(), plan.groupings());
        }
    }

    static class ReplaceReferenceAttributeWithSource extends OptimizerBasicRule {

        @Override
        public LogicalPlan apply(LogicalPlan plan) {
            AttributeMap.Builder<Expression> builder = AttributeMap.builder();
            plan.forEachExpressionUp(Alias.class, a -> builder.put(a.toAttribute(), a.child()));
            final AttributeMap<Expression> collectRefs = builder.build();
            java.util.function.Function<ReferenceAttribute, Expression> replaceReference = r -> collectRefs.resolve(r, r);

            plan = plan.transformUp(p -> {
                if ((p instanceof Pivot || p instanceof Project) == false || p.children().isEmpty()) {
                    if (p instanceof Aggregate agg) {
                        List<Expression> newGrouping = new ArrayList<>(agg.groupings().size());
                        agg.groupings().forEach(e -> newGrouping.add(e.transformUp(ReferenceAttribute.class, replaceReference)));
                        if (agg.groupings().equals(newGrouping) == false) {
                            p = new Aggregate(agg.source(), agg.child(), newGrouping, agg.aggregates());
                        }
                    } else {
                        p = p.transformExpressionsOnly(ReferenceAttribute.class, replaceReference);
                    }
                }
                return p;
            });

            return plan;
        }
    }

    static class CombineDisjunctionsToIn extends org.elasticsearch.xpack.ql.optimizer.OptimizerRules.CombineDisjunctionsToIn {

        @Override
        protected In createIn(Expression key, List<Expression> values, ZoneId zoneId) {
            return new In(key.source(), key, values, zoneId);
        }
    }

    static class PruneLiteralsInGroupBy extends OptimizerRule<Aggregate> {

        @Override
        protected LogicalPlan rule(Aggregate agg) {
            List<Expression> groupings = agg.groupings();
            List<Expression> prunedGroupings = new ArrayList<>();

            for (Expression g : groupings) {
                if (g.foldable()) {
                    prunedGroupings.add(g);
                }
            }

            if (prunedGroupings.size() > 0) {
                List<Expression> newGroupings = new ArrayList<>(groupings);
                newGroupings.removeAll(prunedGroupings);
                return new Aggregate(agg.source(), agg.child(), newGroupings, agg.aggregates());
            }

            return agg;
        }
    }

    static class PruneDuplicatesInGroupBy extends OptimizerRule<Aggregate> {

        @Override
        protected LogicalPlan rule(Aggregate agg) {
            List<Expression> groupings = agg.groupings();
            if (groupings.isEmpty()) {
                return agg;
            }
            ExpressionSet<Expression> unique = new ExpressionSet<>(groupings);
            if (unique.size() != groupings.size()) {
                return new Aggregate(agg.source(), agg.child(), new ArrayList<>(unique), agg.aggregates());
            }
            return agg;
        }
    }

    static class PruneOrderByNestedFields extends OptimizerRule<Project> {

        private static void findNested(Expression exp, AttributeMap<Function> functions, Consumer<FieldAttribute> onFind) {
            exp.forEachUp(e -> {
                if (e instanceof ReferenceAttribute) {
                    Function f = functions.resolve(e);
                    if (f != null) {
                        findNested(f, functions, onFind);
                    }
                }
                if (e instanceof FieldAttribute fa) {
                    if (fa.isNested()) {
                        onFind.accept(fa);
                    }
                }
            });
        }

        @Override
        protected LogicalPlan rule(Project project) {
            if (project.child() instanceof OrderBy ob) {
                AttributeMap.Builder<Function> collectRefs = AttributeMap.builder();

                project.forEachUp(p -> p.forEachExpressionUp(Alias.class, a -> {
                    if (a.child() instanceof Function) {
                        collectRefs.put(a.toAttribute(), (Function) a.child());
                    }
                }));

                AttributeMap<Function> functions = collectRefs.build();

                Map<String, Order> nestedOrders = new LinkedHashMap<>();

                for (Order order : ob.order()) {
                    findNested(order.child(), functions, fa -> nestedOrders.put(fa.nestedParent().name(), order));
                }

                if (nestedOrders.isEmpty()) {
                    return project;
                }

                List<String> nestedTopFields = new ArrayList<>();

                for (NamedExpression ne : project.projections()) {
                    findNested(ne, functions, fa -> nestedTopFields.add(fa.nestedParent().name()));
                }

                List<Order> orders = new ArrayList<>(ob.order());
                if (nestedTopFields.isEmpty()) {
                    orders.removeAll(nestedOrders.values());
                } else {
                    for (Entry<String, Order> entry : nestedOrders.entrySet()) {
                        String parent = entry.getKey();
                        boolean shouldKeep = false;
                        for (String topParent : nestedTopFields) {
                            if (topParent.startsWith(parent)) {
                                shouldKeep = true;
                                break;
                            }
                        }
                        if (shouldKeep == false) {
                            orders.remove(entry.getValue());
                        }
                    }
                }

                if (orders.isEmpty()) {
                    return new Project(project.source(), ob.child(), project.projections());
                }

                if (orders.size() != ob.order().size()) {
                    OrderBy newOrder = new OrderBy(ob.source(), ob.child(), orders);
                    return new Project(project.source(), newOrder, project.projections());
                }
            }
            return project;
        }
    }

    static class PruneOrderByForImplicitGrouping extends OptimizerRule<OrderBy> {

        @Override
        protected LogicalPlan rule(OrderBy ob) {
            Holder<Boolean> foundAggregate = new Holder<>(Boolean.FALSE);
            Holder<Boolean> foundImplicitGroupBy = new Holder<>(Boolean.FALSE);

            ob.forEachDown(Aggregate.class, a -> {
                if (foundAggregate.get() == Boolean.TRUE) {
                    return;
                }
                foundAggregate.set(Boolean.TRUE);
                if (a.groupings().isEmpty()) {
                    foundImplicitGroupBy.set(Boolean.TRUE);
                }
            });

            if (foundImplicitGroupBy.get() == Boolean.TRUE) {
                return ob.child();
            }
            return ob;
        }
    }

    /**
     * Align the order in aggregate based on the order by.
     */
    static class SortAggregateOnOrderBy extends OptimizerRule<OrderBy> {

        @Override
        protected LogicalPlan rule(OrderBy ob) {
            List<Order> order = ob.order();

            List<Order> nonConstant = new LinkedList<>();
            for (int i = order.size() - 1; i >= 0; i--) {
                nonConstant.add(order.get(i));
            }

            Holder<Boolean> foundAggregate = new Holder<>(Boolean.FALSE);

            return ob.transformDown(Aggregate.class, a -> {
                if (foundAggregate.get() == Boolean.TRUE) {
                    return a;
                }
                foundAggregate.set(Boolean.TRUE);

                List<Expression> groupings = new LinkedList<>(a.groupings());

                for (Order o : nonConstant) {
                    Expression fieldToOrder = o.child();
                    for (Expression group : a.groupings()) {
                        Holder<Boolean> isMatching = new Holder<>(Boolean.FALSE);
                        if (equalsAsAttribute(fieldToOrder, group)) {
                            isMatching.set(Boolean.TRUE);
                        } else {
                            a.aggregates().forEach(alias -> {
                                if (alias instanceof Alias) {
                                    Expression child = ((Alias) alias).child();
                                    if ((equalsAsAttribute(child, group)
                                        && (equalsAsAttribute(alias, fieldToOrder) || equalsAsAttribute(child, fieldToOrder)))
                                        || (equalsAsAttribute(alias, group)
                                            && (equalsAsAttribute(alias, fieldToOrder) || equalsAsAttribute(child, fieldToOrder)))) {
                                        isMatching.set(Boolean.TRUE);
                                    }
                                }
                            });
                        }

                        if (isMatching.get()) {
                            groupings.remove(group);
                            groupings.add(0, group);
                        }
                    }
                }

                if (groupings.equals(a.groupings()) == false) {
                    return new Aggregate(a.source(), a.child(), groupings, a.aggregates());
                }

                return a;
            });
        }
    }

    static class CombineLimits extends OptimizerRule<Limit> {

        @Override
        protected LogicalPlan rule(Limit limit) {
            if (limit.child() instanceof Limit) {
                throw new UnsupportedOperationException("not implemented yet");
            }
            throw new UnsupportedOperationException("not implemented yet");
        }
    }

    static class PruneCast extends OptimizerRules.PruneCast<Cast> {

        PruneCast() {
            super(Cast.class);
        }

        @Override
        protected Expression maybePruneCast(Cast cast) {
            return cast.from() == cast.to() ? cast.field() : cast;
        }
    }

    static class CombineProjections extends OptimizerRule<UnaryPlan> {

        CombineProjections() {
            super(TransformDirection.UP);
        }

        @Override
        protected LogicalPlan rule(UnaryPlan plan) {
            LogicalPlan child = plan.child();

            if (plan instanceof Project project) {
                if (child instanceof Project p) {
                    return new Project(p.source(), p.child(), combineProjections(project.projections(), p.projections()));
                }

                if (child instanceof Aggregate a) {
                    return new Aggregate(a.source(), a.child(), a.groupings(), combineProjections(project.projections(), a.aggregates()));
                }

                if (child instanceof Pivot p) {
                    if (project.outputSet().subsetOf(p.groupingSet())) {
                        return new Aggregate(p.source(), p.child(), new ArrayList<>(project.projections()), project.projections());
                    }
                }
            }
            if (plan instanceof Aggregate a) {
                if (child instanceof Project p) {
                    return new Aggregate(a.source(), p.child(), a.groupings(), combineProjections(a.aggregates(), p.projections()));
                }
            }
            return plan;
        }

        private static List<NamedExpression> combineProjections(
            List<? extends NamedExpression> upper,
            List<? extends NamedExpression> lower
        ) {


            AttributeMap.Builder<NamedExpression> aliasesBuilder = AttributeMap.builder();
            for (NamedExpression ne : lower) {
                if ((ne instanceof Attribute) == false) {
                    aliasesBuilder.put(ne.toAttribute(), ne);
                }
            }

            AttributeMap<NamedExpression> aliases = aliasesBuilder.build();
            List<NamedExpression> replaced = new ArrayList<>();

            for (NamedExpression ne : upper) {
                NamedExpression replacedExp = (NamedExpression) ne.transformUp(Attribute.class, a -> aliases.resolve(a, a));
                replaced.add((NamedExpression) CleanAliases.trimNonTopLevelAliases(replacedExp));
            }
            return replaced;
        }
    }


    static class ReplaceFoldableAttributes extends Rule<LogicalPlan, LogicalPlan> {

        @Override
        public LogicalPlan apply(LogicalPlan plan) {
            return rule(plan);
        }

        protected LogicalPlan rule(LogicalPlan plan) {
            Map<Attribute, Alias> aliases = new LinkedHashMap<>();
            List<Attribute> attrs = new ArrayList<>();

            plan.forEachDown(Project.class, p -> {
                for (NamedExpression ne : p.projections()) {
                    if (ne instanceof Alias) {
                        if (((Alias) ne).child().foldable()) {
                            Attribute attr = ne.toAttribute();
                            attrs.add(attr);
                            aliases.put(attr, (Alias) ne);
                        }
                    }
                }
            });

            if (attrs.isEmpty()) {
                return plan;
            }

            Holder<Boolean> stop = new Holder<>(Boolean.FALSE);

            plan = plan.transformUp(p -> {
                if (stop.get() == Boolean.FALSE && canPropagateFoldable(p)) {
                    return p.transformExpressionsDown(Attribute.class, e -> {
                        if (attrs.contains(e)) {
                            Alias as = aliases.get(e);
                            if (as == null) {
                                throw new SqlIllegalArgumentException("unsupported");
                            }
                            return as;
                        }
                        return e;
                    });
                }

                if (p.children().size() > 1) {
                    stop.set(Boolean.TRUE);
                }

                return p;
            });

            return CleanAliases.INSTANCE.apply(plan);

        }

        private static boolean canPropagateFoldable(LogicalPlan p) {
            return p instanceof Project
                || p instanceof Filter
                || p instanceof SubQueryAlias
                || p instanceof Aggregate
                || p instanceof Limit
                || p instanceof OrderBy;
        }
    }

    /**
     * Extend null propagation in SQL to null conditionals
     */
    static class PropagateNullable extends OptimizerRules.PropagateNullable {

        @Override
        protected Expression nullify(Expression exp, Expression nullExp) {
            if (exp instanceof Coalesce) {
                List<Expression> newChildren = new ArrayList<>(exp.children());
                newChildren.removeIf(e -> e.semanticEquals(nullExp));
                if (newChildren.size() != exp.children().size()) {
                    return exp.replaceChildren(newChildren);
                }
            }
            return super.nullify(exp, nullExp);
        }

        @Override
        protected Expression nonNullify(Expression exp, Expression nonNullExp) {
            if (exp instanceof Coalesce) {
                List<Expression> children = exp.children();
                for (int i = 0; i < children.size(); i++) {
                    if (nonNullExp.semanticEquals(children.get(i))) {
                        return exp.replaceChildren(children.subList(0, i + 1));
                    }
                }
            }
            return super.nonNullify(exp, nonNullExp);
        }
    }

    static class SimplifyConditional extends OptimizerExpressionRule<ConditionalFunction> {

        SimplifyConditional() {
            super(TransformDirection.DOWN);
        }

        @Override
        protected Expression rule(ConditionalFunction cf) {
            Expression e = cf;
            List<Expression> children = e.children();
            if (e instanceof NullIf nullIf) {
                if (Expressions.isNull(nullIf.left()) || Expressions.isNull(nullIf.right())) {
                    return nullIf.left();
                }
            }
            if (e instanceof ArbitraryConditionalFunction c) {

                List<Expression> newChildren = new ArrayList<>();
                for (Expression child : children) {
                    if (Expressions.isNull(child) == false) {
                        newChildren.add(child);

                        if (e instanceof Coalesce && child.foldable()) {
                            break;
                        }
                    }
                }

                if (newChildren.size() < children.size()) {
                    e = c.replaceChildren(newChildren);
                }

                if (e instanceof Coalesce && children.size() > 0) {
                    Expression firstChild = children.get(0);
                    boolean sameChild = true;
                    for (int i = 1; i < children.size(); i++) {
                        Expression child = children.get(i);
                        if (firstChild.semanticEquals(child) == false) {
                            sameChild = false;
                            break;
                        }
                    }
                    if (sameChild) {
                        return firstChild;
                    }
                }
            }
            return e;
        }
    }

    static class SimplifyCase extends OptimizerExpressionRule<Case> {

        SimplifyCase() {
            super(TransformDirection.DOWN);
        }

        @Override
        protected Expression rule(Case c) {
            Expression e = c;
            List<IfConditional> newConditions = new ArrayList<>();
            for (IfConditional conditional : c.conditions()) {
                if (conditional.condition().foldable()) {
                    Boolean res = (Boolean) conditional.condition().fold();
                    if (res == Boolean.TRUE) {
                        newConditions.add(conditional);
                        break;
                    }
                } else {
                    newConditions.add(conditional);
                }
            }

            if (newConditions.size() < c.children().size()) {
                e = c.replaceChildren(combine(newConditions, c.elseResult()));
            }

            return e;
        }
    }

    /**
     * Any numeric aggregates (avg, min, max, sum) acting on literals are converted to an iif(count(1)=0, null, literal*count(1)) for sum,
     * and to iif(count(1)=0,null,literal) for the other three.
     * Additionally count(DISTINCT literal) is converted to iif(count(1)=0, 0, 1).
     */
    private static class ReplaceAggregatesWithLiterals extends OptimizerBasicRule {

        @Override
        public LogicalPlan apply(LogicalPlan p) {
            return p.transformExpressionsDown(AggregateFunction.class, a -> {
                if (Stats.isTypeCompatible(a) || (a instanceof Count && ((Count) a).distinct())) {

                    if (a.field().foldable()) {
                        Expression countOne = new Count(a.source(), new Literal(Source.EMPTY, 1, a.dataType()), false);
                        Equals countEqZero = new Equals(a.source(), countOne, new Literal(Source.EMPTY, 0, a.dataType()));
                        Expression argument = a.field();
                        Literal foldedArgument = new Literal(argument.source(), argument.fold(), a.dataType());

                        Expression iifResult = Literal.NULL;
                        Expression iifElseResult = foldedArgument;
                        if (a instanceof Sum) {
                            iifElseResult = new Mul(a.source(), countOne, foldedArgument);
                        } else if (a instanceof Count) {
                            iifResult = new Literal(Source.EMPTY, 0, a.dataType());
                            iifElseResult = new Literal(Source.EMPTY, 1, a.dataType());
                        }

                        return new Iif(a.source(), countEqZero, iifResult, iifElseResult);
                    }
                }
                return a;
            });
        }
    }

    static class ReplaceAggregationsInLocalRelations extends OptimizerRule<UnaryPlan> {

        @Override
        protected LogicalPlan rule(UnaryPlan plan) {
            if (plan instanceof Aggregate || plan instanceof Filter || plan instanceof OrderBy) {
                LocalRelation relation = unfilteredLocalRelation(plan.child());
                if (relation != null) {
                    long count = relation.executable() instanceof EmptyExecutable ? 0L : 1L;

                    return plan.transformExpressionsDown(AggregateFunction.class, aggregateFunction -> {
                        if (aggregateFunction instanceof Count) {
                            return Literal.of(aggregateFunction, count);
                        } else if (count == 0) {
                            return Literal.of(aggregateFunction, null);
                        } else {
                            return aggregateFunction;
                        }
                    });
                }
            }

            return plan;
        }

        private static LocalRelation unfilteredLocalRelation(LogicalPlan plan) {
            List<LogicalPlan> filterOrLeaves = plan.collectFirstChildren(p -> p instanceof Filter || p instanceof LeafPlan);

            if (filterOrLeaves.size() == 1) {
                LogicalPlan filterOrLeaf = filterOrLeaves.get(0);
                if (filterOrLeaf instanceof LocalRelation) {
                    return (LocalRelation) filterOrLeaf;
                }
            }
            return null;
        }

    }

    static class ReplaceAggsWithMatrixStats extends OptimizerBasicRule {

        @Override
        public LogicalPlan apply(LogicalPlan p) {
            final Map<Expression, MatrixStats> seen = new LinkedHashMap<>();

            return p.transformExpressionsUp(AggregateFunction.class, f -> {
                if (f instanceof MatrixStatsEnclosed) {
                    Expression argument = f.field();
                    MatrixStats matrixStats = seen.get(argument);

                    if (matrixStats == null) {
                        Source source = new Source(f.sourceLocation(), "MATRIX(" + argument.sourceText() + ")");
                        matrixStats = new MatrixStats(source, argument);
                        seen.put(argument, matrixStats);
                    }

                    f = new InnerAggregate(f.source(), f, matrixStats, argument);
                }

                return f;
            });
        }
    }

    static class ReplaceAggsWithExtendedStats extends OptimizerBasicRule {

        @Override
        public LogicalPlan apply(LogicalPlan p) {
            final Map<Expression, ExtendedStats> seen = new LinkedHashMap<>();

            return p.transformExpressionsUp(AggregateFunction.class, f -> {
                if (f instanceof ExtendedStatsEnclosed) {
                    Expression argument = f.field();
                    ExtendedStats extendedStats = seen.get(argument);

                    if (extendedStats == null) {
                        Source source = new Source(f.sourceLocation(), "EXT_STATS(" + argument.sourceText() + ")");
                        extendedStats = new ExtendedStats(source, argument);
                        seen.put(argument, extendedStats);
                    }

                    f = new InnerAggregate(f, extendedStats);
                }

                return f;
            });
        }
    }

    static class ReplaceAggsWithStats extends OptimizerBasicRule {

        private static class Match {
            final Stats stats;
            private final Set<Class<? extends AggregateFunction>> functionTypes = new LinkedHashSet<>();
            private Map<Class<? extends AggregateFunction>, InnerAggregate> innerAggs = null;

            Match(Stats stats) {
                this.stats = stats;
            }

            @Override
            public String toString() {
                return stats.toString();
            }

            public void add(Class<? extends AggregateFunction> aggType) {
                functionTypes.add(aggType);
            }

            public AggregateFunction maybePromote(AggregateFunction agg) {
                if (functionTypes.size() > 1) {
                    if (innerAggs == null) {
                        innerAggs = new LinkedHashMap<>();
                    }
                    return innerAggs.computeIfAbsent(agg.getClass(), k -> new InnerAggregate(agg, stats));
                }
                return agg;
            }
        }

        @Override
        public LogicalPlan apply(LogicalPlan p) {
            final Map<Expression, Match> potentialPromotions = new LinkedHashMap<>();

            p.forEachExpressionUp(AggregateFunction.class, f -> {
                if (Stats.isTypeCompatible(f)) {
                    Expression argument = f.field();
                    Match match = potentialPromotions.get(argument);

                    if (match == null) {
                        Source source = new Source(f.sourceLocation(), "STATS(" + argument.sourceText() + ")");
                        match = new Match(new Stats(source, argument));
                        potentialPromotions.put(argument, match);
                    }
                    match.add(f.getClass());
                }
            });

            if (potentialPromotions.isEmpty()) {
                return p;
            }


            return p.transformExpressionsUp(AggregateFunction.class, f -> {
                if (Stats.isTypeCompatible(f)) {
                    Expression argument = f.field();
                    Match match = potentialPromotions.get(argument);

                    if (match != null) {
                        return match.maybePromote(f);
                    }
                }
                return f;
            });
        }
    }

    static class ReplaceSumWithStats extends OptimizerBasicRule {

        @Override
        public LogicalPlan apply(LogicalPlan plan) {
            final Map<Expression, Stats> statsPerField = new LinkedHashMap<>();

            plan.forEachExpressionUp(Sum.class, s -> {
                statsPerField.computeIfAbsent(s.field(), field -> {
                    Source source = new Source(field.sourceLocation(), "STATS(" + field.sourceText() + ")");
                    return new Stats(source, field);
                });
            });

            if (statsPerField.isEmpty() == false) {
                plan = plan.transformExpressionsUp(Sum.class, sum -> new InnerAggregate(sum, statsPerField.get(sum.field())));
            }

            return plan;
        }
    }

    static class PromoteStatsToExtendedStats extends OptimizerBasicRule {

        @Override
        public LogicalPlan apply(LogicalPlan p) {
            final Map<Expression, ExtendedStats> seen = new LinkedHashMap<>();

            p.forEachExpressionUp(InnerAggregate.class, ia -> {
                if (ia.outer() instanceof ExtendedStats extStats) {
                    seen.putIfAbsent(extStats.field(), extStats);
                }
            });

            return p.transformExpressionsUp(InnerAggregate.class, ia -> {
                if (ia.outer() instanceof Stats stats) {
                    ExtendedStats ext = seen.get(stats.field());
                    if (ext != null && stats.field().equals(ext.field())) {
                        return new InnerAggregate(ia.inner(), ext);
                    }
                }
                return ia;
            });
        }
    }

    private record PercentileKey(Expression field, PercentilesConfig percentilesConfig) {
        PercentileKey(Percentile per) {
            this(per.field(), per.percentilesConfig());
        }

        PercentileKey(PercentileRank per) {
            this(per.field(), per.percentilesConfig());
        }
    }

    static class ReplaceAggsWithPercentiles extends OptimizerBasicRule {

        @Override
        public LogicalPlan apply(LogicalPlan p) {
            Map<PercentileKey, Set<Expression>> percentsPerAggKey = new LinkedHashMap<>();

            p.forEachExpressionUp(
                Percentile.class,
                per -> percentsPerAggKey.computeIfAbsent(new PercentileKey(per), v -> new LinkedHashSet<>()).add(per.percent())
            );

            Map<PercentileKey, Percentiles> percentilesPerAggKey = new LinkedHashMap<>();
            percentsPerAggKey.forEach(
                (aggKey, percents) -> percentilesPerAggKey.put(
                    aggKey,
                    new Percentiles(
                        percents.iterator().next().source(),
                        aggKey.field(),
                        new ArrayList<>(percents),
                        aggKey.percentilesConfig()
                    )
                )
            );

            return p.transformExpressionsUp(Percentile.class, per -> {
                PercentileKey a = new PercentileKey(per);
                Percentiles percentiles = percentilesPerAggKey.get(a);
                return new InnerAggregate(per, percentiles);
            });
        }
    }

    static class ReplaceAggsWithPercentileRanks extends OptimizerBasicRule {

        @Override
        public LogicalPlan apply(LogicalPlan p) {
            final Map<PercentileKey, Set<Expression>> valuesPerAggKey = new LinkedHashMap<>();

            p.forEachExpressionUp(
                PercentileRank.class,
                per -> valuesPerAggKey.computeIfAbsent(new PercentileKey(per), v -> new LinkedHashSet<>()).add(per.value())
            );

            Map<PercentileKey, PercentileRanks> ranksPerAggKey = new LinkedHashMap<>();
            valuesPerAggKey.forEach(
                (aggKey, values) -> ranksPerAggKey.put(
                    aggKey,
                    new PercentileRanks(
                        values.iterator().next().source(),
                        aggKey.field(),
                        new ArrayList<>(values),
                        aggKey.percentilesConfig()
                    )
                )
            );

            return p.transformExpressionsUp(PercentileRank.class, per -> {
                PercentileRanks ranks = ranksPerAggKey.get(new PercentileKey(per));
                return new InnerAggregate(per, ranks);
            });
        }
    }

    static class ReplaceMinMaxWithTopHits extends OptimizerRule<LogicalPlan> {

        @Override
        protected LogicalPlan rule(LogicalPlan plan) {
            Map<Expression, TopHits> mins = new HashMap<>();
            Map<Expression, TopHits> maxs = new HashMap<>();
            return plan.transformExpressionsDown(NumericAggregate.class, e -> {
                if (e instanceof Min min) {
                    DataType minType = min.field().dataType();
                    if (DataTypes.isString(minType) || minType == UNSIGNED_LONG) {
                        return mins.computeIfAbsent(min.field(), k -> new First(min.source(), k, null));
                    }
                }
                if (e instanceof Max max) {
                    DataType maxType = max.field().dataType();
                    if (DataTypes.isString(maxType) || maxType == UNSIGNED_LONG) {
                        return maxs.computeIfAbsent(max.field(), k -> new Last(max.source(), k, null));
                    }
                }
                return e;
            });
        }
    }

    static class PruneFilters extends org.elasticsearch.xpack.ql.optimizer.OptimizerRules.PruneFilters {

        @Override
        protected LogicalPlan skipPlan(Filter filter) {
            return Optimizer.skipPlan(filter);
        }
    }

    static class SkipQueryOnLimitZero extends org.elasticsearch.xpack.ql.optimizer.OptimizerRules.SkipQueryOnLimitZero {

        @Override
        protected LogicalPlan skipPlan(Limit limit) {
            return Optimizer.skipPlan(limit);
        }
    }

    private static LogicalPlan skipPlan(UnaryPlan plan) {
        return new LocalRelation(plan.source(), new EmptyExecutable(plan.output()));
    }

    static class PushProjectionsIntoLocalRelation extends OptimizerRule<UnaryPlan> {

        @Override
        protected LogicalPlan rule(UnaryPlan plan) {
            if ((plan instanceof Project || plan instanceof Aggregate) && plan.child() instanceof LocalRelation relation) {
                List<Object> foldedValues = null;

                if (relation.executable() instanceof SingletonExecutable) {
                    if (plan instanceof Aggregate) {
                        foldedValues = extractLiterals(((Aggregate) plan).aggregates());
                    } else {
                        foldedValues = extractLiterals(((Project) plan).projections());
                    }
                } else if (relation.executable() instanceof EmptyExecutable) {
                    if (plan instanceof Aggregate agg) {
                        if (agg.groupings().isEmpty()) {
                            foldedValues = extractLiterals(agg.aggregates());
                        } else {
                            return new LocalRelation(plan.source(), new EmptyExecutable(plan.output()));
                        }
                    }
                }

                if (foldedValues != null) {
                    return new LocalRelation(plan.source(), new SingletonExecutable(plan.output(), foldedValues.toArray()));
                }
            }

            return plan;
        }

        private static List<Object> extractLiterals(List<? extends NamedExpression> named) {
            List<Object> values = new ArrayList<>();
            for (NamedExpression n : named) {
                if (n instanceof Alias a) {
                    if (a.child().foldable()) {
                        values.add(a.child().fold());
                    } else {
                        return null;
                    }
                } else if (n.foldable()) {
                    values.add(n.fold());
                } else {
                    return null;
                }
            }
            return values;
        }

    }

    static class SkipQueryForLiteralAggregations extends OptimizerRule<Aggregate> {

        @Override
        protected LogicalPlan rule(Aggregate plan) {
            if (plan.groupings().isEmpty()
                && plan.child() instanceof EsRelation
                && plan.aggregates().stream().allMatch(SkipQueryForLiteralAggregations::foldable)) {
                return plan.replaceChild(new LocalRelation(plan.source(), new SingletonExecutable()));
            }

            return plan;
        }

        private static boolean foldable(Expression e) {
            return Alias.unwrap(e).foldable();
        }

    }

    abstract static class OptimizerBasicRule extends Rule<LogicalPlan, LogicalPlan> {

        @Override
        public abstract LogicalPlan apply(LogicalPlan plan);

        protected LogicalPlan rule(LogicalPlan plan) {
            return plan;
        }
    }
}
