package org.opengroup.osdu.entitlements.v2.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationFilter;
import org.opengroup.osdu.entitlements.v2.model.deletemember.DeleteMemberDto;
import org.opengroup.osdu.entitlements.v2.service.DeleteMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(DeleteMemberApi.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
public class DeleteMemberApiTest {
    private static final String TOKEN = "ew0KICAidHlwIjogIkpXVCIsDQogICJhbGciOiAiUlMyNTYiLA0KICAia2lkIjogIk1UVTVNRFUx" +
            "TkRrNU9BPT0iDQp9.ew0KICAic3ViIjogImFAYi5jb20iLA0KICAiaXNzIjogInByZXZpZXcuY29tIiwNCiAgImF1ZCI6ICJ0ZXN0LmN" +
            "vbSIsDQogICJpYXQiOiAxNTkwNjAwODI0LA0KICAiZXhwIjogMTU5MDY4NzIyNCwNCiAgInByb3ZpZGVyIjogImEuY29tIiwNCiAgImN" +
            "saWVudCI6ICJ0ZXN0LmNvbSIsDQogICJ1c2VyaWQiOiAiYUBiLmNvbSIsDQogICJlbWFpbCI6ICJhQGIuY29tIiwNCiAgImF1dGh6Ijo" +
            "gIiIsDQogICJsYXN0bmFtZSI6ICJCIiwNCiAgImZpcnN0bmFtZSI6ICJBIiwNCiAgImNvdW50cnkiOiAiIiwNCiAgImNvbXBhbnkiOiA" +
            "iIiwNCiAgImpvYnRpdGxlIjogIiIsDQogICJzdWJpZCI6ICJ1NUxTTkp1aFVmYUgweFAzdVlUbkl4Vk9BSjR4NkRKdWNXc3BzNWdEb28" +
            "0IiwNCiAgImlkcCI6ICJvMzY1IiwNCiAgImhkIjogInNsYi5jb20iLA0KICAiZGVzaWQiOiAiYUBkZXNpZC5jb20iLA0KICAiY29udGF" +
            "jdF9lbWFpbCI6ICJhQGIuY29tIiwNCiAgInJ0X2hhc2giOiAieVMxcHY3a0NvaTZHVld2c3c4S3F5QSINCn0.tvE00W8cZZOZZDc83Sn" +
            "4nKPBlw3boJEjJaTvOyvMXmNSTB4BN7kdLnhXy_CLQ4FZy0Y-PMboMGCH9gfKT1bYcQHllUTLnjtzd0iBJWY-I0ahoMEIa1PkksCytBz" +
            "5qBGunrwr28PqW_t6GN99pUn0zxFn2022C17fnDHGdS1G2Tlag0Jpadl2PgdN_V9u2BndHgkKCFlmS2ZmX59KWQCOJmwnTd1k8vXCpUg" +
            "DVjBK5CzCb9aFp8pjdy0mdMeV-7hYE2acyIrTPVZwAMAgHFdA";
    private static final String DATA_PARTITION_ID = "common";
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private TenantInfo tenantInfo;
    @MockBean
    private ITenantFactory tenantFactory;
    @MockBean
    private AuthorizationFilter authorizationFilter;
    @MockBean
    private DeleteMemberService deleteMemberService;

    @Before
    public void setup() {
        TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setDataPartitionId(DATA_PARTITION_ID);
        tenantInfo.setServiceAccount("internal-service-account");
        when(tenantFactory.listTenantInfo()).thenReturn(Collections.singletonList(tenantInfo));
        when(tenantFactory.getTenantInfo(DATA_PARTITION_ID)).thenReturn(tenantInfo);
        when(authorizationFilter.hasAnyPermission(any())).thenReturn(true);
    }

    @Test
    public void shouldMatchExpectedHttpRequest() throws Exception {
        performRequest("xxx@common.contoso.com")
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldValidatePartitionId() throws Exception {
        performRequest("xxx@common.contoso.com", "common1,common2")
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldInvokeDeleteMemberServiceWithExpectedArg() throws Exception {
        ArgumentCaptor<DeleteMemberDto> captor = ArgumentCaptor.forClass(DeleteMemberDto.class);

        performRequest("XXX@common.contoso.com")
                .andExpect(status().isNoContent());

        verify(deleteMemberService, times(1)).deleteMember(captor.capture());

        assertThat(captor.getValue().getMemberEmail()).isEqualTo("xxx@common.contoso.com");
        assertThat(captor.getValue().getRequesterId()).isEqualTo("a@b.com");
        assertThat(captor.getValue().getPartitionId()).isEqualTo(DATA_PARTITION_ID);
    }

    private ResultActions performRequest(String memberEmail) throws Exception {
        return performRequest(memberEmail, DATA_PARTITION_ID);
    }

    private ResultActions performRequest(String memberEmail, String partitionId) throws Exception {
        return mockMvc.perform(delete("/members/{member_email}", memberEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .header(DpsHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .header(DpsHeaders.USER_ID,"a@b.com")
                .header(DpsHeaders.DATA_PARTITION_ID, partitionId))
                .andDo(print());
    }
}