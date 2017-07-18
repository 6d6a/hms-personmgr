package ru.majordomo.hms.personmgr.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ru.majordomo.hms.personmgr.model.BaseModel;
import ru.majordomo.hms.personmgr.validation.ObjectId;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class ObjectIdValidator implements ConstraintValidator<ObjectId, String> {
    private final MongoOperations operations;
    private Class<? extends BaseModel> objectModel;
    private String collection;
    private String idFieldName;

    @Autowired
    public ObjectIdValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(ObjectId objectId) {
        this.objectModel = objectId.value();
        this.collection = objectId.collection();
        this.idFieldName = objectId.idFieldName();
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        try {
            if (s == null || s.equals("")) {
                return true;
            } else {
                boolean foundObject;
                Query query;

                if (idFieldName.equals("")) {
                    query = new Query(where("_id").is(s));
                } else {
                    query = new Query(where(idFieldName).is(s));
                }

                if (collection.equals("")) {
                    foundObject = operations.exists(query, this.objectModel);
                } else {
                    foundObject = operations.exists(query, this.objectModel, collection);
                }

                return foundObject;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }
}
