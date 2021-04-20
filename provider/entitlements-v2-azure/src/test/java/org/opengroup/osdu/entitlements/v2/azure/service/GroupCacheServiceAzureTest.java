package org.opengroup.osdu.entitlements.v2.azure.service;

import io.github.resilience4j.retry.Retry;
import org.awaitility.Duration;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
        "cache.retry.random.factor=0.1"})
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
                    .setTimeout(100000)
                    .setClientName("test");
            return Redisson.create(config);
        }
    }

    private Set<ParentReference> parents = new HashSet<>();
    private ParentReferences parentReferences = new ParentReferences();
    private EntityNode requester;

    @MockBean
    private RetrieveGroupRepo retrieveGroupRepo;
    @MockBean
    private HitsNMissesMetricService metricService;
    @MockBean
    private RedisCache<String, ParentReferences> redisGroupCache;
    @MockBean
    private JaxRsDpsLog log;
    @MockBean
    private PartitionCacheTtlService partitionCacheTtlService;
    @Mock
    private ParentTreeDto parentTreeDto;

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private Retry retry;
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

        requester = EntityNode.createMemberNodeForNewUser("requesterId", "dp");
        when(partitionCacheTtlService.getCacheTtlOfPartition("dp")).thenReturn(2000L);
    }

    @Test
    public void shouldGetAllParentsFromRepoForTheFirstTime() {
        when(this.redisGroupCache.get("requesterId-dp")).thenReturn(null);
        when(this.retrieveGroupRepo.loadAllParents(this.requester)).thenReturn(this.parentTreeDto);
        when(this.parentTreeDto.getParentReferences()).thenReturn(this.parents);

        Set<ParentReference> result = this.sut.getFromPartitionCache("requesterId", "dp");
        assertEquals(this.parents, result);
        verify(this.retrieveGroupRepo).loadAllParents(this.requester);
        verify(this.redisGroupCache).put("requesterId-dp", 2000L, this.parentReferences);
        verify(this.metricService).sendMissesMetric();
    }

    @Test
    public void shouldGetAllParentsFromCacheForTheSecondTime() {
        when(this.redisGroupCache.get("requesterId-dp")).thenReturn(this.parentReferences);

        Set<ParentReference> result = this.sut.getFromPartitionCache("requesterId", "dp");
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
        when(this.redisGroupCache.get("requesterId-dp")).thenAnswer(AdditionalAnswers.returnsElementsOf(cacheValues));
        when(this.retrieveGroupRepo.loadAllParents(this.requester)).thenAnswer((Answer<ParentTreeDto>) invocationOnMock -> {
            await().atLeast(Duration.FIVE_SECONDS);
            return parentTreeDto;
        });
        when(this.parentTreeDto.getParentReferences()).thenReturn(this.parents);

        int threads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<Set<ParentReference>>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Callable<Set<ParentReference>> task = () -> sut.getFromPartitionCache("requesterId", "dp");
            tasks.add(task);
        }

        List<Future<Set<ParentReference>>> responses = executor.invokeAll(tasks);
        executor.shutdown();

        verify(this.retrieveGroupRepo, times(1)).loadAllParents(this.requester);
        responses.forEach(result -> {
            try {
                assertEquals(this.parents, result.get());
            } catch (InterruptedException | ExecutionException e) {
                fail("No exception expected");
            }
        });
    }

    @Test
    public void shouldReturnEmptyIfTimeout() throws InterruptedException {
        when(this.redisGroupCache.get("requesterId-dp")).thenReturn(null);
        when(this.retrieveGroupRepo.loadAllParents(this.requester)).thenAnswer((Answer<ParentTreeDto>) invocationOnMock -> {
            await().atLeast(Duration.FIVE_SECONDS);
            return parentTreeDto;
        });
        when(this.parentTreeDto.getParentReferences()).thenReturn(this.parents);

        int threads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<Set<ParentReference>>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Callable<Set<ParentReference>> task = () -> sut.getFromPartitionCache("requesterId", "dp");
            tasks.add(task);
        }

        List<Future<Set<ParentReference>>> responses = executor.invokeAll(tasks);
        executor.shutdown();

        verify(this.retrieveGroupRepo, times(1)).loadAllParents(this.requester);
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
}
