package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.validation.BootstrapGroupsConfigurationService;
import org.opengroup.osdu.entitlements.v2.validation.ServiceAccountsConfigurationService;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.removemember.RemoveMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RemoveMemberService {
    private final RemoveMemberRepo removeMemberRepo;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final GroupCacheService groupCacheService;
    private final JaxRsDpsLog log;
    private final ServiceAccountsConfigurationService serviceAccountsConfigurationService;
    private final BootstrapGroupsConfigurationService bootstrapGroupsConfigurationService;
    private final RequestInfo requestInfo;
    private final PermissionService permissionService;

    /**
     * @return a set of ids of impacted users
     */
    public Set<String> removeMember(RemoveMemberServiceDto removeMemberServiceDto) {
        log.info(String.format("requested by %s", removeMemberServiceDto.getRequesterId()));
        String groupEmail = removeMemberServiceDto.getGroupEmail();
        String memberEmail = removeMemberServiceDto.getMemberEmail();
        String partitionId = removeMemberServiceDto.getPartitionId();
        EntityNode existingGroupEntityNode = retrieveGroupRepo.groupExistenceValidation(groupEmail, partitionId);
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester(removeMemberServiceDto.getRequesterId(), partitionId);
        final String serviceAccountId = requestInfo.getTenantInfo().getServiceAccount();
        if (!permissionService.hasOwnerPermissionOf(requesterNode, existingGroupEntityNode) &&
                !removeMemberServiceDto.getRequesterId().equalsIgnoreCase(serviceAccountId)) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Not authorized to manage members");
        }
        EntityNode memberNode = getEntityNode(memberEmail, partitionId);
        removeMemberServiceDto.setChildrenReference(memberNode.getDirectChildReference(retrieveGroupRepo, existingGroupEntityNode).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), String.format("Group %s does not have %s as a child/member", groupEmail, memberEmail))
        ));

        if (serviceAccountsConfigurationService.isMemberProtectedServiceAccount(memberNode, existingGroupEntityNode)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    String.format("Key service accounts hierarchy is enforced, %s cannot be removed from group %s", memberEmail, groupEmail));
        }

        if (memberNode.isUsersDataRootGroup() && (existingGroupEntityNode.isDataGroup() || existingGroupEntityNode.isUserGroup())) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "Users data root group hierarchy is enforced, member users.data.root cannot be removed");
        }

        if (bootstrapGroupsConfigurationService.isMemberProtectedFromRemoval(memberNode, existingGroupEntityNode)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    String.format("Bootstrap group hierarchy is enforced, member %s cannot be removed from group %s",
                            memberNode.getName(), existingGroupEntityNode.getName()));
        }

        Set<String> impactedUsers = removeMemberRepo.removeMember(existingGroupEntityNode, memberNode, removeMemberServiceDto);
        groupCacheService.refreshListGroupCache(impactedUsers, removeMemberServiceDto.getPartitionId());
        return impactedUsers;
    }

    private EntityNode getEntityNode(String id, String partitionId) {
        return retrieveGroupRepo.getEntityNode(id, partitionId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        String.format("Not found entity node by email: %s and partitionId: %s", id, partitionId)));
    }
}
