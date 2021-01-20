package org.opengroup.osdu.entitlements.v2.api;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HealthChecksApiTest {

    private HealthChecksApi sut;

    @Before
    public void setup() {
        this.sut = new HealthChecksApi();
    }

    @Test
    public void should_returnHttp200_when_checkLiveness() {
        assertEquals(200, this.sut.livenessCheck().getStatusCodeValue());
    }

    @Test
    public void should_returnHttp200_when_checkReadiness() {
        assertEquals(200, this.sut.readinessCheck().getStatusCodeValue());
    }
}
