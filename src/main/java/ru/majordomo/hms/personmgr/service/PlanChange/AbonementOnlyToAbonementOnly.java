package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import java.math.BigDecimal;

public class AbonementOnlyToAbonementOnly extends Processor {

    public AbonementOnlyToAbonementOnly(PersonalAccount account, Plan newPlan) {
        super(account, newPlan);
    }

    @Override
    Boolean needToAddAbonement() {
        return true;
    }

    @Override
    BigDecimal calcCashBackAmount() {
        return calcCashBackAmountForAbonementOnlyToAny(currentAccountAbonement);
    }

    @Override
    void deleteServices() {
        deleteAbonements();
        deletePlanService();
        executeCashBackPayment(true);
    }

    @Override
    void addServices() {
        addServiceForAbonementOnlyToAnyNotDecline(account, newPlan, newAbonementRequired, ignoreRestricts);
    }
}
