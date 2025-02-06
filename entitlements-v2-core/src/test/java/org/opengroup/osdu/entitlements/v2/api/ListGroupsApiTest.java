package org.opengroup.osdu.entitlements.v2.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.core.common.provider.interfaces.ITenantFactory;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.listgroup.ListGroupResponseDto;
import org.opengroup.osdu.entitlements.v2.service.ListGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = ListGroupApi.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
public class ListGroupsApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ListGroupService listGroupService;
    @MockBean
    private JaxRsDpsLog logger;
    @MockBean
    private ITenantFactory tenantFactory;
    @MockBean
    private AuthorizationService authService;

    @Before
    public void setup() {
        Set<ParentReference> output = new HashSet<>();
        output.add(ParentReference.builder().name("viewers").id("viewers@dp.domain.com")
                .description("").dataPartitionId("dp").build());
        output.add(ParentReference.builder().name("data.x").id("data.x@dp.domain.com")
                .description("a data group").dataPartitionId("dp").build());
        when(listGroupService.getGroups(any())).thenReturn(output);
        final TenantInfo tenantInfo = new TenantInfo();
        tenantInfo.setDataPartitionId("dp");
        tenantInfo.setServiceAccount("internal-service-account");
        when(tenantFactory.getTenantInfo("dp")).thenReturn(tenantInfo);
        when(authService.isCurrentUserAuthorized(any(),any())).thenReturn(true);
    }

    @Test
    public void shouldMatchExpectedHttpRequest() throws Exception {
        performListGroupRequest("dp").andExpect(status().isOk());
    }

    @Test
    public void shouldSerializeOutput() throws Exception {
        ParentReference g1 = ParentReference.builder().name("data.x").id("data.x@dp.domain.com").description("a data group").build();
        ParentReference g2 = ParentReference.builder().name("viewers").id("viewers@dp.domain.com").description("").build();
        List<ParentReference> groups = Arrays.asList(g1, g2);
        ListGroupResponseDto expectedResult = ListGroupResponseDto.builder().desId("a@b.com").memberEmail("a@b.com").groups(groups).build();

        String result = performListGroupRequest("dp").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(result)
                .isEqualToIgnoringWhitespace(objectMapper.writeValueAsString(expectedResult));
    }

    private ResultActions performListGroupRequest(String partitionId) throws Exception {
        return mockMvc.perform(get("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.USER_ID,"A@b.com")
                .header(DpsHeaders.DATA_PARTITION_ID, partitionId));
    }
}
