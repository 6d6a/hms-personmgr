package ru.majordomo.hms.personmgr.event.account.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.common.TokenType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.Result;
import ru.majordomo.hms.personmgr.dto.fin.PaymentRequest;
import ru.majordomo.hms.personmgr.dto.push.PaymentReceivedPush;
import ru.majordomo.hms.personmgr.dto.partners.ActionStatRequest;
import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.feign.FinFeignClient;
import ru.majordomo.hms.personmgr.feign.PartnersFeignClient;
import ru.majordomo.hms.personmgr.feign.YaPromoterFeignClient;
import ru.majordomo.hms.personmgr.manager.*;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.account.InfoBannerAccountNotice;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.task.SendMailIfAbonementWasNotBought;
import ru.majordomo.hms.personmgr.model.task.State;
import ru.majordomo.hms.personmgr.model.token.Token;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.service.promocodeAction.PaymentPercentBonusActionProcessor;

import static java.lang.String.format;
import static ru.majordomo.hms.personmgr.common.AccountSetting.CREDIT;
import static ru.majordomo.hms.personmgr.common.AccountSetting.CREDIT_ACTIVATION_DATE;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Utils.getBigDecimalFromUnexpectedInput;

@Component
public class AccountEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountEventListener.class);

    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;
    private final TokenManager tokenManager;
    private final ApplicationEventPublisher publisher;
    private final PlanManager planManager;
    private final AbonementService abonementService;
    private final PersonalAccountManager accountManager;
    private final AbonementManager<AccountAbonement> accountAbonementManager;
    private final AccountNotificationHelper accountNotificationHelper;
    private final ChargeHelper chargeHelper;
    private final AccountHistoryManager history;
    private final BackupService backupService;
    private final PartnersFeignClient partnersFeignClient;
    private final TaskManager taskManager;
    private final AccountNoticeManager accountNoticeManager;
    private final PaymentPercentBonusActionProcessor paymentPercentBonusActionProcessor;
    private final YaPromoterFeignClient yaPromoterFeignClient;
    private final FirstMobilePaymentProcessor firstMobilePaymentProcessor;
    private final Consumer<Supplier<AccountBuyAbonement>> buyAbonementPromotionProcessor;
    private final PreorderService preorderService;
    private final FinFeignClient finFeignClient;

    private final int deleteDataAfterDays;

    @Autowired
    public AccountEventListener(
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper,
            TokenManager tokenManager,
            ApplicationEventPublisher publisher,
            PlanManager planManager,
            AbonementService abonementService,
            PersonalAccountManager accountManager,
            AbonementManager<AccountAbonement> accountAbonementManager,
            AccountNotificationHelper accountNotificationHelper,
            ChargeHelper chargeHelper,
            AccountHistoryManager history,
            BackupService backupService,
            PartnersFeignClient partnersFeignClient,
            TaskManager taskManager,
            AccountNoticeManager accountNoticeManager,
            PaymentPercentBonusActionProcessor paymentPercentBonusActionProcessor,
            YaPromoterFeignClient yaPromoterFeignClient,
            FirstMobilePaymentProcessor firstMobilePaymentProcessor,
            BuyAbonementPromotionProcessor buyAbonementPromotionProcessor,
            PreorderService preorderService,
            @Value("${delete_data_after_days}") int deleteDataAfterDays,
            FinFeignClient finFeignClient
    ) {
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
        this.tokenManager = tokenManager;
        this.publisher = publisher;
        this.planManager = planManager;
        this.abonementService = abonementService;
        this.accountManager = accountManager;
        this.accountAbonementManager = accountAbonementManager;
        this.accountNotificationHelper = accountNotificationHelper;
        this.chargeHelper = chargeHelper;
        this.history = history;
        this.backupService = backupService;
        this.partnersFeignClient = partnersFeignClient;
        this.taskManager = taskManager;
        this.accountNoticeManager = accountNoticeManager;
        this.paymentPercentBonusActionProcessor = paymentPercentBonusActionProcessor;
        this.yaPromoterFeignClient = yaPromoterFeignClient;
        this.firstMobilePaymentProcessor = firstMobilePaymentProcessor;
        this.buyAbonementPromotionProcessor = buyAbonementPromotionProcessor;
        this.deleteDataAfterDays = deleteDataAfterDays;
        this.preorderService = preorderService;
        this.finFeignClient = finFeignClient;
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

        accountNoticeManager.save(notification);
        logger.debug("InfoBannerAccountNotice saved: " + notification.toString());
    }

    //Скидка отключена, на 24.07.2019 цена без скидки равна цене со скидкой
    /*@EventListener
    @Async("threadPoolTaskExecutor")
    public void addPromotions(AccountCreatedEvent event) {
        PersonalAccount account = event.getSource();

        Promotion promotion = promotionRepository.findByName(DOMAIN_DISCOUNT_RU_RF);

        for (int i = 1; i <= promotion.getLimitPerAccount(); i++) {
            accountHelper.giveGift(account, promotion);
        }
    }*/

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

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountPromotionProcessByBuyAbonement(AccountBuyAbonement event) {
        buyAbonementPromotionProcessor.accept(() -> event);
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

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onPaymentBillCreatedEvent(PaymentWasReceivedEvent event) {
        SimpleServiceMessage message = event.getSource();

        if (!message.getParam("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {
            return;
        }

        if (!message.getParam("paymentTypeId").equals(ACTION_BONUS_PAYMENT_BILL_ID)) {
            return;
        }

        PersonalAccount account = accountManager.findOne(message.getAccountId());
        if (account == null) {
            return;
        }

        Map<String, ?> paramsForPublisher = message.getParams();
        BigDecimal amount = getBigDecimalFromUnexpectedInput(paramsForPublisher.get(AMOUNT_KEY));

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startDate = LocalDateTime.parse(ACTION_BONUS_PAYMENT_BILL_START_DATE, formatter);
        LocalDateTime endDate = LocalDateTime.parse(ACTION_BONUS_PAYMENT_BILL_END_DATE, formatter);

        if (now.isAfter(startDate) && now.isBefore(endDate)) {

            BigDecimal bonusPaymentAmount = amount.multiply(new BigDecimal("0.1"));

            if (bonusPaymentAmount.compareTo(BigDecimal.ZERO) <= 0) { return; }

            try {
                finFeignClient.addPayment(
                        new PaymentRequest(account.getName())
                                .withAmount(bonusPaymentAmount)
                                .withBonusType()
                                .withMessage("Бонус за пополнение счета")
                                .withDisableAsync(true)
                );
                history.save(
                        account,
                        "Начислено " + bonusPaymentAmount + " бонусов после пополнения баланса за акцию +10% за безнал");
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getClass().getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * @param event должен содержать SimpleServiceMessage с информацией о платеже
     * Начисление бонуса, если пришел первый реальный платеж с помощью мобильного приложения
     *
     * Обрабатывается только реальный платеж, бонусные (например, при возврате), партнерские или кредитные игнорируются
     *
     * Условия акции:
     *              Акция действует только для первого платежа
     */

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void addBonusOnFirstMobilePayment(PaymentWasReceivedEvent event) {
        SimpleServiceMessage message = event.getSource();
        Map<String, ?> params = message.getParams();

        if (!params.get("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {
            return;
        }

        Map<String, String> metadata = (Map<String, String>) params.get(METADATA);

        if (metadata.get(OAUTH_CLIENT_ID) == null || !metadata.get(OAUTH_CLIENT_ID).equals(MOBILE_APP_OAUTH_CLIENT_ID)) {
            return;
        }

        PersonalAccount account = accountManager.findOne(message.getAccountId());
        if (account == null) {
            return;
        }

        if (account.getProperties().getBonusOnFirstMobilePaymentActionUsed() != null &&
                account.getProperties().getBonusOnFirstMobilePaymentActionUsed()) {
            return;
        }

        BigDecimal amount = getBigDecimalFromUnexpectedInput(params.get(AMOUNT_KEY));

        firstMobilePaymentProcessor.process(account, amount);
    }

    //Если пользователь кладёт деньги, но не покупает абонемент, то через 40 минут отправляем ему письмо
    @EventListener
    @Async("threadPoolTaskExecutor")
    public void userCanBuyAbonement(PaymentWasReceivedEvent event) {
        SimpleServiceMessage message = event.getSource();

        if (!message.getParam("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {
            return;
        }

        PersonalAccount account = accountManager.findOne(message.getAccountId());
        if (account == null) {
            return;
        }

        if (preorderService.isPreorder(account.getId())) {
            return;
        }

        if (accountAbonementManager.findAllByPersonalAccountId(account.getId()).stream().allMatch(
                accountAbonement -> accountAbonement.getAbonement().isTrial()
        )) {
            Plan plan = planManager.findOne(account.getPlanId());
            BigDecimal cost = accountServiceHelper.getServiceCostDependingOnDiscount(account.getId(), plan.getNotInternalAbonement().getService());

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

        Lazy<BigDecimal> amount = Lazy.of(() -> getBigDecimalFromUnexpectedInput(message.getParam(AMOUNT_KEY)));

        try {
            if (accountNotificationHelper.isSubscribedToSmsType(account, MailManagerMessageType.SMS_NEW_PAYMENT)) {

                HashMap<String, String> paramsForSms = new HashMap<>();
                paramsForSms.put("client_id", account.getAccountId());
                paramsForSms.put("acc_id", account.getName());
                paramsForSms.put("add_sum", Utils.formatBigDecimalWithCurrency(amount.get()));

                accountNotificationHelper.sendSms(account, "MajordomoHMSNewPayment", 10, paramsForSms);
            }
        } catch (Exception e) {
            logger.error("Can't send SMS for account " + account.getName() + ", exceptionMessage: " + e.getMessage());
            e.printStackTrace();
        }

        accountNotificationHelper.push(
                new PaymentReceivedPush(
                        account.getId(), account.getName() + " получен платеж",
                        format("На аккаунт %s начислен платеж %s", account.getName(), Utils.formatBigDecimalWithCurrency(amount.get())), amount.get()
                )
        );
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

        logger.info("We got onAccountSwitchByPaymentCreatedEvent for account: " + message.getAccountId());

        if (message.getParam("paymentTypeKind").equals(CREDIT_PAYMENT_TYPE_KIND)) {
            return;
        }

        try {
            // Задержка (К примеру, в случае возврата денег в процессе смены тарифа)
            // если на аккаунте есть предзаказы с небольшой задержкой
            Thread.sleep(preorderService.isPreorder(message.getAccountId()) ? 1000 : 20000);
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


        BigDecimal preordersCost = preorderService.getTotalCostPreorders(account);
        if (preordersCost != null || account.isPreorder()) {
            Result result = preorderService.buyOrder(account);
            if (!result.isSuccess()) {
                history.save(account, result.getMessage());
            }
        } else {
            if (!plan.isAbonementOnly()) {

                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    if (account.getCreditActivationDate() != null) {
                        accountManager.removeSettingByName(account.getId(), CREDIT_ACTIVATION_DATE);
                        accountManager.removeSettingByName(account.getId(), CREDIT);
                    }
                    tryProcessChargeAndEnableAccount(account);
                }

            } else {

                String addAbonementId = plan.getNotInternalAbonementId();

                if (addAbonementId != null) {
                    boolean hasAbonement = accountAbonementManager.existsByPersonalAccountId(account.getId());
                    if (!hasAbonement && !plan.isArchival()) {
                        try {
                            abonementService.addAbonement(account, addAbonementId);
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
            logger.info("tryProcessChargeAndEnableAccount for account: " + account.getId()); //todo change to debug
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
        logger.info("Need restore data for account {}, deactivated: {}, isActive: {}, isFreeze: {}, deleted: " + event.getSource(), deactivated, account.isActive(), account.isFreeze(), account.getDeactivated());
        backupService.restoreAccountAfterEnabled(account, deactivated, dataWillBeDeletedAfter);
    }
}