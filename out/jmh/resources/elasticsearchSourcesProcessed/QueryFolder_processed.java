/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.sql.planner;

import org.elasticsearch.core.Tuple;
import org.elasticsearch.xpack.ql.execution.search.AggRef;
import org.elasticsearch.xpack.ql.expression.Alias;
import org.elasticsearch.xpack.ql.expression.Attribute;
import org.elasticsearch.xpack.ql.expression.AttributeMap;
import org.elasticsearch.xpack.ql.expression.Expression;
import org.elasticsearch.xpack.ql.expression.Expressions;
import org.elasticsearch.xpack.ql.expression.FieldAttribute;
import org.elasticsearch.xpack.ql.expression.Foldables;
import org.elasticsearch.xpack.ql.expression.Literal;
import org.elasticsearch.xpack.ql.expression.NameId;
import org.elasticsearch.xpack.ql.expression.NamedExpression;
import org.elasticsearch.xpack.ql.expression.Order;
import org.elasticsearch.xpack.ql.expression.ReferenceAttribute;
import org.elasticsearch.xpack.ql.expression.function.Function;
import org.elasticsearch.xpack.ql.expression.function.Functions;
import org.elasticsearch.xpack.ql.expression.function.aggregate.AggregateFunction;
import org.elasticsearch.xpack.ql.expression.function.aggregate.Count;
import org.elasticsearch.xpack.ql.expression.function.aggregate.InnerAggregate;
import org.elasticsearch.xpack.ql.expression.function.grouping.GroupingFunction;
import org.elasticsearch.xpack.ql.expression.function.scalar.ScalarFunction;
import org.elasticsearch.xpack.ql.expression.gen.pipeline.AggPathInput;
import org.elasticsearch.xpack.ql.expression.gen.pipeline.Pipe;
import org.elasticsearch.xpack.ql.expression.gen.pipeline.UnaryPipe;
import org.elasticsearch.xpack.ql.expression.gen.processor.Processor;
import org.elasticsearch.xpack.ql.expression.gen.script.ScriptTemplate;
import org.elasticsearch.xpack.ql.planner.ExpressionTranslators;
import org.elasticsearch.xpack.ql.querydsl.container.AttributeSort;
import org.elasticsearch.xpack.ql.querydsl.container.ScriptSort;
import org.elasticsearch.xpack.ql.querydsl.container.Sort.Direction;
import org.elasticsearch.xpack.ql.querydsl.container.Sort.Missing;
import org.elasticsearch.xpack.ql.querydsl.query.Query;
import org.elasticsearch.xpack.ql.rule.Rule;
import org.elasticsearch.xpack.ql.rule.RuleExecutor;
import org.elasticsearch.xpack.ql.type.DataType;
import org.elasticsearch.xpack.ql.type.DataTypes;
import org.elasticsearch.xpack.sql.SqlIllegalArgumentException;
import org.elasticsearch.xpack.sql.expression.function.Score;
import org.elasticsearch.xpack.sql.expression.function.aggregate.CompoundNumericAggregate;
import org.elasticsearch.xpack.sql.expression.function.aggregate.TopHits;
import org.elasticsearch.xpack.sql.expression.function.grouping.Histogram;
import org.elasticsearch.xpack.sql.expression.function.scalar.datetime.DateTimeHistogramFunction;
import org.elasticsearch.xpack.sql.expression.literal.interval.IntervalDayTime;
import org.elasticsearch.xpack.sql.expression.literal.interval.IntervalYearMonth;
import org.elasticsearch.xpack.sql.expression.literal.interval.Intervals;
import org.elasticsearch.xpack.sql.plan.logical.Pivot;
import org.elasticsearch.xpack.sql.plan.physical.AggregateExec;
import org.elasticsearch.xpack.sql.plan.physical.EsQueryExec;
import org.elasticsearch.xpack.sql.plan.physical.FilterExec;
import org.elasticsearch.xpack.sql.plan.physical.LimitExec;
import org.elasticsearch.xpack.sql.plan.physical.LocalExec;
import org.elasticsearch.xpack.sql.plan.physical.OrderExec;
import org.elasticsearch.xpack.sql.plan.physical.PhysicalPlan;
import org.elasticsearch.xpack.sql.plan.physical.PivotExec;
import org.elasticsearch.xpack.sql.plan.physical.ProjectExec;
import org.elasticsearch.xpack.sql.planner.QueryTranslator.QueryTranslation;
import org.elasticsearch.xpack.sql.querydsl.agg.AggFilter;
import org.elasticsearch.xpack.sql.querydsl.agg.Aggs;
import org.elasticsearch.xpack.sql.querydsl.agg.GroupByDateHistogram;
import org.elasticsearch.xpack.sql.querydsl.agg.GroupByKey;
import org.elasticsearch.xpack.sql.querydsl.agg.GroupByNumericHistogram;
import org.elasticsearch.xpack.sql.querydsl.agg.GroupByValue;
import org.elasticsearch.xpack.sql.querydsl.agg.LeafAgg;
import org.elasticsearch.xpack.sql.querydsl.container.AggregateSort;
import org.elasticsearch.xpack.sql.querydsl.container.ComputedRef;
import org.elasticsearch.xpack.sql.querydsl.container.GlobalCountRef;
import org.elasticsearch.xpack.sql.querydsl.container.GroupByRef;
import org.elasticsearch.xpack.sql.querydsl.container.GroupByRef.Property;
import org.elasticsearch.xpack.sql.querydsl.container.GroupingFunctionSort;
import org.elasticsearch.xpack.sql.querydsl.container.MetricAggRef;
import org.elasticsearch.xpack.sql.querydsl.container.PivotColumnRef;
import org.elasticsearch.xpack.sql.querydsl.container.QueryContainer;
import org.elasticsearch.xpack.sql.querydsl.container.ScoreSort;
import org.elasticsearch.xpack.sql.querydsl.container.TopHitsAggRef;
import org.elasticsearch.xpack.sql.session.EmptyExecutable;
import org.elasticsearch.xpack.sql.type.SqlDataTypeConverter;
import org.elasticsearch.xpack.sql.util.Check;
import org.elasticsearch.xpack.sql.util.DateUtils;

