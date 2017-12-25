package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        if (currentAccountAbonement == null) {
            deletePlanService();
            return;
        }

        if (hasFreeTestAbonement()) {
            deleteFreeTestAbonement();
        }
    }

    @Override
    void addServices() {
        if (newAbonementRequired) {
            Abonement abonement = newPlan.getNotInternalAbonement();
            accountHelper.charge(
                    account, abonement.getService(),
                    abonement.getService().getCost(),
                    ignoreRestricts,
                    false,
                    LocalDateTime.now()
            );
            addAccountAbonement(abonement);
        }
    }

    @Override
    public void postProcess() {
        super.postProcess();

        //Выключить кредит
        disableCredit();
    }
}
