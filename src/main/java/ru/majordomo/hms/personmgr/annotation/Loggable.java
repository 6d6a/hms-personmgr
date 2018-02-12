package ru.majordomo.hms.personmgr.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE , ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Loggable {

}