import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import static org.elasticsearch.xpack.ql.type.DataTypes.DATETIME;
import static org.elasticsearch.xpack.ql.util.CollectionUtils.combine;
import static org.elasticsearch.xpack.sql.expression.function.grouping.Histogram.DAY_INTERVAL;
import static org.elasticsearch.xpack.sql.expression.function.grouping.Histogram.MONTH_INTERVAL;
import static org.elasticsearch.xpack.sql.expression.function.grouping.Histogram.YEAR_INTERVAL;
import static org.elasticsearch.xpack.sql.planner.QueryTranslator.toAgg;
import static org.elasticsearch.xpack.sql.planner.QueryTranslator.toQuery;
import static org.elasticsearch.xpack.sql.type.SqlDataTypes.DATE;
import static org.elasticsearch.xpack.sql.type.SqlDataTypes.isDateBased;

/**
 * Folds the PhysicalPlan into a {@link Query}.
 */
class QueryFolder extends RuleExecutor<PhysicalPlan> {

    PhysicalPlan fold(PhysicalPlan plan) {
        return execute(plan);
    }

    @Override
    protected Iterable<RuleExecutor.Batch<PhysicalPlan>> batches() {
        var rollup = new Batch<>(
            "Fold queries",
            new FoldPivot(),
            new FoldAggregate(),
            new FoldProject(),
            new FoldFilter(),
            new FoldOrderBy(),
            new FoldLimit()
        );

        var local = new Batch<>("Local queries", new LocalLimit(), new PropagateEmptyLocal());
        var finish = new Batch<>("Finish query", Limiter.ONCE, new PlanOutputToQueryRef());

        return Arrays.asList(rollup, local, finish);
    }

    private static class FoldProject extends FoldingRule<ProjectExec> {

