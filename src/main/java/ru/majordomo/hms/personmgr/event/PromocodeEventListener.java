package ru.majordomo.hms.personmgr.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class PromocodeEventListener extends AbstractMongoEventListener<Promocode> {
    @Autowired
    private MongoOperations mongoOperations;

    @Override
    public void onAfterConvert(AfterConvertEvent<Promocode> event) {
        super.onAfterConvert(event);
        Promocode promocode = event.getSource();

        List<PromocodeAction> actions = mongoOperations.find(new Query(where("_id").in(promocode.getActionIds())), PromocodeAction.class);

        promocode.setActions(actions);
    }
}
