package org.opengroup.osdu.entitlements.v2.spi.renamegroup;

import org.opengroup.osdu.entitlements.v2.model.EntityNode;

import java.util.Set;

public interface RenameGroupRepo {
    Set<String> run(EntityNode groupNode, String newGroupName);
}
