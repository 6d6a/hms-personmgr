package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.partners.ActionStatRequest;
import ru.majordomo.hms.personmgr.event.account.AccountCreatedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountNotifyFinOnChangeAbonementEvent;
import ru.majordomo.hms.personmgr.event.account.AccountNotifySupportOnChangePlanEvent;
import ru.majordomo.hms.personmgr.event.account.AccountOwnerChangeEmailEvent;
import ru.majordomo.hms.personmgr.event.account.AccountPasswordChangedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountPasswordRecoverConfirmedEvent;
import ru.majordomo.hms.personmgr.event.account.AccountPasswordRecoverEvent;
import ru.majordomo.hms.personmgr.event.account.AccountWasEnabled;
import ru.majordomo.hms.personmgr.event.account.PaymentWasReceivedEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.feign.PartnersFeignClient;
import ru.majordomo.hms.personmgr.feign.YaPromoterFeignClient;
import ru.majordomo.hms.personmgr.manager.*;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.DefaultAccountNotice;
import ru.majordomo.hms.personmgr.model.account.InfoBannerAccountNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.model.task.SendMailIfAbonementWasNotBought;
import ru.majordomo.hms.personmgr.model.task.State;
import ru.majordomo.hms.personmgr.model.token.Token;
import ru.majordomo.hms.personmgr.repository.AccountNoticeRepository;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.service.promocodeAction.PaymentPercentBonusActionProcessor;
import ru.majordomo.hms.rc.user.resources.Domain;

import static ru.majordomo.hms.personmgr.common.AccountSetting.CREDIT_ACTIVATION_DATE;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_DISCOUNT_RU_RF_REGISTRATION_FREE_COUNT;
import static ru.majordomo.hms.personmgr.common.Utils.getBigDecimalFromUnexpectedInput;

