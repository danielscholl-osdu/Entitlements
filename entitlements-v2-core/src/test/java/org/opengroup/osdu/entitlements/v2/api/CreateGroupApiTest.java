package org.opengroup.osdu.entitlements.v2.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.CreateGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.model.creategroup.GroupDto;
import org.opengroup.osdu.entitlements.v2.service.CreateGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = CreateGroupApi.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
public class CreateGroupApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private CreateGroupService createGroupService; //service used by api controller
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private ITenantFactory tenantFactory;
    @MockBean
    private AuthorizationService authService;

    @Before
    public void setup() {
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setServiceAccount("internal-service-account");
        when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfo);
        //return the input to service layer as the output
        when(createGroupService.run(any(EntityNode.class), any(CreateGroupServiceDto.class))).thenAnswer((Answer<EntityNode>) invocation -> {
            Object[] args = invocation.getArguments();
            return (EntityNode) args[0];
        });
        when(authService.isAuthorized(any(),any())).thenReturn(true);
    }

    @Test
    public void shouldMatchExpectedHttpRequest() throws Exception {
        CreateGroupDto dto = new CreateGroupDto("service.viewers_123-{{tenant}}", "My viewers group");
        performCreateGroupRequest(dto).andExpect(status().isCreated());
    }

    @Test
    public void shouldValidateGroupNameInput() throws Exception {
        CreateGroupDto dto = new CreateGroupDto("service/viewers", "");

        performCreateGroupRequest(dto).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void shouldSerializeOutput() throws Exception {
        CreateGroupDto dto = new CreateGroupDto("service.viewers", "My viewers group");
        GroupDto expectedResult = new GroupDto("service.viewers", "service.viewers@common.contoso.com", "My viewers group");

        String result = performCreateGroupRequest(dto).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    @Test
    public void shouldCallService() throws Exception {
        CreateGroupDto dto = new CreateGroupDto("service.viewers", "desc");
        ArgumentCaptor<EntityNode> captor1 = ArgumentCaptor.forClass(EntityNode.class);
        ArgumentCaptor<CreateGroupServiceDto> captor2 = ArgumentCaptor.forClass(CreateGroupServiceDto.class);

        performCreateGroupRequest(dto).andExpect(status().isCreated());

        verify(createGroupService, times(1)).run(captor1.capture(), captor2.capture());

        assertThat(captor1.getValue().getDescription()).isEqualTo("desc");
        assertThat(captor1.getValue().getNodeId()).isEqualTo("service.viewers@common.contoso.com");
        assertThat(captor1.getValue().getName()).isEqualTo("service.viewers");

    }

    @Test
    public void shouldCallServiceWithWithUppercaseBody() throws Exception {
        String body = "{\"Name\":\"service.viewers\",\"Description\":\"desc\"}";
        ArgumentCaptor<EntityNode> captor1 = ArgumentCaptor.forClass(EntityNode.class);
        ArgumentCaptor<CreateGroupServiceDto> captor2 = ArgumentCaptor.forClass(CreateGroupServiceDto.class);

        ResultActions resultActions = mockMvc.perform(post("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer ew0KICAidHlwIjogIkpXVCIsDQogICJhbGciOiAiUlMyNTYiLA0KICAia2lkIjogIk1UVTVNRFUxTkRrNU9BPT0iDQp9.ew0KICAic3ViIjogImFAYi5jb20iLA0KICAiaXNzIjogInByZXZpZXcuY29tIiwNCiAgImF1ZCI6ICJ0ZXN0LmNvbSIsDQogICJpYXQiOiAxNTkwNjAwODI0LA0KICAiZXhwIjogMTU5MDY4NzIyNCwNCiAgInByb3ZpZGVyIjogImEuY29tIiwNCiAgImNsaWVudCI6ICJ0ZXN0LmNvbSIsDQogICJ1c2VyaWQiOiAiYUBiLmNvbSIsDQogICJlbWFpbCI6ICJhQGIuY29tIiwNCiAgImF1dGh6IjogIiIsDQogICJsYXN0bmFtZSI6ICJCIiwNCiAgImZpcnN0bmFtZSI6ICJBIiwNCiAgImNvdW50cnkiOiAiIiwNCiAgImNvbXBhbnkiOiAiIiwNCiAgImpvYnRpdGxlIjogIiIsDQogICJzdWJpZCI6ICJ1NUxTTkp1aFVmYUgweFAzdVlUbkl4Vk9BSjR4NkRKdWNXc3BzNWdEb280IiwNCiAgImlkcCI6ICJvMzY1IiwNCiAgImhkIjogInNsYi5jb20iLA0KICAiZGVzaWQiOiAiYUBkZXNpZC5jb20iLA0KICAiY29udGFjdF9lbWFpbCI6ICJhQGIuY29tIiwNCiAgInJ0X2hhc2giOiAieVMxcHY3a0NvaTZHVld2c3c4S3F5QSINCn0.tvE00W8cZZOZZDc83Sn4nKPBlw3boJEjJaTvOyvMXmNSTB4BN7kdLnhXy_CLQ4FZy0Y-PMboMGCH9gfKT1bYcQHllUTLnjtzd0iBJWY-I0ahoMEIa1PkksCytBz5qBGunrwr28PqW_t6GN99pUn0zxFn2022C17fnDHGdS1G2Tlag0Jpadl2PgdN_V9u2BndHgkKCFlmS2ZmX59KWQCOJmwnTd1k8vXCpUgDVjBK5CzCb9aFp8pjdy0mdMeV-7hYE2acyIrTPVZwAMAgHFdA")
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .content(body));

        resultActions.andExpect(status().isCreated());

        verify(createGroupService, times(1)).run(captor1.capture(), captor2.capture());

        assertThat(captor1.getValue().getDescription()).isEqualTo("desc");
        assertThat(captor1.getValue().getNodeId()).isEqualTo("service.viewers@common.contoso.com");
        assertThat(captor1.getValue().getName()).isEqualTo("service.viewers");
    }

    private ResultActions performCreateGroupRequest(CreateGroupDto dto) throws Exception {
        return mockMvc.perform(post("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer ew0KICAidHlwIjogIkpXVCIsDQogICJhbGciOiAiUlMyNTYiLA0KICAia2lkIjogIk1UVTVNRFUxTkRrNU9BPT0iDQp9.ew0KICAic3ViIjogImFAYi5jb20iLA0KICAiaXNzIjogInByZXZpZXcuY29tIiwNCiAgImF1ZCI6ICJ0ZXN0LmNvbSIsDQogICJpYXQiOiAxNTkwNjAwODI0LA0KICAiZXhwIjogMTU5MDY4NzIyNCwNCiAgInByb3ZpZGVyIjogImEuY29tIiwNCiAgImNsaWVudCI6ICJ0ZXN0LmNvbSIsDQogICJ1c2VyaWQiOiAiYUBiLmNvbSIsDQogICJlbWFpbCI6ICJhQGIuY29tIiwNCiAgImF1dGh6IjogIiIsDQogICJsYXN0bmFtZSI6ICJCIiwNCiAgImZpcnN0bmFtZSI6ICJBIiwNCiAgImNvdW50cnkiOiAiIiwNCiAgImNvbXBhbnkiOiAiIiwNCiAgImpvYnRpdGxlIjogIiIsDQogICJzdWJpZCI6ICJ1NUxTTkp1aFVmYUgweFAzdVlUbkl4Vk9BSjR4NkRKdWNXc3BzNWdEb280IiwNCiAgImlkcCI6ICJvMzY1IiwNCiAgImhkIjogInNsYi5jb20iLA0KICAiZGVzaWQiOiAiYUBkZXNpZC5jb20iLA0KICAiY29udGFjdF9lbWFpbCI6ICJhQGIuY29tIiwNCiAgInJ0X2hhc2giOiAieVMxcHY3a0NvaTZHVld2c3c4S3F5QSINCn0.tvE00W8cZZOZZDc83Sn4nKPBlw3boJEjJaTvOyvMXmNSTB4BN7kdLnhXy_CLQ4FZy0Y-PMboMGCH9gfKT1bYcQHllUTLnjtzd0iBJWY-I0ahoMEIa1PkksCytBz5qBGunrwr28PqW_t6GN99pUn0zxFn2022C17fnDHGdS1G2Tlag0Jpadl2PgdN_V9u2BndHgkKCFlmS2ZmX59KWQCOJmwnTd1k8vXCpUgDVjBK5CzCb9aFp8pjdy0mdMeV-7hYE2acyIrTPVZwAMAgHFdA")
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .content(objectMapper.writeValueAsString(dto)));
    }
}
