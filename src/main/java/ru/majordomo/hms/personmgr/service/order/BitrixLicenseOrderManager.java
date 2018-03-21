package ru.majordomo.hms.personmgr.service.order;

import org.springframework.beans.factory.annotation.Autowired;
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
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.AccountNotificationHelper;
import ru.majordomo.hms.personmgr.service.ChargeMessage;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

@Slf4j
@Service
public class BitrixLicenseOrderManager extends BitrixCommonOrderManager {

    @Autowired
    public BitrixLicenseOrderManager(
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper,
            FinFeignClient finFeignClient,
            PersonalAccountManager personalAccountManager,
            PaymentServiceRepository paymentServiceRepository,
            AccountNotificationHelper accountNotificationHelper
    ) {
        super(
                publisher,
                accountHelper,
                personalAccountManager,
                paymentServiceRepository,
                finFeignClient,
                accountNotificationHelper
        );
    }

    @Override
    protected void onCreate(BitrixLicenseOrder order) {
        PaymentService paymentService = paymentServiceRepository.findOne(order.getServiceId());
        BigDecimal cost = paymentService.getCost();

        PersonalAccount account = personalAccountManager.findOne(order.getPersonalAccountId());

        BigDecimal balance = accountHelper.getBalance(account);

        if (cost.compareTo(balance) > 0) {
            updateState(order, OrderState.DECLINED, "service");
            throw new ParameterValidationException("Баланс недостаточен для заказа лицензии");
        }

        //Списываем деньги
        ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService)
                .setAmount(cost)
                .build();

        SimpleServiceMessage response = accountHelper.block(account, chargeMessage);

        order.setDocumentNumber((String) response.getParam("documentNumber"));

        //Уведомление
        notifyStaff(order, account, paymentService.getName());

        notifyClient(account, paymentService.getName());
    }

    @Override
    protected void onDecline(BitrixLicenseOrder order) {
        //Возвращаем блокированные средства
        try {
            finFeignClient.unblock(order.getPersonalAccountId(), order.getDocumentNumber());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in BitrixLicenseOrderManager.onDecline #1 " + e.getMessage());
        }
    }

    @Override
    protected void onFinish(BitrixLicenseOrder order) {
        //Переводим блокировку в реальное списание
        try {
            finFeignClient.chargeBlocked(order.getPersonalAccountId(), order.getDocumentNumber());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception in BitrixLicenseOrderManager.onFinish #1 " + e.getMessage());
        }
    }

    private void notifyStaff(BitrixLicenseOrder order, PersonalAccount account, String licenseName) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", bitrixOrderEmail);
        message.addParam("api_name", "MajordomoServiceMessage");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());

        parameters.put("body", "1. Аккаунт: " + account.getName() + "<br>" +
                "2. Заказ лицензии Битрикс для: " + order.getDomainName() + "<br>" +
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
