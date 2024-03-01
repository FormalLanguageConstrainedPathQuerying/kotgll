/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.session;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.fieldcaps.FieldCapabilities;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.logging.LogManager;
import org.elasticsearch.logging.Logger;
import org.elasticsearch.xpack.esql.action.EsqlQueryRequest;
import org.elasticsearch.xpack.esql.analysis.Analyzer;
import org.elasticsearch.xpack.esql.analysis.AnalyzerContext;
import org.elasticsearch.xpack.esql.analysis.EnrichResolution;
import org.elasticsearch.xpack.esql.analysis.PreAnalyzer;
import org.elasticsearch.xpack.esql.analysis.Verifier;
import org.elasticsearch.xpack.esql.enrich.EnrichPolicyResolver;
import org.elasticsearch.xpack.esql.enrich.ResolvedEnrichPolicy;
import org.elasticsearch.xpack.esql.optimizer.LogicalPlanOptimizer;
import org.elasticsearch.xpack.esql.optimizer.PhysicalOptimizerContext;
import org.elasticsearch.xpack.esql.optimizer.PhysicalPlanOptimizer;
import org.elasticsearch.xpack.esql.parser.EsqlParser;
import org.elasticsearch.xpack.esql.parser.TypedParamValue;
import org.elasticsearch.xpack.esql.plan.logical.Enrich;
import org.elasticsearch.xpack.esql.plan.logical.Keep;
import org.elasticsearch.xpack.esql.plan.logical.RegexExtract;
import org.elasticsearch.xpack.esql.plan.physical.EstimatesRowSize;
import org.elasticsearch.xpack.esql.plan.physical.FragmentExec;
import org.elasticsearch.xpack.esql.plan.physical.PhysicalPlan;
import org.elasticsearch.xpack.esql.planner.Mapper;
import org.elasticsearch.xpack.ql.analyzer.TableInfo;
import org.elasticsearch.xpack.ql.expression.Alias;
import org.elasticsearch.xpack.ql.expression.Attribute;
import org.elasticsearch.xpack.ql.expression.AttributeSet;
import org.elasticsearch.xpack.ql.expression.EmptyAttribute;
import org.elasticsearch.xpack.ql.expression.MetadataAttribute;
import org.elasticsearch.xpack.ql.expression.UnresolvedStar;
import org.elasticsearch.xpack.ql.expression.function.FunctionRegistry;
import org.elasticsearch.xpack.ql.index.IndexResolution;
import org.elasticsearch.xpack.ql.index.IndexResolver;
import org.elasticsearch.xpack.ql.index.MappingException;
import org.elasticsearch.xpack.ql.plan.TableIdentifier;
import org.elasticsearch.xpack.ql.plan.logical.Aggregate;
import org.elasticsearch.xpack.ql.plan.logical.LogicalPlan;
import org.elasticsearch.xpack.ql.plan.logical.Project;
import org.elasticsearch.xpack.ql.type.InvalidMappedField;
import org.elasticsearch.xpack.ql.util.Holder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.xpack.ql.index.IndexResolver.UNMAPPED;
import static org.elasticsearch.xpack.ql.util.ActionListeners.map;
import static org.elasticsearch.xpack.ql.util.StringUtils.WILDCARD;

public class EsqlSession {

    private static final Logger LOGGER = LogManager.getLogger(EsqlSession.class);

    private final String sessionId;
    private final EsqlConfiguration configuration;
    private final IndexResolver indexResolver;
    private final EnrichPolicyResolver enrichPolicyResolver;

    private final PreAnalyzer preAnalyzer;
    private final Verifier verifier;
    private final FunctionRegistry functionRegistry;
    private final LogicalPlanOptimizer logicalPlanOptimizer;

    private final Mapper mapper;
    private final PhysicalPlanOptimizer physicalPlanOptimizer;

    public EsqlSession(
        String sessionId,
        EsqlConfiguration configuration,
        IndexResolver indexResolver,
        EnrichPolicyResolver enrichPolicyResolver,
        PreAnalyzer preAnalyzer,
        FunctionRegistry functionRegistry,
        LogicalPlanOptimizer logicalPlanOptimizer,
        Mapper mapper,
        Verifier verifier
    ) {
        this.sessionId = sessionId;
        this.configuration = configuration;
        this.indexResolver = indexResolver;
        this.enrichPolicyResolver = enrichPolicyResolver;
        this.preAnalyzer = preAnalyzer;
        this.verifier = verifier;
        this.functionRegistry = functionRegistry;
        this.mapper = mapper;
        this.logicalPlanOptimizer = logicalPlanOptimizer;
        this.physicalPlanOptimizer = new PhysicalPlanOptimizer(new PhysicalOptimizerContext(configuration));
    }

    public String sessionId() {
        return sessionId;
    }

