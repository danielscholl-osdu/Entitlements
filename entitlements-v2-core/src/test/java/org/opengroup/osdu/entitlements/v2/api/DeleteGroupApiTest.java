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
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.deletegroup.DeleteGroupServiceDto;
import org.opengroup.osdu.entitlements.v2.service.DeleteGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@WebMvcTest(controllers = DeleteGroupApi.class)
@ComponentScan("org.opengroup.osdu.entitlements.v2")
public class DeleteGroupApiTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private DeleteGroupService service; //service used by api controller
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
        String groupId = "service.viewers.users@common.contoso.com";
        performDeleteGroupRequest(groupId).andExpect(status().isNoContent());
    }

    @Test
    public void shouldValidateGroupEmailParameter() throws Exception {
        String groupId = "service.viewers.com";
        performDeleteGroupRequest(groupId).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldCallService() throws Exception {
        ArgumentCaptor<EntityNode> entityNodeArgumentCaptor = ArgumentCaptor.forClass(EntityNode.class);
        ArgumentCaptor<DeleteGroupServiceDto> dgsDtoArgumentCaptor = ArgumentCaptor.forClass(DeleteGroupServiceDto.class);
        String groupId = "service.VIEWERS@common.contoso.com";
        performDeleteGroupRequest(groupId).andExpect(status().isNoContent());

        verify(service).run(entityNodeArgumentCaptor.capture(), dgsDtoArgumentCaptor.capture());
        EntityNode actualEntityNode = entityNodeArgumentCaptor.getValue();
        assertThat(actualEntityNode.getNodeId()).isEqualTo("service.viewers@common.contoso.com");
        assertThat(actualEntityNode.getName()).isEqualTo("service.viewers");
        assertThat(actualEntityNode.getDataPartitionId()).isEqualTo("common");
        assertThat(actualEntityNode.getType()).isEqualTo(NodeType.GROUP);
        DeleteGroupServiceDto deleteGroupServiceDto = dgsDtoArgumentCaptor.getValue();
        assertThat(deleteGroupServiceDto.getRequesterId()).isEqualTo("a@b.com");
        assertThat(deleteGroupServiceDto.getPartitionId()).isEqualTo("common");
    }

    private ResultActions performDeleteGroupRequest(String groupEmail) throws Exception {
        return mockMvc.perform(delete("/groups/{group_email}", groupEmail)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF8")
                .header(DpsHeaders.AUTHORIZATION, "Bearer token")
                .header(DpsHeaders.USER_ID,"a@b.com")
                .header(DpsHeaders.DATA_PARTITION_ID, "common"));
    }

}
