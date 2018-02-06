package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.order.AccountPartnerCheckoutOrder;
import ru.majordomo.hms.personmgr.repository.AccountPartnerCheckoutOrderRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;

@Service
public class PartnerCheckoutOrderMangerFactory {
    private ApplicationEventPublisher publisher;
    private AccountPartnerCheckoutOrderRepository orderRepository;
    private AccountHelper accountHelper;
    private PersonalAccountManager personalAccountManager;
    private PaymentServiceRepository paymentServiceRepository;
    private FinFeignClient finFeignClient;

    @Autowired
    PartnerCheckoutOrderMangerFactory(
            ApplicationEventPublisher publisher,
            AccountPartnerCheckoutOrderRepository orderRepository,
            AccountHelper accountHelper,
            PersonalAccountManager personalAccountManager,
            PaymentServiceRepository paymentServiceRepository,
            FinFeignClient finFeignClient
    ) {
        this.publisher = publisher;
        this.orderRepository = orderRepository;
        this.accountHelper = accountHelper;
        this.personalAccountManager = personalAccountManager;
        this.paymentServiceRepository = paymentServiceRepository;
        this.finFeignClient = finFeignClient;
    }

    public PartnerCheckoutOrderManager createManager(
            AccountPartnerCheckoutOrder accountPartnerCheckoutOrder
    ) {
        PartnerCheckoutOrderManager manager = new PartnerCheckoutOrderManager(accountPartnerCheckoutOrder);
        this.setAllRequirements(manager);

        return manager;
    }

    private void setAllRequirements(PartnerCheckoutOrderManager manager) {
        manager.init(
                publisher,
                orderRepository,
                accountHelper,
                personalAccountManager,
                paymentServiceRepository,
                finFeignClient
        );
    }
}
