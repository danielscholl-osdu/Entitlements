package org.opengroup.osdu.entitlements.v2.service.featureflag;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.*;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.opengroup.osdu.entitlements.v2.config.PartitionFeatureFlagConfig;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PartitionFeatureFlagServiceImpl implements PartitionFeatureFlagService {
    private final IPartitionFactory factory;
    private final IServiceAccountJwtClient tokenService;
    private final JaxRsDpsLog log;
    private final FeatureFlagCache featureFlagCache;
    private final DpsHeaders headers;
    private final PartitionFeatureFlagConfig partitionFeatureFlagConfig;

    @Override
    public boolean getFeature(String ffName, String dataPartitionId) {
        String cacheKey = String.format("%s-%s", ffName, dataPartitionId);
        Boolean featureFlag = this.featureFlagCache.getFeatureFlag(ffName, cacheKey);
        if (featureFlag != null) {
            return featureFlag;
        }
        boolean ffValue = partitionFeatureFlagConfig.getDefaults().getOrDefault(ffName, false);
        try {
            PartitionInfo partitionInfo = this.getPartitionInfo(dataPartitionId);
            ffValue = this.getFeatureFlagFromPartitionServiceIfExists(partitionInfo, ffName).orElse(ffValue);
        } catch (Exception e) {
            this.log.error(String.format("PartitionService: Error getting %s for dataPartition with Id: %s", ffName, dataPartitionId), e);
        }
        this.featureFlagCache.setFeatureFlag(ffName, cacheKey, ffValue);
        return ffValue;
    }

    private PartitionInfo getPartitionInfo(String dataPartitionId) {
        try {
            DpsHeaders partitionHeaders = DpsHeaders.createFromMap(headers.getHeaders());
            String idToken = this.tokenService.getIdToken(dataPartitionId);
            partitionHeaders.put(DpsHeaders.AUTHORIZATION, idToken.startsWith("Bearer ") ? idToken : "Bearer " + idToken);

            IPartitionProvider partitionProvider = this.factory.create(partitionHeaders);
            PartitionInfo partitionInfo = partitionProvider.get(dataPartitionId);
            return partitionInfo;
        } catch (PartitionException e) {
            throw new AppException(HttpStatus.SC_FORBIDDEN, "Service unavailable", String.format("Error getting partition info for data-partition: %s", dataPartitionId), e);
        }
    }

    private Optional<Boolean> getFeatureFlagFromPartitionServiceIfExists(PartitionInfo partitionInfo, String ffName) {
        if (partitionInfo == null || partitionInfo.getProperties() == null || !partitionInfo.getProperties().containsKey(ffName)) {
            return Optional.empty();
        }

        Property property = partitionInfo.getProperties().get(ffName);
        return Optional.of(Boolean.parseBoolean((String)property.getValue()));
    }
}
