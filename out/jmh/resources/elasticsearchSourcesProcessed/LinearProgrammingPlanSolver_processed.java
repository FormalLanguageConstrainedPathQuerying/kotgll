/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.inference.assignment.planning;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.core.SuppressForbidden;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.xpack.ml.inference.assignment.planning.AssignmentPlan.Deployment;
import org.elasticsearch.xpack.ml.inference.assignment.planning.AssignmentPlan.Node;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.ojalgo.structure.Access1D;
import org.ojalgo.type.CalendarDateDuration;
import org.ojalgo.type.CalendarDateUnit;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An allocation plan solver based on linear programming.
 * This solver uses the linear programming solver from the ojalgo library.
 */
class LinearProgrammingPlanSolver {

    private static final Logger logger = LogManager.getLogger(LinearProgrammingPlanSolver.class);

    private static final long RANDOMIZATION_SEED = 738921734L;
    private static final double L1 = 0.9; 
    private static final double INITIAL_W = 0.2;
    private static final int RANDOMIZED_ROUNDING_ROUNDS = 20; 

    /**
     * Ojalgo solver may throw a OOM exception if the problem is too large.
     * When the solver is dense, a 2D double array is created with size of complexity {@link #memoryComplexity()}.
     * We empirically determine a threshold for complexity after which we switch the solver to sparse mode
     * which consumes significantly less memory at the cost of speed.
     */
    private static final int MEMORY_COMPLEXITY_SPARSE_THRESHOLD = 4_000_000;

    /**
     * Ojalgo solver may throw a OOM exception if the problem is too large.
     * When the solver is dense, a 2D double array is created with size of complexity {@link #memoryComplexity()}.
     * Using the same function for when the solver is sparse we empirically determine a threshold after which
     * we do not invoke the solver at all and fall back to our bin packing solution.
     */
    private static final int MEMORY_COMPLEXITY_LIMIT = 10_000_000;

    private final Random random = new Random(RANDOMIZATION_SEED);

    private final List<Node> nodes;
    private final List<AssignmentPlan.Deployment> deployments;
    private final Map<Node, Double> normalizedMemoryPerNode;
    private final Map<Node, Integer> coresPerNode;
    private final Map<AssignmentPlan.Deployment, Double> normalizedMemoryPerModel;
    private final Map<AssignmentPlan.Deployment, Double> normalizedMemoryPerAllocation;
    private final Map<AssignmentPlan.Deployment, Double> normalizedMinimumDeploymentMemoryRequired;

    private final int maxNodeCores;
    private final long maxModelMemoryBytes;

    LinearProgrammingPlanSolver(List<Node> nodes, List<AssignmentPlan.Deployment> deployments) {
        this.nodes = nodes;
        maxNodeCores = this.nodes.stream().map(Node::cores).max(Integer::compareTo).orElse(0);

        long maxNodeMemory = nodes.stream().map(Node::availableMemoryBytes).max(Long::compareTo).orElse(0L);
        this.deployments = deployments.stream()
            .filter(m -> m.currentAllocationsByNodeId().isEmpty() == false || m.memoryBytes() <= maxNodeMemory)
            .filter(m -> m.threadsPerAllocation() <= maxNodeCores)
            .toList();

        maxModelMemoryBytes = this.deployments.stream().map(m -> m.minimumMemoryRequiredBytes()).max(Long::compareTo).orElse(1L);
        normalizedMemoryPerNode = this.nodes.stream()
            .collect(Collectors.toMap(Function.identity(), n -> n.availableMemoryBytes() / (double) maxModelMemoryBytes));
        coresPerNode = this.nodes.stream().collect(Collectors.toMap(Function.identity(), Node::cores));
        normalizedMemoryPerModel = this.deployments.stream()
            .collect(Collectors.toMap(Function.identity(), m -> m.estimateMemoryUsageBytes(0) / (double) maxModelMemoryBytes));
        normalizedMemoryPerAllocation = this.deployments.stream()
            .collect(Collectors.toMap(Function.identity(), m -> m.perAllocationMemoryBytes() / (double) maxModelMemoryBytes));
        normalizedMinimumDeploymentMemoryRequired = this.deployments.stream()
            .collect(Collectors.toMap(Function.identity(), m -> m.minimumMemoryRequiredBytes() / (double) maxModelMemoryBytes));
    }

