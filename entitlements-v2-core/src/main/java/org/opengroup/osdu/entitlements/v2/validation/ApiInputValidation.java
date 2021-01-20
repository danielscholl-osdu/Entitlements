package org.opengroup.osdu.entitlements.v2.validation;

import org.opengroup.osdu.core.common.model.http.AppException;
import org.springframework.http.HttpStatus;

public class ApiInputValidation {

    private ApiInputValidation() {
        //Instance should be created
    }

    public static void validateEmailAndBelongsToPartition(String groupEmail, String partitionDomain) {
        validateEmail(groupEmail);
        if (!groupEmail.endsWith("@" + partitionDomain)) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Data Partition Id does not match with the group");
        }
    }

    /**
     * Either the given member is a group or a SAuth service des id
     * If it is a group, we don't allow cross data partition in this API, it should return 400
     * If it is SAuth service des id, we expect primary id, so it should return 400 as well
     */
    public static void validateEmailAgainstCrossPartition(String memberEmail, String domain, String partitionDomain) {
        validateEmail(memberEmail);
        if (memberEmail.endsWith(domain)) {
            validateEmailAndBelongsToPartition(memberEmail, partitionDomain);
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]{1,256}+@[A-Za-z0-9+_.-]{1,256}$")) {
            throw new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), "Invalid email provided");
        }
    }
}
