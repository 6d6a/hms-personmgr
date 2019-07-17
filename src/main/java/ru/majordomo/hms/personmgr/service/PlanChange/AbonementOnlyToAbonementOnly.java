package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.service.PlanChange.behavior.AbonementOnlyToAnyCashBackCalculator;
import ru.majordomo.hms.personmgr.service.PlanChange.behavior.CashBackCalculator;

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
        CashBackCalculator<AccountAbonement> calculator = new AbonementOnlyToAnyCashBackCalculator();
        return currentAccountAbonements.stream().map(calculator::calc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    void deleteServices() {
        deleteAbonements();
        deletePlanService();
        executeCashBackPayment(true);
    }

    @Override
    void addServices() {
        if (newAbonementRequired) {
            buyNotInternalAbonement();
        } else {
            addPlanService();
        }
    }
}
