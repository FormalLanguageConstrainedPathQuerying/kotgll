package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link CountDistinctDoubleAggregator}.
 * This class is generated. Do not edit it.
 */
public final class CountDistinctDoubleAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  private final int precision;

  public CountDistinctDoubleAggregatorFunctionSupplier(List<Integer> channels, int precision) {
    this.channels = channels;
    this.precision = precision;
  }

  @Override
  public CountDistinctDoubleAggregatorFunction aggregator(DriverContext driverContext) {
    return CountDistinctDoubleAggregatorFunction.create(driverContext, channels, precision);
  }

  @Override
  public CountDistinctDoubleGroupingAggregatorFunction groupingAggregator(
      DriverContext driverContext) {
    return CountDistinctDoubleGroupingAggregatorFunction.create(channels, driverContext, precision);
  }

  @Override
  public String describe() {
    return "count_distinct of doubles";
  }
}