    AssignmentPlan solvePlan(boolean useBinPackingOnly) {
        if (deployments.isEmpty() || maxNodeCores == 0) {
            return AssignmentPlan.builder(nodes, deployments).build();
        }

        Tuple<Map<Tuple<AssignmentPlan.Deployment, Node>, Double>, AssignmentPlan> weightsAndBinPackingPlan =
            calculateWeightsAndBinPackingPlan();

        if (useBinPackingOnly) {
            return weightsAndBinPackingPlan.v2();
        }

        Map<Tuple<AssignmentPlan.Deployment, Node>, Double> allocationValues = new HashMap<>();
        Map<Tuple<AssignmentPlan.Deployment, Node>, Double> assignmentValues = new HashMap<>();
        if (solveLinearProgram(weightsAndBinPackingPlan.v1(), allocationValues, assignmentValues) == false) {
            return weightsAndBinPackingPlan.v2();
        }

        RandomizedAssignmentRounding randomizedRounding = new RandomizedAssignmentRounding(
            random,
            RANDOMIZED_ROUNDING_ROUNDS,
            nodes,
            deployments
        );
        AssignmentPlan assignmentPlan = randomizedRounding.computePlan(allocationValues, assignmentValues);
        AssignmentPlan binPackingPlan = weightsAndBinPackingPlan.v2();
        if (binPackingPlan.compareTo(assignmentPlan) > 0) {
            assignmentPlan = binPackingPlan;
            logger.debug(() -> "Best plan is from bin packing");
        } else {
            logger.debug(() -> "Best plan is from LP solver");
        }

        return assignmentPlan;
    }

    private double weightForAllocationVar(
        AssignmentPlan.Deployment m,
        Node n,
        Map<Tuple<AssignmentPlan.Deployment, Node>, Double> weights
    ) {
        return (1 + weights.get(Tuple.tuple(m, n)) - (m.minimumMemoryRequiredBytes() > n.availableMemoryBytes() ? 10 : 0)) - L1
            * normalizedMemoryPerModel.get(m) / maxNodeCores;
    }

    private Tuple<Map<Tuple<Deployment, Node>, Double>, AssignmentPlan> calculateWeightsAndBinPackingPlan() {
        logger.debug(() -> "Calculating weights and bin packing plan");

        double w = INITIAL_W;
        double dw = w / nodes.size() / deployments.size();

        Map<Tuple<Deployment, Node>, Double> weights = new HashMap<>();
        AssignmentPlan.Builder assignmentPlan = AssignmentPlan.builder(nodes, deployments);

        for (AssignmentPlan.Deployment m : deployments.stream()
            .sorted(Comparator.comparingDouble(this::descendingSizeAnyFitsModelOrder))
            .toList()) {
            double lastW;
            do {
                lastW = w;
                List<Node> orderedNodes = nodes.stream()
                    .sorted(Comparator.comparingDouble(n -> descendingSizeAnyFitsNodeOrder(n, m, assignmentPlan)))
                    .toList();
                for (Node n : orderedNodes) {
                    int allocations = m.findOptimalAllocations(
                        Math.min(assignmentPlan.getRemainingCores(n) / m.threadsPerAllocation(), assignmentPlan.getRemainingAllocations(m)),
                        assignmentPlan.getRemainingMemory(n)
                    );
                    if (allocations > 0 && assignmentPlan.canAssign(m, n, allocations)) {
                        assignmentPlan.assignModelToNode(m, n, allocations);
                        weights.put(Tuple.tuple(m, n), w);
                        w -= dw;
                        break;
                    }
                }
            } while (lastW != w && assignmentPlan.getRemainingAllocations(m) > 0);
        }

        final double finalW = w;
        for (Deployment m : deployments) {
            for (Node n : nodes) {
                weights.computeIfAbsent(Tuple.tuple(m, n), key -> random.nextDouble(minWeight(m, n, finalW), maxWeight(m, n, finalW)));
            }
        }

        logger.trace(() -> "Weights = " + weights);
        AssignmentPlan binPackingPlan = assignmentPlan.build();
        logger.debug(() -> "Bin packing plan =\n" + binPackingPlan.prettyPrint());

        return Tuple.tuple(weights, binPackingPlan);
    }

