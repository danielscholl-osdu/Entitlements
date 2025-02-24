package org.opengroup.osdu.entitlements.v2.spi.removemember;

import org.opengroup.osdu.entitlements.v2.model.removemember.RemoveMemberServiceDto;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;

import java.util.Set;

public interface RemoveMemberRepo {

    /**
     * Returns impacted users, they should be explicitly processed to refresh the cache.
     * For null-safe, never returns null, instead of it returns an empty set.
     */
    Set<String> removeMember(EntityNode groupNode, EntityNode memberNode, RemoveMemberServiceDto removeMemberServiceDto);
}