        @Override
        protected PhysicalPlan rule(ProjectExec project) {
            if (project.child() instanceof EsQueryExec exec) {
                QueryContainer queryC = exec.queryContainer();

                AttributeMap.Builder<Expression> aliases = AttributeMap.<Expression>builder().putAll(queryC.aliases());
                AttributeMap.Builder<Pipe> processors = AttributeMap.<Pipe>builder().putAll(queryC.scalarFunctions());

                List<QueryContainer.FieldInfo> fields = new ArrayList<>(queryC.fields().size());
                List<QueryContainer.FieldInfo> hiddenFields = new ArrayList<>(queryC.fields());

                for (NamedExpression pj : project.projections()) {
                    Attribute attr = pj.toAttribute();
                    NameId attributeId = attr.id();

                    if (pj instanceof Alias) {
                        Expression e = ((Alias) pj).child();

                        aliases.put(attr, e);

                        if (e instanceof ScalarFunction) {
                            processors.put(attr, ((ScalarFunction) e).asPipe());
                        }

                        if (e instanceof NamedExpression) {
                            attributeId = ((NamedExpression) e).toAttribute().id();
                        }
                    }

                    for (QueryContainer.FieldInfo field : queryC.fields()) {
                        if (field.attribute().id().equals(attributeId)) {
                            fields.add(field);
                            hiddenFields.remove(field);
                            break;
                        }
                    }
                }

                fields.addAll(hiddenFields);

                QueryContainer clone = new QueryContainer(
                    queryC.query(),
                    queryC.aggs(),
                    fields,
                    aliases.build(),
                    queryC.pseudoFunctions(),
                    processors.build(),
                    queryC.sort(),
                    queryC.limit(),
                    queryC.shouldTrackHits(),
                    queryC.shouldIncludeFrozen(),
                    queryC.minPageSize(),
                    queryC.allowPartialSearchResults()
                );
                return new EsQueryExec(exec.source(), exec.index(), project.output(), clone);
            }
            return project;
        }
    }

    private static class FoldFilter extends FoldingRule<FilterExec> {
        @Override
        protected PhysicalPlan rule(FilterExec plan) {
            if (plan.child() instanceof EsQueryExec exec) {
                QueryContainer qContainer = exec.queryContainer();

                QueryTranslation qt = toQuery(plan.condition(), plan.isHaving());

                Query query = null;
                if (qContainer.query() != null || qt.query != null) {
                    query = ExpressionTranslators.and(plan.source(), qContainer.query(), qt.query);
                }
                Aggs aggs = addPipelineAggs(qContainer, qt, plan);

                qContainer = new QueryContainer(
                    query,
                    aggs,
                    qContainer.fields(),
                    qContainer.aliases(),
                    qContainer.pseudoFunctions(),
                    qContainer.scalarFunctions(),
                    qContainer.sort(),
                    qContainer.limit(),
                    qContainer.shouldTrackHits(),
                    qContainer.shouldIncludeFrozen(),
                    qContainer.minPageSize(),
                    qContainer.allowPartialSearchResults()
                );

                return exec.with(qContainer);
            }
            return plan;
        }

        private Aggs addPipelineAggs(QueryContainer qContainer, QueryTranslation qt, FilterExec fexec) {
            AggFilter filter = qt.aggFilter;
            Aggs aggs = qContainer.aggs();

            if (filter == null) {
                return qContainer.aggs();
            } else {
                aggs = aggs.addAgg(filter);
            }

            return aggs;
        }
    }

    static class FoldAggregate extends FoldingRule<AggregateExec> {

        static class GroupingContext {
            final Map<Integer, GroupByKey> groupMap;
            final GroupByKey tail;

            GroupingContext(Map<Integer, GroupByKey> groupMap) {
                this.groupMap = groupMap;

                GroupByKey lastAgg = null;
                for (Entry<Integer, GroupByKey> entry : groupMap.entrySet()) {
                    lastAgg = entry.getValue();
                }

                tail = lastAgg;
            }

