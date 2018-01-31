package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.annotation.Transient;
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

    @Autowired
    PartnerCheckoutOrder(
            ApplicationEventPublisher publisher,
            AccountPartnerCheckoutOrderRepository orderRepository,
            AccountHelper accountHelper,
            PersonalAccountManager personalAccountManager,
            PaymentServiceRepository paymentServiceRepository
    ) {
        this.publisher = publisher;
        this.orderRepository = orderRepository;
        this.accountHelper = accountHelper;
        this.personalAccountManager = personalAccountManager;
        this.paymentServiceRepository = paymentServiceRepository;
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

        PersonalAccount account = personalAccountManager.findByAccountId(this.accountOrder.getPersonalAccountId());

        BigDecimal partnerBalance = accountHelper.getPartnerBalance(account.getId());

        if (amountToPayout.compareTo(partnerBalance) > 0) {
            super.updateState(OrderState.DECLINED, "service");
            throw new ParameterValidationException("Партнерский баланс недостаточен для вывода суммы");
        }

        PaymentService paymentService = paymentServiceRepository.findOne(PARTNER_PAYOUT_SERVICE_ID);

        //Списываем деньги
        PartnerCharge charge = new PartnerCharge(paymentService);
        SimpleServiceMessage response = accountHelper.charge(account, charge.getPaymentOperationMessage());
        this.accountOrder.setDocumentNumber((String) response.getParam("documentNumber"));

        //Уведомление
        notifyPro(account);
    }

    @Override
    protected void onDecline() {
        //Получаем сумму которую, мы списывали
        BigDecimal amountToPayout = this.accountOrder.getAmount();

        //TODO Вернуть
    }

    @Override
    protected void onFinish() {
        //При завершении выставляется только статус FINISHED
    }





    public void notifyPro(PersonalAccount account) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", proEmail);
        message.addParam("api_name", "MajordomoServiceMessage");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());

        //TODO
        parameters.put("body", "1. Аккаунт: " + account.getName() + "<br>");
//                "2. Сумма к выводу партнёрских средств: " + clientEmails + "<br>" +
//                "3. Имя сайта: " + domainName + "<br><br>" +
//                "Услуга " + serviceName + " оплачена из ПУ.");
//        parameters.put("subject", "Услуга " + serviceName + " оплачена");

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }
}
