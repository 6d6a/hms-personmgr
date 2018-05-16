package ru.majordomo.hms.personmgr.validation;

import ru.majordomo.hms.personmgr.validation.validator.RemoteObjectIdValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RemoteObjectIdValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoteObjectId {
    String message() default "Object with ObjectId ${validatedValue} with type {value} not found in DB";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Class<?> value();

    String collection() default "";

    String idFieldName() default "";

    boolean nullable() default false;
}
