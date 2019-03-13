package ru.majordomo.hms.personmgr.validation;

import ru.majordomo.hms.personmgr.validation.validator.SSLOrderValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SSLOrderValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSSLOrder {
    String message() default "{ru.majordomo.hms.personmgr.validation.ValidSSLOrder.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
