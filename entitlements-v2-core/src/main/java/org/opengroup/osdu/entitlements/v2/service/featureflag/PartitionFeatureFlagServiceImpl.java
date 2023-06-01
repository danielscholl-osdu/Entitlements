package org.opengroup.osdu.entitlements.v2.service.featureflag;

import lombok.RequiredArgsConstructor;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartitionFeatureFlagServiceImpl implements PartitionFeatureFlagService {
    private final IFeatureFlag featureFlag;
    private final JaxRsDpsLog log;
    private final FeatureFlagCache featureFlagCache;

    @Override
    public boolean getFeature(String ffName, String dataPartitionId) {
        String cacheKey = String.format("%s-%s", ffName, dataPartitionId);
        Boolean featureFlag = this.featureFlagCache.getFeatureFlag(ffName, cacheKey);
        if (featureFlag != null) {
            return featureFlag;
        }
        boolean ffValue = false;
        try {
            ffValue = this.featureFlag.isFeatureEnabled(ffName);
        } catch (Exception e) {
            this.log.error(String.format("PartitionService: Error getting %s for dataPartition with Id: %s", ffName, dataPartitionId), e);
        }
        this.featureFlagCache.setFeatureFlag(ffName, cacheKey, ffValue);
        return ffValue;
    }
}
