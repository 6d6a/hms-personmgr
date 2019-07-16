package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

public class AbonementOnlyToRegularDecline extends AbonementOnlyToRegular {
    private final boolean refund;

    AbonementOnlyToRegularDecline(PersonalAccount account, boolean refund) {
        super(account, null);
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

        deleteAbonements();

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
