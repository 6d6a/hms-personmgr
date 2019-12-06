package ru.majordomo.hms.personmgr.event.personalAccount.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.model.Preorder;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.discount.AccountDiscount;
import ru.majordomo.hms.personmgr.model.discount.Discount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.DiscountedService;
import ru.majordomo.hms.personmgr.service.DiscountServiceHelper;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class PersonalAccountMongoEventListener extends AbstractMongoEventListener<PersonalAccount> {
    private final MongoOperations mongoOperations;
    private final DiscountServiceHelper discountServiceHelper;

    @Autowired
    public PersonalAccountMongoEventListener(
            MongoOperations mongoOperations,
            DiscountServiceHelper discountServiceHelper
    ) {
        this.mongoOperations = mongoOperations;
        this.discountServiceHelper = discountServiceHelper;
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<PersonalAccount> event) {
        super.onAfterConvert(event);
        PersonalAccount personalAccount = event.getSource();

        personalAccount.setServices(mongoOperations.find(new Query(where("personalAccountId").is(personalAccount.getId())), AccountService.class));

        if (personalAccount.getDiscounts() != null) {
            for (AccountDiscount accountDiscount : personalAccount.getDiscounts()) {
                accountDiscount.setDiscount(mongoOperations.findById(accountDiscount.getDiscountId(), Discount.class, "discount"));
            }
        }

        List<AccountService> accountServiceList = personalAccount.getServices();
        List<AccountService> accountServiceListAfterDiscountConvert = new ArrayList<>();
        accountServiceListAfterDiscountConvert.addAll(accountServiceList);

        if (personalAccount.getServices() != null && personalAccount.getDiscounts() != null) {
            for (AccountService accountService : personalAccount.getServices()) {
                DiscountedService discountedService = discountServiceHelper.getDiscountedService(personalAccount.getDiscounts(), accountService);

                if (discountedService != null) {
                    accountServiceListAfterDiscountConvert.remove(accountService);
                    accountServiceListAfterDiscountConvert.add(discountedService);
                }
            }
            personalAccount.setServices(accountServiceListAfterDiscountConvert);
        }

        personalAccount.setPreorder(mongoOperations.exists(new Query(where("personalAccountId").is(personalAccount.getId())), Preorder.class));
    }
}
