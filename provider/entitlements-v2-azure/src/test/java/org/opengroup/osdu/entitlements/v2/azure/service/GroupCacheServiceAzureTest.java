package org.opengroup.osdu.entitlements.v2.azure.service;

import org.awaitility.Duration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.opengroup.osdu.azure.cache.RedisAzureCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.entitlements.v2.azure.config.CacheConfig;
import org.opengroup.osdu.entitlements.v2.azure.service.metrics.hitsnmisses.HitsNMissesMetricService;
import org.opengroup.osdu.entitlements.v2.model.EntityNode;
import org.opengroup.osdu.entitlements.v2.model.ParentReference;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.model.ParentTreeDto;
import org.opengroup.osdu.entitlements.v2.spi.retrievegroup.RetrieveGroupRepo;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {"spring.application.name=test",
        "redis.redisson.lock.acquisition.timeout=10",
        "redisson.lock.expiration=5000",
        "cache.retry.max=15",
        "cache.retry.interval=200",
        "cache.retry.random.factor=0.1",
        "cache.flush.ttl.base=500",
        "cache.flush.ttl.jitter=1000"})
@RunWith(SpringRunner.class)
public class GroupCacheServiceAzureTest {
    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public RedissonClient mockRedissonClient() {
            Config config = new Config();
            config.useSingleServer().setAddress(String.format("redis://%s:%d", "localhost", 7000))
                    .setPassword("pass")
                    .setDatabase(0)
                    .setKeepAlive(true)
                    .setClientName("test");
            return Redisson.create(config);
        }
    }

    private Set<ParentReference> parents = new HashSet<>();
    private ParentReferences parentReferences = new ParentReferences();
    private EntityNode requester1;
    private EntityNode requester2;

    @MockBean
    private RetrieveGroupRepo retrieveGroupRepo;
    @MockBean
    private HitsNMissesMetricService metricService;
    @MockBean
    private RedisAzureCache<String, ParentReferences> redisGroupCache;
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private CacheConfig cacheConfig;
    @Mock
    private ParentTreeDto parentTreeDto;

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private GroupCacheServiceAzure sut;

    private static RedisServer redisServer;

    @BeforeClass
    public static void setupClass() {
        redisServer = RedisServer.builder().port(7000).setting("requirepass pass").build();
        redisServer.start();
    }

    @AfterClass
    public static void end() {
        redisServer.stop();
    }

    @Before
    public void setup() {
        ParentReference parent1 = ParentReference.builder()
                .name("parent1")
                .id("id1")
                .description("test description")
                .dataPartitionId("dp")
                .build();
        ParentReference parent2 = ParentReference.builder()
                .name("parent2")
                .id("id2")
                .description("test description")
                .dataPartitionId("dp")
                .build();
        parents.add(parent1);
        parents.add(parent2);
        parentReferences.setParentReferencesOfUser(parents);

        requester1 = EntityNode.createMemberNodeForNewUser("requesterId1", "dp");
        requester2 = EntityNode.createMemberNodeForNewUser("requesterId2", "dp");
        when(redisGroupCache.getLock(any())).thenReturn(redissonClient.getLock("cache-key"));
    }

    @Test
    public void shouldGetAllParentsFromRepoForTheFirstTime() {
        when(this.redisGroupCache.get("requesterId1-dp")).thenReturn(null);
        when(this.retrieveGroupRepo.loadAllParents(this.requester1)).thenReturn(this.parentTreeDto);
        when(this.parentTreeDto.getParentReferences()).thenReturn(this.parents);

        Set<ParentReference> result = this.sut.getFromPartitionCache("requesterId1", "dp");
        assertEquals(this.parents, result);
        verify(this.retrieveGroupRepo).loadAllParents(this.requester1);
        verify(this.redisGroupCache).put("requesterId1-dp", 1000000L, this.parentReferences);
        verify(this.metricService).sendMissesMetric();
    }

    @Test
    public void shouldGetAllParentsFromCacheForTheSecondTime() {
        when(this.redisGroupCache.get("requesterId1-dp")).thenReturn(this.parentReferences);

        Set<ParentReference> result = this.sut.getFromPartitionCache("requesterId1", "dp");
        assertEquals(this.parents, result);
        verifyNoInteractions(this.retrieveGroupRepo);
        verify(this.metricService, times(1)).sendHitsMetric();
    }

    @Test
    public void shouldOnlyOneThreadRebuildCacheInConcurrentScenarioAndOtherThreadWaitAndReturnTheRebuiltCache() throws InterruptedException {
        List<ParentReferences> cacheValues = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            cacheValues.add(null);
        }
        cacheValues.add(this.parentReferences);
        when(this.redisGroupCache.get("requesterId1-dp")).thenAnswer(AdditionalAnswers.returnsElementsOf(cacheValues));
        when(this.retrieveGroupRepo.loadAllParents(this.requester1)).thenAnswer((Answer<ParentTreeDto>) invocationOnMock -> {
            await().pollDelay(Duration.FIVE_SECONDS).until(() -> true);
            return parentTreeDto;
        });
        when(this.parentTreeDto.getParentReferences()).thenReturn(this.parents);

        int threads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<Set<ParentReference>>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Callable<Set<ParentReference>> task = () -> sut.getFromPartitionCache("requesterId1", "dp");
            tasks.add(task);
        }

        List<Future<Set<ParentReference>>> responses = executor.invokeAll(tasks);
        executor.shutdown();

        verify(this.retrieveGroupRepo, times(1)).loadAllParents(this.requester1);
        responses.forEach(result -> {
            try {
                assertEquals(this.parents, result.get());
            } catch (InterruptedException | ExecutionException e) {
                fail("No exception expected");
            }
        });
    }

    @Test
    public void shouldThrowExceptionIfTimeout() throws InterruptedException {
        when(this.redisGroupCache.get("requesterId1-dp")).thenReturn(null);
        when(this.retrieveGroupRepo.loadAllParents(this.requester1)).thenAnswer((Answer<ParentTreeDto>) invocationOnMock -> {
            await().pollDelay(Duration.FIVE_SECONDS).until(() -> true);
            return parentTreeDto;
        });
        when(this.parentTreeDto.getParentReferences()).thenReturn(this.parents);

        int threads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<Set<ParentReference>>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Callable<Set<ParentReference>> task = () -> sut.getFromPartitionCache("requesterId1", "dp");
            tasks.add(task);
        }

        List<Future<Set<ParentReference>>> responses = executor.invokeAll(tasks);
        executor.shutdown();

        verify(this.retrieveGroupRepo, times(1)).loadAllParents(this.requester1);
        assertEquals(1, responses.stream().filter(result -> {
            try {
                return this.parents.equals(result.get());
            } catch (InterruptedException | ExecutionException e) {
            }
            return false;
        }).count());
        assertEquals(2, responses.stream().filter(result -> {
            try {
                result.get();
            } catch (InterruptedException | ExecutionException e) {
                AppException appEx = (AppException)e.getCause();
                if (appEx.getError().getCode() == 503) {
                    return true;
                }
            }
            return false;
        }).count());
    }

    @Test
    public void shouldRefreshListGroupCache() {
        this.redisGroupCache.put("requesterId1-dp", 600000L, this.parentReferences);
        this.redisGroupCache.put("requesterId2-dp", 600000L, this.parentReferences);
        when(this.redisGroupCache.getTtl("requesterId1-dp")).thenReturn(600000L);
        when(this.redisGroupCache.getTtl("requesterId2-dp")).thenReturn(600000L);
        ArgumentCaptor<Long> ttlCapture1 = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> ttlCapture2 = ArgumentCaptor.forClass(Long.class);
        this.sut.refreshListGroupCache(new HashSet<>(Arrays.asList("requesterId1", "requesterId2")), "dp");

        verify(this.redisGroupCache, times(1)).getTtl("requesterId1-dp");
        verify(this.redisGroupCache, times(1)).getTtl("requesterId2-dp");
        verify(this.redisGroupCache, times(1)).updateTtl(eq("requesterId1-dp"), ttlCapture1.capture());
        verify(this.redisGroupCache, times(1)).updateTtl(eq("requesterId2-dp"), ttlCapture2.capture());
        await().pollDelay(ttlCapture1.getValue(), TimeUnit.MILLISECONDS);
        assertNull(this.redisGroupCache.get("requesterId1-dp"));
        await().pollDelay(ttlCapture2.getValue(), TimeUnit.MILLISECONDS);
        assertNull(this.redisGroupCache.get("requesterId2-dp"));
    }

    @Test
    public void shouldFlushListGroupCacheForUserIfRedisEntryExists() {
        this.redisGroupCache.put("requesterId1-dp", 600000L, this.parentReferences);
        when(this.redisGroupCache.getTtl("requesterId1-dp")).thenReturn(600000L);
        ArgumentCaptor<Long> ttlCapture = ArgumentCaptor.forClass(Long.class);
        this.sut.flushListGroupCacheForUser("requesterId1", "dp");
        verify(this.redisGroupCache, times(1)).getTtl("requesterId1-dp");
        verify(this.redisGroupCache, times(1)).updateTtl(eq("requesterId1-dp"), ttlCapture.capture());
        await().pollDelay(ttlCapture.getValue(), TimeUnit.MILLISECONDS);
        assertNull(this.redisGroupCache.get("requesterId1-dp"));
    }

    @Test
    public void shouldDoNothingWhenFlushListGroupCacheAndRedisEntryDoesNotExist() {
        this.sut.flushListGroupCacheForUser("requesterId1", "dp");
        verify(this.redisGroupCache, times(1)).getTtl("requesterId1-dp");
        assertNull(this.redisGroupCache.get("requesterId1-dp"));
    }
}
