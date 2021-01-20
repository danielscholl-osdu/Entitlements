package org.opengroup.osdu.entitlements.v2.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.entitlements.v2.AppProperties;
import org.opengroup.osdu.entitlements.v2.auth.JwtClaimExtractor;
import org.opengroup.osdu.entitlements.v2.auth.JwtClaims;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class RequestInfoUtilServiceTest {

    @Mock
    private AppProperties appProperties;

    @Mock
    private JwtClaimExtractor jwtClaimExtractor;

    @InjectMocks
    private RequestInfoUtilService requestInfoUtilService;

    @Test
    public void shouldReturnPartitionIdListWithSpaceTrimmed() {
        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put(DpsHeaders.AUTHORIZATION, "aaa");
        httpHeaders.put(DpsHeaders.CORRELATION_ID, "cor123");
        httpHeaders.put(DpsHeaders.DATA_PARTITION_ID, "123 , 456, common ");
        httpHeaders.put("context", "67890");
        DpsHeaders dpsHeaders = new DpsHeaders();
        dpsHeaders.getHeaders().putAll(httpHeaders);
        List<String> partitionIdList = requestInfoUtilService.getPartitionIdList(dpsHeaders);
        assertEquals(3, partitionIdList.size());
        assertTrue(partitionIdList.contains("123"));
        assertTrue(partitionIdList.contains("456"));
        assertTrue(partitionIdList.contains("common"));
    }

    @Test
    public void shouldThrowExceptionIfNoPartitionIdProvided() {
        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put(DpsHeaders.DATA_PARTITION_ID, " ");
        DpsHeaders dpsHeaders = new DpsHeaders();
        dpsHeaders.getHeaders().putAll(httpHeaders);
        try {
            requestInfoUtilService.getPartitionIdList(dpsHeaders);
            fail();
        } catch (AppException e) {
            assertEquals("Invalid data partition header provided", e.getError().getMessage());
            assertEquals(401, e.getError().getCode());
            assertEquals("Unauthorized", e.getError().getReason());
        }
    }

    @Test
    public void shouldReturnCorrectDomain() {
        Mockito.when(appProperties.getDomain()).thenReturn("domain");

        Assert.assertEquals("id.domain", requestInfoUtilService.getDomain("id"));
    }

    @Test
    public void shouldReturnAppIdSuccessfully() {
        JwtClaims jwtClaims = new JwtClaims("email", "appId");
        Mockito.when(jwtClaimExtractor.extract("token")).thenReturn(jwtClaims);
        Map<String, String> map = new HashMap<>();
        map.put(DpsHeaders.AUTHORIZATION, "token");
        DpsHeaders dpsHeaders = new DpsHeaders();
        dpsHeaders.getHeaders().putAll(map);

        Assert.assertEquals(jwtClaims.getAppId(), requestInfoUtilService.getAppId(dpsHeaders));
    }

    @Test
    public void shouldReturnUserIdSuccessfully() {
        JwtClaims jwtClaims = new JwtClaims("email", "appId");
        Mockito.when(jwtClaimExtractor.extract("token")).thenReturn(jwtClaims);
        Map<String, String> map = new HashMap<>();
        map.put(DpsHeaders.AUTHORIZATION, "token");
        DpsHeaders dpsHeaders = new DpsHeaders();
        dpsHeaders.getHeaders().putAll(map);

        Assert.assertEquals(jwtClaims.getUserId(), requestInfoUtilService.getUserId(dpsHeaders));
    }
}
