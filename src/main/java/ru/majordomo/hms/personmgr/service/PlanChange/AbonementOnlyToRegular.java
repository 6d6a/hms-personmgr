package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import java.math.BigDecimal;

public class AbonementOnlyToRegular extends Processor {

    AbonementOnlyToRegular(PersonalAccount account, Plan newPlan) {
        super(account, newPlan);
    }

    @Override
    public Boolean needToAddAbonement() {
        return false;
    }

    @Override
    public BigDecimal calcCashBackAmount() {
        return calcCashBackAmountForAbonementOnlyToAny(currentAccountAbonement);
    }

    @Override
    void deleteServices() {
        if (currentAccountAbonement == null) {
            deletePlanService();
            return;
        }

        deleteAbonements();

        executeCashBackPayment(ignoreRestricts);
    }

    @Override
    void addServices() {
        addServiceForAbonementOnlyToAnyNotDecline(account, newPlan, newAbonementRequired, ignoreRestricts);
    }
}
