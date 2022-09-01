package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.updateappids;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.DATA_PARTITION_ID;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getDataGroupNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getRequesterNode;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.SpiJdbcTestConfig;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.JdbcTemplateRunner;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.MemberRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.retrievegroup.RetrieveGroupRepoJdbc;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = SpiJdbcTestConfig.class)
@RunWith(SpringRunner.class)
public class UpdateAppIdsJdbcTest {

    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private AuditLogger auditLogger;

    @Autowired
    private UpdateAppIdsRepoJdbc sut;

    @MockBean
    private GroupRepository groupRepository;
    @MockBean
    private JdbcTemplateRunner jdbcTemplateRunner;
    @MockBean
    private MemberRepository memberRepository;

    @Autowired
    private RetrieveGroupRepoJdbc retrieveGroupRepoJdbc;

    @Test
    public void should_updateAppIds() {
        EntityNode requesterNode = getRequesterNode();
        EntityNode groupNode = getDataGroupNode("x");
        groupNode.setAppIds(new HashSet<>(Arrays.asList("app1", "app2")));

        GroupInfoEntity savedGroup = GroupInfoEntity.fromEntityNode(groupNode);
        savedGroup.setId(1L);

        when(jdbcTemplateRunner.getRecursiveParentIds(any())).thenReturn(Collections.singletonList(1L));
        when(groupRepository.findAllById(anyList())).thenReturn(Collections.singletonList(savedGroup));
        when(groupRepository.findByEmail(any())).thenReturn(Collections.singletonList(savedGroup));

        //when
        sut.updateAppIds(groupNode, new HashSet<>(Arrays.asList("app1", "app2")));

        //then
        verify(groupRepository).updateAppId(1L, "app1");
        verify(groupRepository).updateAppId(1L, "app2");


        Set<ParentReference> groups = retrieveGroupRepoJdbc.loadAllParents(requesterNode).getParentReferences();
        Set<ParentReference> noAppIdParents = retrieveGroupRepoJdbc.filterParentsByAppId(groups,
                DATA_PARTITION_ID, "no-app-id");
        Set<ParentReference> app1Parents = retrieveGroupRepoJdbc.filterParentsByAppId(groups,
                DATA_PARTITION_ID, "app1");
        Set<ParentReference> app2Parents = retrieveGroupRepoJdbc.filterParentsByAppId(groups,
                DATA_PARTITION_ID, "app2");

        assertTrue(noAppIdParents.isEmpty());
        assertTrue(app1Parents.stream()
                .map(ParentReference::getId)
                .collect(Collectors.toList())
                .contains(groupNode.getNodeId()));
        assertTrue(app2Parents.stream()
                .map(ParentReference::getId)
                .collect(Collectors.toList())
                .contains(groupNode.getNodeId()));
    }

}
