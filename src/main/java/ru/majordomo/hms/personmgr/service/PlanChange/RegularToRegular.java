package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.service.PlanChange.behavior.CashBackCalculator;
import ru.majordomo.hms.personmgr.service.PlanChange.behavior.RegularToRegularCashBackCalculator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RegularToRegular extends Processor {

    private LocalDateTime freeTestAbonementExpired;

    RegularToRegular(PersonalAccount account, Plan newPlan) {
        super(account, newPlan);
    }

    @Override
    public Boolean needToAddAbonement() {
        return !currentAccountAbonements.isEmpty();
    }

    // результат может быть как отрицательный (будет списано), так и положительный (будет начислено)
    @Override
    public BigDecimal calcCashBackAmount() {
        CashBackCalculator<AccountAbonement> cashBackCalculator = new RegularToRegularCashBackCalculator(currentPlan);

        return currentAccountAbonements.stream().map(cashBackCalculator::calc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    void deleteServices() {
        if (currentAccountAbonements.isEmpty()) {
            deletePlanService();
            return;
        }

        if (hasOnlyFreeTestAbonement()) {
            freeTestAbonementExpired = currentAccountAbonements.get(0).getExpired();
            deleteAbonements();
            return;
        }

        deleteAbonements();
        executeCashBackPayment(ignoreRestricts);
    }

    @Override
    void addServices() {
        if (newAbonementRequired) {
            Abonement free14DaysAbonement = newPlan.getFree14DaysAbonement();
            if (freeTestAbonementExpired != null && free14DaysAbonement != null) {
                addAccountAbonement(free14DaysAbonement, freeTestAbonementExpired);
            } else {
                buyNotInternalAbonement();
            }
        } else {
            addPlanService();
        }
    }
}
