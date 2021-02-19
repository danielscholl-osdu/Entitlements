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
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupOperation;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.model.updategroup.UpdateGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.service.UpdateGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = UpdateGroupApi.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
public class UpdateGroupApiTests {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UpdateGroupService service; //service used by api controller
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private ITenantFactory tenantFactory;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private AuthorizationService authService;

    @Before
    public void setup() {
        final TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setDataPartitionId("common");
        tenantInfo.setServiceAccount("a@desid.com");
        when(tenantFactory.listTenantInfo()).thenReturn(Collections.singletonList(tenantInfo));
        when(tenantFactory.getTenantInfo("common")).thenReturn(tenantInfo);
        when(authService.isAuthorized(any(),any())).thenReturn(true);
    }

    @Test
    public void shouldThrowBadRequestWhenOperationTypeIsWrong() throws Exception {
        String groupEmail = "users.common.test@common.contoso.com";
        String newGroupEmail = "users.test";
        List<UpdateGroupOperation> request = getRequestBody("invalidOp", "/name", newGroupEmail);
        performRequest(request, groupEmail).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldThrowBadRequestWhenPathValueIsWrong() throws Exception {
        String groupEmail = "users.common.test@common.contoso.com";
        String newGroupEmail = "users.test";
        List<UpdateGroupOperation> request = getRequestBody("replace", "/invalidPath", newGroupEmail);
        performRequest(request, groupEmail).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldThrowBadRequestWhenValueListIsEmpty() throws Exception {
        String groupEmail = "users.common.test@common.contoso.com";
        List<UpdateGroupOperation> request = getRequestBody("replace", "/invalidPath");
        performRequest(request, groupEmail).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldUpdateAppIdsMatchExpectedHttpRequest() throws Exception {
        String groupEmail = "users.common.test@common.contoso.com";
        List<UpdateGroupOperation> request = getRequestBody("replace", "/appIds", "app1", "app2");
        performRequest(request, groupEmail).andExpect(status().isOk());
    }

    @Test
    public void should_matchExpectedHttpRequest() throws Exception {
        String groupEmail = "users.common.test@common.contoso.com";
        String newGroupEmail = "users.test";
        List<UpdateGroupOperation> request = getRequestBody("replace", "/name", newGroupEmail);
        performRequest(request, groupEmail).andExpect(status().isOk());
    }

    @Test
    public void should_returnCorrectResponseContent() throws Exception {
        String groupName = "users.common.test";
        String partitionDomain = "common.contoso.com";
        String groupEmail = String.format("%s@%s", groupName, partitionDomain);
        String newGroupName = "users.test";
        String newGroupEmail = String.format("%s@%s", newGroupName, partitionDomain);
        UpdateGroupResponseDto response = new UpdateGroupResponseDto(newGroupName, newGroupEmail, Collections.EMPTY_LIST);
        when(service.updateGroup(any())).thenReturn(response);
        List<UpdateGroupOperation> request = getRequestBody("replace", "/name", newGroupEmail);
        String result = performRequest(request, groupEmail)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(result).isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(response));
    }

    @Test
    public void should_validateGroupEmailParameter() throws Exception {
        String groupEmail = "users.common.test.com";
        String newGroupName = "users.test";
        List<UpdateGroupOperation> request = getRequestBody("replace", "/name", newGroupName);
        performRequest(request, groupEmail).andExpect(status().isBadRequest());
    }

    @Test
    public void should_validateRenameValue() throws Exception {
        String groupEmail = "users.common.test@common.contoso.com";
        List<UpdateGroupOperation> request = getRequestBody("replace", "/name", "name1", "name2");
        performRequest(request, groupEmail).andExpect(status().isBadRequest());
    }

    @Test
    public void should_callService_withExpectedInputs() throws Exception {
        String groupEmail = "users.COMMON.test@common.contoso.com";
        String newGroupName = "users.TEST";
        ArgumentCaptor<UpdateGroupServiceDto> captor = ArgumentCaptor.forClass(UpdateGroupServiceDto.class);
        List<UpdateGroupOperation> request = getRequestBody("replace", "/name", newGroupName);
        performRequest(request, groupEmail).andExpect(status().isOk());
        verify(service, times(1)).updateGroup(captor.capture());
        assertThat(captor.getValue().getExistingGroupEmail()).isEqualTo("users.common.test@common.contoso.com");
        assertThat(captor.getValue().getRenameOperation().getValue().get(0)).isEqualTo("users.TEST");
        assertThat(captor.getValue().getPartitionId()).isEqualTo("common");
    }

    private ResultActions performRequest(List<UpdateGroupOperation> updateGroupRequest, String groupEmail) throws Exception {
        return mockMvc.perform(patch("/groups/{group_email}", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer ew0KICAidHlwIjogIkpXVCIsDQogICJhbGciOiAiUlMyNTYiLA0KICAia2lkIjogIk1UVTVNRFUxTkRrNU9BPT0iDQp9.ew0KICAic3ViIjogImFAYi5jb20iLA0KICAiaXNzIjogInByZXZpZXcuY29tIiwNCiAgImF1ZCI6ICJ0ZXN0LmNvbSIsDQogICJpYXQiOiAxNTkwNjAwODI0LA0KICAiZXhwIjogMTU5MDY4NzIyNCwNCiAgInByb3ZpZGVyIjogImEuY29tIiwNCiAgImNsaWVudCI6ICJ0ZXN0LmNvbSIsDQogICJ1c2VyaWQiOiAiYUBiLmNvbSIsDQogICJlbWFpbCI6ICJhQGIuY29tIiwNCiAgImF1dGh6IjogIiIsDQogICJsYXN0bmFtZSI6ICJCIiwNCiAgImZpcnN0bmFtZSI6ICJBIiwNCiAgImNvdW50cnkiOiAiIiwNCiAgImNvbXBhbnkiOiAiIiwNCiAgImpvYnRpdGxlIjogIiIsDQogICJzdWJpZCI6ICJ1NUxTTkp1aFVmYUgweFAzdVlUbkl4Vk9BSjR4NkRKdWNXc3BzNWdEb280IiwNCiAgImlkcCI6ICJvMzY1IiwNCiAgImhkIjogInNsYi5jb20iLA0KICAiZGVzaWQiOiAiYUBkZXNpZC5jb20iLA0KICAiY29udGFjdF9lbWFpbCI6ICJhQGIuY29tIiwNCiAgInJ0X2hhc2giOiAieVMxcHY3a0NvaTZHVld2c3c4S3F5QSINCn0.tvE00W8cZZOZZDc83Sn4nKPBlw3boJEjJaTvOyvMXmNSTB4BN7kdLnhXy_CLQ4FZy0Y-PMboMGCH9gfKT1bYcQHllUTLnjtzd0iBJWY-I0ahoMEIa1PkksCytBz5qBGunrwr28PqW_t6GN99pUn0zxFn2022C17fnDHGdS1G2Tlag0Jpadl2PgdN_V9u2BndHgkKCFlmS2ZmX59KWQCOJmwnTd1k8vXCpUgDVjBK5CzCb9aFp8pjdy0mdMeV-7hYE2acyIrTPVZwAMAgHFdA")
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .content(objectMapper.writeValueAsString(updateGroupRequest)));
    }

    private List<UpdateGroupOperation> getRequestBody(String operation, String path, String... value) {
        List<UpdateGroupOperation> request = new ArrayList<>();
        List<String> operationValue = new ArrayList<>();
        for (String v : value) {
            operationValue.add(v);
        }

        UpdateGroupOperation updateGroupOperation = UpdateGroupOperation.builder()
                .operation(operation)
                .path(path)
                .value(operationValue)
                .build();
        request.add(updateGroupOperation);

        return request;
    }
}
