package org.opengroup.osdu.entitlements.v2.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.service.RemoveMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = DeleteGroupApi.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
public class RemoveMemberApiTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RemoveMemberService service; //service used by api controller
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private ITenantFactory tenantFactory;
    @MockBean
    private AuthorizationService authService;

    @Before
    public void setup() {
        final TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setDataPartitionId("common");
        tenantInfo.setServiceAccount("internal-service-account");
        when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfo);
        when(authService.isAuthorized(any(),any())).thenReturn(true);
    }

    @Test
    public void shouldMatchExpectedHttpRequest() throws Exception {
        performRequest("a@common.com", "service.viewers.users@common.contoso.com")
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldValidateGroupEmailParameter() throws Exception {
        performRequest("a@common.com", "service.viewers.com")
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldCallServiceWithExpectedInputs() throws Exception {
        ArgumentCaptor<RemoveMemberServiceDto> captor = ArgumentCaptor.forClass(RemoveMemberServiceDto.class);

        performRequest("ABC@common.com", "service.VIEWERS.users@common.contoso.com")
                .andExpect(status().isNoContent());

        verify(service, times(1)).removeMember(captor.capture());

        assertThat(captor.getValue().getGroupEmail()).isEqualTo("service.viewers.users@common.contoso.com");
        assertThat(captor.getValue().getMemberEmail()).isEqualTo("abc@common.com");
        assertThat(captor.getValue().getRequesterId()).isEqualTo("a@b.com");
        assertThat(captor.getValue().getPartitionId()).isEqualTo("common");
    }

    private ResultActions performRequest(String memberEmail, String groupEmail) throws Exception {
        return mockMvc.perform(delete("/groups/{group_email}/members/{member_email}", groupEmail, memberEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .header(DpsHeaders.DATA_PARTITION_ID, "common"));
    }

}
