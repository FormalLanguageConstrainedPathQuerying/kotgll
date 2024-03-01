package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link CountDistinctBooleanAggregator}.
 * This class is generated. Do not edit it.
 */
public final class CountDistinctBooleanAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  public CountDistinctBooleanAggregatorFunctionSupplier(List<Integer> channels) {
    this.channels = channels;
  }

  @Override
  public CountDistinctBooleanAggregatorFunction aggregator(DriverContext driverContext) {
    return CountDistinctBooleanAggregatorFunction.create(driverContext, channels);
  }

  @Override
  public CountDistinctBooleanGroupingAggregatorFunction groupingAggregator(
      DriverContext driverContext) {
    return CountDistinctBooleanGroupingAggregatorFunction.create(channels, driverContext);
  }

  @Override
  public String describe() {
    return "count_distinct of booleans";
  }
}
