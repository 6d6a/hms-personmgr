package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.DAYS;

public class AbonementToRegular extends Processor {

    AbonementToRegular(Plan currentPlan, Plan newPlan) {
        super(currentPlan, newPlan);
    }

    @Override
    public Boolean needToAddAbonement() {
        return false;
    }

    @Override
    public BigDecimal hulkCashBackAmount() {

        AccountAbonement accountAbonement = getAccountAbonementManager().findByPersonalAccountId(getAccount().getId());

        if (accountAbonement == null) {
            return BigDecimal.ZERO;
        }

        if (accountAbonement.getAbonement().isInternal()) {
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

        executeCashBackPayment(false);
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

    @Override
    public void postProcess() {
        //Укажем новый тариф
        getAccountManager().setPlanId(getAccount().getId(), getNewPlan().getId());

        //Разрешён ли сертификат на новом тарифе
        sslCertAllowed();

        //При необходимости отправляем письмо в саппорт
        supportNotification();

        //Сохраним статистику смены тарифа
        saveStat();

        //Сохраним историю аккаунта
        saveHistory();
    }

}
