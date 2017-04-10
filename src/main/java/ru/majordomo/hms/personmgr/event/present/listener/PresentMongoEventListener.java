package ru.majordomo.hms.personmgr.event.present.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

import ru.majordomo.hms.personmgr.model.present.Present;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class PresentMongoEventListener extends AbstractMongoEventListener<Present> {

        private final MongoOperations mongoOperations;

        @Autowired
        public PresentMongoEventListener(MongoOperations mongoOperations) {
            this.mongoOperations = mongoOperations;
        }

        @Override
        public void onAfterConvert(AfterConvertEvent<Present> event) {
            super.onAfterConvert(event);
            Present present = event.getSource();

            List<PromocodeAction> actions = mongoOperations.find(new Query(where("_id").in(present.getActionIds())), PromocodeAction.class);

            present.setActions(actions);
        }
}
