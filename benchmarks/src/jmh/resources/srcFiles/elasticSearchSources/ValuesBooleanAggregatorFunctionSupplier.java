package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link ValuesBooleanAggregator}.
 * This class is generated. Do not edit it.
 */
public final class ValuesBooleanAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  public ValuesBooleanAggregatorFunctionSupplier(List<Integer> channels) {
    this.channels = channels;
  }

  @Override
  public ValuesBooleanAggregatorFunction aggregator(DriverContext driverContext) {
    return ValuesBooleanAggregatorFunction.create(driverContext, channels);
  }

  @Override
  public ValuesBooleanGroupingAggregatorFunction groupingAggregator(DriverContext driverContext) {
    return ValuesBooleanGroupingAggregatorFunction.create(channels, driverContext);
  }

  @Override
  public String describe() {
    return "values of booleans";
  }
}
