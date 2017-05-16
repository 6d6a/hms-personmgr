package ru.majordomo.hms.personmgr.controller.amqp;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountProcessChargesEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.model.AccountStat;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.repository.*;
import ru.majordomo.hms.personmgr.service.AbonementService;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.FinFeignClient;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.AccountSetting.CREDIT_ACTIVATION_DATE;
import static ru.majordomo.hms.personmgr.common.Constants.*;

@EnableRabbit
@Service
public class PaymentAmqpController extends CommonAmqpController  {
    private final AccountPromocodeRepository accountPromocodeRepository;
    private final FinFeignClient finFeignClient;
    private final AccountStatRepository accountStatRepository;
    private final PlanRepository planRepository;
    private final AccountPromotionRepository accountPromotionRepository;
    private final PromotionRepository promotionRepository;
    private final AccountHelper accountHelper;
    private final AbonementService abonementService;
    private final AbonementRepository abonementRepository;

    @Autowired
    public PaymentAmqpController(
            AccountPromocodeRepository accountPromocodeRepository,
            FinFeignClient finFeignClient,
            AccountStatRepository accountStatRepository,
            PlanRepository planRepository,
            AccountPromotionRepository accountPromotionRepository,
            PromotionRepository promotionRepository,
            AccountHelper accountHelper,
            AbonementService abonementService,
            AbonementRepository abonementRepository
    ) {
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.finFeignClient = finFeignClient;
        this.accountStatRepository = accountStatRepository;
        this.planRepository = planRepository;
        this.accountPromotionRepository = accountPromotionRepository;
        this.promotionRepository = promotionRepository;
        this.accountHelper = accountHelper;
        this.abonementService = abonementService;
        this.abonementRepository = abonementRepository;
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

        PersonalAccount account = accountRepository.findOne(message.getAccountId());

        if (!message.getParam("paymentTypeKind").equals(CREDIT_PAYMENT_TYPE_KIND)) {

            if (account != null) {

                // Если баланс после пополнения положительный
                BigDecimal balance = accountHelper.getBalance(account);
                Plan plan = planRepository.findOne(account.getPlanId());
                if (!plan.isAbonementOnly()) {
                    if (balance.compareTo(BigDecimal.ZERO) > 0) {
                        // Обнуляем дату активации кредита
                        if (account.getCreditActivationDate() != null) {
                            account.removeSettingByName(CREDIT_ACTIVATION_DATE);
                            accountRepository.save(account);
                        }

                        // Включаем аккаунт, если был выключен
                        if (!account.isActive()) {
                            accountHelper.switchAccountResources(account, true);
                            // сразу списываем за текущий день после включения (если не хватает - аккаунт снова выключится)
                            publisher.publishEvent(new AccountProcessChargesEvent(account));
                        }
                    }
                } else {

                    List<String> abonementIds = plan.getAbonementIds();

                    String addAbonementId = null;

                    // Ищем соответствующий abonementId по периоду и плану
                    for (String abonementId : abonementIds) {
                        if ( (abonementRepository.findOne(abonementId).getPeriod()).equals("P1Y") ) {
                            addAbonementId = abonementId;
                            break;
                        }
                    }

                    try {
                        abonementService.addAbonement(account, addAbonementId, true, false, false);
                        accountHelper.switchAccountResources(account, true);
                    } catch (Exception e) {
                        logger.info("Ошибка при покупке абонемента для AbonementOnly плана.");
                        e.printStackTrace();
                    }

                }
            }

        }

        // Пополнение баланса партнера при поступлении средств клиенту, если этот клиент регистрировался по промокоду
        if (message.getParam("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {

            if (account != null) {

                BigDecimal amount = new BigDecimal((Integer) message.getParam("amount"));
                Plan plan = planRepository.findOne(account.getPlanId());
                // При открытии нового аккаунта виртуального хостинга по тарифным планам «Безлимитный», «Безлимитный+», «Бизнес», «Бизнес+»
                // мы бесплатно зарегистрируем на Вас 1 домен в зоне .ru или .рф при единовременной оплате за
                // 3 месяца. Бонус предоставляется при открытии аккаунта для первого домена на аккаунте.
                if (amount.compareTo((plan.getService().getCost()).multiply(new BigDecimal(3L))) >= 0) {
                    //Проверка на то что аккаунт новый (на нём не было доменов)
                    if (account.isAccountNew()) {
                        List<Domain> domains = accountHelper.getDomains(account);
                        if (!plan.isAbonementOnly() && plan.isActive() && (domains == null || domains.size() == 0)) {
                            Promotion promotion = promotionRepository.findByName(FREE_DOMAIN_PROMOTION);
                            List<AccountPromotion> accountPromotions = accountPromotionRepository.findByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());
                            if (accountPromotions == null || accountPromotions.isEmpty()) {
                                accountHelper.giveGift(account, promotion);
                            }
                        }
                    }
                }

                if (accountPromocodeRepository.countByPersonalAccountIdAndOwnedByAccount(account.getId(), false) > 1) {
                    logger.error("Account has more than one AccountPromocodes with OwnedByAccount == false. Id: " + account.getId());
                    return;
                }

                // Проверка на то что аккаунт создан по партнерскому промокоду
                AccountPromocode accountPromocode = accountPromocodeRepository.findOneByPersonalAccountIdAndOwnedByAccount(account.getId(), false);

                if (accountPromocode != null) {

                    // Аккаунт которому необходимо начислить средства
                    PersonalAccount accountForPartnerBonus = accountRepository.findByAccountId(accountPromocode.getOwnerPersonalAccountId());

                    if (accountForPartnerBonus == null) {
                        logger.error("PersonalAccount with ID: " + accountPromocode.getOwnerPersonalAccountId() + " not found.");
                        return;
                    }

                    // Проверка даты создания аккаунта
                    if (account.getCreated().isBefore(accountForPartnerBonus.getCreated().plusYears(1))) {
                        // Все условия выполнены

                        BigDecimal percent = new BigDecimal(BONUS_PARTNER_PERCENT);

                        BigDecimal promocodeBonus = amount.multiply(percent);

                        Map<String, Object> payment = new HashMap<>();
                        payment.put("accountId", accountForPartnerBonus.getName());
                        payment.put("paymentTypeId", BONUS_PARTNER_TYPE_ID);
                        payment.put("amount", promocodeBonus);
                        payment.put("message", "Бонусный платеж за использование промокода " + accountPromocode.getPromocode().getCode() + " на аккаунте: " + account.getName());

                        try {
                            String responseMessage = finFeignClient.addPayment(payment);
                            logger.debug("Processed promocode addPayment: " + responseMessage);

                            //Save history
                            Map<String, String> params = new HashMap<>();
                            params.put(HISTORY_MESSAGE_KEY, "Произведено начисление процента от пополнения (" + promocodeBonus.toString() + " руб. от " + amount.toString() + " руб.) владельцу партнерского промокода" + accountPromocode.getPromocode().getCode() + " - " + accountForPartnerBonus.getName());
                            params.put(OPERATOR_KEY, "service");

                            publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));

                            //Статистика
                            AccountStat accountStat = new AccountStat();
                            accountStat.setPersonalAccountId(accountForPartnerBonus.getId());
                            accountStat.setCreated(LocalDateTime.now());
                            accountStat.setType(AccountStatType.VIRTUAL_HOSTING_PARTNER_PROMOCODE_BALANCE_FILL);

                            Map<String, String> data = new HashMap<>();
                            data.put("usedByPersonalAccountId", account.getId());
                            data.put("usedByPersonalAccountName", account.getName());
                            data.put("amount", String.valueOf(promocodeBonus));

                            accountStat.setData(data);

                            accountStatRepository.save(accountStat);

                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.error("Exception in pm.payment.create AMQP listener: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }
}