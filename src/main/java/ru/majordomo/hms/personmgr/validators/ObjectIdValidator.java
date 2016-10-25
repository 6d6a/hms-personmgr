package ru.majordomo.hms.personmgr.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.personmgr.model.BaseModel;

/**
 * ObjectIdValidator
 */
@Component
class ObjectIdValidator implements ConstraintValidator<ObjectId, String> {
    private final MongoOperations operations;
    private Class<? extends BaseModel> objectModel;
    private String collection;
    private boolean notNull;

    @Autowired
    public ObjectIdValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(ObjectId objectId) {
        this.objectModel = objectId.value();
        this.collection = objectId.collection();
        this.notNull = objectId.notNull();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        try {
            if (!notNull && s == null) {
                return true;
            } else {
                BaseModel foundObject;
                if (!collection.equals("")) {
                    foundObject = operations.findById(s, this.objectModel, collection);
                } else {
                    foundObject = operations.findById(s, this.objectModel);
                }

                return foundObject != null;
            }
        } catch (RuntimeException e) {
            return false;
        }
    }
}
