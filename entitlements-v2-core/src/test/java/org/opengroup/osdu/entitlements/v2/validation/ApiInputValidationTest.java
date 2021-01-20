package org.opengroup.osdu.entitlements.v2.validation;

import org.junit.Assert;
import org.junit.Test;
import org.opengroup.osdu.core.common.model.http.AppException;

public class ApiInputValidationTest {

    @Test
    public void shouldThrowErrorWhenDataPartitionIdDoesNotMatchWithGroupEmail() {
        try {
            ApiInputValidation.validateEmailAndBelongsToPartition("data.x.viewers@common.contoso.com", "common2.contoso.com");
            Assert.fail();
        } catch (AppException e) {
            Assert.assertEquals("Data Partition Id does not match with the group", e.getError().getMessage());
            Assert.assertEquals(400, e.getError().getCode());
            Assert.assertEquals("Bad Request", e.getError().getReason());
        }
    }

    @Test
    public void shouldValidateEmailSuccessfullyForPartition() {
        try {
            ApiInputValidation.validateEmailAndBelongsToPartition("data.x.viewers@common.contoso.com", "common.contoso.com");
        } catch (AppException e) {
            Assert.fail();
        }
    }

    @Test
    public void shouldThrowErrorWhenMemberIsAGroupAndBelongsToDifferentPartition() {
        try {
            ApiInputValidation.validateEmailAgainstCrossPartition("user.x.viewers@tenant1.contoso.com", "contoso.com", "common.contoso.com");
        } catch (AppException e) {
            Assert.assertEquals("Data Partition Id does not match with the group", e.getError().getMessage());
            Assert.assertEquals(400, e.getError().getCode());
            Assert.assertEquals("Bad Request", e.getError().getReason());
        }
    }

    @Test
    public void shouldThrowErrorWhenMemberIsDesIdOfSAuthServiceAccount() {
        try {
            ApiInputValidation.validateEmailAgainstCrossPartition("service.account@desid.contoso.com", "contoso.com", "common.contoso.com");
        } catch (AppException e) {
            Assert.assertEquals("Data Partition Id does not match with the group", e.getError().getMessage());
            Assert.assertEquals(400, e.getError().getCode());
            Assert.assertEquals("Bad Request", e.getError().getReason());
        }
    }

    @Test
    public void shouldValidateSuccessfullyWhenMemberIsAUser() {
        try {
            ApiInputValidation.validateEmailAgainstCrossPartition("member@xxx.com", "contoso.com", "common.contoso.com");
        } catch (AppException e) {
            Assert.fail();
        }
    }

    @Test
    public void shouldValidateSuccessfullyWhenMemberIsAGroupAndBelongsToSamePartition() {
        try {
            ApiInputValidation.validateEmailAgainstCrossPartition("user.x.viewers@common.contoso.com", "contoso.com", "common.contoso.com");
        } catch (AppException e) {
            Assert.fail();
        }
    }

    @Test
    public void shouldThrowErrorWhenNotEmailProvided() {
        try {
            ApiInputValidation.validateEmail("data.x|viewers@comm|on.contoso.com");
        } catch (AppException e) {
            Assert.assertEquals("Invalid email provided", e.getError().getMessage());
            Assert.assertEquals(400, e.getError().getCode());
            Assert.assertEquals("Bad Request", e.getError().getReason());
        }
    }

    @Test
    public void shouldValidateEmailSuccessfully() {
        try {
            ApiInputValidation.validateEmail("data.viewers@common.contoso.com");
        } catch (AppException e) {
            Assert.fail();
        }
    }
}
