package org.opengroup.osdu.entitlements.v2.validation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.util.FileReaderService;
import org.powermock.reflect.Whitebox;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BootstrapGroupsConfigurationServiceTest {

    @Mock
    private AppProperties appProperties;
    @Spy
    private FileReaderService fileReaderService = new FileReaderService();

    @InjectMocks
    private BootstrapGroupsConfigurationService bootstrapGroupsConfigurationService;

    @Before
    public void setup() throws Exception {
        when(appProperties.getProtectedMembers()).thenReturn(Collections.singletonList("/test_groups.json"));
        Whitebox.invokeMethod(bootstrapGroupsConfigurationService, "init");
        verify(fileReaderService).readFile("/test_groups.json");
    }

    @Test
    public void shouldReturnTrueIfMemberIsProtectedFromRemoval() {
        EntityNode memberNode = EntityNode.builder().nodeId("memberId").name("users").type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode groupNode = EntityNode.builder().nodeId("groupId").name("data.default.viewers").type(NodeType.GROUP).dataPartitionId("common").build();
        Assert.assertTrue(bootstrapGroupsConfigurationService.isMemberProtectedFromRemoval(memberNode, groupNode));
    }

    @Test
    public void shouldReturnFalseIfMemberIsFromAnotherPartition() {
        EntityNode memberNode = EntityNode.builder().nodeId("memberId").name("users").type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode groupNode = EntityNode.builder().nodeId("groupId").name("data.default.viewers").type(NodeType.GROUP).dataPartitionId("tenant1").build();
        Assert.assertFalse(bootstrapGroupsConfigurationService.isMemberProtectedFromRemoval(memberNode, groupNode));
    }

    @Test
    public void shouldReturnFalseIfMemberIsUnprotected() {
        EntityNode memberNode = EntityNode.builder().nodeId("memberId").name("memberId").type(NodeType.GROUP).dataPartitionId("common").build();
        EntityNode groupNode = EntityNode.builder().nodeId("groupId").name("data.default.viewers").type(NodeType.GROUP).dataPartitionId("tenant1").build();
        Assert.assertFalse(bootstrapGroupsConfigurationService.isMemberProtectedFromRemoval(memberNode, groupNode));
    }
}
