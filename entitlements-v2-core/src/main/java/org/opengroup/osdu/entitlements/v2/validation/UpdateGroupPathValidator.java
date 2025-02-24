package org.opengroup.osdu.entitlements.v2.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UpdateGroupPathValidator implements ConstraintValidator<ValidUpdateGroupPath, String> {
    @Override
    public void initialize(ValidUpdateGroupPath constraintAnnotation) {
        //do nothing
    }

    @Override
    public boolean isValid(String path, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (!path.equalsIgnoreCase("/name") && !path.equalsIgnoreCase("/appIds")) {
            context.buildConstraintViolationWithTemplate("Invalid Update Group Path Provided.")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
