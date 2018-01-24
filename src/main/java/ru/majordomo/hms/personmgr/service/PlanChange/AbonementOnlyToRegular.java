package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.DAYS;

public class AbonementOnlyToRegular extends Processor {

    AbonementOnlyToRegular(PersonalAccount account, Plan newPlan) {
        super(account, newPlan);
    }

    @Override
    public Boolean needToAddAbonement() {
        return false;
    }

    @Override
    public BigDecimal calcCashBackAmount() {
        if (currentAccountAbonement == null || currentAccountAbonement.getAbonement().isInternal()) {
            return BigDecimal.ZERO;
        }

        if (currentAccountAbonement.getExpired().isAfter(LocalDateTime.now())) {
            long remainingDays = DAYS.between(LocalDateTime.now(), currentAccountAbonement.getExpired());
            //Получим стоимость тарифа в день с точностью до семи знаков, округляя в меньшую сторону
            BigDecimal dayCost = currentAccountAbonement.getAbonement().getService().getCost().divide(BigDecimal.valueOf(365L), 7, RoundingMode.DOWN);
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
        if (currentAccountAbonement == null) {
            deletePlanService();
            return;
        }

        deleteRegularAbonement();

        executeCashBackPayment(ignoreRestricts);
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
        } else {
            addPlanService();
        }
    }
}