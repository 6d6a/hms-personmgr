package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.config.PaymentBonusConfig;
import ru.majordomo.hms.personmgr.dto.fin.PaymentRequest;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;

@Slf4j
@Service
public class FirstMobilePaymentProcessor {
    private final PersonalAccountManager accountManager;
    private final AccountHistoryManager history;
    private final FinFeignClient finFeignClient;
    private final PaymentBonusConfig paymentBonusConfig;

    @Autowired
    public FirstMobilePaymentProcessor(
            PersonalAccountManager accountManager,
            AccountHistoryManager history,
            FinFeignClient finFeignClient,
            PaymentBonusConfig paymentBonusConfig) {
        this.accountManager = accountManager;
        this.history = history;
        this.finFeignClient = finFeignClient;
        this.paymentBonusConfig = paymentBonusConfig;
    }

    public void process(PersonalAccount account, BigDecimal amount) {
        BigDecimal bonusPercent = paymentBonusConfig.getFirstMobilePaymentBonusPercent();

        if (bonusPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal bonusPaymentAmount = amount.multiply(
                    bonusPercent
                            .divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP)
            );

            if (bonusPaymentAmount.compareTo(BigDecimal.ZERO) <= 0) { return; }

            try {
                accountManager.setBonusOnFirstMobilePaymentActionUsed(
                        account.getId(),
                        true
                );
                finFeignClient.addPayment(
                        new PaymentRequest(account.getName())
                                .withAmount(bonusPaymentAmount)
                                .withBonusType()
                                .withMessage("Бонус за пополнение баланса из мобильного приложения")
                                .withDisableAsync(true)
                );
                history.save(
                        account,
                        "Начислено " + bonusPaymentAmount + " бонусов (" + bonusPercent + " %) после первого пополнения баланса из мобильного приложения");
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getClass().getName() + ": " + e.getMessage());
            }
        }
    }
}
