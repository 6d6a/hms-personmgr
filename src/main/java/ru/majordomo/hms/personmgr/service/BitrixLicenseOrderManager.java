package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;

@Slf4j
@Service
public class BitrixLicenseOrderManager extends OrderManager<BitrixLicenseOrder> {

    @Value("${mail_manager.bitrix_order_email}")
    private String bitrixOrderEmail;

    private final ApplicationEventPublisher publisher;
    private final AccountHelper accountHelper;
    private final PersonalAccountManager personalAccountManager;
    private final PaymentServiceRepository paymentServiceRepository;
    private final FinFeignClient finFeignClient;
    private final AccountNotificationHelper accountNotificationHelper;

    public BitrixLicenseOrderManager(
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper,
            PersonalAccountManager personalAccountManager,
            PaymentServiceRepository paymentServiceRepository,
            FinFeignClient finFeignClient,
            AccountNotificationHelper accountNotificationHelper
    ) {
        this.publisher = publisher;
        this.accountHelper = accountHelper;
        this.personalAccountManager = personalAccountManager;
        this.paymentServiceRepository = paymentServiceRepository;
        this.finFeignClient = finFeignClient;
        this.accountNotificationHelper = accountNotificationHelper;
    }

    @Override
    protected void onCreate(BitrixLicenseOrder accountOrder) {
        PaymentService paymentService = paymentServiceRepository.findOne(accountOrder.getServiceId());
        BigDecimal cost = paymentService.getCost();

        PersonalAccount account = personalAccountManager.findOne(accountOrder.getPersonalAccountId());

        BigDecimal balance = accountHelper.getBalance(account);

        if (cost.compareTo(balance) > 0) {
            updateState(accountOrder, OrderState.DECLINED, "service");
            throw new ParameterValidationException("Баланс недостаточен для заказа лицензии");
        }

        //Списываем деньги
        ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService)
                .setAmount(cost)
                .build();

        SimpleServiceMessage response = accountHelper.block(account, chargeMessage);

        accountOrder.setDocumentNumber((String) response.getParam("documentNumber"));

        //Уведомление
        notifyStaff(accountOrder, account, paymentService.getName());

        notifyClient(account, paymentService.getName());
    }

    @Override
    protected void onDecline(BitrixLicenseOrder accountOrder) {
        //Возвращаем блокированные средства
        try {
            finFeignClient.unblock(accountOrder.getPersonalAccountId(), accountOrder.getDocumentNumber());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in BitrixLicenseOrderManager.onDecline #1 " + e.getMessage());
        }
    }

    @Override
    protected void onFinish(BitrixLicenseOrder accountOrder) {
        //Переводим блокировку в реальное списание
        try {
            finFeignClient.chargeBlocked(accountOrder.getPersonalAccountId(), accountOrder.getDocumentNumber());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in BitrixLicenseOrderManager.onFinish #1 " + e.getMessage());
        }
    }

    private void notifyStaff(BitrixLicenseOrder accountOrder, PersonalAccount account, String licenseName) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", bitrixOrderEmail);
        message.addParam("api_name", "MajordomoServiceMessage");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());

        parameters.put("body", "1. Аккаунт: " + account.getName() + "<br>" +
                "2. Заказ лицензии Битрикс для: " + accountOrder.getDomainName() + "<br>" +
                "3. Тип лицензии: " + licenseName + "<br>");
        parameters.put("subject", "Заказ лицензии 1С-Битрикс Управление сайтом");

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    private void notifyClient(PersonalAccount account, String licenseName) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("license_name", licenseName);
        parameters.put("from", "noreply@majordomo.ru");

        accountNotificationHelper.sendMail(account, "HmsVHMajordomoZakaz1CBitrix", 1, parameters);
    }
}
