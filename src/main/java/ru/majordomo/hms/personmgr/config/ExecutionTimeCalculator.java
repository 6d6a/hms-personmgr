package ru.majordomo.hms.personmgr.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import ru.majordomo.hms.personmgr.Application;

@Aspect
@Component
public class ExecutionTimeCalculator {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Around("@annotation(org.springframework.context.event.EventListener)")
    public void timeCalcMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = null;
        StopWatch watch = new StopWatch();
        try {
            watch.start();
            result = joinPoint.proceed();
        } finally {
            watch.stop();
            long executionTime = watch.getLastTaskTimeMillis();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            String name = executionTime > 10000 ? "ExecutionTimeCalculator ETCAttention" : "ExecutionTimeCalculator ETCDefault";
            logger.info(String.format("[ %s ] Class: '%s', Method: '%s', Time took: [%s] ms", name, className, methodName, executionTime));
        }
    }
}