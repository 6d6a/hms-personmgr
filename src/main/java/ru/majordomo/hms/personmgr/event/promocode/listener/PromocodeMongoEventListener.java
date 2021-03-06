package ru.majordomo.hms.personmgr.event.promocode.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeTag;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class PromocodeMongoEventListener extends AbstractMongoEventListener<Promocode> {
    private final MongoOperations mongoOperations;

    @Autowired
    public PromocodeMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<Promocode> event) {
        super.onAfterConvert(event);
        Promocode promocode = event.getSource();

        promocode.setActions(
                mongoOperations.find(new Query(where("_id").in(promocode.getActionIds())), PromocodeAction.class)
        );


        promocode.setTags(
                mongoOperations.find(new Query(where("_id").in(promocode.getTagIds())), PromocodeTag.class)
        );
    }
}
