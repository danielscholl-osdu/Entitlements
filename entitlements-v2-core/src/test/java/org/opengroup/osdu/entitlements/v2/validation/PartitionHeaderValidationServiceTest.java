package org.opengroup.osdu.entitlements.v2.validation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.core.common.model.tenant.TenantInfo;

import java.util.Arrays;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class PartitionHeaderValidationServiceTest {

    @Mock
    private JaxRsDpsLog log;

    @InjectMocks
    private PartitionHeaderValidationService partitionHeaderValidationService;

    @Before
    public void setup() {
        final TenantInfo common = new TenantInfo();
        common.setDataPartitionId("common");
        final TenantInfo dp1 = new TenantInfo();
        dp1.setDataPartitionId("dp1");
        final TenantInfo dp2 = new TenantInfo();
        dp2.setDataPartitionId("dp2");
    }

    @Test
    public void shouldThrowExceptionWhenMultiplePartitionsInHeaderProvided() {
        try {
            partitionHeaderValidationService.validateSinglePartitionProvided("dp1,dp2");
            Assert.fail();
        } catch (AppException e) {
            Assert.assertEquals("Invalid data partition header provided", e.getError().getMessage());
            Assert.assertEquals(401, e.getError().getCode());
            Assert.assertEquals("Unauthorized", e.getError().getReason());
        }
    }

    @Test
    public void shouldValidateWithNoExceptionsWhenSinglePartitionProvided() {
        try {
            partitionHeaderValidationService.validateSinglePartitionProvided("common");
        } catch (AppException e) {
            Assert.fail();
        }
    }

    @Test
    public void shouldThrowErrorIfMoreThanTwoDataPartitionIdsAreProvidedWhenIsSpecialListGroupPartitionProvided() {
        try {
            partitionHeaderValidationService.validateIfSpecialListGroupPartitionIsProvided(Arrays.asList("dp1", "dp2", "dp3"));
            Assert.fail();
        } catch (AppException e) {
            Assert.assertEquals("Invalid data partition header provided", e.getError().getMessage());
            Assert.assertEquals(401, e.getError().getCode());
            Assert.assertEquals("Unauthorized", e.getError().getReason());
        }
    }

    @Test
    public void shouldThrowErrorIfTwoDataPartitionIdsButNoCommonIsProvidedWhenIsSpecialListGroupPartitionProvided() {
        try {
            partitionHeaderValidationService.validateIfSpecialListGroupPartitionIsProvided(Arrays.asList("dp1", "dp2"));
            Assert.fail();
        } catch (AppException e) {
            Assert.assertEquals("Invalid data partition header provided", e.getError().getMessage());
            Assert.assertEquals(401, e.getError().getCode());
            Assert.assertEquals("Unauthorized", e.getError().getReason());
        }
    }

    @Test
    public void shouldReturnNothingWhenCommonAndDPProvidedForIsSpecialListGroupPartitionProvided() {
        try {
            partitionHeaderValidationService.validateIfSpecialListGroupPartitionIsProvided(Arrays.asList("dp1", "common"));
        } catch (AppException e) {
            Assert.fail();
        }
    }

    @Test
    public void shouldReturnNothingWhenSingleDpProvidedWhenIsSpecialListGroupPartitionProvided() {
        try {
            partitionHeaderValidationService.validateIfSpecialListGroupPartitionIsProvided(Collections.singletonList("dp1"));
        } catch (AppException e) {
            Assert.fail();
        }
    }
}
