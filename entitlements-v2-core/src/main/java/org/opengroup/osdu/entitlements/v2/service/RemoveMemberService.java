package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.core.common.status.IEventPublisher;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeAction;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeEvent;
import org.opengroup.osdu.entitlements.v2.model.events.EntitlementsChangeType;
import org.opengroup.osdu.entitlements.v2.service.featureflag.FeatureFlag;
import org.opengroup.osdu.entitlements.v2.service.featureflag.PartitionFeatureFlagService;
import org.opengroup.osdu.entitlements.v2.validation.BootstrapGroupsConfigurationService;
import org.opengroup.osdu.entitlements.v2.validation.ServiceAccountsConfigurationService;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.spi.removemember.RemoveMemberRepo;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RemoveMemberService {
    private final RemoveMemberRepo removeMemberRepo;
    private final RetrieveGroupRepo retrieveGroupRepo;
    private final GroupCacheService groupCacheService;
    private final JaxRsDpsLog log;
    private final ServiceAccountsConfigurationService serviceAccountsConfigurationService;
    private final BootstrapGroupsConfigurationService bootstrapGroupsConfigurationService;
    private final PermissionService permissionService;
    private final RequestInfo requestInfo;
    private final PartitionFeatureFlagService partitionFeatureFlagService;

    @Autowired(required = false)
    private IEventPublisher eventPublisher;
    @Value("${event-publishing.enabled:false}")
    private Boolean eventPublishingEnabled;

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
        permissionService.verifyCanManageMembers(requesterNode, existingGroupEntityNode);
        EntityNode memberNode = retrieveGroupRepo.getMemberNodeForRemovalFromGroup(memberEmail, partitionId);
        removeMemberServiceDto.setChildrenReference(memberNode.getDirectChildReference(retrieveGroupRepo, existingGroupEntityNode).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(), String.format("Group %s does not have"
                    + " %s as a direct child/member. Please check the group hierarchy for an "
                    + "explicit member declaration.", groupEmail, memberEmail))
        ));

        checkIfMemberCanBeRemoved(groupEmail, memberEmail, existingGroupEntityNode, memberNode);

        Set<String> impactedUsers = removeMemberRepo.removeMember(existingGroupEntityNode, memberNode, removeMemberServiceDto);
        groupCacheService.refreshListGroupCache(impactedUsers, removeMemberServiceDto.getPartitionId());
        publishRemoveMemberEntitlementsChangeEvent(removeMemberServiceDto);
        return impactedUsers;
    }

    private void checkIfMemberCanBeRemoved(String groupEmail, String memberEmail, EntityNode existingGroupEntityNode, EntityNode memberNode) {
        if (serviceAccountsConfigurationService.isMemberProtectedServiceAccount(memberNode, existingGroupEntityNode)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    String.format("Key service accounts hierarchy is enforced, %s cannot be removed from group %s", memberEmail, groupEmail));
        }

        if (violateDataRootGroupHierarchy(memberNode, existingGroupEntityNode, memberNode.getDataPartitionId())) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "Users data root group hierarchy is enforced, member users.data.root cannot be removed");
        }

        if (bootstrapGroupsConfigurationService.isMemberProtectedFromRemoval(memberNode, existingGroupEntityNode)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    String.format("Bootstrap group hierarchy is enforced, member %s cannot be removed from group %s",
                            memberNode.getName(), existingGroupEntityNode.getName()));
        }

        //Removing a user from elementary data partition is not allowed unless it's the last group to be deleted
        //ADR: https://community.opengroup.org/osdu/platform/security-and-compliance/entitlements/-/issues/162
        if((groupEmail.equals(
                bootstrapGroupsConfigurationService.getElementaryDataPartitionUsersGroup(memberNode.getDataPartitionId()))
                && getDirectParentsEmails(memberNode.getDataPartitionId(), memberEmail).size() > 1)){
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    String.format("Member %s cannot be removed from elementary data partition group %s, since the user is still provisioned inside other groups. Please use Delete Member API to remove the user from all the groups.",
                            memberEmail, groupEmail));
        }
    }

    private void publishRemoveMemberEntitlementsChangeEvent(RemoveMemberServiceDto removeMemberServiceDto) {
        if (eventPublishingEnabled) {
            EntitlementsChangeEvent[] event = {EntitlementsChangeEvent.builder()
                    .kind(EntitlementsChangeType.groupChanged)
                    .group(removeMemberServiceDto.getGroupEmail())
                    .user(removeMemberServiceDto.getMemberEmail())
                    .action(EntitlementsChangeAction.remove)
                    .modifiedBy(removeMemberServiceDto.getRequesterId())
                    .modifiedOn(System.currentTimeMillis()).build()};
            eventPublisher.publish(event, requestInfo.getHeaders().getHeaders());
        }
    }

    private boolean violateDataRootGroupHierarchy(EntityNode memberNode, EntityNode existingGroupEntityNode, String dataPartitionId) {
        return !this.partitionFeatureFlagService.getFeature(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, dataPartitionId)
            && memberNode.isUsersDataRootGroup() && existingGroupEntityNode.isDataGroup();
    }

    public Set<String> getDirectParentsEmails(String dataPartitionId, String memberEmail) {
        List<ParentReference> directParents = retrieveGroupRepo.loadDirectParents(
                dataPartitionId, memberEmail);
        return directParents.parallelStream()
                .map(ParentReference::getId)
                .collect(Collectors.toSet());
    }
}
