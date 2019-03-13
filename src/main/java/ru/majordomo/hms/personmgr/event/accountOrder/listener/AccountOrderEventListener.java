package ru.majordomo.hms.personmgr.event.accountOrder.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.event.accountOrder.SSLCertificateOrderProcessEvent;
import ru.majordomo.hms.personmgr.service.order.ssl.SSLOrderManager;

@Component
@Slf4j
public class AccountOrderEventListener {
    private final SSLOrderManager orderManager;

    @Autowired
    public AccountOrderEventListener(SSLOrderManager orderManager) {
        this.orderManager = orderManager;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(SSLCertificateOrderProcessEvent event) {
        log.debug("on event {}", event.getClass());

        orderManager.process(
                orderManager.getPendingOrders()
        );
    }

}
