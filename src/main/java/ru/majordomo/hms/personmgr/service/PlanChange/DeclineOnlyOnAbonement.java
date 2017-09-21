package ru.majordomo.hms.personmgr.service.PlanChange;

import org.apache.commons.lang.NotImplementedException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;

public class DeclineOnlyOnAbonement extends AbonementToRegular {

    DeclineOnlyOnAbonement(PersonalAccount account, Plan newPlan) {
        super(account, newPlan);
    }

    @Override
    public Boolean needToAddAbonement() {
        return false;
    }

    @Override
    void preValidate() {
        if (getAccount() == null) {
            throw new ParameterValidationException("Аккаунт не найден");
        }
    }

    @Override
    void deleteServices() {

        AccountAbonement accountAbonement = getAccountAbonementManager().findByPersonalAccountId(getAccount().getId());

        if (accountAbonement == null) {
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
    void postProcess() {}

    @Override
    void replaceServices() {}

}
