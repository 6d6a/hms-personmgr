package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.service.PlanChange.behavior.AbonementOnlyToAnyCashBackCalculator;
import ru.majordomo.hms.personmgr.service.PlanChange.behavior.CashBackCalculator;

import java.math.BigDecimal;
import java.util.List;

public class RegularToPartner extends RegularToRegular {
    RegularToPartner(PersonalAccount account, Plan newPlan) {
        super(account, newPlan);
    }

    @Override
    public BigDecimal calcCashBackAmount() {
        CashBackCalculator<AccountAbonement> calculator = new AbonementOnlyToAnyCashBackCalculator();

        List<AccountServiceAbonement> serviceAbonements = accountServiceHelper.getAllAccountServiceAbonements(account);
        BigDecimal cashBack = serviceAbonements
                .stream()
                .map(calculator::calc)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return cashBack.add(super.calcCashBackAmount());
    }

    @Override
    void deleteServices() {
        accountServiceHelper.completeDisableAllAdditionalServiceAbonements(account);
        accountServiceHelper.completeDisableAllAdditionalServices(account, "переход на тариф Партнёр");
        super.deleteServices();
    }
}
