package org.opengroup.osdu.entitlements.v2.service.featureflag;

public interface PartitionFeatureFlagService {
    boolean getFeature(String ffName, String dataPartitionId);
}
