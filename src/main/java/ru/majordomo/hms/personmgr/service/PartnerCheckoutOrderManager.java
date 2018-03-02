package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.AccountPartnerCheckoutOrder;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;

import java.math.BigDecimal;
import java.util.HashMap;

import static ru.majordomo.hms.personmgr.common.Constants.PARTNER_CHECKOUT_MIN_SUMM;
import static ru.majordomo.hms.personmgr.common.Constants.PARTNER_CHECKOUT_SERVICE_ID;

@Slf4j
@Service
public class PartnerCheckoutOrderManager extends OrderManager<AccountPartnerCheckoutOrder> {

    @Value("${mail_manager.partner_checkout_order_email}")
    private String proEmail;

    private final ApplicationEventPublisher publisher;
    private final AccountHelper accountHelper;
    private final PersonalAccountManager personalAccountManager;
    private final PaymentServiceRepository paymentServiceRepository;
    private final FinFeignClient finFeignClient;

    public PartnerCheckoutOrderManager(
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper,
            PersonalAccountManager personalAccountManager,
            PaymentServiceRepository paymentServiceRepository,
            FinFeignClient finFeignClient
    ) {
        this.publisher = publisher;
        this.accountHelper = accountHelper;
        this.personalAccountManager = personalAccountManager;
        this.paymentServiceRepository = paymentServiceRepository;
        this.finFeignClient = finFeignClient;
    }

    @Override
    protected void onCreate(AccountPartnerCheckoutOrder accountOrder) {
        //Проверить сколько партнёрских средств на аккаунте
        BigDecimal amountToCheckout = accountOrder.getAmount();

        if (amountToCheckout.compareTo(BigDecimal.valueOf(PARTNER_CHECKOUT_MIN_SUMM)) < 0) {
            updateState(accountOrder, OrderState.DECLINED, "service");
            throw new ParameterValidationException("Минимальная сумма вывода - " + PARTNER_CHECKOUT_MIN_SUMM + " руб.");
        }

        PersonalAccount account = personalAccountManager.findOne(accountOrder.getPersonalAccountId());

        BigDecimal partnerBalance = accountHelper.getPartnerBalance(account.getId());

        if (amountToCheckout.compareTo(partnerBalance) > 0) {
            updateState(accountOrder, OrderState.DECLINED, "service");
            throw new ParameterValidationException("Партнерский баланс недостаточен для вывода суммы");
        }

        PaymentService paymentService = paymentServiceRepository.findOne(PARTNER_CHECKOUT_SERVICE_ID);

        //Списываем деньги
        ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService)
                .setAmount(amountToCheckout)
                .partnerOnlyPaymentType()
                .build();
        SimpleServiceMessage response = accountHelper.charge(account, chargeMessage);
        accountOrder.setDocumentNumber((String) response.getParam("documentNumber"));

        //Уведомление
        notifyPro(accountOrder, account);
    }

    @Override
    protected void onDecline(AccountPartnerCheckoutOrder accountOrder) {
        //Возвращаем партнёрские средства, которые мы списывали
        try {
            finFeignClient.unblock(accountOrder.getPersonalAccountId(), accountOrder.getDocumentNumber());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in PartnerCheckoutOrderManager.onDecline #1 " + e.getMessage());
        }
    }

    @Override
    protected void onFinish(AccountPartnerCheckoutOrder accountOrder) {
        //При завершении выставляется только статус FINISHED
    }

    private void notifyPro(AccountPartnerCheckoutOrder accountOrder, PersonalAccount account) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", proEmail);
        message.addParam("api_name", "MajordomoServiceMessage");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());

        parameters.put("body", "1. Аккаунт: " + account.getName() + "<br>" +
                "2. Сумма к выводу партнёрских средств: " + accountOrder.getAmount() + "<br>" +
                "3. Номер кошелька: " + accountOrder.getPurse() + "<br>"
        );
        parameters.put("subject", "Заказ на вывод партнёрских средств");

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }
}