            GroupByKey groupFor(Expression exp) {
                Integer hash = null;
                if (Functions.isAggregate(exp)) {
                    AggregateFunction f = (AggregateFunction) exp;
                    if (groupMap.isEmpty() == false) {
                        GroupByKey matchingGroup = null;
                        hash = Integer.valueOf(f.field().hashCode());
                        matchingGroup = groupMap.get(hash);
                        return matchingGroup != null ? matchingGroup : tail;
                    } else {
                        return null;
                    }
                }

                hash = Integer.valueOf(exp.hashCode());
                return groupMap.get(hash);
            }

            @Override
            public String toString() {
                return groupMap.toString();
            }
        }

        /**
         * Creates the list of GroupBy keys
         */
        static GroupingContext groupBy(List<? extends Expression> groupings) {
            if (groupings.isEmpty()) {
                return null;
            }

            Map<Integer, GroupByKey> aggMap = new LinkedHashMap<>();

            for (Expression exp : groupings) {
                GroupByKey key = null;

                Integer hash = Integer.valueOf(exp.hashCode());
                String aggId = Expressions.id(exp);

                if (exp instanceof FieldAttribute field) {
                    field = field.exactAttribute();
                    key = new GroupByValue(aggId, field.name());
                }
                else if (exp instanceof Function) {
                    if (exp instanceof DateTimeHistogramFunction dthf) {

                        Expression field = dthf.field();
                        if (field instanceof FieldAttribute) {
                            if (dthf.calendarInterval() != null) {
                                key = new GroupByDateHistogram(aggId, QueryTranslator.nameOf(exp), dthf.calendarInterval(), dthf.zoneId());
                            } else {
                                key = new GroupByDateHistogram(aggId, QueryTranslator.nameOf(exp), dthf.fixedInterval(), dthf.zoneId());
                            }
                        }
                        else if (field instanceof Function) {
                            ScriptTemplate script = ((Function) field).asScript();
                            if (dthf.calendarInterval() != null) {
                                key = new GroupByDateHistogram(aggId, script, dthf.calendarInterval(), dthf.zoneId());
                            } else {
                                key = new GroupByDateHistogram(aggId, script, dthf.fixedInterval(), dthf.zoneId());
                            }
                        }
                    }
                    else if (exp instanceof ScalarFunction sf) {
                        key = new GroupByValue(aggId, sf.asScript());
                    }
                    else if (exp instanceof GroupingFunction) {
                        if (exp instanceof Histogram h) {
                            Expression field = h.field();

                            if (isDateBased(h.dataType())) {
                                Object value = h.interval().value();

                                if (value instanceof IntervalYearMonth
                                    && (((IntervalYearMonth) value).interval().equals(Period.ofYears(1))
                                        || ((IntervalYearMonth) value).interval().equals(Period.ofMonths(1)))) {
                                    Period yearMonth = ((IntervalYearMonth) value).interval();
                                    String calendarInterval = yearMonth.equals(Period.ofYears(1)) ? YEAR_INTERVAL : MONTH_INTERVAL;

                                    if (field instanceof FieldAttribute) {
                                        key = new GroupByDateHistogram(aggId, QueryTranslator.nameOf(field), calendarInterval, h.zoneId());
                                    } else if (field instanceof Function) {
                                        key = new GroupByDateHistogram(aggId, ((Function) field).asScript(), calendarInterval, h.zoneId());
                                    }
                                }
                                else if (value instanceof IntervalDayTime
                                    && ((IntervalDayTime) value).interval().equals(Duration.ofDays(1))) {
                                        if (field instanceof FieldAttribute) {
                                            key = new GroupByDateHistogram(aggId, QueryTranslator.nameOf(field), DAY_INTERVAL, h.zoneId());
                                        } else if (field instanceof Function) {
                                            key = new GroupByDateHistogram(aggId, ((Function) field).asScript(), DAY_INTERVAL, h.zoneId());
                                        }
                                    }
                                else {
                                    long intervalAsMillis = Intervals.inMillis(h.interval());

                                    if (h.dataType() == DATE) {
                                        intervalAsMillis = DateUtils.minDayInterval(intervalAsMillis);
                                    }

                                    if (field instanceof FieldAttribute) {
                                        key = new GroupByDateHistogram(aggId, QueryTranslator.nameOf(field), intervalAsMillis, h.zoneId());
                                    } else if (field instanceof Function) {
                                        key = new GroupByDateHistogram(aggId, ((Function) field).asScript(), intervalAsMillis, h.zoneId());
                                    }
                                }
                            }
                            else {
                                if (field instanceof FieldAttribute || field instanceof Function) {
                                    Double interval = (Double) SqlDataTypeConverter.convert(
                                        Foldables.valueOf(h.interval()),
                                        DataTypes.DOUBLE
                                    );
                                    if (field instanceof FieldAttribute) {
                                        key = new GroupByNumericHistogram(aggId, QueryTranslator.nameOf(field), interval);
                                    } else {
                                        key = new GroupByNumericHistogram(aggId, ((Function) field).asScript(), interval);
                                    }
                                }
                            }
                            if (key == null) {
                                throw new SqlIllegalArgumentException("Unsupported histogram field {}", field);
                            }
                        } else {
                            throw new SqlIllegalArgumentException("Unsupproted grouping function {}", exp);
                        }
                    }
                    else {
                        throw new SqlIllegalArgumentException("Cannot GROUP BY function {}", exp);
                    }
                }
                else {
                    throw new SqlIllegalArgumentException("Cannot GROUP BY {}", exp);
                }

                aggMap.put(hash, key);
            }
            return new GroupingContext(aggMap);
        }

