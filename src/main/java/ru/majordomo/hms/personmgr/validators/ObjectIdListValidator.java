package ru.majordomo.hms.personmgr.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.personmgr.model.BaseModel;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * ObjectIdListValidator
 */
@Component
class ObjectIdListValidator implements ConstraintValidator<ObjectIdList, List<String>> {
    private final MongoOperations operations;
    private Class<? extends BaseModel> objectModel;
    private String collection;

    @Autowired
    public ObjectIdListValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(ObjectIdList objectId) {
        this.objectModel = objectId.value();
        this.collection = objectId.collection();
    }

    @Override
    public boolean isValid(List<String> items, ConstraintValidatorContext constraintValidatorContext) {
        for (String next : items) {
            try {
                boolean foundObject;
                if (!collection.equals("")) {
                    foundObject = operations.exists(new Query(where("_id").is(next)), this.objectModel, collection);
                } else {
                    foundObject = operations.exists(new Query(where("_id").is(next)), this.objectModel);
                }

                if (!foundObject) {
                    return false;
                }
            } catch (RuntimeException e) {
                return false;
            }
        }

        return true;
    }
}
