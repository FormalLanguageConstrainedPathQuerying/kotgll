package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link MedianAbsoluteDeviationLongAggregator}.
 * This class is generated. Do not edit it.
 */
public final class MedianAbsoluteDeviationLongAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  public MedianAbsoluteDeviationLongAggregatorFunctionSupplier(List<Integer> channels) {
    this.channels = channels;
  }

  @Override
  public MedianAbsoluteDeviationLongAggregatorFunction aggregator(DriverContext driverContext) {
    return MedianAbsoluteDeviationLongAggregatorFunction.create(driverContext, channels);
  }

  @Override
  public MedianAbsoluteDeviationLongGroupingAggregatorFunction groupingAggregator(
      DriverContext driverContext) {
    return MedianAbsoluteDeviationLongGroupingAggregatorFunction.create(channels, driverContext);
  }

  @Override
  public String describe() {
    return "median_absolute_deviation of longs";
  }
}
