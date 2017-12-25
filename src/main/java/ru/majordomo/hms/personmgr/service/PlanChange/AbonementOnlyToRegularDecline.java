package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;

public class AbonementOnlyToRegularDecline extends AbonementOnlyToRegular {
    private boolean refund = true;

    AbonementOnlyToRegularDecline(PersonalAccount account) {
        super(account, null);
    }

    AbonementOnlyToRegularDecline(PersonalAccount account, boolean refund) {
        this(account);
        this.refund = refund;
    }

    @Override
    public Boolean needToAddAbonement() {
        return false;
    }

    @Override
    void preValidate() {
        if (account == null) {
            throw new ParameterValidationException("Аккаунт не найден");
        }
    }

    @Override
    void deleteServices() {
        if (currentAccountAbonement == null) {
            return;
        }

        deleteRegularAbonement();

        if (refund) {
            executeCashBackPayment(true);
        }
    }

    @Override
    void addServices() {
        if (!accountHasService(currentPlan.getServiceId())) {
            addServiceById(currentPlan.getServiceId());
        }
    }

    @Override
    void postProcess() {}

    @Override
    void replaceServices() {}
}
