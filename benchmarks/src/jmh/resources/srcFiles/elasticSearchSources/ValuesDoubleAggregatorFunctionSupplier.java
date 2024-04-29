package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link ValuesDoubleAggregator}.
 * This class is generated. Do not edit it.
 */
public final class ValuesDoubleAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  public ValuesDoubleAggregatorFunctionSupplier(List<Integer> channels) {
    this.channels = channels;
  }

  @Override
  public ValuesDoubleAggregatorFunction aggregator(DriverContext driverContext) {
    return ValuesDoubleAggregatorFunction.create(driverContext, channels);
  }

  @Override
  public ValuesDoubleGroupingAggregatorFunction groupingAggregator(DriverContext driverContext) {
    return ValuesDoubleGroupingAggregatorFunction.create(channels, driverContext);
  }

  @Override
  public String describe() {
    return "values of doubles";
  }
}
