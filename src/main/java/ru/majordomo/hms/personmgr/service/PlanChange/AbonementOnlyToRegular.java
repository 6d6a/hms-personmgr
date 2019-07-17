package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.service.PlanChange.behavior.CashBackCalculator;
import ru.majordomo.hms.personmgr.service.PlanChange.behavior.AbonementOnlyToAnyCashBackCalculator;

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
        CashBackCalculator<AccountAbonement> calculator = new AbonementOnlyToAnyCashBackCalculator();
        return currentAccountAbonements.stream().map(calculator::calc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    void deleteServices() {
        if (currentAccountAbonements.isEmpty()) {
            deletePlanService();
            return;
        }

        deleteAbonements();

        executeCashBackPayment(ignoreRestricts);
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
