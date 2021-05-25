package org.opengroup.osdu.entitlements.v2.spi.updateappids;

import org.opengroup.osdu.entitlements.v2.model.EntityNode;

import java.util.Set;

public interface UpdateAppIdsRepo {
    Set<String> updateAppIds(EntityNode groupNode, Set<String> allowedAppIds);
}
