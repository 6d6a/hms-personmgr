package ru.majordomo.hms.personmgr.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;


import java.lang.reflect.Method;

public class MyAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
    private final static Logger logger = LoggerFactory.getLogger(MyAsyncUncaughtExceptionHandler.class);

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... obj) {
        String error = "";
        error += "Exception message - " + throwable.getMessage() + "\n";
        error += "Method name - " + method.getName() + "\n";

        if (throwable instanceof HttpStatusCodeException) {
            error += "ResponseBody - " + ((HttpStatusCodeException) throwable).getResponseBodyAsString()  + "\n";
        } else {
            for (Object param : obj) {
                error += "Parameter value - " + param  + "\n";
            }
        }
        logger.error(error);
    }
}