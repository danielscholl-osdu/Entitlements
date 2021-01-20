package org.opengroup.osdu.entitlements.v2.service;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.RequestInfo;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.di.KeySvcAccBeanConfiguration;
import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
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
    private final JaxRsDpsLog log;
    private final KeySvcAccBeanConfiguration keySvcAccBeanConfiguration;
    private final RequestInfo requestInfo;
    private final AppProperties config;
    private final PermissionService permissionService;

    /**
     * @return a set of ids of impacted users
     */
    public Set<String> removeMember(RemoveMemberServiceDto removeMemberServiceDto) {
        log.info(String.format("requested by %s", removeMemberServiceDto.getRequesterId()));
        String memberDesId = removeMemberServiceDto.getMemberEmail();
        EntityNode existingGroupEntityNode = retrieveGroupRepo.groupExistenceValidation(removeMemberServiceDto.getGroupEmail(), removeMemberServiceDto.getPartitionId());
        EntityNode requesterNode = EntityNode.createMemberNodeForRequester(removeMemberServiceDto.getRequesterId(), removeMemberServiceDto.getPartitionId());
        final String serviceAccountId = requestInfo.getTenantInfo().getServiceAccount();
        if (!permissionService.hasOwnerPermissionOf(requesterNode, existingGroupEntityNode) &&
                !removeMemberServiceDto.getRequesterId().equalsIgnoreCase(serviceAccountId)) {
            throw new AppException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(), "Not authorized to manage members");
        }
        EntityNode memberNode = EntityNode.createNodeFromEmail(memberDesId, removeMemberServiceDto.getPartitionId(), config.getDomain());
        removeMemberServiceDto.setChildrenReference(memberNode.getDirectChildReference(retrieveGroupRepo, existingGroupEntityNode).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), String.format("Group %s does not have %s as a child/member", removeMemberServiceDto.getGroupEmail(), removeMemberServiceDto.getMemberEmail()))
        ));

        // check whether it deletes key service account from bootstrap groups
        if (keySvcAccBeanConfiguration.isKeySvcAccountInBootstrapGroup(removeMemberServiceDto.getGroupEmail(), removeMemberServiceDto.getMemberEmail())) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), String.format("Key service accounts hierarchy is enforced, %s cannot be removed from group %s", memberDesId, removeMemberServiceDto.getGroupEmail()));
        }

        if (memberNode.isUsersDataRootGroup() && (existingGroupEntityNode.isDataGroup() || existingGroupEntityNode.isUserGroup())) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "users data root group hierarchy is enforced, cannot be removed");
        }

        return removeMemberRepo.removeMember(existingGroupEntityNode, memberNode, removeMemberServiceDto);
    }
}
