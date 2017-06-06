package ru.majordomo.hms.personmgr.validation.event.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Set;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import ru.majordomo.hms.personmgr.model.account.AccountOwner;
import ru.majordomo.hms.personmgr.validation.groupSequenceProvider.AccountOwnerGroupSequenceProvider;

public class CustomValidatingMongoEventListener extends AbstractMongoEventListener<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(CustomValidatingMongoEventListener.class);

    private final Validator validator;

    /**
     * Creates a new {@link CustomValidatingMongoEventListener} using the given {@link Validator}.
     *
     * @param validator must not be {@literal null}.
     */
    public CustomValidatingMongoEventListener(Validator validator) {

        Assert.notNull(validator, "Validator must not be null!");
        this.validator = validator;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onBeforeSave(BeforeSaveEvent<Object> event) {

        Object source = event.getSource();

        LOG.debug("Validating object: {}", source);

        Set violations;

        if (source instanceof AccountOwner) {
            AccountOwnerGroupSequenceProvider accountOwnerGroupSequenceProvider = new AccountOwnerGroupSequenceProvider();

            violations = validator.validate(
                    source,
                    accountOwnerGroupSequenceProvider.getValidationGroupsCustom((AccountOwner) source)
                            .toArray(new Class<?>[]{})
            );
        } else {
            violations = validator.validate(source);
        }

        if (!violations.isEmpty()) {

            LOG.info("During object: {} validation violations found: {}", source, violations);
            throw new ConstraintViolationException(violations);
        }
    }
}
