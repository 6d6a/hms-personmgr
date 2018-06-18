package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.service.ChargeMessage;

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
        if (currentAccountAbonement == null) {
            deletePlanService();
        } else if (hasFreeTestAbonement()) {
            deleteFreeTestAbonement();
        } else {
            deleteRegularAbonement();
        }
    }

    @Override
    void addServices() {
        if (newAbonementRequired) {
            Abonement abonement = newPlan.getNotInternalAbonement();

            ChargeMessage chargeMessage = new ChargeMessage.Builder(abonement.getService())
                    .setForceCharge(ignoreRestricts)
                    .build();

            accountHelper.charge(account, chargeMessage);

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
