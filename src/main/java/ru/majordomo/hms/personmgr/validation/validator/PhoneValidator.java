package ru.majordomo.hms.personmgr.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.personmgr.common.PhoneNumberManager;
import ru.majordomo.hms.personmgr.validation.ValidPhone;


public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    @Override
    public void initialize(ValidPhone validPhone) {
    }

    @Override
    public boolean isValid(final String phone, ConstraintValidatorContext constraintValidatorContext) {
        return PhoneNumberManager.phoneValid(phone);
    }
}
