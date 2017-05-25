package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.AccountStat;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.*;
import ru.majordomo.hms.personmgr.service.AbonementService;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.FinFeignClient;
import ru.majordomo.hms.personmgr.service.TokenHelper;
import ru.majordomo.hms.rc.user.resources.Domain;

import static ru.majordomo.hms.personmgr.common.AccountSetting.CREDIT_ACTIVATION_DATE;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Constants.BONUS_PARTNER_PERCENT;
import static ru.majordomo.hms.personmgr.common.Constants.BONUS_PARTNER_TYPE_ID;

@Component
public class AccountEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountEventListener.class);

    private final AccountHelper accountHelper;
    private final TokenHelper tokenHelper;
    private final ApplicationEventPublisher publisher;
    private final AccountPromocodeRepository accountPromocodeRepository;
    private final FinFeignClient finFeignClient;
    private final AccountStatRepository accountStatRepository;
    private final PlanRepository planRepository;
    private final AccountPromotionRepository accountPromotionRepository;
    private final PromotionRepository promotionRepository;
    private final AbonementService abonementService;
    private final PersonalAccountManager accountManager;
    private final AccountAbonementRepository accountAbonementRepository;

    @Autowired
    public AccountEventListener(
            AccountHelper accountHelper,
            TokenHelper tokenHelper,
            ApplicationEventPublisher publisher,
            AccountPromocodeRepository accountPromocodeRepository,
            FinFeignClient finFeignClient,
            AccountStatRepository accountStatRepository,
            PlanRepository planRepository,
            AccountPromotionRepository accountPromotionRepository,
            PromotionRepository promotionRepository,
            AbonementService abonementService,
            PersonalAccountManager accountManager,
            AccountAbonementRepository accountAbonementRepository
    ) {
        this.accountHelper = accountHelper;
        this.tokenHelper = tokenHelper;
        this.publisher = publisher;
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.finFeignClient = finFeignClient;
        this.accountStatRepository = accountStatRepository;
        this.planRepository = planRepository;
        this.accountPromotionRepository = accountPromotionRepository;
        this.promotionRepository = promotionRepository;
        this.abonementService = abonementService;
        this.accountManager = accountManager;
        this.accountAbonementRepository = accountAbonementRepository;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountCreated(AccountCreatedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountCreatedEvent");

        String emails = accountHelper.getEmail(account);

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setParams(new HashMap<>());
        message.addParam("email", emails);
        message.addParam("api_name", "MajordomoHMSClientCreatedConfirmation");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put(PASSWORD_KEY, (String) params.get(PASSWORD_KEY));
        parameters.put("ftp_ip", "FTP_IP");
        parameters.put("ftp_login", "FTP_LOGIN");
        parameters.put("ftp_password", "FTP_PASSWORD");

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountQuotaAdded(AccountQuotaAddedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountQuotaAddedEvent");

        String email = accountHelper.getEmail(account);
        String planName = (String) params.get(SERVICE_NAME_KEY);

        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", email);
        message.addParam("api_name", "MajordomoVHQuotaAdd");
        message.addParam("priority", 10);


        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("tariff", planName);

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountQuotaDiscard(AccountQuotaDiscardEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountQuotaDiscardEvent");

        String email = accountHelper.getEmail(account);
        String planName = (String) params.get(SERVICE_NAME_KEY);

        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", email);
        message.addParam("api_name", "MajordomoVHQuotaDiscard");
        message.addParam("priority", 10);


        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("tariff", planName);
        List<Domain> domains = accountHelper.getDomains(account);
        List<String> domainNames = new ArrayList<>();
        for (Domain domain: domains) {
            domainNames.add(domain.getName());
        }
        parameters.put("domains", String.join("<br>", domainNames));

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountNotifySupportOnChangePlan(AccountNotifySupportOnChangePlanEvent event) {
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountNotifySupportOnChangePlanEvent");

        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", TECHNICAL_SUPPORT_EMAIL);
        message.addParam("api_name", "MajordomoCorporatePlanChange");
        message.addParam("priority", 1);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", message.getAccountId());

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountNotifyRemainingDays(AccountNotifyRemainingDaysEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountNotifyRemainingDaysEvent");

        String email = accountHelper.getEmail(account);
        Integer remainingDays = (Integer) params.get("remainingDays");

        SimpleServiceMessage message = new SimpleServiceMessage();

        message.setAccountId(account.getId());
        message.setParams(new HashMap<>());
        message.addParam("email", email);
        message.addParam("api_name", "MajordomoVHMoneyLowLevel");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("acc_id", account.getName());
        parameters.put("days", Utils.pluralizef("остался %d день", "осталось %d дня", "осталось %d дней", remainingDays));
        List<Domain> domains = accountHelper.getDomains(account);
        List<String> domainNames = new ArrayList<>();
        for (Domain domain : domains) {
            domainNames.add(domain.getName());
        }
        parameters.put("domains", "<br>" + String.join("<br>", domainNames));

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountPasswordRecover(AccountPasswordRecoverEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountPasswordRecoverEvent");

        String emails = accountHelper.getEmail(account);

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setParams(new HashMap<>());
        message.addParam("email", emails);
        message.addParam("api_name", "MajordomoHMSPasswordChangeRequest");
        message.addParam("priority", 10);

        String token = tokenHelper.generateToken(account, TokenType.PASSWORD_RECOVERY_REQUEST);

        String ip = (String) params.get(IP_KEY);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getAccountId());
        parameters.put("account", account.getName());
        parameters.put("ip", ip);
        parameters.put("token", token);

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));

        //Запишем в историю клиента
        Map<String, String> historyParams = new HashMap<>();
        historyParams.put(HISTORY_MESSAGE_KEY, "Получена заявка на смену пароля к панели управления с IP: " + ip);
        historyParams.put(OPERATOR_KEY, "service");

        publisher.publishEvent(new AccountHistoryEvent(account.getId(), historyParams));
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountPasswordRecoverConfirmed(AccountPasswordRecoverConfirmedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountPasswordRecoverConfirmedEvent");

        String emails = accountHelper.getEmail(account);

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setParams(new HashMap<>());
        message.addParam("email", emails);
        message.addParam("api_name", "MajordomoVHResetPassConfirm");
        message.addParam("priority", 10);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("pass", (String) params.get(PASSWORD_KEY));

        message.addParam("parametrs", parameters);

        publisher.publishEvent(new SendMailEvent(message));

        String ip = (String) params.get(IP_KEY);

        //Запишем в историю клиента
        Map<String, String> historyParams = new HashMap<>();
        historyParams.put(HISTORY_MESSAGE_KEY, "Произведена смена пароля к панели управления с IP: " + ip);
        historyParams.put(OPERATOR_KEY, "service");

        publisher.publishEvent(new AccountHistoryEvent(account.getId(), historyParams));
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountPasswordChangedEvent(AccountPasswordChangedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountPasswordChangedEvent");

        String ip = (String) params.get(IP_KEY);

        //Запишем в историю клиента
        Map<String, String> historyParams = new HashMap<>();
        historyParams.put(HISTORY_MESSAGE_KEY, "Произведена смена пароля к панели управления с IP: " + ip);
        historyParams.put(OPERATOR_KEY, "service");

        publisher.publishEvent(new AccountHistoryEvent(account.getId(), historyParams));

        if (account.hasNotification(MailManagerMessageType.EMAIL_CHANGE_ACCOUNT_PASSWORD)) {
            String emails = accountHelper.getEmail(account);

            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setParams(new HashMap<>());
            message.addParam("email", emails);
            message.addParam("api_name", "MajordomoVHPassChAccount");
            message.addParam("priority", 10);

            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("client_id", account.getAccountId());
            parameters.put("acc_id", account.getAccountId());
            parameters.put("ip", ip);
            parameters.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy H:m:s")));

            message.addParam("parametrs", parameters);

            publisher.publishEvent(new SendMailEvent(message));
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountPromotionProcessByPaymentCreatedEvent(AccountPromotionProcessByPaymentCreatedEvent event) {

        // Задержка
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            logger.debug("Exeption in AccountEventListener on sleep");
            e.printStackTrace();
        }

        PersonalAccount account = event.getSource();
        // При задержке аккаунт мог мутировать
        account = accountManager.findOne(account.getId());

        Map<String, ?> paramsForPublisher = event.getParams();

        logger.debug("We got AccountPromotionProcessByPaymentCreatedEvent");

        // Пополнение баланса партнера при поступлении средств клиенту, если этот клиент регистрировался по промокоду

        BigDecimal amount = new BigDecimal((Integer) paramsForPublisher.get("amount"));
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
            PersonalAccount accountForPartnerBonus = accountManager.findOne(accountPromocode.getOwnerPersonalAccountId());

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

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountSwitchByPaymentCreatedEvent(AccountSwitchByPaymentCreatedEvent event) {

        // Задержка (К примеру, в случае возврата денег в процессе смены тарифа)
        try {
            Thread.sleep(20000);
        } catch (Exception e) {
            logger.debug("Exception in AccountEventListener on sleep");
            e.printStackTrace();
        }

        PersonalAccount account = event.getSource();
        // При задержке аккаунт мог мутировать
        account = accountManager.findOne(account.getId());

        logger.debug("We got AccountSwitchByPaymentCreatedEvent");

        // Если баланс после пополнения положительный
        BigDecimal balance = accountHelper.getBalance(account);
        Plan plan = planRepository.findOne(account.getPlanId());

        if (!plan.isAbonementOnly()) {

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                // Обнуляем дату активации кредита
                if (account.getCreditActivationDate() != null) {
                    accountManager.removeSettingByName(account.getId(), CREDIT_ACTIVATION_DATE);
                }

                // Включаем аккаунт, если был выключен
                if (!account.isActive()) {
                    accountHelper.switchAccountResources(account, true);
                    // сразу списываем за текущий день после включения (если не хватает - аккаунт снова выключится)
                    publisher.publishEvent(new AccountProcessChargesEvent(account));
                }
            }

        } else {

            String addAbonementId = plan.getNotInternalAbonementId();

            if (addAbonementId != null) {
                AccountAbonement accountAbonement = accountAbonementRepository.findByPersonalAccountId(account.getId());
                if (accountAbonement == null) {
                    try {
                        abonementService.addAbonement(account, addAbonementId, true);
                        accountHelper.switchAccountResources(account, true);
                    } catch (Exception e) {
                        logger.info("Ошибка при покупке абонемента для AbonementOnly плана.");
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
