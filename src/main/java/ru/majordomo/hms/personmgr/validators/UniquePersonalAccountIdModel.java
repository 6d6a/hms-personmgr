package ru.majordomo.hms.personmgr.validators;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.validators.validator.UniquePersonalAccountIdValidator;

@Documented
@Constraint(validatedBy = UniquePersonalAccountIdValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniquePersonalAccountIdModel {
    String message() default "{ru.majordomo.hms.personmgr.validators.UniqueNameResource.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Class<? extends BaseModel> value();
}
