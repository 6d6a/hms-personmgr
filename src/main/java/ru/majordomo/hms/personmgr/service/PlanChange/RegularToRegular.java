package ru.majordomo.hms.personmgr.service.PlanChange;

import org.apache.commons.collections.map.HashedMap;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.service.ChargeMessage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class RegularToRegular extends Processor {

    private LocalDateTime freeTestAbonementExpired;

    RegularToRegular(PersonalAccount account, Plan newPlan) {
        super(account, newPlan);
    }

    @Override
    public Boolean needToAddAbonement() {
        return currentAccountAbonement != null;
    }

    @Override
    public BigDecimal calcCashBackAmount() {
        if (currentAccountAbonement == null || hasFreeTestAbonement() || currentAccountAbonement.getAbonement().isInternal()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal delta;
        BigDecimal currentPlanCost = currentPlan.getService().getCost();

        Period abonementPeriod = Period.parse(currentAccountAbonement.getAbonement().getPeriod());

        LocalDate accountAbonementCreated = currentAccountAbonement.getCreated().toLocalDate();
        LocalDate accountAbonementExpired = currentAccountAbonement.getExpired().toLocalDate();

        // Если смена тарифа с абонементом в тот же день, что он был куплен - возвращаем полную стоимость
        if (accountAbonementCreated.isEqual(LocalDate.now())) {
            LocalDate nextDate = accountAbonementExpired; // первая дата для начала пересчета АБ

            long abonementCount = 0;

            //Вычитаем по одному абонементу пока не получим первую дату до текущей, с неё будем начинать расчет
            while (!nextDate.isEqual(accountAbonementCreated) && nextDate.isAfter(accountAbonementCreated)) {
                nextDate = nextDate.minus(abonementPeriod);
                abonementCount++;
            }

            delta = currentAccountAbonement
                    .getAbonement()
                    .getService()
                    .getCost()
                    .multiply(BigDecimal.valueOf(abonementCount));
        } else {
            LocalDate nextDate = accountAbonementExpired; // первая дата для начала пересчета АБ
            LocalDate stopDate = LocalDate.now(); // дата окончания пересчета абонемента

            long abonementCount = 0;

            //Вычитаем по одному абонементу пока не получим первую дату до текущей, с неё будем начинать расчет
            while (nextDate.isAfter(stopDate)) {
                nextDate = nextDate.minus(abonementPeriod);
                abonementCount++;
            }

            while (stopDate.isAfter(nextDate)) {
                Integer daysInMonth = nextDate.lengthOfMonth();
                total = total.add(currentPlanCost.divide(BigDecimal.valueOf(daysInMonth), 4, BigDecimal.ROUND_HALF_UP));
                nextDate = nextDate.plusDays(1L);
            }

            delta = (currentAccountAbonement.
                    getAbonement()
                    .getService()
                    .getCost()
                    .multiply(BigDecimal.valueOf(abonementCount))
            ).subtract(total);
        }

        // delta может быть как отрицательной (будет списано), так и положительной (будет начислено)
        return delta;
    }

    @Override
    void deleteServices() {
        if (currentAccountAbonement == null) {
            deletePlanService();
            return;
        }

        if (hasFreeTestAbonement()) {
            freeTestAbonementExpired = currentAccountAbonement.getExpired();

            deleteFreeTestAbonement();
            return;
        }

        deleteRegularAbonement();

        executeCashBackPayment(ignoreRestricts);
    }

    @Override
    void addServices() {
        if (newAbonementRequired) {
            if (freeTestAbonementExpired != null) {
                addFreeTestAbonement(freeTestAbonementExpired);
                return;
            }

            Abonement abonement = newPlan.getNotInternalAbonement();

            Map<String, Object> paymentOperationMessage;

            if (ignoreRestricts) {
                paymentOperationMessage = new ChargeMessage.ChargeBuilder(abonement.getService())
                        .forceCharge()
                        .build()
                        .getFullMessage();
            } else {
                paymentOperationMessage = new ChargeMessage.ChargeBuilder(abonement.getService())
                        .build()
                        .getFullMessage();
            }

            accountHelper.charge(account, paymentOperationMessage);
            addAccountAbonement(abonement);

        } else {
            addPlanService();
        }
    }

    /**
     * Добавление бесплатного абонмента
     *
     * @param expiredFreeTestAbonementDate дата истечения бесплатного абонемента
     */
    private void addFreeTestAbonement(
            LocalDateTime expiredFreeTestAbonementDate
    ) {
        if (expiredFreeTestAbonementDate.isAfter(LocalDateTime.now())) {
            Abonement abonement = newPlan.getFree14DaysAbonement();
            if (abonement != null) {
                AccountAbonement newFreeTestAbonement = addAccountAbonement(abonement);
                accountAbonementManager.setExpired(newFreeTestAbonement.getId(), expiredFreeTestAbonementDate);
            }
        }
    }
}
