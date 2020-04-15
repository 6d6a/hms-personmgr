package ru.majordomo.hms.personmgr.service.PlanChange.behavior;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AbonementBuyInfo;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class RegularToRegularCashBackCalculator implements CashBackCalculator<AccountAbonement> {
    private final Plan currentPlan;

    public RegularToRegularCashBackCalculator(@NotNull Plan currentPlan) {
        this.currentPlan = currentPlan;
    }

    public BigDecimal calc(AccountAbonement currentAccountAbonement) {
        List<AbonementBuyInfo> buyInfo = currentAccountAbonement.getAbonementBuyInfos();
        buyInfo.sort(Comparator.comparing(AbonementBuyInfo::getBuyDate).reversed());

        Abonement abonement = currentAccountAbonement.getAbonement();
        BigDecimal abonementCost = abonement.getService().getCost();

        if (abonement.isInternal() || abonementCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (currentAccountAbonement.getExpired() == null) {
            return abonementCost;
        }

        if (currentAccountAbonement.getExpired().isBefore(LocalDateTime.now())) {
            return BigDecimal.ZERO;
        }

        LocalDate accountAbonementExpired = currentAccountAbonement.getExpired().toLocalDate();
        Period abonementPeriod = Period.parse(abonement.getPeriod());
        LocalDate now = LocalDate.now();
        LocalDate nextDate = accountAbonementExpired; // первая дата для начала пересчета АБ

        BigDecimal sumAbCost = BigDecimal.ZERO;
        Iterator<AbonementBuyInfo> iter = buyInfo.iterator();

        while (!nextDate.isEqual(now) && nextDate.isAfter(now)) {
            nextDate = nextDate.minus(abonementPeriod);
            if (iter.hasNext()) {
                AbonementBuyInfo a = iter.next();
                sumAbCost = sumAbCost.add(a.getBuyPrice());
            } else {
                sumAbCost = sumAbCost.add(abonementCost);
            }
        }

        BigDecimal dailyChargesAmount = BigDecimal.ZERO;
        BigDecimal currentPlanCost = currentPlan.getService().getCost();
        if (!nextDate.isEqual(now)) {
            while (now.isAfter(nextDate)) {
                int daysInMonth = nextDate.lengthOfMonth();
                dailyChargesAmount = dailyChargesAmount.add(currentPlanCost.divide(BigDecimal.valueOf(daysInMonth), 4, BigDecimal.ROUND_HALF_UP));
                nextDate = nextDate.plusDays(1L);
            }
        }

        return sumAbCost.subtract(dailyChargesAmount);
    }
}