        @Override
        protected PhysicalPlan rule(AggregateExec a) {
            if (a.child() instanceof EsQueryExec exec) {
                return fold(a, exec);
            }
            return a;
        }

        static EsQueryExec fold(AggregateExec a, EsQueryExec exec) {

            QueryContainer queryC = exec.queryContainer();

            String id = null;

            AttributeMap.Builder<Expression> aliases = AttributeMap.builder();
            for (NamedExpression ne : a.aggregates()) {
                if (ne instanceof Alias) {
                    aliases.put(ne.toAttribute(), ((Alias) ne).child());
                }
            }

            if (aliases.build().isEmpty() == false) {
                aliases.putAll(queryC.aliases());
                queryC = queryC.withAliases(aliases.build());
            }

            GroupingContext groupingContext = groupBy(a.groupings());

            if (groupingContext != null) {
                queryC = queryC.addGroups(groupingContext.groupMap.values());
            }

            Map<CompoundNumericAggregate, String> compoundAggMap = new LinkedHashMap<>();

            for (NamedExpression ne : a.aggregates()) {





                Expression target = ne;

                if (ne instanceof Alias) {
                    target = ((Alias) ne).child();
                }

                id = Expressions.id(target);

                if (target.foldable()) {
                    queryC = queryC.addColumn(ne.toAttribute());
                }

                else if (target instanceof Function) {


                    if (target instanceof ScalarFunction f) {
                        Pipe proc = f.asPipe();

                        final AtomicReference<QueryContainer> qC = new AtomicReference<>(queryC);

                        proc = proc.transformUp(p -> {
                            if (p.resolved()) {
                                return p;
                            }

                            Expression exp = p.expression();
                            GroupByKey matchingGroup = null;
                            if (groupingContext != null) {
                                matchingGroup = groupingContext.groupFor(exp);
                            } else {
                                if (exp instanceof ScalarFunction) {
                                    throw new FoldingException(
                                        exp,
                                        "Scalar function " + exp.toString() + " can be used only if included already in grouping"
                                    );
                                }
                            }

                            if (matchingGroup != null) {
                                if (exp instanceof Attribute || exp instanceof ScalarFunction || exp instanceof GroupingFunction) {
                                    Processor action = null;
                                    DataType dataType = exp.dataType();
                                    /*
                                     * special handling of dates since aggs return the typed Date object which needs
                                     * extraction instead of handling this in the scroller, the folder handles this
                                     * as it already got access to the extraction action
                                     */
                                    if (exp instanceof DateTimeHistogramFunction) {
                                        action = ((UnaryPipe) p).action();
                                        dataType = DATETIME;
                                    }
                                    return new AggPathInput(exp.source(), exp, new GroupByRef(matchingGroup.id(), null, dataType), action);
                                }
                            }
                            if (Functions.isAggregate(exp)) {
                                Tuple<QueryContainer, AggPathInput> withFunction = addAggFunction(
                                    matchingGroup,
                                    (AggregateFunction) exp,
                                    compoundAggMap,
                                    qC.get()
                                );
                                qC.set(withFunction.v1());
                                return withFunction.v2();
                            }
                            return p;
                        });

                        if (proc.resolved() == false) {
                            throw new FoldingException(target, "Cannot find grouping for '{}'", Expressions.name(target));
                        }

                        queryC = qC.get().addColumn(new ComputedRef(proc), id, ne.toAttribute());
                    }

                    else {
                        GroupByKey matchingGroup = null;
                        if (groupingContext != null) {
                            matchingGroup = groupingContext.groupFor(target);
                        }
                        if (target instanceof Attribute) {
                            Check.notNull(matchingGroup, "Cannot find group [{}]", Expressions.name(target));
                            queryC = queryC.addColumn(new GroupByRef(matchingGroup.id(), null, target.dataType()), id, ne.toAttribute());
                        }
                        else if (target instanceof GroupingFunction) {
                            queryC = queryC.addColumn(new GroupByRef(matchingGroup.id(), null, target.dataType()), id, ne.toAttribute());
                        }
                        else if (target.foldable()) {
                            queryC = queryC.addColumn(ne.toAttribute());
                        }
                        else {
                            Check.isTrue(
                                Functions.isAggregate(target),
                                "Expected aggregate function inside alias; got [{}]",
                                target.nodeString()
                            );
                            AggregateFunction af = (AggregateFunction) target;
                            Tuple<QueryContainer, AggPathInput> withAgg = addAggFunction(matchingGroup, af, compoundAggMap, queryC);
                            queryC = withAgg.v1().addColumn(withAgg.v2().context(), id, ne.toAttribute());
                        }
                    }

                }
                else {
                    GroupByKey matchingGroup = null;
                    if (groupingContext != null) {
                        target = queryC.aliases().resolve(target, target);
                        id = Expressions.id(target);
                        matchingGroup = groupingContext.groupFor(target);
                        Check.notNull(matchingGroup, "Cannot find group [{}]", Expressions.name(ne));

                        queryC = queryC.addColumn(new GroupByRef(matchingGroup.id(), null, ne.dataType()), id, ne.toAttribute());
                    }
                    else {
                        throw new SqlIllegalArgumentException("Cannot fold aggregate {}", ne);
                    }
                }
            }
            if (a.aggregates().stream().allMatch(e -> e.anyMatch(Expression::foldable))) {
                for (Expression grouping : a.groupings()) {
                    GroupByKey matchingGroup = groupingContext.groupFor(grouping);
                    queryC = queryC.addColumn(new GroupByRef(matchingGroup.id(), null, grouping.dataType()), id, null);
                }
            }
            return new EsQueryExec(exec.source(), exec.index(), a.output(), queryC);
        }

