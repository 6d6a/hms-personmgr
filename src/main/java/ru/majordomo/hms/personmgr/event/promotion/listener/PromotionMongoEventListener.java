package ru.majordomo.hms.personmgr.event.promotion.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;

import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class PromotionMongoEventListener extends AbstractMongoEventListener<Promotion> {

        private final MongoOperations mongoOperations;

        @Autowired
        public PromotionMongoEventListener(MongoOperations mongoOperations) {
            this.mongoOperations = mongoOperations;
        }

        @Override
        public void onAfterConvert(AfterConvertEvent<Promotion> event) {
            super.onAfterConvert(event);
            Promotion promotion = event.getSource();

            List<PromocodeAction> actions = mongoOperations.find(new Query(where("_id").in(promotion.getActionIds())), PromocodeAction.class);

            promotion.setActions(actions);
        }
}
