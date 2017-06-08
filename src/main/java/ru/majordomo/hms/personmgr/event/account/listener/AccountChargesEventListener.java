package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import ru.majordomo.hms.personmgr.event.account.AccountProcessChargesEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.service.PaymentChargesProcessorService;

@Component
public class AccountChargesEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountChargesEventListener.class);

    private final PaymentChargesProcessorService paymentChargesProcessorService;

    @Autowired
    public AccountChargesEventListener(
            PaymentChargesProcessorService paymentChargesProcessorService
    ) {
        this.paymentChargesProcessorService = paymentChargesProcessorService;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountProcessCharges(AccountProcessChargesEvent event) {
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountProcessChargesEvent");

        try {
            paymentChargesProcessorService.processCharge(account);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.event.account.listener.AccountChargesEventListener.onAccountProcessCharges " + e.getMessage());
        }
    }
}
