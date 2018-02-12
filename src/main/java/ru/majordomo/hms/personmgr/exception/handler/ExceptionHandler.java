package ru.majordomo.hms.personmgr.exception.handler;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.exception.*;
import ru.majordomo.hms.personmgr.exception.newExceptions.BaseException;
import ru.majordomo.hms.personmgr.exception.newExceptions.NotEnoughMoneyException;

import javax.validation.ConstraintViolationException;

@Aspect
@Component
public class ExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

//    @AfterThrowing(
//            pointcut = "@annotation(ru.majordomo.hms.personmgr.annotation.ReplacingExceptionWithBaseException)",
//            throwing = "ex")
//    public void logAfterThrowingAllMethods(Throwable ex) throws Throwable {
//        if (ex instanceof BaseException) {
//            logger.info("Кидаем исключение дальше, стандартное для API исключение...");
//            throw ex;
//        } else if (ex instanceof ConstraintViolationException) {
//            throw new ru.majordomo.hms.personmgr.exception.newExceptions.ConstraintViolationException(
//                    (ConstraintViolationException) ex
//            );
//        }
//        logger.info("Подменяем исключение " + ex.getClass().getSimpleName() + " на ParameterValidationException");
//        throw new InternalApiException(ex.getMessage());
//    }

    @AfterThrowing(
            pointcut = "@annotation(ru.majordomo.hms.personmgr.annotation.ReplacingExceptionWithBaseException)",
            throwing = "ex")
    public void logAfterThrowingAllMethods(Throwable ex) throws Throwable {
        ex = convertThrowableToBaseException(ex);
        throw ex;
    }

//    @Before("@annotation(ru.majordomo.hms.personmgr.annotation.Loggable)")
//    public void beforeLogging(JoinPoint joinPoint){
//        System.out.println("Before running loggingAdvice on method=" + joinPoint.toShortString());
//
//    }
//
//    @After("@annotation(ru.majordomo.hms.personmgr.annotation.Loggable)")
//    public void afterLogging(JoinPoint joinPoint){
//        System.out.println("After running loggingAdvice on method=" + joinPoint.toShortString());
//    }

//    @After("execution(* ru.majordomo.hms.personmgr.controller.rest.*.*(..))")
//    public void logAfterV2(JoinPoint joinPoint)
//    {
//        System.out.println("EmployeeCRUDAspect.logAfterV2() : " + joinPoint.getSignature().getName());
//    }

//    @After("@annotation(org.springframework.web.bind.annotation.RequestMapping), " +
//            "execution(* ru.majordomo.hms.personmgr.controller.rest.*.*(..))")
//    public void afterLogginV3(JoinPoint joinPoint){
//        logger.info("logginV3, method: " + joinPoint.getSignature().getName());
//    }

//    @AfterThrowing(pointcut = "execution(* ru.majordomo.hms.personmgr.controller.rest.DoSomeShitController.*(..))", throwing = "ex")

//    @AfterThrowing(
//            pointcut = "@annotation(ru.majordomo.hms.personmgr.annotation.ReplacingExceptionWithBaseException)",
//            throwing = "ex")
//    public void replaceConstraintViolationException(ConstraintViolationException ex) throws Throwable {
//        logger.info("Подменяем исключение " + ex.getClass().getSimpleName() + " на ParameterValidationException");
//        throw new ru.majordomo.hms.personmgr.exception.newExceptions.ConstraintViolationException(ex.getConstraintViolations());
//    }

    private static BaseException convertThrowableToBaseException(Throwable ex) {
        if (ex instanceof BaseException) {
            return (BaseException) ex;
        } else if (ex instanceof BusinessActionNotFoundException) {
            return new ru.majordomo.hms.personmgr.exception.newExceptions.BusinessActionNotFoundException(ex.getMessage());
        } else if (ex instanceof ConstraintViolationException) {
            return new ru.majordomo.hms.personmgr.exception.newExceptions.ConstraintViolationException((ConstraintViolationException) ex);
        } else if (ex instanceof ChargeException) {
            return new NotEnoughMoneyException(ex.getMessage(), ((ChargeException) ex).getRequiredAmount());
        } else if (ex instanceof LowBalanceException) {
            return new NotEnoughMoneyException(ex.getMessage(), ((LowBalanceException) ex).getRequiredAmount());
        } else if (ex instanceof DomainNotAvailableException) {
            return new ru.majordomo.hms.personmgr.exception.newExceptions.DomainNotAvailableException(ex.getMessage());
        } else if (ex instanceof ParameterValidationException) {
            return new ru.majordomo.hms.personmgr.exception.newExceptions.ParameterValidationException(ex.getMessage());
        } else if (ex instanceof ParameterWithRoleSecurityException) {
            return new ru.majordomo.hms.personmgr.exception.newExceptions.ParameterWithRoleSecurityException(ex.getMessage());
        } else {
            return new ru.majordomo.hms.personmgr.exception.newExceptions.InternalApiException(ex.getMessage());
        }
    }
}