    private double descendingSizeAnyFitsModelOrder(AssignmentPlan.Deployment m) {
        return (m.currentAllocationsByNodeId().isEmpty() ? 1 : 2) * -normalizedMinimumDeploymentMemoryRequired.get(m) * m
            .threadsPerAllocation();
    }

    private double descendingSizeAnyFitsNodeOrder(Node n, AssignmentPlan.Deployment m, AssignmentPlan.Builder assignmentPlan) {
        return (m.currentAllocationsByNodeId().containsKey(n.id()) ? 0 : 1) + (assignmentPlan.getRemainingCores(n) >= assignmentPlan
            .getRemainingThreads(m) ? 0 : 1) + (0.01 * distance(assignmentPlan.getRemainingCores(n), assignmentPlan.getRemainingThreads(m)))
            - (0.01 * normalizedMemoryPerNode.get(n));
    }

    @SuppressForbidden(reason = "Math#abs(int) is safe here as we protect against MIN_VALUE")
    private static int distance(int x, int y) {
        int distance = x - y;
        return distance == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(distance);
    }

    private static double minWeight(Deployment m, Node n, double w) {
        return m.currentAllocationsByNodeId().containsKey(n.id()) ? w / 2 : 0;
    }

    private static double maxWeight(Deployment m, Node n, double w) {
        return m.currentAllocationsByNodeId().containsKey(n.id()) ? w : w / 2;
    }

