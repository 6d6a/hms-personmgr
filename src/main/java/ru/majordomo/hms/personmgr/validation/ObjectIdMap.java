package ru.majordomo.hms.personmgr.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.validation.validator.ObjectIdMapValidator;

@Documented
@Constraint(validatedBy = ObjectIdMapValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ObjectIdMap {
    String message() default "{ru.majordomo.hms.personmgr.validation.ObjectIdMap.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Class<? extends BaseModel> value();

    String collection() default "";
}
