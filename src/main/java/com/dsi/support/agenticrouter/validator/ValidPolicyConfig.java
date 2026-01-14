package com.dsi.support.agenticrouter.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = PolicyConfigValidator.class)
@Target(
    {
        TYPE
    }
)
@Retention(
    RUNTIME
)
public @interface ValidPolicyConfig {

    String message() default "Invalid policy config value for its type and constraints";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
