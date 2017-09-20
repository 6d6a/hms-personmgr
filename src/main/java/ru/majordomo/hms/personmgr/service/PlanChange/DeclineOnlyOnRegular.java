package ru.majordomo.hms.personmgr.service.PlanChange;

import org.apache.commons.lang.NotImplementedException;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;

public class DeclineOnlyOnRegular extends RegularToRegular {

    DeclineOnlyOnRegular(Plan currentPlan, Plan newPlan) {
        super(currentPlan, newPlan);
    }

    @Override
    public Boolean needToAddAbonement() {
        throw new NotImplementedException();
    }

    @Override
    void deleteServices() {

        AccountAbonement accountAbonement = getAccountAbonementManager().findByPersonalAccountId(getAccount().getId());

        if (accountAbonement == null) {
            return;
        }

        if (hasFreeTestAbonement(accountAbonement)) {
            deleteFreeTestAbonement();
            return;
        }

        deleteRegularAbonement();

        executeCashBackPayment(true);
    }

    @Override
    void addServices() {

        if (!accountHasService(getCurrentPlan().getServiceId())) {
            addServiceById(getCurrentPlan().getServiceId());
        }
    }

    @Override
    public void postProcess() {
        throw new NotImplementedException();
    }

}
