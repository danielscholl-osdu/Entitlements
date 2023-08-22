package org.opengroup.osdu.entitlements.v2.service;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.auth.AuthorizationService;
import org.opengroup.osdu.entitlements.v2.model.ChildrenReference;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.Role;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.opengroup.osdu.entitlements.v2.util.RequestInfoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    @Autowired
    private RetrieveGroupRepo retrieveGroupRepo;

    @Autowired
    private RequestInfo requestInfo;

    @Autowired
    private RequestInfoUtilService requestInfoUtilService;

    @Autowired
    private AuthorizationService authorizationService;

    public void verifyCanManageMembers(final EntityNode requestNode, final EntityNode group) {
        if (!hasOwnerPermissionOf(requestNode, group)) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Not authorized to manage members");
        }
    }

    /**
     * Returns true if requestNode id is same as internalService account
     * Returns true if requestNode has owner permissions of group
     * Returns true if requestNode is a direct child of rootGroupNode
     */
    public boolean hasOwnerPermissionOf(final EntityNode requestNode, final EntityNode group) {
        final String internalServiceAccount = requestInfo.getTenantInfo().getServiceAccount();
        if (requestNode.getNodeId().equalsIgnoreCase(internalServiceAccount)) {
            return true;
        }
        if (isCallerHasOpsPermissions()) {
            return true;
        }
        final String dataPartitionId = requestNode.getDataPartitionId();
        final String rootGroupId = String.format(EntityNode.ROOT_DATA_GROUP_EMAIL_FORMAT, requestInfoUtilService.getDomain(dataPartitionId));
        return hasOwnerPermissionOf(requestNode, group, rootGroupId);
    }

    /**
     * Returns true if requestNode has owner permissions of group
     * Returns true if requestNode is a direct child of rootGroupNode
     */
    public boolean hasOwnerPermissionOf(final EntityNode requestNode, final EntityNode group, final String rootGroupId) {
        if (Boolean.TRUE.equals(retrieveGroupRepo.hasDirectChild(group, ChildrenReference.createChildrenReference(requestNode, Role.OWNER)))) {
            return true;
        }
        final String dataPartitionId = requestNode.getDataPartitionId();
        EntityNode dataRootGroupNode = retrieveGroupRepo.groupExistenceValidation(rootGroupId, dataPartitionId);
        return (group.isDataGroup() || group.isUserGroup()) && hasDirectChildReference(requestNode, dataRootGroupNode);
    }

    private boolean isCallerHasOpsPermissions() {
        try {
            return authorizationService.isCurrentUserAuthorized(requestInfo.getHeaders(), AppProperties.OPS);
        } catch (AppException e) {
            return false;
        }
    }

    private boolean hasDirectChildReference(EntityNode requestNode, EntityNode groupNode) {
        ChildrenReference ownerRef = ChildrenReference.createChildrenReference(requestNode, Role.OWNER);
        if (Boolean.TRUE.equals(retrieveGroupRepo.hasDirectChild(groupNode, ownerRef))) {
            return true;
        }
        ChildrenReference memberRef = ChildrenReference.createChildrenReference(requestNode, Role.MEMBER);
        return Boolean.TRUE.equals(retrieveGroupRepo.hasDirectChild(groupNode, memberRef));
    }
}
