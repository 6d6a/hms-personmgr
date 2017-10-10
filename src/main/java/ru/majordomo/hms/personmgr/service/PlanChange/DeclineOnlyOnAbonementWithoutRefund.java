package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;

public class DeclineOnlyOnAbonementWithoutRefund extends DeclineOnlyOnAbonement {
    DeclineOnlyOnAbonementWithoutRefund(PersonalAccount account, Plan newPlan) {
        super(account, newPlan);
    }

    @Override
    void deleteServices() {

        AccountAbonement accountAbonement = getAccountAbonementManager().findByPersonalAccountId(getAccount().getId());

        if (accountAbonement == null) {
            return;
        }

        deleteRegularAbonement();
    }
}
