package org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.updateappids;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.junit.Assert.assertTrue;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.DATA_PARTITION_ID;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getDataGroupNode;
import static org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.util.JdbcTestDataProvider.getRequesterNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.entitlements.v2.jdbc.model.GroupInfoEntity;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.addmember.AddMemberRepoJdbc;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.repository.GroupRepository;
import org.opengroup.osdu.entitlements.v2.jdbc.spi.jdbc.retrievegroup.RetrieveGroupRepoJdbc;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit4.SpringRunner;

@Sql(executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:db/create-db-script.sql")
@Sql(executionPhase = ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:db/drop-db-script.sql")
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@SpringBootTest
@RunWith(SpringRunner.class)
public class UpdateAppIdsJdbcTest {

    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private AuditLogger auditLogger;

    @Autowired
    private UpdateAppIdsRepoJdbc sut;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private RetrieveGroupRepoJdbc retrieveGroupRepoJdbc;
    @Autowired
    private AddMemberRepoJdbc addMemberRepoJdbc;

    @Test
    public void should_updateAppIds() {
        EntityNode requesterNode = getRequesterNode();
        EntityNode groupNode = getDataGroupNode("x");

        groupRepository.save(GroupInfoEntity.fromEntityNode(groupNode));
        addMemberRepoJdbc.addMember(groupNode, AddMemberRepoDto.builder()
                .memberNode(requesterNode)
                .role(Role.MEMBER)
                .partitionId(DATA_PARTITION_ID)
                .build());

        //when
        sut.updateAppIds(groupNode, new HashSet<>(Arrays.asList("app1", "app2")));

        //then

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
