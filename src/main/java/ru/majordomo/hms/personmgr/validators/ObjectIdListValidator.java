package ru.majordomo.hms.personmgr.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
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
    private final MongoTemplate mongoTemplate;
    private Class<? extends BaseModel> objectModel;
    private String collection;

    @Autowired
    public ObjectIdListValidator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void initialize(ObjectIdList objectId) {
        this.objectModel = objectId.value();
        this.collection = objectId.collection();
    }

    @Override
    public boolean isValid(List<String> items, ConstraintValidatorContext constraintValidatorContext) {
        if (items == null || items.isEmpty()) {
            return true;
        } else {
            for (String next : items) {
                try {
                    boolean foundObject;
                    if (!collection.equals("")) {
                        foundObject = mongoTemplate.exists(
                                new Query(where("_id").is(next)),
                                this.objectModel,
                                collection
                        );
                    } else {
                        foundObject = mongoTemplate.exists(
                                new Query(where("_id").is(next)),
                                this.objectModel
                        );
                    }

                    if (!foundObject) {
                        return false;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        return true;
    }
}
