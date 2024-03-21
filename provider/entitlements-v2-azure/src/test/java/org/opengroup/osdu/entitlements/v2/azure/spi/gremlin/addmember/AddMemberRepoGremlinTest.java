package org.opengroup.osdu.entitlements.v2.azure.spi.gremlin.addmember;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.azure.graph.GraphService;
import org.opengroup.osdu.azure.util.AuthUtils;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;
import org.opengroup.osdu.entitlements.v2.azure.service.GraphTraversalSourceUtilService;
import org.opengroup.osdu.entitlements.v2.logging.AuditLogger;
import org.opengroup.osdu.entitlements.v2.model.ChildrenTreeDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.NodeType;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
@ExtendWith(MockitoExtension.class)
class AddMemberRepoGremlinTest {
    @InjectMocks
    AddMemberRepoGremlin sut;
    @Mock
    private GraphTraversalSourceUtilService graphTraversalSourceUtilService;

    @Mock
    private RetrieveGroupRepo retrieveGroupRepo;

    @Mock
    private AuthUtils authUtils;

    @Mock
    private GraphService graphService;

    @Mock
    private IFeatureFlag featureFlag;

    @Mock
    private RequestInfo requestInfo;

    @Mock
    private DpsHeaders dpsHeaders;

    @Mock
    private TenantInfo tenantInfo;

    @Mock
    private AuditLogger auditLogger;

    private static String AAD_TOKEN = "921828";

    private static String GROUP_EMAIL = "group-mail";

    private static String USER_OID = "user OID";
    
    private static String SERVICE_ACCOUNT_CLIENT_ID = "service-account";
    
    private static String OID_VALIDATION_FEATURE_FLAG = "oid_validation";
    private static String DATA_PARTITION = "data-partition";
    
    @BeforeEach
    void init(){
        ReflectionTestUtils.setField(sut, "graphService", graphService);
        ReflectionTestUtils.setField(sut, "featureFlag", featureFlag);
        ReflectionTestUtils.setField(sut, "authUtils", authUtils);
        ReflectionTestUtils.setField(sut, "requestInfo", requestInfo);

        lenient().when(requestInfo.getHeaders()).thenReturn(dpsHeaders);
        lenient().when(dpsHeaders.getAuthorization()).thenReturn(String.format("Bearer %s", AAD_TOKEN));
        lenient().when(dpsHeaders.getPartitionId()).thenReturn(DATA_PARTITION);

        lenient().when(requestInfo.getTenantInfo()).thenReturn(tenantInfo);
        lenient().when(tenantInfo.getServiceAccount()).thenReturn(SERVICE_ACCOUNT_CLIENT_ID);
    }

    @Test
    void addMember_allowsToAdd_withoutAnyValidation_whenFeatureFlagIsOff () {
        EntityNode node = new EntityNode();
        node.setType(NodeType.GROUP);
        node.setNodeId(GROUP_EMAIL);

        AddMemberRepoDto addMemberDto = mock(AddMemberRepoDto.class);
        EntityNode userNode = new EntityNode();
        userNode.setType(NodeType.USER);
        userNode.setNodeId(USER_OID);

        when(addMemberDto.getMemberNode()).thenReturn(userNode);
        when(addMemberDto.getRole()).thenReturn(Role.MEMBER);
        when(featureFlag.isFeatureEnabled(OID_VALIDATION_FEATURE_FLAG)).thenReturn(false);

        ChildrenTreeDto childrenTreeDto = mock(ChildrenTreeDto.class);
        
        when(retrieveGroupRepo.loadAllChildrenUsers(userNode)).thenReturn(childrenTreeDto);
        
        sut.addMember(node, addMemberDto);

        verify(graphService, times(0)).isOidValid(any(), any());
    }

    @Test
    void addMember_throwsAppException_whenInvalidOidIsProvided () {
        EntityNode node = new EntityNode();
        node.setType(NodeType.GROUP);
        node.setNodeId(GROUP_EMAIL);

        AddMemberRepoDto addMemberDto = mock(AddMemberRepoDto.class);
        EntityNode userNode = new EntityNode();
        userNode.setType(NodeType.USER);
        userNode.setNodeId(USER_OID);

        when(addMemberDto.getMemberNode()).thenReturn(userNode);
        when(featureFlag.isFeatureEnabled(OID_VALIDATION_FEATURE_FLAG)).thenReturn(true);
        when(graphService.isOidValid(DATA_PARTITION, USER_OID)).thenReturn(false);

        when(authUtils.isAadToken(AAD_TOKEN)).thenReturn(true);

        AppException exception = Assertions.assertThrows(AppException.class, ()->{
            sut.addMember(node, addMemberDto);
        });

        assertEquals(400, exception.getError().getCode());
    }

