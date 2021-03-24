package org.opengroup.osdu.entitlements.v2.acceptance;

import com.sun.jersey.api.client.ClientResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.entitlements.v2.acceptance.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.acceptance.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.EntitlementsV2Service;
import org.opengroup.osdu.entitlements.v2.acceptance.util.HttpClientService;
import org.opengroup.osdu.entitlements.v2.acceptance.util.TokenService;

public abstract class AcceptanceBaseTest {
    protected final TokenService tokenService;
    protected final HttpClientService httpClientService;
    protected final EntitlementsV2Service entitlementsV2Service;
    protected final ConfigurationService configurationService;
    protected final long currentTime;

    public AcceptanceBaseTest(ConfigurationService configurationService, TokenService tokenService) {
        this.tokenService = tokenService;
        this.configurationService = configurationService;
        this.httpClientService = new HttpClientService(configurationService);
        this.entitlementsV2Service = new EntitlementsV2Service(configurationService, httpClientService);
        this.currentTime = System.currentTimeMillis();
    }

    protected abstract RequestData getRequestDataForNoTokenTest();

    protected void cleanup() throws Exception {
    }

    @Test
    public void shouldReturn401WhenMakingHttpRequestWithoutToken() throws Exception {
        ClientResponse response = httpClientService.send(getRequestDataForNoTokenTest());
        Assert.assertEquals(401, response.getStatus());
    }

    @After
    public void cleanupAfterTests() throws Exception {
        cleanup();
    }
}
