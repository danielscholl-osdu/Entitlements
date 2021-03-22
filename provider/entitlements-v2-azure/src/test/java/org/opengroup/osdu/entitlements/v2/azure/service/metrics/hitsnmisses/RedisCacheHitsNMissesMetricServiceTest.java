package org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RedisCacheHitsNMissesMetricServiceTest {
    @Mock
    private TelemetryClient telemetryClient;
    @InjectMocks
    private RedisCacheHitsNMissesMetricService redisCacheHitsNMissesMetricService;

    @Test
    public void shouldSendHitsMetricSuccessfully() {
        ArgumentCaptor<MetricTelemetry> metricCaptor = ArgumentCaptor.forClass(MetricTelemetry.class);

        redisCacheHitsNMissesMetricService.sendHitsMetric();

        verify(telemetryClient, times(1)).trackMetric(metricCaptor.capture());
        assertEquals("[Entitlements service] Redis cache HITS", metricCaptor.getValue().getName());
        assertEquals(1, metricCaptor.getValue().getValue(), 0);
        verify(telemetryClient, times(1)).flush();
    }

    @Test
    public void shouldSendMissesMetricSuccessfully() {
        ArgumentCaptor<MetricTelemetry> metricCaptor = ArgumentCaptor.forClass(MetricTelemetry.class);

        redisCacheHitsNMissesMetricService.sendMissesMetric();

        verify(telemetryClient, times(1)).trackMetric(metricCaptor.capture());
        assertEquals("[Entitlements service] Redis cache MISSES", metricCaptor.getValue().getName());
        assertEquals(1, metricCaptor.getValue().getValue(), 0);
        verify(telemetryClient, times(1)).flush();
    }
}
