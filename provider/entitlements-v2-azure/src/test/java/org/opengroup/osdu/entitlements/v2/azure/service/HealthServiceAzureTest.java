package org.opengroup.osdu.entitlements.v2.azure.service;

import com.lambdaworks.redis.RedisCommandTimeoutException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.azure.cache.IRedisClientFactory;
import org.opengroup.osdu.core.common.cache.IRedisCache;
import org.opengroup.osdu.core.common.cache.RedisCache;
import org.opengroup.osdu.entitlements.v2.azure.config.CacheConfig;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

@SpringBootTest
@RunWith(SpringRunner.class)
public class HealthServiceAzureTest {

    @Autowired
    private HealthService healthService;

    @MockBean
    private CacheConfig cacheConfig;

    @MockBean
    private IRedisClientFactory redisClientFactory;

    @SpyBean
    private IRedisCache<String, ParentReferences> redisGroupCache;

    private static RedisServer redisServer;
    private static RedisCache<String, ParentReferences> redisCache;

    @BeforeClass
    public static void setupClass() {
        redisServer = RedisServer.builder().port(7000).build();
        redisServer.start();
        redisCache = new RedisCache<String, ParentReferences>("localhost", 7000, 3600, String.class, ParentReferences.class);
    }

    @AfterClass
    public static void end() {
        redisServer.stop();
    }

    @Before
    public void setup() {
        Mockito.when(redisClientFactory.getClient(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(redisCache);
    }

    @Test
    public void shouldSucceedInHealthCheck() {
        healthService.performHealthCheck();
        Mockito.verify(redisGroupCache).get("some-key");
    }

    @Test
    public void shouldFailInHealthCheck() {
        redisServer.stop();
        try {
            healthService.performHealthCheck();
            Assert.fail("Expecting exception here");
        } catch (RedisCommandTimeoutException e) {
            Assert.assertEquals("Command timed out after 30 SECONDS", e.getMessage());
        }
    }
}
