package org.opengroup.osdu.entitlements.v2.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountResponseDto;
import org.opengroup.osdu.entitlements.v2.model.memberscount.MembersCountServiceDto;
import org.opengroup.osdu.entitlements.v2.service.MembersCountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = MembersCountApi.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
public class MembersCountApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private MembersCountService service; //service used by api controller
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private ITenantFactory tenantFactory;
    @MockBean
    private AuthorizationService authService;

    @Before
    public void setup() {
        MembersCountResponseDto membersCountResponseDto = MembersCountResponseDto
                .builder()
                .membersCount(10)
                .groupEmail("service.viewers.users@common.contoso.com").build();
        when(service.getMembersCount(any(MembersCountServiceDto.class))).thenReturn(membersCountResponseDto);
        final TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setDataPartitionId("common");
        tenantInfo.setServiceAccount("internal-service-account");
        when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfo);
        when(authService.isCurrentUserAuthorized(any(), any())).thenReturn(true);
    }

    @Test
    public void shouldMatchExpectedHttpRequest() throws Exception {
        String group = "service.viewers.users@common.contoso.com";

        performMembersCountRequest(group).andExpect(status().isOk());
    }

    @Test
    public void shouldValidateGroupEmailParameter() throws Exception {
        String group = "service.viewers.com";

        performMembersCountRequest(group).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldSerializeOutputGivenDefaultQueryParameters() throws Exception {
        String group = "service.viewers.users@common.contoso.com";
        MembersCountResponseDto expectedResult = MembersCountResponseDto.builder()
                .membersCount(10).groupEmail(group)
                .build();

        String result = performMembersCountRequest(group).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    @Test
    public void shouldCallServiceWithExpectedInputs() throws Exception {
        String group = "service.VIEWERS.users@common.contoso.com";
        ArgumentCaptor<MembersCountServiceDto> captor = ArgumentCaptor.forClass(MembersCountServiceDto.class);

        performMembersCountRequest(group).andExpect(status().isOk());

        verify(service, times(1)).getMembersCount(captor.capture());

        assertThat(captor.getValue().getGroupId()).isEqualTo("service.viewers.users@common.contoso.com");
        assertThat(captor.getValue().getRequesterId()).isEqualTo("a@b.com");
        assertThat(captor.getValue().getPartitionId()).isEqualTo("common");
    }

    private ResultActions performMembersCountRequest(String groupEmail) throws Exception {
        return mockMvc.perform(get("/groups/{group_email}/membersCount", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID, "a@b.com"));
    }
}
