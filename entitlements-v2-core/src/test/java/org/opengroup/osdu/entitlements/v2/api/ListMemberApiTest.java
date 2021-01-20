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
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberRequestArgs;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberResponseDto;
import org.opengroup.osdu.entitlements.v2.model.listmember.ListMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.model.listmember.MemberDto;
import org.opengroup.osdu.entitlements.v2.service.ListMemberService;
import org.opengroup.osdu.entitlements.v2.spi.tenantinfo.TenantInfoRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = ListMemberApi.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
public class ListMemberApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ListMemberService service; //service used by api controller
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private ITenantFactory tenantFactory;
    @MockBean
    private AuthorizationService authService;
    @MockBean
    private TenantInfoRepo tenantInfoRepo;

    @Before
    public void setup() {
        List<ChildrenReference> output = new ArrayList<>();
        output.add(ChildrenReference.builder().id("user.owner@dp.domain.com").dataPartitionId("dp").role(Role.OWNER).type(NodeType.USER).build());
        output.add(ChildrenReference.builder().id("group.owner@dp.domain.com").dataPartitionId("dp").role(Role.OWNER).type(NodeType.GROUP).build());
        output.add(ChildrenReference.builder().id("member.user@dp.domain.com").dataPartitionId("dp").role(Role.MEMBER).type(NodeType.USER).build());
        output.add(ChildrenReference.builder().id("member.group@dp.domain.com").dataPartitionId("dp").role(Role.MEMBER).type(NodeType.GROUP).build());
        when(service.run(any(ListMemberServiceDto.class))).thenReturn(output);
        final TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setDataPartitionId("common");
        tenantInfo.setServiceAccount("internal-service-account");
        when(tenantFactory.listTenantInfo()).thenReturn(Collections.singletonList(tenantInfo));
        when(tenantInfoRepo.getServiceAccountOrServicePrincipal(any())).thenReturn("serviceaccount");
        when(authService.isAuthorized(any(),any())).thenReturn(true);
    }

    @Test
    public void shouldMatchExpectedHttpRequest() throws Exception {
        String group = "service.viewers.users@common.contoso.com";
        ListMemberRequestArgs args = ListMemberRequestArgs.builder().build();
        performListMemberRequest(group, args).andExpect(status().isOk());
    }

    @Test
    public void shouldValidateGroupEmailParameter() throws Exception {
        String group = "service.viewers.com";
        ListMemberRequestArgs args = ListMemberRequestArgs.builder().build();
        performListMemberRequest(group, args).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldSerializeOutputGivenDefaultQueryParameters() throws Exception {
        String group = "service.viewers.users@common.contoso.com";
        ListMemberRequestArgs args = ListMemberRequestArgs.builder().build();
        MemberDto m1 = MemberDto.builder().email("user.owner@dp.domain.com").role(Role.OWNER).build();
        MemberDto m2 = MemberDto.builder().email("group.owner@dp.domain.com").role(Role.OWNER).build();
        MemberDto m3 = MemberDto.builder().email("member.user@dp.domain.com").role(Role.MEMBER).build();
        MemberDto m4 = MemberDto.builder().email("member.group@dp.domain.com").role(Role.MEMBER).build();
        List<MemberDto> members = new ArrayList<>();
        members.add(m1);
        members.add(m2);
        members.add(m3);
        members.add(m4);
        ListMemberResponseDto expectedResult = ListMemberResponseDto.builder()
                .members(members)
                .build();

        String result = performListMemberRequest(group, args).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    @Test
    public void shouldCallServiceWithExpectedInputs() throws Exception {
        String group = "service.VIEWERS.users@common.contoso.com";
        ListMemberRequestArgs args = ListMemberRequestArgs.builder().build();
        ArgumentCaptor<ListMemberServiceDto> captor = ArgumentCaptor.forClass(ListMemberServiceDto.class);

        performListMemberRequest(group, args).andExpect(status().isOk());

        verify(service, times(1)).run(captor.capture());

        assertThat(captor.getValue().getGroupId()).isEqualTo("service.viewers.users@common.contoso.com");
        assertThat(captor.getValue().getRequesterId()).isEqualTo("a@b.com");
        assertThat(captor.getValue().getPartitionId()).isEqualTo("common");
    }

    @Test
    public void shouldReturnGivenIncludeTypeIsTrue() throws Exception {
        String group = "service.viewers.users@common.contoso.com";
        ListMemberRequestArgs args = ListMemberRequestArgs.builder().includeType(Boolean.TRUE).build();
        MemberDto m1 = MemberDto.builder().email("user.owner@dp.domain.com").role(Role.OWNER).memberType(NodeType.USER).build();
        MemberDto m2 = MemberDto.builder().email("group.owner@dp.domain.com").role(Role.OWNER).memberType(NodeType.GROUP).dataPartitionId("dp").build();
        MemberDto m3 = MemberDto.builder().email("member.user@dp.domain.com").role(Role.MEMBER).memberType(NodeType.USER).build();
        MemberDto m4 = MemberDto.builder().email("member.group@dp.domain.com").role(Role.MEMBER).memberType(NodeType.GROUP).dataPartitionId("dp").build();
        List<MemberDto> members = new ArrayList<>();
        members.add(m1);
        members.add(m2);
        members.add(m3);
        members.add(m4);
        ListMemberResponseDto expectedResult = ListMemberResponseDto.builder()
                .members(members)
                .build();

        String result = performListMemberRequest(group, args).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    @Test
    public void shouldReturnGivenRoleIsMember() throws Exception {
        String group = "service.viewers.users@common.contoso.com";
        ListMemberRequestArgs args = ListMemberRequestArgs.builder().role(Role.MEMBER).build();
        MemberDto m1 = MemberDto.builder().email("member.user@dp.domain.com").role(Role.MEMBER).build();
        MemberDto m2 = MemberDto.builder().email("member.group@dp.domain.com").role(Role.MEMBER).build();
        List<MemberDto> members = new ArrayList<>();
        members.add(m1);
        members.add(m2);
        ListMemberResponseDto expectedResult = ListMemberResponseDto.builder()
                .members(members)
                .build();

        String result = performListMemberRequest(group, args).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    @Test
    public void shouldReturnGivenRoleIsOwner() throws Exception {
        String group = "service.viewers.users@common.contoso.com";
        ListMemberRequestArgs args = ListMemberRequestArgs.builder().role(Role.OWNER).build();
        MemberDto m1 = MemberDto.builder().email("user.owner@dp.domain.com").role(Role.OWNER).build();
        MemberDto m2 = MemberDto.builder().email("group.owner@dp.domain.com").role(Role.OWNER).build();
        List<MemberDto> members = new ArrayList<>();
        members.add(m1);
        members.add(m2);
        ListMemberResponseDto expectedResult = ListMemberResponseDto.builder()
                .members(members)
                .build();

        String result = performListMemberRequest(group, args).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    @Test
    public void shouldReturnGivenRoleIsMemberAndIncludeTypeIsTrue() throws Exception {
        String group = "service.viewers.users@common.contoso.com";
        ListMemberRequestArgs args = ListMemberRequestArgs.builder().role(Role.MEMBER).includeType(Boolean.TRUE).build();
        MemberDto m1 = MemberDto.builder().email("member.user@dp.domain.com").role(Role.MEMBER).memberType(NodeType.USER).build();
        MemberDto m2 = MemberDto.builder().email("member.group@dp.domain.com").role(Role.MEMBER).memberType(NodeType.GROUP).dataPartitionId("dp").build();
        List<MemberDto> members = new ArrayList<>();
        members.add(m1);
        members.add(m2);
        ListMemberResponseDto expectedResult = ListMemberResponseDto.builder()
                .members(members)
                .build();

        String result = performListMemberRequest(group, args).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));

    }

    private ResultActions performListMemberRequest(String groupEmail, ListMemberRequestArgs args) throws Exception {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("includeType", args.getIncludeType().toString());
        if (args.getRole() != null) {
            queryParams.add("role", args.getRole().toString());
        }

        return mockMvc.perform(get("/groups/{group_email}/members", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer ew0KICAidHlwIjogIkpXVCIsDQogICJhbGciOiAiUlMyNTYiLA0KICAia2lkIjogIk1UVTVNRFUxTkRrNU9BPT0iDQp9.ew0KICAic3ViIjogImFAYi5jb20iLA0KICAiaXNzIjogInByZXZpZXcuY29tIiwNCiAgImF1ZCI6ICJ0ZXN0LmNvbSIsDQogICJpYXQiOiAxNTkwNjAwODI0LA0KICAiZXhwIjogMTU5MDY4NzIyNCwNCiAgInByb3ZpZGVyIjogImEuY29tIiwNCiAgImNsaWVudCI6ICJ0ZXN0LmNvbSIsDQogICJ1c2VyaWQiOiAiYUBiLmNvbSIsDQogICJlbWFpbCI6ICJhQGIuY29tIiwNCiAgImF1dGh6IjogIiIsDQogICJsYXN0bmFtZSI6ICJCIiwNCiAgImZpcnN0bmFtZSI6ICJBIiwNCiAgImNvdW50cnkiOiAiIiwNCiAgImNvbXBhbnkiOiAiIiwNCiAgImpvYnRpdGxlIjogIiIsDQogICJzdWJpZCI6ICJ1NUxTTkp1aFVmYUgweFAzdVlUbkl4Vk9BSjR4NkRKdWNXc3BzNWdEb280IiwNCiAgImlkcCI6ICJvMzY1IiwNCiAgImhkIjogInNsYi5jb20iLA0KICAiZGVzaWQiOiAiYUBkZXNpZC5jb20iLA0KICAiY29udGFjdF9lbWFpbCI6ICJhQGIuY29tIiwNCiAgInJ0X2hhc2giOiAieVMxcHY3a0NvaTZHVld2c3c4S3F5QSINCn0.tvE00W8cZZOZZDc83Sn4nKPBlw3boJEjJaTvOyvMXmNSTB4BN7kdLnhXy_CLQ4FZy0Y-PMboMGCH9gfKT1bYcQHllUTLnjtzd0iBJWY-I0ahoMEIa1PkksCytBz5qBGunrwr28PqW_t6GN99pUn0zxFn2022C17fnDHGdS1G2Tlag0Jpadl2PgdN_V9u2BndHgkKCFlmS2ZmX59KWQCOJmwnTd1k8vXCpUgDVjBK5CzCb9aFp8pjdy0mdMeV-7hYE2acyIrTPVZwAMAgHFdA")
                .header(DpsHeaders.DATA_PARTITION_ID, "common")
                .queryParams(queryParams));
    }
}
