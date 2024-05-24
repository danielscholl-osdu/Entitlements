package org.opengroup.osdu.entitlements.v2.service;

import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
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
    private JaxRsDpsLog log;

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
     * Returns true if requestNode is a child of rootGroupNode
     */
    public boolean hasOwnerPermissionOf(final EntityNode requestNode, final EntityNode group, final String rootGroupId) {
        if ((group.isDataGroup() || group.isUserGroup()) && isCallerHasDataRootPermissions()){
            return true;
        }

        return Boolean.TRUE.equals(retrieveGroupRepo.hasDirectChild(group, ChildrenReference.createChildrenReference(requestNode, Role.OWNER))) ? true : false;
    }

    private boolean isCallerHasOpsPermissions() {
        try {
            return authorizationService.isCurrentUserAuthorized(requestInfo.getHeaders(), AppProperties.OPS);
        } catch (AppException e) {
            log.warning("Caller does not have OP permissions", e);
            return false;
        }
    }

    private boolean isCallerHasDataRootPermissions() {
        try {
            return authorizationService.isCurrentUserAuthorized(requestInfo.getHeaders(), AppProperties.DATA_ROOT);
        } catch (AppException e) {
            log.warning("Caller does not have DATA ROOT permissions", e);
            return false;
        }
    }
}
