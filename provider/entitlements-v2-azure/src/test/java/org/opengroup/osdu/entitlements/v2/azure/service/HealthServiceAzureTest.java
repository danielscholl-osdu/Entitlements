package org.opengroup.osdu.entitlements.v2.azure.service;

import com.lambdaworks.redis.RedisCommandTimeoutException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.cache.ICache;
import org.opengroup.osdu.entitlements.v2.model.ParentReferences;
import org.opengroup.osdu.entitlements.v2.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

@SpringBootTest
@RunWith(SpringRunner.class)
public class HealthServiceAzureTest {

    @Autowired
    private HealthService healthService;

    @SpyBean
    private ICache<String, ParentReferences> redisGroupCache;

    private static RedisServer redisServer;

    @BeforeClass
    public static void setupClass() {
        redisServer = RedisServer.builder().port(7000).build();
        redisServer.start();
    }

    @AfterClass
    public static void end() {
        redisServer.stop();
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
