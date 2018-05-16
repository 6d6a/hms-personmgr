package ru.majordomo.hms.personmgr.validation.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ru.majordomo.hms.personmgr.validation.RemoteObjectId;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class RemoteObjectIdValidator implements ConstraintValidator<RemoteObjectId, String> {
    private Class objectModel;
//    private final PmFeignClient pmFeignClient;

    @Autowired
    public RemoteObjectIdValidator(
//            PmFeignClient pmFeignClient
    ) {
//        this.pmFeignClient = pmFeignClient;
    }

    @Override
    public void initialize(RemoteObjectId objectId) {
        this.objectModel = objectId.value();
    }

    @Override
    public boolean isValid(String id, ConstraintValidatorContext constraintValidatorContext) {
        try {
//            if (objectModel == PaymentService.class) {
//                PaymentService paymentService = pmFeignClient.getPaymentService(id);
//                return paymentService != null;
//            } else {
                // not implemented
                return false;
//            }
        } catch (RuntimeException e) {
            log.error("Catch exception in " + getClass() + " message: " + e.getMessage());
            return false;
        }
    }
}
