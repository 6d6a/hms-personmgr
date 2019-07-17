package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import java.math.BigDecimal;

public class RegularToAbonementOnly extends Processor {

    RegularToAbonementOnly(PersonalAccount account, Plan newPlan) {
        super(account, newPlan);
    }

    @Override
    public Boolean needToAddAbonement() {
        return true;
    }

    @Override
    public BigDecimal calcCashBackAmount() {
        // С активным абонементом нельзя переходить на Парковку (Если только это не тестовый)
        return BigDecimal.ZERO;
    }

    @Override
    void deleteServices() {
        if (currentAccountAbonements.isEmpty()) {
            deletePlanService();
        } else {
            deleteAbonements();
        }
    }

    @Override
    void addServices() {
        if (newAbonementRequired) {
            buyNotInternalAbonement();
        }
    }

    @Override
    public void postProcess() {
        super.postProcess();

        //Выключить кредит
        disableCredit();
    }
}
