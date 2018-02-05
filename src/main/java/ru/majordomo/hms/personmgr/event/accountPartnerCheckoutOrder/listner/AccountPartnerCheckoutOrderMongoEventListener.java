package ru.majordomo.hms.personmgr.event.accountPartnerCheckoutOrder.listner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.AccountPartnerCheckoutOrder;

@Component
public class AccountPartnerCheckoutOrderMongoEventListener extends AbstractMongoEventListener<AccountPartnerCheckoutOrder> {
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountPartnerCheckoutOrderMongoEventListener(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<AccountPartnerCheckoutOrder> event) {
        super.onAfterConvert(event);
        AccountPartnerCheckoutOrder accountPartnerCheckoutOrder = event.getSource();

        PersonalAccount account = mongoOperations.findById(accountPartnerCheckoutOrder.getPersonalAccountId(), PersonalAccount.class);

        if (account != null) {
            accountPartnerCheckoutOrder.setPersonalAccountName(account.getName());
        }
    }
}
