package org.opengroup.osdu.entitlements.v2.service.featureflag;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.*;
import org.opengroup.osdu.core.common.util.IServiceAccountJwtClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@RunWith(SpringRunner.class)
public class PartitionFeatureFlagServiceImplTests {
    @MockBean
    private IPartitionFactory factory;
    @MockBean
    private IServiceAccountJwtClient tokenService;
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private FeatureFlagCache featureFlagCache;
    @MockBean
    private DpsHeaders headers;
    @MockBean
    private IPartitionProvider partitionProvider;
    @MockBean
    private PartitionInfo partitionInfo;

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
        when(factory.create(any())).thenReturn(partitionProvider);
        when(partitionProvider.get("dp")).thenReturn(partitionInfo);
        Map<String, Property> partitionProperties = new HashMap<>();
        Property property = new Property();
        property.setSensitive(false);
        property.setValue("true");
        partitionProperties.put(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, property);
        when(tokenService.getIdToken("dp")).thenReturn("token");
        when(partitionInfo.getProperties()).thenReturn(partitionProperties);
        assertThat(partitionFeatureFlagService.getFeature(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "dp")).isTrue();
    }

    @Test
    public void shouldGetFeatureFlagFromPartitionServiceAndReturnFalseIfPartitionPropertiesIsNull() throws PartitionException {
        when(featureFlagCache.getFeatureFlag(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "disable-data-root-group-hierarchy-dp")).thenReturn(null);
        when(factory.create(any())).thenReturn(partitionProvider);
        when(partitionProvider.get("dp")).thenReturn(partitionInfo);
        when(partitionInfo.getProperties()).thenReturn(null);
        assertThat(partitionFeatureFlagService.getFeature(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "dp")).isFalse();
    }

    @Test
    public void shouldGetFeatureFlagFromPartitionServiceAndReturnFalseIfPartitionPropertiesDoesNotHaveSuchFeatureFlag() throws PartitionException {
        when(featureFlagCache.getFeatureFlag(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "disable-data-root-group-hierarchy-dp")).thenReturn(null);
        when(factory.create(any())).thenReturn(partitionProvider);
        when(partitionProvider.get("dp")).thenReturn(partitionInfo);
        Map<String, Property> partitionProperties = new HashMap<>();
        when(partitionInfo.getProperties()).thenReturn(partitionProperties);
        assertThat(partitionFeatureFlagService.getFeature(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "dp")).isFalse();
    }

    @Test
    public void shouldEatTheExceptionAndReturnFalseIfPartitionServiceError() throws PartitionException {
        when(featureFlagCache.getFeatureFlag(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "disable-data-root-group-hierarchy-dp")).thenReturn(null);
        when(factory.create(any())).thenReturn(partitionProvider);
        when(partitionProvider.get("dp")).thenThrow(new PartitionException("error", null));
        assertThat(partitionFeatureFlagService.getFeature(FeatureFlag.DISABLE_DATA_ROOT_GROUP_HIERARCHY.label, "dp")).isFalse();
        verify(log, times(1)).error((String) any(), (Exception) any());
    }
}

