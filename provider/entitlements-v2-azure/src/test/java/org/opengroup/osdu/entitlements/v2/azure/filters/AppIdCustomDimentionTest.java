package org.opengroup.osdu.entitlements.v2.azure.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.entitlements.v2.api.AddMemberApi;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.azure.AzureAppProperties;
import org.opengroup.osdu.entitlements.v2.azure.config.CacheConfig;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberDto;
import org.opengroup.osdu.entitlements.v2.service.AddMemberService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = AddMemberApi.class)
@ComponentScan({"org.opengroup.osdu.entitlements.v2"})
public class AppIdCustomDimentionTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private AddMemberService service;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private ITenantFactory tenantFactory;
    @MockBean
    private AuthorizationService authService;
    @MockBean
    private CacheConfig cacheConfig;
    @MockBean
    private AzureAppProperties azureAppProperties;

    @Test
    public void shouldMatchExpectedHttpRequest() throws Exception {
        final TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setDataPartitionId("common");
        tenantInfo.setServiceAccount("internal-service-account");
        Mockito.when(tenantFactory.listTenantInfo()).thenReturn(Collections.singletonList(tenantInfo));
        Mockito.when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfo);
        Mockito.when(authService.isCurrentUserAuthorized(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(true);
        Mockito.when(azureAppProperties.getDomain()).thenReturn("contoso.com");
        String appIdValue = "app-id-value";
        performRequest(appIdValue);
        Assert.assertTrue(MDC.getCopyOfContextMap().entrySet().stream()
                .anyMatch(entry -> DpsHeaders.APP_ID.equals(entry.getKey()) && appIdValue.equals(entry.getValue())));
    }

    private void performRequest(String appId) throws Exception {
        AddMemberDto dto = new AddMemberDto("a@common.com", Role.OWNER);
        String group = "service.viewers.users@common.contoso.com";
        mockMvc.perform(MockMvcRequestBuilders.post("/groups/{group_email}/members", group)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.toString())
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID, "a@b.com")
                .header(DpsHeaders.APP_ID, appId)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
