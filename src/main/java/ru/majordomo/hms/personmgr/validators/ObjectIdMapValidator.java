package ru.majordomo.hms.personmgr.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.personmgr.model.BaseModel;

/**
 * ObjectIdListValidator
 */
@Component
class ObjectIdMapValidator implements ConstraintValidator<ObjectIdMap, Map<String, Integer>> {
    private final MongoOperations operations;
    private Class<? extends BaseModel> objectModel;
    private String collection;

    @Autowired
    public ObjectIdMapValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(ObjectIdMap objectId) {
        this.objectModel = objectId.value();
        this.collection = objectId.collection();
    }

    @Override
    public boolean isValid(Map<String, Integer> items, ConstraintValidatorContext constraintValidatorContext) {
        Set<Map.Entry<String, Integer>> entries = items.entrySet();

        for (Map.Entry<String, Integer> next : entries) {
            try {
                BaseModel foundObject;
                if (!collection.equals("")) {
                    foundObject = operations.findById(next.getKey(), this.objectModel, collection);
                } else {
                    foundObject = operations.findById(next.getKey(), this.objectModel);
                }

                if (foundObject == null) {
                    return false;
                }
            } catch (RuntimeException e) {
                return false;
            }
        }

        return true;
    }
}
