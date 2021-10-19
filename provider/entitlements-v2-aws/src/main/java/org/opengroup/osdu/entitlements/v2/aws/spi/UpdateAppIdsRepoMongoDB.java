package org.opengroup.osdu.entitlements.v2.aws.spi;

import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.spi.updateappids.UpdateAppIdsRepo;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class UpdateAppIdsRepoMongoDB extends BasicEntitlementsHelper implements UpdateAppIdsRepo {

    @Override
    public Set<String> updateAppIds(EntityNode groupNode, Set<String> allowedAppIds) {

        //TODO: check is need to replace IDS
        groupHelper.updateAppIds(groupNode, allowedAppIds);

        //TODO: return IDS then cash will work
        return new HashSet<>();
    }
}
