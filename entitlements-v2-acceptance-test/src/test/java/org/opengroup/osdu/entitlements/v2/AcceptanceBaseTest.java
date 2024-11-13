package org.opengroup.osdu.entitlements.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.entitlements.v2.util.EntitlementsV2Service;
import org.opengroup.osdu.entitlements.v2.model.request.RequestData;
import org.opengroup.osdu.entitlements.v2.util.ConfigurationService;
import org.opengroup.osdu.entitlements.v2.util.HttpClientService;
import org.opengroup.osdu.entitlements.v2.util.TokenService;

public abstract class AcceptanceBaseTest {
    protected final TokenService tokenService;
    protected final HttpClientService httpClientService;
    protected final EntitlementsV2Service entitlementsV2Service;
    protected final ConfigurationService configurationService;
    protected final long currentTime;

    public TokenService getTokenService() {
        return tokenService;
    }
    public HttpClientService getHttpClientService() {
        return httpClientService;
    }
    public EntitlementsV2Service getEntitlementsV2Service() {
        return entitlementsV2Service;
    }
    public ConfigurationService getConfigurationService() {
        return configurationService;
    }
    public Long getcurrentTime() {
        return currentTime;
    }


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
        CloseableHttpResponse response = httpClientService.send(getRequestDataForNoTokenTest());
        assertEquals(401, response.getCode());
    }

    @AfterEach
    public void cleanupAfterTests() throws Exception {
        cleanup();
    }
}
