package ru.majordomo.hms.personmgr.validators;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ru.majordomo.hms.personmgr.model.BaseModel;

/**
 * ObjectIdMap
 */
@Documented
@Constraint(validatedBy = ObjectIdMapValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ObjectIdMap {
    String message() default "Object with ObjectId ${validatedValue} with type {value} not found in DB";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Class<? extends BaseModel> value();

    String collection() default "";
}
