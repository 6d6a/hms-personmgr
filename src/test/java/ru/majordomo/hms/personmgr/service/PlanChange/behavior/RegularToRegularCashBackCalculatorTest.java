package ru.majordomo.hms.personmgr.service.PlanChange.behavior;

import org.junit.Assert;
import org.junit.Test;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RegularToRegularCashBackCalculatorTest {

    @Test
    public void calc() {
        LocalDateTime now = LocalDateTime.now();

        PaymentService planService = new PaymentService();
        planService.setCost(new BigDecimal("300"));

        Plan plan = new Plan();
        plan.setService(planService);

        RegularToRegularCashBackCalculator calculator = new RegularToRegularCashBackCalculator(plan);

        BigDecimal abonementCost = new BigDecimal("3000");
        PaymentService abonementService = new PaymentService();
        abonementService.setCost(abonementCost);

        Abonement abonement = new Abonement();
        abonement.setPeriod("P1Y");
        abonement.setService(abonementService);

        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setAbonement(abonement);

        //expired is not set, return full cost
        Assert.assertEquals(abonementCost, calculator.calc(accountAbonement));

        //for 2 full abonement periods (cost * 2)
        accountAbonement.setExpired(now.plusYears(2));
        Assert.assertEquals(new BigDecimal("6000"), calculator.calc(accountAbonement));

        int daysInMonth = now.toLocalDate().lengthOfMonth();
        BigDecimal dailyCost = new BigDecimal("300")
                .divide(BigDecimal.valueOf(daysInMonth), 4, BigDecimal.ROUND_HALF_UP);

        //for every day that can daily charge subtract from abonement cost
        accountAbonement.setExpired(now.plusYears(1).minusDays(1));
        Assert.assertEquals(abonementCost.subtract(dailyCost), calculator.calc(accountAbonement));

        //for decline almost full year cashBack is negative because (daily cost * day in year) > abonement cost
        accountAbonement.setExpired(now.plusDays(1));
        Assert.assertTrue(calculator.calc(accountAbonement).compareTo(BigDecimal.ZERO) < 0);

        //for internal abonement always zero
        abonement.setInternal(true);
        Assert.assertEquals(BigDecimal.ZERO, calculator.calc(accountAbonement));

        //for zero cost abonement always zero
        abonement.setInternal(false);
        accountAbonement.setExpired(now.plusYears(1).plusMonths(1).plusDays(1));
        accountAbonement.getAbonement().getService().setCost(BigDecimal.ZERO);
        Assert.assertEquals(BigDecimal.ZERO, calculator.calc(accountAbonement));
    }
}