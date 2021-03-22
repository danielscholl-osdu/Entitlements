package org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses;

public interface HitsNMissesMetricService {
    /**
     * Sends one 'hits' metric to application insights,
     * to send several such metrics, you need to call this method exactly as many times as metrics you expect to send.
     * <p>
     * 'hits' refers to the number of times an action has reached the destination.
     */
    void sendHitsMetric();
    /**
     * Sends one 'misses' metric to application insights,
     * to send several such metrics, you need to call this method exactly as many times as metrics you expect to send.
     * <p>
     * 'misses' refers to the number of times an action has NOT reached the destination.
     */
    void sendMissesMetric();
}