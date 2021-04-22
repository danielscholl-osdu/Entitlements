package org.opengroup.osdu.entitlements.v2.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.entitlements.v2.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(HealthChecksApi.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
public class HealthChecksApiTest {

    private static final String DATA_PARTITION_ID = "common";

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ITenantFactory tenantFactory;
    @MockBean
    private HealthService healthService;

    @Test
    public void shouldReturnHttp200WhenCheckingLiveness() throws Exception {
        performHealthCheckRequest("/_ah/liveness_check").andExpect(status().isOk());
    }

    @Test
    public void shouldReturn200WhenCheckingReadiness() throws Exception {
        performHealthCheckRequest("/_ah/readiness_check").andExpect(status().isOk());

        Mockito.verify(healthService).performHealthCheck();
    }

    @Test
    public void shouldReturnInternalServerErrorOnErrorFromHealthService() throws Exception {
        Mockito.doThrow(new RuntimeException("some error")).when(healthService).performHealthCheck();

        performHealthCheckRequest("/_ah/readiness_check").andExpect(status().is5xxServerError());

        Mockito.verify(healthService).performHealthCheck();
    }

    private ResultActions performHealthCheckRequest(String path) throws Exception {
        return mockMvc.perform(get(path)
                .contentType(MediaType.APPLICATION_JSON)
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.USER_ID, "a@b.com")
                .header(DpsHeaders.DATA_PARTITION_ID, DATA_PARTITION_ID));
    }
}
