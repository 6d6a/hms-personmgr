package ru.majordomo.hms.personmgr.service.PlanChange;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

public class RegularToRegular extends Processor {

    private LocalDateTime freeTestAbonementExpired;

    RegularToRegular(PersonalAccount account, Plan newPlan) {
        super(account, newPlan);
    }

    @Override
    public Boolean needToAddAbonement() {

        AccountAbonement accountAbonement = getAccountAbonementManager().findByPersonalAccountId(getAccount().getId());

        return accountAbonement != null;
    }

    @Override
    public BigDecimal calcCashBackAmount() {

        AccountAbonement accountAbonement = getAccountAbonementManager().findByPersonalAccountId(getAccount().getId());

        if (accountAbonement == null || hasFreeTestAbonement(accountAbonement) || accountAbonement.getAbonement().isInternal()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal delta;
        BigDecimal currentPlanCost = getCurrentPlan().getService().getCost();

        // Если смена тарифа с абонементом в тот же день, что он был куплен - возвращаем полную стоимость
        if (accountAbonement.getCreated().toLocalDate().isEqual(LocalDate.now())) {
            delta = accountAbonement.getAbonement().getService().getCost();
        } else {
            LocalDateTime nextDate = accountAbonement.getExpired()
                    .minus(Period.parse(accountAbonement.getAbonement().getPeriod())); // первая дата для начала пересчета АБ
            LocalDateTime stopDate = LocalDateTime.now(); // дата окончания пересчета абонемента
            while (stopDate.isAfter(nextDate)) {
                Integer daysInMonth = nextDate.toLocalDate().lengthOfMonth();
                total = total.add(currentPlanCost.divide(BigDecimal.valueOf(daysInMonth), 4, BigDecimal.ROUND_HALF_UP));
                nextDate = nextDate.plusDays(1L);
            }

            delta = (accountAbonement.getAbonement().getService().getCost()).subtract(total);
        }

        // delta может быть как отрицательной (будет списано), так и положительной (будет начислено)
        return delta;
    }

    @Override
    void deleteServices() {

        AccountAbonement accountAbonement = getAccountAbonementManager().findByPersonalAccountId(getAccount().getId());

        if (accountAbonement == null) {
            deletePlanService();
            return;
        }

        if (hasFreeTestAbonement(accountAbonement)) {

            freeTestAbonementExpired = accountAbonement.getExpired();

            deleteFreeTestAbonement();
            return;
        }

        deleteRegularAbonement();

        executeCashBackPayment(getIgnoreRestricts());
    }

    @Override
    void addServices() {

        if (getNewAbonementRequired()) {

            if (freeTestAbonementExpired != null) {
                addFreeTestAbonement(freeTestAbonementExpired);
                return;
            }

            Abonement abonement = getNewPlan().getNotInternalAbonement();
            getAccountHelper().charge(
                    getAccount(), abonement.getService(),
                    abonement.getService().getCost(),
                    getIgnoreRestricts(),
                    false
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
            Abonement abonement = getNewPlan().getFree14DaysAbonement();
            if (abonement != null) {
                AccountAbonement newFreeTestAbonement = addAccountAbonement(abonement);
                getAccountAbonementManager().setExpired(newFreeTestAbonement.getId(), expiredFreeTestAbonementDate);
            }
        }
    }


}
