package ru.majordomo.hms.personmgr.service.PlanChange.behavior;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import static java.time.temporal.ChronoUnit.DAYS;

public class AbonementOnlyToAnyCashBackCalculator implements CashBackCalculator<AccountAbonement> {

    public BigDecimal calc(AccountAbonement currentAccountAbonement) {
        Abonement abonement = currentAccountAbonement.getAbonement();
        BigDecimal abonementCost = abonement.getService().getCost();

        if (abonement.isInternal() || abonementCost.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (currentAccountAbonement.getExpired() == null) {
            return abonementCost;
        }

        if (currentAccountAbonement.getExpired().isAfter(LocalDateTime.now())) {
            long remainingDays = DAYS.between(LocalDateTime.now(), currentAccountAbonement.getExpired());

            //Длительность абонемента в днях
            Period abonementPeriod = Period.parse(abonement.getPeriod());
            LocalDate now = LocalDate.now();
            BigDecimal durationAbonementInDays = BigDecimal.valueOf(DAYS.between(now, now.plus(abonementPeriod)));

            //Получим стоимость тарифа в день с точностью до семи знаков, округляя в меньшую сторону
            BigDecimal dayCost = abonementCost.divide(durationAbonementInDays, 7, RoundingMode.DOWN);

            BigDecimal remainedServiceCost = (BigDecimal.valueOf(remainingDays)).multiply(dayCost);
            //Округлим до двух знаков в большую сторону
            remainedServiceCost = remainedServiceCost.setScale(2, RoundingMode.HALF_UP);

            //Не можем вернуть отрицательное количество неиспользованных средств
            if (remainedServiceCost.compareTo(BigDecimal.ZERO) > 0) {
                return remainedServiceCost;
            }
        }

        return BigDecimal.ZERO;
    }
}
