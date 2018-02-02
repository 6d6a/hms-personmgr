package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.AccountPartnerCheckoutOrder;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountPartnerCheckoutOrderRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.PARTNER_PAYOUT_SERVICE_ID;

@Service
public class PartnerCheckoutOrder extends Order<AccountPartnerCheckoutOrder> {

    @Value("${mail_manager.pro_email}")
    private String proEmail;

    private ApplicationEventPublisher publisher;
    private AccountPartnerCheckoutOrderRepository orderRepository;
    private AccountHelper accountHelper;
    private PersonalAccountManager personalAccountManager;
    private PaymentServiceRepository paymentServiceRepository;
    private FinFeignClient finFeignClient;

    private final static Logger logger = LoggerFactory.getLogger(PartnerCheckoutOrder.class);

    @Autowired
    PartnerCheckoutOrder(
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

    @Override
    protected void save() {
        orderRepository.save(this.accountOrder);
    }

    @Override
    protected void onCreate() {
        //Проверить сколько партнёрских средств на аккаунте
        BigDecimal amountToPayout = this.accountOrder.getAmount();

        if (amountToPayout.compareTo(BigDecimal.valueOf(1500L)) < 0) {
            super.updateState(OrderState.DECLINED, "service");
            throw new ParameterValidationException("Минимальная сумма вывода - 1500 руб.");
        }

        PersonalAccount account = personalAccountManager.findOne(this.accountOrder.getPersonalAccountId());

        BigDecimal partnerBalance = accountHelper.getPartnerBalance(account.getId());

        if (amountToPayout.compareTo(partnerBalance) > 0) {
            super.updateState(OrderState.DECLINED, "service");
            throw new ParameterValidationException("Партнерский баланс недостаточен для вывода суммы");
        }

        PaymentService paymentService = paymentServiceRepository.findOne(PARTNER_PAYOUT_SERVICE_ID);

        //Списываем деньги
        Map<String, Object> paymentOperationMessage = new ChargeMessage.ChargeBuilder(paymentService)
                .setAmount(amountToPayout)
                .partnerOnlyPaymentType()
                .build()
                .getFullMessage();
        SimpleServiceMessage response = accountHelper.charge(account, paymentOperationMessage);
        this.accountOrder.setDocumentNumber((String) response.getParam("documentNumber"));

        //Уведомление
        notifyPro(account);
    }

    @Override
    protected void onDecline() {
        //Возвращаем партнёрские средства, которые мы списывали
        try {
            finFeignClient.unblock(this.accountOrder.getPersonalAccountId(), this.accountOrder.getDocumentNumber());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception in ru.majordomo.hms.personmgr.service.PartnerCheckoutOrder #1 " + e.getMessage());
        }
    }

    @Override
    protected void onFinish() {
        //При завершении выставляется только статус FINISHED
    }

    private void notifyPro(PersonalAccount account) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", proEmail);
        message.addParam("api_name", "MajordomoServiceMessage");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());

        parameters.put("body", "1. Аккаунт: " + account.getName() + "<br>" +
                "2. Сумма к выводу партнёрских средств: " + this.accountOrder.getAmount() + "<br>");
        parameters.put("subject", "Заказ на вывод партнёрских средств");

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }
}
