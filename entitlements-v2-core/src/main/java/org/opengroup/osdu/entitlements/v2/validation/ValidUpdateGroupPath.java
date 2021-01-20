package org.opengroup.osdu.entitlements.v2.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { UpdateGroupPathValidator.class })
@Documented
public @interface ValidUpdateGroupPath {
    String message() default "Invalid Update Group Path";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