        private static Tuple<QueryContainer, AggPathInput> addAggFunction(
            GroupByKey groupingAgg,
            AggregateFunction f,
            Map<CompoundNumericAggregate, String> compoundAggMap,
            QueryContainer queryC
        ) {

            String functionId = Expressions.id(f);
            if (f instanceof Count c) {
                if (c.field().foldable()) {
                    AggRef ref = null;

                    if (groupingAgg == null) {
                        ref = GlobalCountRef.INSTANCE;
                        queryC = queryC.withTrackHits();
                    } else {
                        ref = new GroupByRef(groupingAgg.id(), Property.COUNT, c.dataType());
                    }

                    Map<String, GroupByKey> pseudoFunctions = new LinkedHashMap<>(queryC.pseudoFunctions());
                    pseudoFunctions.put(functionId, groupingAgg);
                    return new Tuple<>(queryC.withPseudoFunctions(pseudoFunctions), new AggPathInput(f, ref));
                } else if (c.distinct() == false) {
                    LeafAgg leafAgg = toAgg(functionId, f);
                    AggPathInput a = new AggPathInput(f, new MetricAggRef(leafAgg.id(), "doc_count", "_count", null));
                    queryC = queryC.with(queryC.aggs().addAgg(leafAgg));
                    return new Tuple<>(queryC, a);
                }
            }

            AggPathInput aggInput = null;

            if (f instanceof InnerAggregate ia) {
                CompoundNumericAggregate outer = (CompoundNumericAggregate) ia.outer();
                String cAggPath = compoundAggMap.get(outer);

                if (cAggPath == null) {
                    LeafAgg leafAgg = toAgg(Expressions.id(outer), outer);
                    cAggPath = leafAgg.id();
                    compoundAggMap.put(outer, cAggPath);
                    queryC = queryC.with(queryC.aggs().addAgg(leafAgg));
                }

                aggInput = new AggPathInput(
                    f,
                    new MetricAggRef(
                        cAggPath,
                        ia.innerName(),
                        ia.innerKey() != null ? QueryTranslator.nameOf(ia.innerKey()) : null,
                        ia.dataType()
                    )
                );
            } else {
                LeafAgg leafAgg = toAgg(functionId, f);
                if (f instanceof TopHits) {
                    aggInput = new AggPathInput(f, new TopHitsAggRef(leafAgg.id(), f.dataType()));
                } else {
                    aggInput = new AggPathInput(f, new MetricAggRef(leafAgg.id(), f.dataType()));
                }
                queryC = queryC.with(queryC.aggs().addAgg(leafAgg));
            }

            return new Tuple<>(queryC, aggInput);
        }
    }

