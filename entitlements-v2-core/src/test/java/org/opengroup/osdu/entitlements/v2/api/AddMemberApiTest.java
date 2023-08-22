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
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberDto;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.service.AddMemberService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = AddMemberApi.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
public class AddMemberApiTest {
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

    @Before
    public void setup() {
        final TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setDataPartitionId("common");
        tenantInfo.setServiceAccount("internal-service-account");
        when(tenantFactory.listTenantInfo()).thenReturn(Collections.singletonList(tenantInfo));
        when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfo);
        when(authService.isCurrentUserAuthorized(any(),any())).thenReturn(true);
    }

    @Test
    public void shouldMatchExpectedHttpRequest() throws Exception {
        AddMemberDto dto = new AddMemberDto("a@common.com", Role.OWNER);
        String group = "service.viewers.users@common.contoso.com";
        performRequest(dto, group).andExpect(status().isOk());
    }

    @Test
    public void shouldValidateGroupEmailParameter() throws Exception {
        AddMemberDto dto = new AddMemberDto("a@common.com", Role.OWNER);
        String group = "service.viewers.com";
        performRequest(dto, group).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldSerializeOutput() throws Exception {
        AddMemberDto dto = new AddMemberDto("a@common.com", Role.OWNER);
        String group = "service.viewers.users@common.contoso.com";

        String result = performRequest(dto, group).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(dto));
    }

    @Test
    public void shouldCallServiceWithExpectedInputs() throws Exception {
        ArgumentCaptor<AddMemberDto> captor1 = ArgumentCaptor.forClass(AddMemberDto.class);
        ArgumentCaptor<AddMemberServiceDto> captor2 = ArgumentCaptor.forClass(AddMemberServiceDto.class);

        AddMemberDto dto = new AddMemberDto("MEMBER@xxx.com", Role.OWNER);
        String group = "service.viewers.users@common.contoso.com";
        performRequest(dto, group).andExpect(status().isOk());

        verify(service, times(1)).run(captor1.capture(), captor2.capture());

        assertThat(captor1.getValue().getEmail()).isEqualTo("member@xxx.com");
        assertThat(captor1.getValue().getRole()).isEqualTo(Role.OWNER);
        assertThat(captor2.getValue().getGroupEmail()).isEqualTo("service.viewers.users@common.contoso.com");
        assertThat(captor2.getValue().getRequesterId()).isEqualTo("a@b.com");
        assertThat(captor2.getValue().getPartitionId()).isEqualTo("common");
    }

    @Test
    public void shouldCallServiceWithUppercaseBodyProperties() throws Exception {
        ArgumentCaptor<AddMemberDto> captor1 = ArgumentCaptor.forClass(AddMemberDto.class);
        ArgumentCaptor<AddMemberServiceDto> captor2 = ArgumentCaptor.forClass(AddMemberServiceDto.class);

        String body = "{\"Email\":\"member@xxx.com\",\"Role\":\"OWNER\"}";
        String group = "service.viewers.users@common.contoso.com";

        ResultActions resultActions = mockMvc.perform(post("/groups/{group_email}/members", group)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .content(body));

        resultActions.andExpect(status().isOk());

        verify(service, times(1)).run(captor1.capture(), captor2.capture());

        assertThat(captor1.getValue().getEmail()).isEqualTo("member@xxx.com");
        assertThat(captor1.getValue().getRole()).isEqualTo(Role.OWNER);
        assertThat(captor2.getValue().getGroupEmail()).isEqualTo("service.viewers.users@common.contoso.com");
        assertThat(captor2.getValue().getRequesterId()).isEqualTo("a@b.com");
        assertThat(captor2.getValue().getPartitionId()).isEqualTo("common");
    }

    @Test
    public void shouldCallServiceWithLowerCasedMemberAndGroupEmail() throws Exception {
        ArgumentCaptor<AddMemberDto> captor1 = ArgumentCaptor.forClass(AddMemberDto.class);
        ArgumentCaptor<AddMemberServiceDto> captor2 = ArgumentCaptor.forClass(AddMemberServiceDto.class);

        String body = "{\"Email\":\"MEMBER@xxx.com\",\"Role\":\"OWNER\"}";
        String group = "service.VIEWERS.users@common.contoso.com";

        ResultActions resultActions = mockMvc.perform(post("/groups/{group_email}/members", group)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .content(body));

        resultActions.andExpect(status().isOk());

        verify(service, times(1)).run(captor1.capture(), captor2.capture());

        assertThat(captor1.getValue().getEmail()).isEqualTo("member@xxx.com");
        assertThat(captor1.getValue().getRole()).isEqualTo(Role.OWNER);
        assertThat(captor2.getValue().getGroupEmail()).isEqualTo("service.viewers.users@common.contoso.com");
        assertThat(captor2.getValue().getRequesterId()).isEqualTo("a@b.com");
        assertThat(captor2.getValue().getPartitionId()).isEqualTo("common");
    }

    @Test
    public void shouldReturnBadRequestGivenInvalidInput() throws Exception {
        String body = "{\"test\":\"test\"}";
        String group = "service.VIEWERS.users@common.contoso.com";

        ResultActions resultActions = mockMvc.perform(post("/groups/{group_email}/members", group)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .content(body));

        resultActions.andExpect(status().isBadRequest());
    }

    private ResultActions performRequest(AddMemberDto dto, String groupEmail) throws Exception {
        return mockMvc.perform(post("/groups/{group_email}/members", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .content(objectMapper.writeValueAsString(dto)));
    }
}
