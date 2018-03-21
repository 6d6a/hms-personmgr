package ru.majordomo.hms.personmgr.service.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;

@Slf4j
@Service
public class BitrixLicenseProlongManager extends BitrixCommonOrderManager {

    private final int DAYS_AFTER_EXPIRED = 30;
    private final String COST_PERCENT_BEFORE = "0.22";
    private final String COST_PERCENT_AFTER = "0.6";

    @Autowired
    public BitrixLicenseProlongManager(
            ApplicationEventPublisher publisher,
            AccountHelper accountHelper,
            PersonalAccountManager personalAccountManager,
            PaymentServiceRepository paymentServiceRepository,
            FinFeignClient finFeignClient,
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
        BitrixLicenseOrder previousOrder = accountOrderRepository.findOne(order.getPreviousOrderId());

        checkProlongLicenceOnCreate(previousOrder);

        order.setPreviousOrder(previousOrder);

        PaymentService paymentService = paymentServiceRepository.findOne(previousOrder.getServiceId());


        BigDecimal discountPercent;

        if(previousOrder.getUpdated().isBefore(LocalDateTime.now().plusDays(DAYS_AFTER_EXPIRED))){
            discountPercent = new BigDecimal(COST_PERCENT_BEFORE);
        } else {
            discountPercent = new BigDecimal(COST_PERCENT_AFTER);
        }

        BigDecimal cost = paymentService.getCost().multiply(discountPercent);

        PersonalAccount account = personalAccountManager.findOne(order.getPersonalAccountId());

        BigDecimal balance = accountHelper.getBalance(account);

        if (cost.compareTo(balance) > 0) {
            updateState(order, OrderState.DECLINED, "service");
            throw new ParameterValidationException("Баланс недостаточен для продления лицензии");
        }

        //Списываем деньги
        ChargeMessage chargeMessage = new ChargeMessage.Builder(paymentService)
                .setAmount(cost)
                .setComment("Скидка на продление, цена умножена на " + discountPercent)
                .build();

        SimpleServiceMessage response = accountHelper.block(account, chargeMessage);

        order.setDocumentNumber((String) response.getParam("documentNumber"));

        //Уведомление
        notifyStaff(order, account, paymentService.getName(), cost);

        notifyClient(account, paymentService.getName());
    }

    private void notifyStaff(BitrixLicenseOrder accountOrder, PersonalAccount account, String licenseName, BigDecimal cost) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", bitrixOrderEmail);
        message.addParam("api_name", "MajordomoServiceMessage");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());

        parameters.put("body",
                "1. Аккаунт: " + account.getName() + "<br/>" +
                "2. Продление лицензии Битрикс для: " + accountOrder.getDomainName() + "<br/>" +
                "3. Тип лицензии: " + licenseName + "<br/>" +
                "4. Списано " + cost + " рублей<br/>");
        parameters.put("subject", "Продление лицензии 1С-Битрикс Управление сайтом");

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    private void notifyClient(PersonalAccount account, String licenseName) {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("license_name", licenseName);
        parameters.put("from", "noreply@majordomo.ru");

        accountNotificationHelper.sendMail(account, "HmsVHMajordomoZakazprodleniya1CBitrix", 1, parameters);
    }

    private void checkProlongLicenceOnCreate(BitrixLicenseOrder previousOrder){
        if (!previousOrder.getState().equals(OrderState.FINISHED)) {
            throw new ParameterValidationException("Продлить можно только успешно заказанную лицензию");
        }

        if (!previousOrder.getUpdated().plusMonths(11).isBefore(LocalDateTime.now())) {
            throw new ParameterValidationException("Заказ продления лицензии доступно через 11 месяцев после заказа");
        }

        if (isProlonged(previousOrder)) {
            throw new ParameterValidationException("Данная лицензия уже продлена");
        }
    }
}
