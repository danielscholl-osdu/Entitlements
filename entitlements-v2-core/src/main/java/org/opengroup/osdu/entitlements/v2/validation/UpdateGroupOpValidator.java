package org.opengroup.osdu.entitlements.v2.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UpdateGroupOpValidator implements ConstraintValidator<ValidUpdateGroupOp, String> {
    @Override
    public void initialize(ValidUpdateGroupOp constraintAnnotation) {
        // do nothing
    }

    @Override
    public boolean isValid(String operation, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (!operation.equalsIgnoreCase("replace")) {
            context.buildConstraintViolationWithTemplate("Invalid Update Group Op Provided.")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
