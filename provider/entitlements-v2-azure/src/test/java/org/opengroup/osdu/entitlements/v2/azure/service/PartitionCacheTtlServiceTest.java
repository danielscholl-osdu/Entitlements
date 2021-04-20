package org.opengroup.osdu.entitlements.v2.azure.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.azure.util.AzureServicePrincipleTokenService;
import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.partition.Property;
import org.powermock.reflect.Whitebox;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RunWith(MockitoJUnitRunner.class)
public class PartitionCacheTtlServiceTest {

    @Mock
    private ILogger logger;

    @Mock
    private AzureServicePrincipleTokenService azureServicePrincipleTokenService;

    @Mock
    private IPartitionFactory partitionFactory;

    @InjectMocks
    private PartitionCacheTtlService partitionCacheTtlService;

    @Test
    public void shouldReturnTtlOfKnownDataPartitionId() {
        ConcurrentMap<String, Long> ttlPerDataPartition = new ConcurrentHashMap<>();
        ttlPerDataPartition.put("common", 2000L);
        Whitebox.setInternalState(partitionCacheTtlService, "ttlPerDataPartition", ttlPerDataPartition);

        Long actual = partitionCacheTtlService.getCacheTtlOfPartition("common");

        Assert.assertEquals(new Long(2000L), actual);
    }

    @Test
    public void shouldReturnDefaultTtlOfUnknownDataPartitionId() {
        Whitebox.setInternalState(partitionCacheTtlService, "cacheTtl", 3);

        Long actual = partitionCacheTtlService.getCacheTtlOfPartition("common");

        Assert.assertEquals(new Long(3000L), actual);
    }

    @Test
    public void shouldDoInitSuccessfully() throws Exception {
        Mockito.when(azureServicePrincipleTokenService.getAuthorizationToken()).thenReturn("token");
        IPartitionProvider provider = Mockito.mock(IPartitionProvider.class);
        Mockito.when(partitionFactory.create(Mockito.any(DpsHeaders.class))).thenReturn(provider);
        Mockito.when(provider.list()).thenReturn(Collections.singletonList("dp1"));
        Long ttl = 2000L;
        Map<String, Property> properties = new HashMap<>();
        Property property = new Property();
        property.setValue(ttl);
        properties.put("ent-cache-ttl", property);
        PartitionInfo partitionInfo = PartitionInfo.builder().properties(properties).build();
        Mockito.when(provider.get("dp1")).thenReturn(partitionInfo);

        Whitebox.invokeMethod(partitionCacheTtlService, "init");

        Mockito.verify(logger).info("CronJobLogger", "Starting a scheduled cron job to update cache ttls for data partitions", null);
        Mockito.verifyNoMoreInteractions(logger);
    }

    @Test
    public void shouldLogWarningWhenNoDpIdsGotFromPartitionService() throws Exception {
        Mockito.when(azureServicePrincipleTokenService.getAuthorizationToken()).thenReturn("token");
        IPartitionProvider provider = Mockito.mock(IPartitionProvider.class);
        Mockito.when(partitionFactory.create(Mockito.any(DpsHeaders.class))).thenReturn(provider);
        PartitionException exception = new PartitionException("error", null);
        Mockito.when(provider.list()).thenThrow(exception);

        Whitebox.invokeMethod(partitionCacheTtlService, "init");

        Mockito.verify(logger).info("CronJobLogger", "Starting a scheduled cron job to update cache ttls for data partitions", null);
        Mockito.verify(logger).warning("CronJobLogger", "Couldn't get data partition ids from partition service", exception, null);
        Mockito.verifyNoMoreInteractions(logger);
    }

    @Test
    public void shouldLogWarningWhenNoDpIdsGotFromPartitionService1() throws Exception {
        Mockito.when(azureServicePrincipleTokenService.getAuthorizationToken()).thenReturn("token");
        IPartitionProvider provider = Mockito.mock(IPartitionProvider.class);
        Mockito.when(partitionFactory.create(Mockito.any(DpsHeaders.class))).thenReturn(provider);
        PartitionException exception = new PartitionException("error", null);
        Mockito.when(provider.list()).thenReturn(Collections.singletonList("dp1"));
        Mockito.when(provider.get("dp1")).thenThrow(exception);

        Whitebox.invokeMethod(partitionCacheTtlService, "init");

        Mockito.verify(logger).info("CronJobLogger", "Starting a scheduled cron job to update cache ttls for data partitions", null);
        Mockito.verify(logger).warning("CronJobLogger", "Couldn't get PartitionInfo from partition service for partition id: dp1", exception, null);
        Mockito.verifyNoMoreInteractions(logger);
    }
}
