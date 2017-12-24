package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;

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
            long abonementDaysToExpired = ChronoUnit.DAYS.between(accountAbonementCreated, accountAbonementExpired);
            long oneAbonementDays = ChronoUnit.DAYS.between(accountAbonementCreated, accountAbonementCreated.plus(abonementPeriod));

            if (abonementDaysToExpired%oneAbonementDays != 0) {
                throw new ParameterValidationException("Количество дней до окончания абонемента не " +
                        "кратно количеству дней в одном абонементе");
            }

            long abonementCount = abonementDaysToExpired/oneAbonementDays;

            delta = currentAccountAbonement
                    .getAbonement()
                    .getService()
                    .getCost()
                    .multiply(BigDecimal.valueOf(abonementCount));
        } else {
            LocalDate nextDate = accountAbonementExpired; // первая дата для начала пересчета АБ
            LocalDate stopDate = LocalDate.now(); // дата окончания пересчета абонемента

            //Вычитаем по одному абонементу пока не получим первую дату до текущей, с неё будем начинать расчет
            while (nextDate.isAfter(stopDate)) {
                nextDate = nextDate.minus(abonementPeriod);
            }

            long abonementDaysToExpired = ChronoUnit.DAYS.between(nextDate, accountAbonementExpired);
            long oneAbonementDays = ChronoUnit.DAYS.between(nextDate, nextDate.plus(abonementPeriod));

            if (abonementDaysToExpired%oneAbonementDays != 0) {
                throw new ParameterValidationException("Количество дней до окончания абонемента не " +
                        "кратно количеству дней в одном абонементе");
            }

            long abonementCount = abonementDaysToExpired/oneAbonementDays;

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
