package ru.majordomo.hms.personmgr.service.PlanChange.behavior;

import org.junit.Assert;
import org.junit.Test;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;

import static java.time.temporal.ChronoUnit.DAYS;

//Расчёт возвращаемых средств за абонемент дополнительных услуг
public class AnyToPartnerServiceAbonementCashBackCalculatorTest {
    @Test
    public void calcYearAbonement() {
        AbonementOnlyToAnyCashBackCalculator calculator = new AbonementOnlyToAnyCashBackCalculator();

        LocalDateTime now = LocalDateTime.now().plusSeconds(1L);
        long daysInYear = DAYS.between(now, now.plus(Period.parse("P1Y")));

        AccountServiceAbonement serviceAbonement = createYearAbonement();

        BigDecimal cost = serviceAbonement.getAbonement().getService().getCost();
        BigDecimal dailyCost = cost.divide(BigDecimal.valueOf(daysInYear), 2, BigDecimal.ROUND_HALF_UP);

        //AccountServiceAbonement#expired is not set -> full abonement cost
        Assert.assertEquals(cost, calculator.calc(serviceAbonement));

        //Whole year -> full abonement cost
        serviceAbonement.setExpired(now.plusYears(1L));
        Assert.assertEquals(cost.setScale(2, BigDecimal.ROUND_HALF_UP), calculator.calc(serviceAbonement));

        //1 day to expire -> one charge for one day only
        serviceAbonement.setExpired(now.plusDays(1L));
        Assert.assertEquals(dailyCost, calculator.calc(serviceAbonement));
    }

    @Test
    public void calcMonthAbonement() {
        AbonementOnlyToAnyCashBackCalculator calculator = new AbonementOnlyToAnyCashBackCalculator();

        LocalDateTime now = LocalDateTime.now().plusSeconds(1L);
        long daysInMonth = DAYS.between(now, now.plus(Period.parse("P1M")));

        AccountServiceAbonement serviceAbonement = createMonthAbonement();

        BigDecimal cost = serviceAbonement.getAbonement().getService().getCost();
        BigDecimal dailyCost = cost.divide(BigDecimal.valueOf(daysInMonth), 2, BigDecimal.ROUND_HALF_UP);

        //AccountServiceAbonement#expired is not set -> full abonement cost
        Assert.assertEquals(cost, calculator.calc(serviceAbonement));

        //Whole month to expire -> full abonement cost
        serviceAbonement.setExpired(now.plusMonths(1L));
        Assert.assertEquals(cost.setScale(2, BigDecimal.ROUND_HALF_UP), calculator.calc(serviceAbonement));

        //Expire tomorrow -> one day cost
        serviceAbonement.setExpired(now.plusDays(1L));
        Assert.assertEquals(dailyCost, calculator.calc(serviceAbonement));

        //Expire today -> 0
        serviceAbonement.setExpired(now);
        Assert.assertEquals(BigDecimal.ZERO, calculator.calc(serviceAbonement));

        //Internal abonement -> 0
        serviceAbonement.getAbonement().setInternal(true);
        Assert.assertEquals(BigDecimal.ZERO, calculator.calc(serviceAbonement));
        serviceAbonement.getAbonement().setInternal(false);

        //Zero cost -> 0
        serviceAbonement.getAbonement().getService().setCost(BigDecimal.ZERO);
        Assert.assertEquals(BigDecimal.ZERO, calculator.calc(serviceAbonement));
    }

    private AccountServiceAbonement createMonthAbonement() {
        return createAccountServiceAbonement("Тестовая услуга (абонемент 1 месяц)", "P1M", "99");
    }

    private AccountServiceAbonement createYearAbonement() {
        return createAccountServiceAbonement("Тестовая услуга (абонемент 1 год)", "P1Y", "999");
    }

    private AccountServiceAbonement createAccountServiceAbonement(String name, String period, String cost) {
        PaymentService paymentService = new PaymentService();
        paymentService.setCost(new BigDecimal(cost));

        Abonement abonement = new Abonement();
        abonement.setName(name);
        abonement.setPeriod(period);
        abonement.setService(paymentService);

        AccountServiceAbonement serviceAbonement = new AccountServiceAbonement();
        serviceAbonement.setAbonement(abonement);
        return serviceAbonement;
    }
}