    public void execute(EsqlQueryRequest request, ActionListener<PhysicalPlan> listener) {
        LOGGER.debug("ESQL query:\n{}", request.query());
        optimizedPhysicalPlan(
            parse(request.query(), request.params()),
            listener.map(plan -> EstimatesRowSize.estimateRowSize(0, plan.transformUp(FragmentExec.class, f -> {
                QueryBuilder filter = request.filter();
                if (filter != null) {
                    var fragmentFilter = f.esFilter();
                    filter = fragmentFilter != null ? boolQuery().filter(fragmentFilter).must(filter) : filter;
                    LOGGER.debug("Fold filter {} to EsQueryExec", filter);
                    f = new FragmentExec(f.source(), f.fragment(), filter, f.estimatedRowSize());
                }
                return f;
            })))
        );
    }

    private LogicalPlan parse(String query, List<TypedParamValue> params) {
        var parsed = new EsqlParser().createStatement(query, params);
        LOGGER.debug("Parsed logical plan:\n{}", parsed);
        return parsed;
    }

    public void analyzedPlan(LogicalPlan parsed, ActionListener<LogicalPlan> listener) {
        if (parsed.analyzed()) {
            listener.onResponse(parsed);
            return;
        }

        preAnalyze(parsed, (indices, policies) -> {
            Analyzer analyzer = new Analyzer(new AnalyzerContext(configuration, functionRegistry, indices, policies), verifier);
            var plan = analyzer.analyze(parsed);
            LOGGER.debug("Analyzed plan:\n{}", plan);
            return plan;
        }, listener);
    }

    private <T> void preAnalyze(LogicalPlan parsed, BiFunction<IndexResolution, EnrichResolution, T> action, ActionListener<T> listener) {
        PreAnalyzer.PreAnalysis preAnalysis = preAnalyzer.preAnalyze(parsed);
        var unresolvedPolicies = preAnalysis.enriches.stream()
            .map(e -> new EnrichPolicyResolver.UnresolvedPolicy((String) e.policyName().fold(), e.mode()))
            .collect(Collectors.toSet());
        final Set<String> targetClusters = enrichPolicyResolver.groupIndicesPerCluster(
            preAnalysis.indices.stream()
                .flatMap(t -> Arrays.stream(Strings.commaDelimitedListToStringArray(t.id().index())))
                .toArray(String[]::new)
        ).keySet();
        enrichPolicyResolver.resolvePolicies(targetClusters, unresolvedPolicies, listener.delegateFailureAndWrap((l, enrichResolution) -> {
            var matchFields = enrichResolution.resolvedEnrichPolicies()
                .stream()
                .map(ResolvedEnrichPolicy::matchField)
                .collect(Collectors.toSet());
            preAnalyzeIndices(parsed, l.delegateFailureAndWrap((ll, indexResolution) -> {
                if (indexResolution.isValid()) {
                    Set<String> newClusters = enrichPolicyResolver.groupIndicesPerCluster(
                        indexResolution.get().concreteIndices().toArray(String[]::new)
                    ).keySet();
                    if (targetClusters.containsAll(newClusters) == false) {
                        enrichPolicyResolver.resolvePolicies(
                            newClusters,
                            unresolvedPolicies,
                            ll.map(newEnrichResolution -> action.apply(indexResolution, newEnrichResolution))
                        );
                        return;
                    }
                }
                ll.onResponse(action.apply(indexResolution, enrichResolution));
            }), matchFields);
        }));
    }

    private <T> void preAnalyzeIndices(LogicalPlan parsed, ActionListener<IndexResolution> listener, Set<String> enrichPolicyMatchFields) {
        PreAnalyzer.PreAnalysis preAnalysis = new PreAnalyzer().preAnalyze(parsed);
        if (preAnalysis.indices.size() > 1) {
            listener.onFailure(new MappingException("Queries with multiple indices are not supported"));
        } else if (preAnalysis.indices.size() == 1) {
            TableInfo tableInfo = preAnalysis.indices.get(0);
            TableIdentifier table = tableInfo.id();
            var fieldNames = fieldNames(parsed, enrichPolicyMatchFields);

            indexResolver.resolveAsMergedMapping(
                table.index(),
                fieldNames,
                false,
                Map.of(),
                listener,
                EsqlSession::specificValidity,
                IndexResolver.PRESERVE_PROPERTIES,
                IndexResolver.INDEX_METADATA_FIELD
            );
        } else {
            try {
                listener.onResponse(IndexResolution.invalid("[none specified]"));
            } catch (Exception ex) {
                listener.onFailure(ex);
            }
        }
    }