    private static class FoldOrderBy extends FoldingRule<OrderExec> {
        @Override
        protected PhysicalPlan rule(OrderExec plan) {
            if (plan.child() instanceof EsQueryExec exec) {
                QueryContainer qContainer = exec.queryContainer();

                ListIterator<Order> it = plan.order().listIterator(plan.order().size());
                while (it.hasPrevious()) {
                    Order order = it.previous();

                    Direction direction = Direction.from(order.direction());
                    Missing missing = Missing.from(order.nullsPosition());

                    Expression orderExpression = order.child();

                    if (orderExpression instanceof ReferenceAttribute) {
                        orderExpression = qContainer.aliases().resolve(orderExpression);
                    }
                    String lookup = Expressions.id(orderExpression);
                    GroupByKey group = qContainer.findGroupForAgg(lookup);

                    if (group != null && group != Aggs.IMPLICIT_GROUP_KEY) {
                        qContainer = qContainer.updateGroup(group.with(direction, missing));
                    }

                    if (orderExpression instanceof FieldAttribute) {
                        qContainer = qContainer.prependSort(
                            lookup,
                            new AttributeSort((FieldAttribute) orderExpression, direction, missing)
                        );
                    }
                    else if (orderExpression instanceof ScalarFunction sf) {
                        qContainer = qContainer.prependSort(lookup, new ScriptSort(sf.asScript(), direction, missing));
                    }
                    else if (orderExpression instanceof Histogram) {
                        qContainer = qContainer.prependSort(lookup, new GroupingFunctionSort(direction, missing));
                    }
                    else if (orderExpression instanceof Score) {
                        qContainer = qContainer.prependSort(lookup, new ScoreSort(direction, missing));
                    }
                    else if (orderExpression instanceof AggregateFunction) {
                        qContainer = qContainer.prependSort(
                            lookup,
                            new AggregateSort((AggregateFunction) orderExpression, direction, missing)
                        );
                    }
                    else {
                        throw new SqlIllegalArgumentException("unsupported sorting expression {}", orderExpression);
                    }
                }

                return exec.with(qContainer);
            }
            return plan;
        }
    }

