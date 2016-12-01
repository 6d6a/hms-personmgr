package ru.majordomo.hms.personmgr.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.personmgr.model.BaseModel;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * ObjectIdValidator
 */
@Component
class ObjectIdValidator implements ConstraintValidator<ObjectId, String> {
    private final MongoOperations operations;
    private Class<? extends BaseModel> objectModel;
    private String collection;

    @Autowired
    public ObjectIdValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(ObjectId objectId) {
        this.objectModel = objectId.value();
        this.collection = objectId.collection();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        try {
            if (s == null || s.equals("")) {
                return true;
            } else {
                boolean foundObject;
                if (!collection.equals("")) {
                    foundObject = operations.exists(new Query(where("_id").is(s)), this.objectModel, collection);
                } else {
                    foundObject = operations.exists(new Query(where("_id").is(s)), this.objectModel);
                }

                return foundObject;
            }
        } catch (RuntimeException e) {
            return false;
        }
    }
}
