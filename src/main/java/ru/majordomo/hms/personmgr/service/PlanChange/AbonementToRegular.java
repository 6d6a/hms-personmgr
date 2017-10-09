package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.DAYS;

public class AbonementToRegular extends Processor {

    AbonementToRegular(PersonalAccount account, Plan newPlan) {
        super(account, newPlan);
    }

    @Override
    public Boolean needToAddAbonement() {
        return false;
    }

    @Override
    public BigDecimal calcCashBackAmount() {

        AccountAbonement accountAbonement = getAccountAbonementManager().findByPersonalAccountId(getAccount().getId());

        if (accountAbonement == null || accountAbonement.getAbonement().isInternal()) {
            return BigDecimal.ZERO;
        }

        if (accountAbonement.getExpired().isAfter(LocalDateTime.now())) {
            long remainingDays = DAYS.between(LocalDateTime.now(), accountAbonement.getExpired());
            //Получим стоимость тарифа в день с точностью до семи знаков, округляя в меньшую сторону
            BigDecimal dayCost = accountAbonement.getAbonement().getService().getCost().divide(BigDecimal.valueOf(365L), 7, RoundingMode.DOWN);
            BigDecimal remainedServiceCost = (BigDecimal.valueOf(remainingDays)).multiply(dayCost);
            //Округлим до двух знаков в большую сторону
            remainedServiceCost = remainedServiceCost.setScale(2, RoundingMode.HALF_UP);

            //За парковку только возвращаем средства
            if (remainedServiceCost.compareTo(BigDecimal.ZERO) > 0) {
                return remainedServiceCost;
            }
        }

        return BigDecimal.ZERO;
    }

    @Override
    void deleteServices() {

        AccountAbonement accountAbonement = getAccountAbonementManager().findByPersonalAccountId(getAccount().getId());

        if (accountAbonement == null) {
            deletePlanService();
            return;
        }

        deleteRegularAbonement();

        executeCashBackPayment(getIgnoreRestricts());
    }

    @Override
    void addServices() {

        if (getNewAbonementRequired()) {

            Abonement abonement = getNewPlan().getNotInternalAbonement();
            getAccountHelper().charge(getAccount(), abonement.getService());
            addAccountAbonement(abonement);

        } else {
            addPlanService();
        }

    }

}
