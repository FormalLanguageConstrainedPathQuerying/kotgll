package org.elasticsearch.compute.aggregation;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link PercentileIntAggregator}.
 * This class is generated. Do not edit it.
 */
public final class PercentileIntAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  private final double percentile;

  public PercentileIntAggregatorFunctionSupplier(List<Integer> channels, double percentile) {
    this.channels = channels;
    this.percentile = percentile;
  }

  @Override
  public PercentileIntAggregatorFunction aggregator(DriverContext driverContext) {
    return PercentileIntAggregatorFunction.create(driverContext, channels, percentile);
  }

  @Override
  public PercentileIntGroupingAggregatorFunction groupingAggregator(DriverContext driverContext) {
    return PercentileIntGroupingAggregatorFunction.create(channels, driverContext, percentile);
  }

  @Override
  public String describe() {
    return "percentile of ints";
  }
}
