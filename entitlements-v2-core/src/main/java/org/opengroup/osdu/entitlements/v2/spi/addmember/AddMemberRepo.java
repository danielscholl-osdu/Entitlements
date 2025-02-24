package org.opengroup.osdu.entitlements.v2.spi.addmember;

import org.opengroup.osdu.entitlements.v2.model.addmember.AddMemberRepoDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.Operation;

import java.util.Deque;
import java.util.Set;

public interface AddMemberRepo {
    /**
     * In case of unexpected error all changes made are reverted.
     * <p>
     * Returns impacted users, they should be explicitly processed to refresh the cache.
     * For null-safe, never returns null, instead of it returns an empty set.
     */
    Set<String> addMember(EntityNode groupNode, AddMemberRepoDto addMemberRepoDto);

    /**
     * In case of unexpected error all changes made are not reverted.
     * <p>
     * Returns impacted users, they should be explicitly processed to refresh the cache.
     * For null-safe, never returns null, instead of it returns an empty set.
     */
    Set<String> addMember(Deque<Operation> executedCommandsDeque, EntityNode groupEntityNode, AddMemberRepoDto addMemberRepoDto);
}
