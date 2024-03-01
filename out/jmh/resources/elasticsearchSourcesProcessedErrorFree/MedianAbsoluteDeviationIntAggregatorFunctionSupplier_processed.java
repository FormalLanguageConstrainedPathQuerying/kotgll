package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link MedianAbsoluteDeviationIntAggregator}.
 * This class is generated. Do not edit it.
 */
public final class MedianAbsoluteDeviationIntAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  public MedianAbsoluteDeviationIntAggregatorFunctionSupplier(List<Integer> channels) {
    this.channels = channels;
  }

  @Override
  public MedianAbsoluteDeviationIntAggregatorFunction aggregator(DriverContext driverContext) {
    return MedianAbsoluteDeviationIntAggregatorFunction.create(driverContext, channels);
  }

  @Override
  public MedianAbsoluteDeviationIntGroupingAggregatorFunction groupingAggregator(
      DriverContext driverContext) {
    return MedianAbsoluteDeviationIntGroupingAggregatorFunction.create(channels, driverContext);
  }

  @Override
  public String describe() {
    return "median_absolute_deviation of ints";
  }
}
