package org.elasticsearch.compute.aggregation.spatial;

import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import org.elasticsearch.compute.aggregation.AggregatorFunctionSupplier;
import org.elasticsearch.compute.operator.DriverContext;

/**
 * {@link AggregatorFunctionSupplier} implementation for {@link SpatialCentroidGeoPointDocValuesAggregator}.
 * This class is generated. Do not edit it.
 */
public final class SpatialCentroidGeoPointDocValuesAggregatorFunctionSupplier implements AggregatorFunctionSupplier {
  private final List<Integer> channels;

  public SpatialCentroidGeoPointDocValuesAggregatorFunctionSupplier(List<Integer> channels) {
    this.channels = channels;
  }

  @Override
  public SpatialCentroidGeoPointDocValuesAggregatorFunction aggregator(
      DriverContext driverContext) {
    return SpatialCentroidGeoPointDocValuesAggregatorFunction.create(driverContext, channels);
  }

  @Override
  public SpatialCentroidGeoPointDocValuesGroupingAggregatorFunction groupingAggregator(
      DriverContext driverContext) {
    return SpatialCentroidGeoPointDocValuesGroupingAggregatorFunction.create(channels, driverContext);
  }

  @Override
  public String describe() {
    return "spatial_centroid_geo_point_doc of valuess";
  }
}
