package ru.majordomo.hms.personmgr.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.validation.UniquePersonalAccountIdModel;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class UniquePersonalAccountIdValidator implements ConstraintValidator<UniquePersonalAccountIdModel, ModelBelongsToPersonalAccount> {
    private final MongoOperations operations;
    private Class<? extends BaseModel> objectModel;

    @Autowired
    public UniquePersonalAccountIdValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(UniquePersonalAccountIdModel uniquePersonalAccountIdModel) {
        this.objectModel = uniquePersonalAccountIdModel.value();
    }

    @Override
    public boolean isValid(final ModelBelongsToPersonalAccount modelBelongsToPersonalAccount, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid;
        try {
            Query query;

            if (modelBelongsToPersonalAccount.getId() != null) {
                query = new Query(where("_id").nin(modelBelongsToPersonalAccount.getId()).and("personalAccountId").is(modelBelongsToPersonalAccount.getPersonalAccountId()));
            } else {
                query = new Query(where("personalAccountId").is(modelBelongsToPersonalAccount.getPersonalAccountId()));
            }

            isValid = !operations.exists(query, this.objectModel);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
        return isValid;
    }
}
