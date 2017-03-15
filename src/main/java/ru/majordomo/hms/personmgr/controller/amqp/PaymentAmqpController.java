package ru.majordomo.hms.personmgr.controller.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.AccountStat;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
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
    private final AccountStatRepository accountStatRepository;

    @Autowired
    public PaymentAmqpController(
            PersonalAccountRepository accountRepository,
            AccountPromocodeRepository accountPromocodeRepository,
            FinFeignClient finFeignClient,
            AccountStatRepository accountStatRepository
    ) {
        this.accountRepository = accountRepository;
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.finFeignClient = finFeignClient;
        this.accountStatRepository = accountStatRepository;
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

                if (accountPromocodeRepository.countByPersonalAccountIdAndOwnedByAccount(account.getId(), false) > 1) {
                    throw new ParameterValidationException("Account has more than one AccountPromocodes with OwnedByAccount == false. Id: " + account.getId());
                }

                // Проверка на то что аккаунт создан по партнерскому промокоду
                AccountPromocode accountPromocode = accountPromocodeRepository.findOneByPersonalAccountIdAndOwnedByAccount(account.getId(), false);

                if (accountPromocode != null) {

                    // Аккаунт которому необходимо начислить средства
                    PersonalAccount accountForPartnerBonus = accountRepository.findByAccountId(accountPromocode.getOwnerPersonalAccountId());

                    if (accountForPartnerBonus == null) {
                        throw new ParameterValidationException("PersonalAccount with ID: " + accountPromocode.getOwnerPersonalAccountId() + " not found.");
                    }

                    // Проверка даты создания аккаунта
                    if (account.getCreated().isBefore(accountForPartnerBonus.getCreated().plusYears(1))) {
                        // Все условия выполнены

                        BigDecimal amount = new BigDecimal((Integer) message.getParam("amount"));
                        BigDecimal percent = new BigDecimal(BONUS_PARTNER_PERCENT);

                        BigDecimal promocodeBonus = amount.multiply(percent);

                        Map<String, Object> payment = new HashMap<>();
                        payment.put("accountId", accountForPartnerBonus.getName());
                        payment.put("paymentTypeId", BONUS_PARTNER_TYPE_ID);
                        payment.put("amount", promocodeBonus);
                        payment.put("message", "Бонусный платеж за использование промокода " + accountPromocode.getPromocode().getCode() + " на аккаунте: " + accountForPartnerBonus.getName());

                        try {
                            String responseMessage = finFeignClient.addPayment(payment);
                            logger.debug("Processed promocode addPayment: " + responseMessage);

                            //Статистика
                            AccountStat accountStat = new AccountStat();
                            accountStat.setPersonalAccountId(accountForPartnerBonus.getId());
                            accountStat.setCreated(LocalDateTime.now());
                            accountStat.setType(AccountStatType.VIRTUAL_HOSTING_PARTNER_PROMOCODE_BALANCE_FILL);

                            Map<String, String> data = new HashMap<>();
                            data.put("usedByPersonalAccountId", account.getId());
                            data.put("amount", String.valueOf(promocodeBonus));

                            accountStat.setData(data);

                            accountStatRepository.save(accountStat);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }


            }
        }

    }
}