@Component
public class AccountEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountEventListener.class);

    private final AccountHelper accountHelper;
    private final TokenManager tokenManager;
    private final ApplicationEventPublisher publisher;
    private final PlanManager planManager;
    private final AccountPromotionManager accountPromotionManager;
    private final PromotionRepository promotionRepository;
    private final AbonementService abonementService;
    private final PersonalAccountManager accountManager;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final AccountNotificationHelper accountNotificationHelper;
    private final ChargeHelper chargeHelper;
    private final AccountHistoryManager history;
    private final BackupService backupService;
    private final PartnersFeignClient partnersFeignClient;
    private final TaskManager taskManager;
    private final AccountNoticeRepository accountNoticeRepository;
    private final PaymentPercentBonusActionProcessor paymentPercentBonusActionProcessor;
    private final YaPromoterFeignClient yaPromoterFeignClient;

    private final int deleteDataAfterDays;

    @Autowired
    public AccountEventListener(
            AccountHelper accountHelper,
            TokenManager tokenManager,
            ApplicationEventPublisher publisher,
            PlanManager planManager,
            AccountPromotionManager accountPromotionManager,
            PromotionRepository promotionRepository,
            AbonementService abonementService,
            PersonalAccountManager accountManager,
            AbonementManager<AccountAbonement> accountAbonementManager,
            AccountNotificationHelper accountNotificationHelper,
            ChargeHelper chargeHelper,
            AccountHistoryManager history,
            BackupService backupService,
            PartnersFeignClient partnersFeignClient,
            TaskManager taskManager,
            AccountNoticeRepository accountNoticeRepository,
            PaymentPercentBonusActionProcessor paymentPercentBonusActionProcessor,
            YaPromoterFeignClient yaPromoterFeignClient,
            @Value("${delete_data_after_days}") int deleteDataAfterDays
    ) {
        this.accountHelper = accountHelper;
        this.tokenManager = tokenManager;
        this.publisher = publisher;
        this.planManager = planManager;
        this.accountPromotionManager = accountPromotionManager;
        this.promotionRepository = promotionRepository;
        this.abonementService = abonementService;
        this.accountManager = accountManager;
        this.accountAbonementManager = accountAbonementManager;
        this.accountNotificationHelper = accountNotificationHelper;
        this.chargeHelper = chargeHelper;
        this.history = history;
        this.backupService = backupService;
        this.partnersFeignClient = partnersFeignClient;
        this.taskManager = taskManager;
        this.accountNoticeRepository = accountNoticeRepository;
        this.paymentPercentBonusActionProcessor = paymentPercentBonusActionProcessor;
        this.yaPromoterFeignClient = yaPromoterFeignClient;
        this.deleteDataAfterDays = deleteDataAfterDays;
    }

    @EventListener
    @Async("vipThreadPoolTaskExecutor")
    public void onAccountCreated(AccountCreatedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountCreatedEvent");

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put(PASSWORD_KEY, (String) params.get(PASSWORD_KEY));
        parameters.put("ftp_ip", "FTP_IP");
        parameters.put("ftp_login", "FTP_LOGIN");
        parameters.put("ftp_password", "FTP_PASSWORD");

        accountNotificationHelper.sendMail(account, "MajordomoHMSClientCreatedConfirmation", 10, parameters);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void createInfoBanner(AccountCreatedEvent event) {
        PersonalAccount account = event.getSource();

        InfoBannerAccountNotice notification = new InfoBannerAccountNotice();
        notification.setPersonalAccountId(account.getId());
        notification.setCreated(LocalDateTime.now());
        notification.setViewed(false);
        notification.setComponent("hello_user");

        accountNoticeRepository.save(notification);
        logger.debug("InfoBannerAccountNotice saved: " + notification.toString());
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void addPromotions(AccountCreatedEvent event) {
        PersonalAccount account = event.getSource();

        Promotion promotion = promotionRepository.findByName(DOMAIN_DISCOUNT_RU_RF);

        for (int i = 1; i <= promotion.getLimitPerAccount(); i++) {
            accountHelper.giveGift(account, promotion);
        }
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
    public void onAccountNotifyFinOnChangeAbonementEvent(AccountNotifyFinOnChangeAbonementEvent event) {
        PersonalAccount account = event.getSource();

        logger.debug("We got AccountNotifyFinOnChangeAbonementEvent");

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("type", accountHelper.getOwnerType(account.getId()).name());

        accountNotificationHelper.sendEmailToFinDepartment("HmsVHMajordomoCompanyChangeAbonement", account.getId(), parameters);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountPasswordRecover(AccountPasswordRecoverEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountPasswordRecoverEvent");

        String token = tokenManager.generateToken(account, TokenType.PASSWORD_RECOVERY_REQUEST);

        String ip = (String) params.get(IP_KEY);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getAccountId());
        parameters.put("account", account.getName());
        parameters.put("ip", ip);
        parameters.put("token", token);

        accountNotificationHelper.sendMail(account, "MajordomoHMSPasswordChangeRequest", 10, parameters);

        //Запишем в историю клиента
        history.saveForOperatorService(account, "Получена заявка на смену пароля к панели управления с IP: " + ip);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountPasswordRecoverConfirmed(AccountPasswordRecoverConfirmedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountPasswordRecoverConfirmedEvent");

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("pass", (String) params.get(PASSWORD_KEY));

        accountNotificationHelper.sendMail(account, "MajordomoVHResetPassConfirm", 10, parameters);

        String ip = (String) params.get(IP_KEY);

        //Запишем в историю клиента
        history.saveForOperatorService(account, "Произведена смена пароля к панели управления с IP: " + ip);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountPasswordChangedEvent(AccountPasswordChangedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountPasswordChangedEvent");

        String ip = (String) params.get(IP_KEY);

        //Запишем в историю клиента
        history.saveForOperatorService(account, "Произведена смена пароля к панели управления с IP: " + ip);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("acc_id", account.getAccountId());
        parameters.put("ip", ip);
        parameters.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy H:m:s")));

        accountNotificationHelper.sendMail(account, "MajordomoVHPassChAccount", 10, parameters);
    }

    /**
     * @param event должен содержать SimpleServiceMessage с информацией о платеже
     *
     * Начисление AccountPromotion для бесплатной регистрации домена .ru или .рф
     *
     * Обрабатывается только реальный платеж, бонусные (например, при возврате), партнерские или кредитные игнорируются
     *
     * Условия акции:
     * При открытии нового аккаунта виртуального хостинга по тарифным планам «Безлимитный», «Безлимитный+», «Бизнес», «Бизнес+»
     * мы бесплатно зарегистрируем на Вас 1 домен в зоне .ru или .рф при единовременной оплате за
     * 3 месяца. Бонус предоставляется при открытии аккаунта для первого домена на аккаунте.
     *
     * Аккаунт считается новым, если на нём не было доменов
     */
    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountPromotionProcessByPayment(PaymentWasReceivedEvent event) {
        SimpleServiceMessage message = event.getSource();
        Map<String, ?> params = message.getParams();

        if (!params.get("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {
            logger.info("paymentTypeKind is not real, message: " + message.toString());
            return;
        }

        PersonalAccount account = accountManager.findOne(message.getAccountId());
        if (account == null || !account.isAccountNew()) {
            logger.info("account is null or not new, message: " + message.toString());
            return;
        }

        Plan plan = planManager.findOne(account.getPlanId());
        if (!plan.isActive() || plan.isAbonementOnly() || plan.getOldId().equals(((Integer) PLAN_START_ID).toString())) {
            logger.info("plan is abonementOnly or 'start' or not active, message: " + message.toString());
            return;
        }

        BigDecimal costFor3Month = plan.getService().getCost().multiply(new BigDecimal(3L));
        BigDecimal amount = getBigDecimalFromUnexpectedInput(params.get(AMOUNT_KEY));
        if (amount.compareTo(costFor3Month) < 0) {
            logger.info("amount less than cost for 3 month, message: " + message.toString());
            return;
        }

        List<Domain> domains = accountHelper.getDomains(account);
        if (domains != null && !domains.isEmpty()) {
            logger.info("account has domains, message: " + message.toString());
            return;
        }

        Promotion promotion = promotionRepository.findByName(FREE_DOMAIN_PROMOTION);
        List<AccountPromotion> accountPromotions = accountPromotionManager.findByPersonalAccountIdAndPromotionId(account.getId(), promotion.getId());
        if (accountPromotions != null && !accountPromotions.isEmpty()) {
            logger.info("account has accountPromotions with id " + promotion.getId() + " , message: " + message.toString());
            return;
        }

        accountHelper.giveGift(account, promotion);

        Map<String, Object> data = new HashMap<>();
        data.put("event", "freeDomain");

        DefaultAccountNotice notice = new DefaultAccountNotice();
        notice.setPersonalAccountId(account.getId());
        notice.setData(data);

        accountNoticeRepository.insert(notice);
    }

    /**
     * @param event должен содержать SimpleServiceMessage с информацией о платеже
     * Пополнение баланса партнера при поступлении средств клиенту, если этот клиент регистрировался по промокоду
     *
     * Обрабатывается только реальный платеж, бонусные (например, при возврате), партнерские или кредитные игнорируются
     *
     * Условия акции:
     *              Аккаунт партнёра должен быть зарегистрирован менее года назад
     *              Если зарегистрирован до 2018-01-25 00:00:00, то начисляется 25% от суммы платежа
     *                                  после 2018-01-25 00:00:00, то начисляется 30%
     */

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountPromotionProcessByPaymentCreatedEvent(PaymentWasReceivedEvent event) {
        SimpleServiceMessage message = event.getSource();

        if (!message.getParam("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {
            return;
        }

        PersonalAccount account = accountManager.findOne(message.getAccountId());
        if (account == null) {
            return;
        }

        Map<String, ?> paramsForPublisher = message.getParams();
        BigDecimal amount = getBigDecimalFromUnexpectedInput(paramsForPublisher.get(AMOUNT_KEY));

        try {
            ActionStatRequest actionStatRequest = new ActionStatRequest();
            actionStatRequest.setAmount(amount);

            partnersFeignClient.actionByAccountIdAndAmount(account.getId(), actionStatRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("amount", amount);
            yaPromoterFeignClient.paymentEvent(account.getId(), request);
        } catch (Exception e) {
            logger.error(
                    "can't send a payment event yaPromoterFeignClient.paymentEvent(), account.id: {} e: {}, message: {}",
                    account.getId(), e.getClass(), e.getMessage()
            );
        }

        paymentPercentBonusActionProcessor.processPayment(account, amount);
    }

    //Если пользователь кладёт деньги, но не покупает абонемент, то через 40 минут отправляем ему письмо
    @EventListener
    @Async("threadPoolTaskExecutor")
    public void userCanBuyAbonement(PaymentWasReceivedEvent event) {
        logger.info("userCanBuyAbonement");

        SimpleServiceMessage message = event.getSource();

        if (!message.getParam("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {
            return;
        }

        PersonalAccount account = accountManager.findOne(message.getAccountId());
        if (account == null) {
            return;
        }

        AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());

        if (accountAbonement == null
                ||
                accountAbonement.getAbonement().isInternal() && accountAbonement.getAbonement().getPeriod().equals("P14D")
        ) {
            Plan plan = planManager.findOne(account.getPlanId());
            BigDecimal cost = plan.getNotInternalAbonement().getService().getCost();

            SendMailIfAbonementWasNotBought example = new SendMailIfAbonementWasNotBought();
            example.setPersonalAccountId(account.getId());
            example.setState(State.NEW);

            List<SendMailIfAbonementWasNotBought> willExecute = taskManager.findByExample(example);

            if (willExecute != null && !willExecute.isEmpty()) {
                logger.info("accountId " + account.getId() + " already has task for mail");
                return;
            }

            BigDecimal balance = accountHelper.getBalance(account);

            if (balance.compareTo(cost) < 0) {
                logger.info("can't buy abonement accountId " + account.getId() + " cost " + cost + " balance " + balance);
                return;
            }

            taskManager.save(
                    new SendMailIfAbonementWasNotBought(
                            account.getId(),
                            LocalDateTime.now().plusMinutes(40)
                    )
            );
        }
    }

    /**
     * @param event должен содержать SimpleServiceMessage с информацией о платеже
     *
     * При поступлении платежа на контактный номер телефона отправляется СМС
     *
     * Обрабатывается только реальный платеж, бонусные (например, при возврате), партнерские или кредитные игнорируются
     */
    @EventListener
    @Async
    public void notifyByPayment(PaymentWasReceivedEvent event) {
        SimpleServiceMessage message = event.getSource();

        if (!message.getParam("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {
            return;
        }

        PersonalAccount account = accountManager.findOne(message.getAccountId());
        if (account == null) {
            return;
        }

        try {
            if (accountNotificationHelper.isSubscribedToSmsType(account, MailManagerMessageType.SMS_NEW_PAYMENT)) {

                HashMap<String, String> paramsForSms = new HashMap<>();
                paramsForSms.put("client_id", account.getAccountId());
                paramsForSms.put("acc_id", account.getName());
                paramsForSms.put("add_sum", Utils.formatBigDecimalWithCurrency(Utils.getBigDecimalFromUnexpectedInput(message.getParam(AMOUNT_KEY))));

                accountNotificationHelper.sendSms(account, "MajordomoHMSNewPayment", 10, paramsForSms);
            }
        } catch (Exception e) {
            logger.error("Can't send SMS for account " + account.getName() + ", exceptionMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * @param event должен содержать SimpleServiceMessage с информацией о платеже
     *
     * При поступлении платежа производится попытка списания и включения аккаунта
     *
     * Обрабатываются все платежи, кроме кредитных
     */
    @EventListener
    @Async("vipThreadPoolTaskExecutor")
    public void onAccountSwitchByPaymentCreatedEvent(PaymentWasReceivedEvent event) {
        SimpleServiceMessage message = event.getSource();

        if (message.getParam("paymentTypeKind").equals(CREDIT_PAYMENT_TYPE_KIND)) {
            return;
        }

        // Задержка (К примеру, в случае возврата денег в процессе смены тарифа)
        try {
            Thread.sleep(20000);
        } catch (Exception e) {
            logger.error("Exception in class AccountEventListener.onAccountSwitchByPaymentCreatedEvent on sleep");
            e.printStackTrace();
        }

        PersonalAccount account = accountManager.findOne(message.getAccountId());
        if (account == null) {
            return;
        }

        BigDecimal balance = accountHelper.getBalance(account);
        Plan plan = planManager.findOne(account.getPlanId());

        if (!plan.isAbonementOnly()) {

            if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                if (account.getCreditActivationDate() != null) {
                    accountManager.removeSettingByName(account.getId(), CREDIT_ACTIVATION_DATE);
                }
                tryProcessChargeAndEnableAccount(account);
            }

        } else {

            String addAbonementId = plan.getNotInternalAbonementId();

            if (addAbonementId != null) {
                AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());
                if (accountAbonement == null) {
                    try {
                        abonementService.addAbonement(account, addAbonementId, true);
                        accountHelper.enableAccount(account);
                    } catch (Exception e) {
                        logger.info("Ошибка при покупке абонемента для AbonementOnly плана.");
                        e.printStackTrace();
                    }
                } else if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    tryProcessChargeAndEnableAccount(account);
                }
            }
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountOwnerChangeEmailEvent(AccountOwnerChangeEmailEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, Object> params = event.getParams();

        logger.debug("We got AccountOwnerChangeEmailEvent\n");

        Token oldToken = tokenManager.getToken(TokenType.CHANGE_OWNER_EMAILS, account.getId());
        if (oldToken != null) { tokenManager.deleteToken(oldToken); }

        String token = tokenManager.generateToken(account, TokenType.CHANGE_OWNER_EMAILS, params);

        HashMap<String, String> paramsForEmail = new HashMap<>();
        paramsForEmail.put("acc_id", account.getName());
        paramsForEmail.put("new_emails", String.join("<br>", (List) params.get("newemails")));
        paramsForEmail.put("token", token);
        paramsForEmail.put("ip", (String) params.get("ip"));
        accountNotificationHelper.sendMail(account, "MajordomoHmsChangeEmail", 10, paramsForEmail);
    }

    private void tryProcessChargeAndEnableAccount(PersonalAccount account) {
        if (!account.isActive()) {
            // сразу списываем за текущий день
            chargeHelper.prepareAndProcessChargeRequest(account.getId(), LocalDate.now());
        }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountWasEnabled event) {
        LocalDateTime deactivated = event.getDeactivated();

        logger.debug("We got AccountWasEnabled event, accountId: {} deactivated: {}", event.getSource(), deactivated);

        if (deactivated == null) {
            return;
        }

        LocalDate dataWillBeDeletedAfter = deactivated.toLocalDate().plusDays(deleteDataAfterDays);
        LocalDate now = LocalDate.now();

        if (dataWillBeDeletedAfter.isAfter(now)) {
            logger.info("account with id '{}', data not deleted yet, now: '{}', will be deleted after: '{}' , return",
                    event.getSource(),
                    now.format(DateTimeFormatter.ISO_DATE),
                    dataWillBeDeletedAfter.format(DateTimeFormatter.ISO_DATE));
            return;
        }

        // Задержка для включения ресурсов аккаунта перед восстановлением из бекапов
        try {
            Thread.sleep(20000);
        } catch (Exception e) {
            logger.error("Exception in class AccountEventListener.on(AccountWasEnabled) on sleep");
            e.printStackTrace();
        }

        PersonalAccount account = accountManager.findOne(event.getSource());

        backupService.restoreAccountAfterEnabled(account, deactivated, dataWillBeDeletedAfter);
    }
}