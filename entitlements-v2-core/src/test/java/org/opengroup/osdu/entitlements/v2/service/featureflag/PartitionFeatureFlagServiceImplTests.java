package org.opengroup.osdu.entitlements.v2.service.featureflag;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.feature.IFeatureFlag;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
public class PartitionFeatureFlagServiceImplTests {
    @MockBean
    private IFeatureFlag featureFlag;
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private FeatureFlagCache featureFlagCache;

    @Autowired
    private PartitionFeatureFlagService partitionFeatureFlagService;

    @Test
    public void shouldGetFeatureFlagFromCacheIfHit() {
        when(featureFlagCache.getFeatureFlag(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "disable-data-root-group-hierarchy-dp")).thenReturn(true).thenReturn(false);
        assertThat(partitionFeatureFlagService.getFeature(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "dp")).isTrue();
        assertThat(partitionFeatureFlagService.getFeature(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "dp")).isFalse();
    }

    @Test
    public void shouldGetFeatureFlagFromPartitionServiceIfCacheMiss() throws PartitionException {
        when(featureFlagCache.getFeatureFlag(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "disable-data-root-group-hierarchy-dp")).thenReturn(null);
        when(featureFlag.isFeatureEnabled(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label)).thenReturn(true);
        assertThat(partitionFeatureFlagService.getFeature(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "dp")).isTrue();
        verify(featureFlagCache).setFeatureFlag(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "disable-data-root-group-hierarchy-dp", true);
    }
}

