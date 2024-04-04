package org.opengroup.osdu.entitlements.v2.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { UpdateGroupOpValidator.class })
@Documented
public @interface ValidUpdateGroupOp {
    String message() default "Invalid Update Group Op";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
