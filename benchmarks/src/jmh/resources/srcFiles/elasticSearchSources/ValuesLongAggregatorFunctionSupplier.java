package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link ValuesLongAggregator}.
 * This class is generated. Do not edit it.
 */
public final class ValuesLongAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  public ValuesLongAggregatorFunctionSupplier(List<Integer> channels) {
    this.channels = channels;
  }

  @Override
  public ValuesLongAggregatorFunction aggregator(DriverContext driverContext) {
    return ValuesLongAggregatorFunction.create(driverContext, channels);
  }

  @Override
  public ValuesLongGroupingAggregatorFunction groupingAggregator(DriverContext driverContext) {
    return ValuesLongGroupingAggregatorFunction.create(channels, driverContext);
  }

  @Override
  public String describe() {
    return "values of longs";
  }
}