    @Test
    void addMember_allowsToAdd_withoutAnyValidation_whenAddedMemberTypeIsGroup () {
        EntityNode node = new EntityNode();
        node.setType(NodeType.GROUP);
        node.setNodeId(GROUP_EMAIL);

        AddMemberRepoDto addMemberDto = mock(AddMemberRepoDto.class);
        EntityNode userNode = new EntityNode();
        userNode.setType(NodeType.GROUP);
        userNode.setNodeId("member group");

        when(addMemberDto.getMemberNode()).thenReturn(userNode);
        when(featureFlag.isFeatureEnabled(OID_VALIDATION_FEATURE_FLAG)).thenReturn(true);
        when(addMemberDto.getRole()).thenReturn(Role.MEMBER);

        ChildrenTreeDto childrenTreeDto = mock(ChildrenTreeDto.class);

        when(retrieveGroupRepo.loadAllChildrenUsers(userNode)).thenReturn(childrenTreeDto);

        sut.addMember(node, addMemberDto);

        verify(graphService, times(0)).isOidValid(any(), any());
        verify(graphTraversalSourceUtilService, times(2)).addEdge(any());
    }

    @Test
    void addMember_allowsToAdd_whenAuthTokenProvidedIsNotIssuedByAAD () {
        EntityNode node = new EntityNode();
        node.setType(NodeType.GROUP);
        node.setNodeId(GROUP_EMAIL);

        AddMemberRepoDto addMemberDto = mock(AddMemberRepoDto.class);
        EntityNode userNode = new EntityNode();
        userNode.setType(NodeType.USER);
        userNode.setNodeId(USER_OID);

        when(addMemberDto.getMemberNode()).thenReturn(userNode);
        when(featureFlag.isFeatureEnabled(OID_VALIDATION_FEATURE_FLAG)).thenReturn(true);
        when(authUtils.isAadToken(AAD_TOKEN)).thenReturn(false);
        when(addMemberDto.getRole()).thenReturn(Role.MEMBER);

        ChildrenTreeDto childrenTreeDto = mock(ChildrenTreeDto.class);
        when(retrieveGroupRepo.loadAllChildrenUsers(userNode)).thenReturn(childrenTreeDto);

        sut.addMember(node, addMemberDto);

        verify(graphService, times(0)).isOidValid(any(), any());
        verify(requestInfo, times(0)).getTenantInfo();
        verify(graphTraversalSourceUtilService, times(2)).addEdge(any());
    }

    @Test
    void addMember_allowsToAdd_whenServicePrincipalClientIdIsAdded () {
        EntityNode node = new EntityNode();
        node.setType(NodeType.GROUP);
        node.setNodeId(GROUP_EMAIL);

        AddMemberRepoDto addMemberDto = mock(AddMemberRepoDto.class);
        EntityNode userNode = new EntityNode();
        userNode.setType(NodeType.USER);
        userNode.setNodeId(SERVICE_ACCOUNT_CLIENT_ID);

        when(addMemberDto.getMemberNode()).thenReturn(userNode);
        when(featureFlag.isFeatureEnabled(OID_VALIDATION_FEATURE_FLAG)).thenReturn(true);
        when(authUtils.isAadToken(AAD_TOKEN)).thenReturn(true);

        when(addMemberDto.getRole()).thenReturn(Role.MEMBER);

        ChildrenTreeDto childrenTreeDto = mock(ChildrenTreeDto.class);
        when(retrieveGroupRepo.loadAllChildrenUsers(userNode)).thenReturn(childrenTreeDto);

        sut.addMember(node, addMemberDto);

        verify(graphService, times(0)).isOidValid(any(), any());
        verify(requestInfo, times(1)).getTenantInfo();
        verify(graphTraversalSourceUtilService, times(2)).addEdge(any());
    }

    @Test
    void addMember_allowsToAdd_whenValidOidIsAdded () {
        EntityNode node = new EntityNode();
        node.setType(NodeType.GROUP);
        node.setNodeId(GROUP_EMAIL);

        AddMemberRepoDto addMemberDto = mock(AddMemberRepoDto.class);
        EntityNode userNode = new EntityNode();
        userNode.setType(NodeType.USER);
        userNode.setNodeId(USER_OID);

        when(addMemberDto.getMemberNode()).thenReturn(userNode);
        when(featureFlag.isFeatureEnabled(OID_VALIDATION_FEATURE_FLAG)).thenReturn(true);
        when(authUtils.isAadToken(AAD_TOKEN)).thenReturn(true);
        when(graphService.isOidValid(any(), any())).thenReturn(true);
        when(addMemberDto.getRole()).thenReturn(Role.MEMBER);

        ChildrenTreeDto childrenTreeDto = mock(ChildrenTreeDto.class);
        when(retrieveGroupRepo.loadAllChildrenUsers(userNode)).thenReturn(childrenTreeDto);

        sut.addMember(node, addMemberDto);

        verify(graphService, times(1)).isOidValid(any(), any());
        verify(requestInfo, times(1)).getTenantInfo();
        verify(graphTraversalSourceUtilService, times(2)).addEdge(any());
    }
}