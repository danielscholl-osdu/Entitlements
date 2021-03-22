package org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractHitsNMissesMetricService implements HitsNMissesMetricService {
    private static final int DEFAULT_METRIC_VALUE = 1;
    private final TelemetryClient telemetryClient;

    /**
     * This value will be used to send the 'hits' metric to application insights.
     * Based on this name, it will be possible to filter metrics in the Metrics Explorer.
     *
     * @return The name of the 'hits' metric.
     */
    protected abstract String hitsMetricName();

    /**
     * This value will be used to send the 'misses' metric to application insights.
     * Based on this name, it will be possible to filter metrics in the Metrics Explorer.
     *
     * @return The name of the 'misses' metric.
     */
    protected abstract String missesMetricName();

    @Override
    public void sendHitsMetric() {
        sendMetric(hitsMetricName());
    }

    @Override
    public void sendMissesMetric() {
        sendMetric(missesMetricName());
    }

    private void sendMetric(String name) {
        MetricTelemetry metric = new MetricTelemetry();
        metric.setName(name);
        metric.setValue(DEFAULT_METRIC_VALUE);
        telemetryClient.trackMetric(metric);
        telemetryClient.flush();
    }
}