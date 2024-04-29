package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link ValuesBytesRefAggregator}.
 * This class is generated. Do not edit it.
 */
public final class ValuesBytesRefAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  public ValuesBytesRefAggregatorFunctionSupplier(List<Integer> channels) {
    this.channels = channels;
  }

  @Override
  public ValuesBytesRefAggregatorFunction aggregator(DriverContext driverContext) {
    return ValuesBytesRefAggregatorFunction.create(driverContext, channels);
  }

  @Override
  public ValuesBytesRefGroupingAggregatorFunction groupingAggregator(DriverContext driverContext) {
    return ValuesBytesRefGroupingAggregatorFunction.create(channels, driverContext);
  }

  @Override
  public String describe() {
    return "values of bytes";
  }
}