    private boolean solveLinearProgram(
        Map<Tuple<AssignmentPlan.Deployment, Node>, Double> weights,
        Map<Tuple<Deployment, Node>, Double> allocationValues,
        Map<Tuple<AssignmentPlan.Deployment, Node>, Double> assignmentValues
    ) {
        if (memoryComplexity() > MEMORY_COMPLEXITY_LIMIT) {
            logger.debug(() -> "Problem size to big to solve with linear programming; falling back to bin packing solution");
            return false;
        }


        Optimisation.Options options = new Optimisation.Options().abort(new CalendarDateDuration(10, CalendarDateUnit.SECOND));
        if (memoryComplexity() > MEMORY_COMPLEXITY_SPARSE_THRESHOLD) {
            logger.debug(() -> "Problem size is large enough to switch to sparse solver");
            options.sparse = true;
        }
        ExpressionsBasedModel model = new ExpressionsBasedModel(options);

        Map<Tuple<Deployment, Node>, Variable> allocationVars = new HashMap<>();

        for (AssignmentPlan.Deployment m : deployments) {
            for (Node n : nodes) {
                Variable allocationVar = model.addVariable("allocations_of_model_" + m.id() + "_on_node_" + n.id())
                    .integer(false) 
                    .lower(0.0) 
                    .weight(weightForAllocationVar(m, n, weights));
                allocationVars.put(Tuple.tuple(m, n), allocationVar);
            }
        }

        for (Deployment m : deployments) {
            model.addExpression("allocations_of_model_" + m.id() + "_not_more_than_required")
                .lower(m.getCurrentAssignedAllocations())
                .upper(m.allocations())
                .setLinearFactorsSimple(varsForModel(m, allocationVars));
        }

        double[] threadsPerAllocationPerModel = deployments.stream().mapToDouble(m -> m.threadsPerAllocation()).toArray();
        for (Node n : nodes) {
            model.addExpression("threads_on_node_" + n.id() + "_not_more_than_cores")
                .upper(coresPerNode.get(n))
                .setLinearFactors(varsForNode(n, allocationVars), Access1D.wrap(threadsPerAllocationPerModel));
        }

        for (Node n : nodes) {
            List<Variable> allocations = new ArrayList<>();
            List<Double> modelMemories = new ArrayList<>();
            deployments.stream().filter(m -> m.currentAllocationsByNodeId().containsKey(n.id()) == false).forEach(m -> {
                allocations.add(allocationVars.get(Tuple.tuple(m, n)));
                modelMemories.add(
                    (normalizedMemoryPerModel.get(m) / (double) coresPerNode.get(n) + normalizedMemoryPerAllocation.get(m)) * m
                        .threadsPerAllocation()
                );
            });
            model.addExpression("used_memory_on_node_" + n.id() + "_not_more_than_available")
                .upper(normalizedMemoryPerNode.get(n))
                .setLinearFactors(allocations, Access1D.wrap(modelMemories));
        }

        Optimisation.Result result = privilegedModelMaximise(model);

        if (result.getState().isFeasible() == false) {
            logger.debug("Linear programming solution state [{}] is not feasible", result.getState());
            return false;
        }

        for (AssignmentPlan.Deployment m : deployments) {
            for (Node n : nodes) {
                Tuple<AssignmentPlan.Deployment, Node> assignment = Tuple.tuple(m, n);
                allocationValues.put(assignment, allocationVars.get(assignment).getValue().doubleValue());
                assignmentValues.put(
                    assignment,
                    allocationVars.get(assignment).getValue().doubleValue() * m.threadsPerAllocation() / (double) coresPerNode.get(n)
                );

            }
        }
        logger.debug(() -> "LP solver result =\n" + prettyPrintSolverResult(assignmentValues, allocationValues));
        return true;
    }

    @SuppressWarnings("removal")
    private static Optimisation.Result privilegedModelMaximise(ExpressionsBasedModel model) {
        return AccessController.doPrivileged((PrivilegedAction<Optimisation.Result>) () -> model.maximise());
    }

    private int memoryComplexity() {
        return (nodes.size() + deployments.size()) * nodes.size() * deployments.size();
    }

    private List<Variable> varsForModel(AssignmentPlan.Deployment m, Map<Tuple<Deployment, Node>, Variable> vars) {
        return nodes.stream().map(n -> vars.get(Tuple.tuple(m, n))).toList();
    }

    private List<Variable> varsForNode(Node n, Map<Tuple<AssignmentPlan.Deployment, Node>, Variable> vars) {
        return deployments.stream().map(m -> vars.get(Tuple.tuple(m, n))).toList();
    }

    private String prettyPrintSolverResult(
        Map<Tuple<AssignmentPlan.Deployment, Node>, Double> assignmentValues,
        Map<Tuple<AssignmentPlan.Deployment, Node>, Double> threadValues
    ) {
        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            msg.append(n + " ->");
            for (Deployment m : deployments) {
                if (threadValues.get(Tuple.tuple(m, n)) > 0) {
                    msg.append(" ");
                    msg.append(m.id());
                    msg.append(" (mem = ");
                    msg.append(ByteSizeValue.ofBytes(m.memoryBytes()));
                    msg.append(") (allocations = ");
                    msg.append(threadValues.get(Tuple.tuple(m, n)));
                    msg.append("/");
                    msg.append(m.allocations());
                    msg.append(") (y = ");
                    msg.append(assignmentValues.get(Tuple.tuple(m, n)));
                    msg.append(")");
                }
            }
            if (i < nodes.size() - 1) {
                msg.append('\n');
            }
        }
        return msg.toString();
    }
}