    static Set<String> fieldNames(LogicalPlan parsed, Set<String> enrichPolicyMatchFields) {
        if (false == parsed.anyMatch(plan -> plan instanceof Aggregate || plan instanceof Project)) {
            return IndexResolver.ALL_FIELDS;
        }

        Holder<Boolean> projectAll = new Holder<>(false);
        parsed.forEachExpressionDown(UnresolvedStar.class, us -> {
            if (projectAll.get()) {
                return;
            }
            projectAll.set(true);
        });
        if (projectAll.get()) {
            return IndexResolver.ALL_FIELDS;
        }

        AttributeSet references = new AttributeSet();
        AttributeSet keepCommandReferences = new AttributeSet();

        parsed.forEachDown(p -> {
            if (p instanceof RegexExtract re) { 
                AttributeSet dissectRefs = p.references();
                dissectRefs.removeAll(re.extractedFields());
                references.addAll(dissectRefs);
                for (Attribute extracted : re.extractedFields()) {
                    references.removeIf(attr -> matchByName(attr, extracted.qualifiedName(), false));
                }
            } else if (p instanceof Enrich) {
                AttributeSet enrichRefs = p.references();
                enrichRefs.removeIf(attr -> attr instanceof EmptyAttribute);
                references.addAll(enrichRefs);
            } else {
                references.addAll(p.references());
                if (p instanceof Keep) {
                    keepCommandReferences.addAll(p.references());
                }
            }

            p.forEachExpressionDown(Alias.class, alias -> {
                if (p.references().names().contains(alias.qualifiedName())) {
                    return;
                }
                references.removeIf(attr -> matchByName(attr, alias.qualifiedName(), keepCommandReferences.contains(attr)));
            });
        });

        references.removeIf(a -> a instanceof MetadataAttribute || MetadataAttribute.isSupported(a.qualifiedName()));
        Set<String> fieldNames = references.names();
        if (fieldNames.isEmpty() && enrichPolicyMatchFields.isEmpty()) {
            return IndexResolver.INDEX_METADATA_FIELD;
        } else {
            fieldNames.addAll(subfields(fieldNames));
            fieldNames.addAll(enrichPolicyMatchFields);
            fieldNames.addAll(subfields(enrichPolicyMatchFields));
            return fieldNames;
        }
    }

    private static boolean matchByName(Attribute attr, String other, boolean skipIfPattern) {
        boolean isPattern = Regex.isSimpleMatchPattern(attr.qualifiedName());
        if (skipIfPattern && isPattern) {
            return false;
        }
        return isPattern ? Regex.simpleMatch(attr.qualifiedName(), other) : attr.qualifiedName().equals(other);
    }

    private static Set<String> subfields(Set<String> names) {
        return names.stream().filter(name -> name.endsWith(WILDCARD) == false).map(name -> name + ".*").collect(Collectors.toSet());
    }

    public void optimizedPlan(LogicalPlan logicalPlan, ActionListener<LogicalPlan> listener) {
        analyzedPlan(logicalPlan, map(listener, p -> {
            var plan = logicalPlanOptimizer.optimize(p);
            LOGGER.debug("Optimized logicalPlan plan:\n{}", plan);
            return plan;
        }));
    }

    public void physicalPlan(LogicalPlan optimized, ActionListener<PhysicalPlan> listener) {
        optimizedPlan(optimized, map(listener, p -> {
            var plan = mapper.map(p);
            LOGGER.debug("Physical plan:\n{}", plan);
            return plan;
        }));
    }

    public void optimizedPhysicalPlan(LogicalPlan logicalPlan, ActionListener<PhysicalPlan> listener) {
        physicalPlan(logicalPlan, map(listener, p -> {
            var plan = physicalPlanOptimizer.optimize(p);
            LOGGER.debug("Optimized physical plan:\n{}", plan);
            return plan;
        }));
    }

    public static InvalidMappedField specificValidity(String fieldName, Map<String, FieldCapabilities> types) {
        boolean hasUnmapped = types.containsKey(UNMAPPED);
        boolean hasTypeConflicts = types.size() > (hasUnmapped ? 2 : 1);
        String metricConflictsTypeName = null;
        boolean hasMetricConflicts = false;

        if (hasTypeConflicts == false) {
            for (Map.Entry<String, FieldCapabilities> type : types.entrySet()) {
                if (UNMAPPED.equals(type.getKey())) {
                    continue;
                }
                if (type.getValue().metricConflictsIndices() != null && type.getValue().metricConflictsIndices().length > 0) {
                    hasMetricConflicts = true;
                    metricConflictsTypeName = type.getKey();
                    break;
                }
            }
        }

        InvalidMappedField result = null;
        if (hasMetricConflicts) {
            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append(
                "mapped as different metric types in indices: ["
                    + String.join(", ", types.get(metricConflictsTypeName).metricConflictsIndices())
                    + "]"
            );
            result = new InvalidMappedField(fieldName, errorMessage.toString());
        }
        return result;
    };
}