    private static class FoldLimit extends FoldingRule<LimitExec> {

        @Override
        protected PhysicalPlan rule(LimitExec plan) {
            if (plan.child() instanceof EsQueryExec exec) {
                int limit = (Integer) SqlDataTypeConverter.convert(Foldables.valueOf(plan.limit()), DataTypes.INTEGER);
                int currentSize = exec.queryContainer().limit();
                int newSize = currentSize < 0 ? limit : Math.min(currentSize, limit);
                return exec.with(exec.queryContainer().withLimit(newSize));
            }
            return plan;
        }
    }

    private static class PlanOutputToQueryRef extends FoldingRule<EsQueryExec> {
        @Override
        protected PhysicalPlan rule(EsQueryExec exec) {
            QueryContainer qContainer = exec.queryContainer();

            if (qContainer.hasColumns()) {
                return exec;
            }

            for (Attribute attr : exec.output()) {
                qContainer = qContainer.addColumn(attr);
            }

            return exec.with(qContainer);
        }
    }

    private static class FoldPivot extends FoldingRule<PivotExec> {

        @Override
        protected PhysicalPlan rule(PivotExec plan) {
            if (plan.child() instanceof EsQueryExec exec) {
                Pivot p = plan.pivot();
                EsQueryExec fold = FoldAggregate.fold(
                    new AggregateExec(plan.source(), exec, new ArrayList<>(p.groupingSet()), combine(p.groupingSet(), p.aggregates())),
                    exec
                );

                QueryContainer query = fold.queryContainer();

                List<QueryContainer.FieldInfo> fields = new ArrayList<>(query.fields());
                int startingIndex = fields.size() - p.aggregates().size() - 1;
                QueryContainer.FieldInfo groupField = fields.remove(startingIndex);
                AttributeMap<Literal> values = p.valuesToLiterals();

                for (int i = startingIndex; i < fields.size(); i++) {
                    QueryContainer.FieldInfo field = fields.remove(i);
                    for (Map.Entry<Attribute, Literal> entry : values.entrySet()) {
                        fields.add(
                            new QueryContainer.FieldInfo(
                                new PivotColumnRef(groupField.extraction(), field.extraction(), entry.getValue().value()),
                                Expressions.id(entry.getKey()),
                                entry.getKey()
                            )
                        );
                    }
                    i += values.size();
                }

                return fold.with(
                    new QueryContainer(
                        query.query(),
                        query.aggs(),
                        fields,
                        query.aliases(),
                        query.pseudoFunctions(),
                        query.scalarFunctions(),
                        query.sort(),
                        query.limit(),
                        query.shouldTrackHits(),
                        query.shouldIncludeFrozen(),
                        values.size(),
                        query.allowPartialSearchResults()
                    )
                );
            }
            return plan;
        }
    }


    private static class PropagateEmptyLocal extends FoldingRule<PhysicalPlan> {

        @Override
        protected PhysicalPlan rule(PhysicalPlan plan) {
            if (plan.children().size() == 1) {
                PhysicalPlan p = plan.children().get(0);
                if (p instanceof LocalExec) {
                    if (((LocalExec) p).isEmpty()) {
                        return new LocalExec(plan.source(), new EmptyExecutable(plan.output()));
                    } else {
                        throw new SqlIllegalArgumentException("Encountered a bug; {} is a LocalExec but is not empty", p);
                    }
                }
            }
            return plan;
        }
    }

    private static class LocalLimit extends FoldingRule<LimitExec> {

        @Override
        protected PhysicalPlan rule(LimitExec plan) {
            if (plan.child() instanceof LocalExec) {
                return plan.child();
            }
            return plan;
        }
    }

    abstract static class FoldingRule<SubPlan extends PhysicalPlan> extends Rule<SubPlan, PhysicalPlan> {

        @Override
        public final PhysicalPlan apply(PhysicalPlan plan) {
            return plan.transformUp(typeToken(), this::rule);
        }

        protected abstract PhysicalPlan rule(SubPlan plan);
    }
}
