package ru.majordomo.hms.personmgr.controller.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.Constants.BONUS_PARTNER_PERCENT;
import static ru.majordomo.hms.personmgr.common.Constants.BONUS_PARTNER_TYPE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.REAL_PAYMENT_TYPE_KIND;

@EnableRabbit
@Service
public class PaymentAmqpController extends CommonAmqpController  {

    private final static Logger logger = LoggerFactory.getLogger(PaymentAmqpController.class);

    private final PersonalAccountRepository accountRepository;
    private final AccountPromocodeRepository accountPromocodeRepository;
    private final FinFeignClient finFeignClient;

    @Autowired
    public PaymentAmqpController(
            PersonalAccountRepository accountRepository,
            AccountPromocodeRepository accountPromocodeRepository,
            FinFeignClient finFeignClient
    ) {
        this.accountRepository = accountRepository;
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.finFeignClient = finFeignClient;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.payment.create",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "payment.create",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received payment create message from " + provider + ": " + message.toString());

        // Пополнение баланса партнера при поступлении средств клиенту, если этот клиент регистрировался по промокоду
        if (message.getParam("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {

            PersonalAccount account = accountRepository.findByAccountId(message.getAccountId());
            if (account != null) {

                // Проверка на то что аккаунт создан по партнерскому промокоду
                List<AccountPromocode> accountPromocodes = accountPromocodeRepository.findByPersonalAccountIdAndOwnedByAccount(account.getId(), false);

                if (accountPromocodes.size() != 0) {

                    if (accountPromocodes.size() > 2) {
                        throw new ParameterValidationException("Account has more than one AccountPromocodes with OwnedByAccount == false. AccountId: " + account.getAccountId());
                    }

                    // Аккаунт которому необходимо начислить средства
                    PersonalAccount accountForPartnerBonus = accountRepository.findByAccountId(accountPromocodes.get(0).getOwnerPersonalAccountId());

                    // Проверка даты создания аккаунта
                    LocalDateTime createdDate = account.getCreated();
                    LocalDateTime createdDateForPartnerBonus = accountForPartnerBonus.getCreated().plusYears(1);
                    if (createdDate.isBefore(createdDateForPartnerBonus)) {
                        // Все условия выполнены

                        BigDecimal amount = new BigDecimal((Integer) message.getParam("amount"));
                        BigDecimal percent = new BigDecimal(BONUS_PARTNER_PERCENT);

                        BigDecimal promocodeBonus = amount.multiply(percent);

                        Map<String, Object> payment = new HashMap<>();
                        payment.put("accountId", accountForPartnerBonus.getName());
                        payment.put("paymentTypeId", BONUS_PARTNER_TYPE_ID);
                        payment.put("amount", promocodeBonus);
                        //payment.put("documentNumber", "test0000000000000012");
                        payment.put("message", "Бонусный платеж за использование промокода " + accountPromocodes.get(0).getPromocode().getCode() + " на аккаунте: " + accountForPartnerBonus.getName());

                        try {
                            finFeignClient.addPayment(payment);
                            //logger.debug("Processed promocode addPayment: " + payment.toString());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }


            }
        }

    }
}
