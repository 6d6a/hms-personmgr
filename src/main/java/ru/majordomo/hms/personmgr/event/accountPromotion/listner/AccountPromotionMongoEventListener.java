package ru.majordomo.hms.personmgr.event.accountPromotion.listner;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;

@Component
public class AccountPromotionMongoEventListener extends AbstractMongoEventListener<AccountPromotion> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountPromotionMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountPromotion> event) {
        super.onAfterConvert(event);
        AccountPromotion accountPromotion = event.getSource();

        Promotion promotion = mongoOperations.findById(accountPromotion.getPromotionId(), Promotion.class);

        accountPromotion.setPromotion(promotion);

        accountPromotion.setPersonalAccountName(mongoOperations.findById(accountPromotion.getPersonalAccountId(), PersonalAccount.class).getName());

        accountPromotion.setAction(mongoOperations.findById(accountPromotion.getActionId(), PromocodeAction.class));
    }
}
