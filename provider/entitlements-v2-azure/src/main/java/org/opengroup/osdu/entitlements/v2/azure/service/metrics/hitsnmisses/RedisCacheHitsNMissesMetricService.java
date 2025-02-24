package org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses;
import com.microsoft.applicationinsights.TelemetryClient;
import org.springframework.stereotype.Service;

@Service
public class RedisCacheHitsNMissesMetricService extends AbstractHitsNMissesMetricService {
    private static final String HITS_METRIC_NAME = "[Entitlements service] Redis cache HITS";
    private static final String MISSES_METRIC_NAME = "[Entitlements service] Redis cache MISSES";

    public RedisCacheHitsNMissesMetricService(TelemetryClient telemetryClient) {
        super(telemetryClient);
    }

    @Override
    protected String hitsMetricName() {
        return HITS_METRIC_NAME;
    }

    @Override
    protected String missesMetricName() {
        return MISSES_METRIC_NAME;
    }
}
