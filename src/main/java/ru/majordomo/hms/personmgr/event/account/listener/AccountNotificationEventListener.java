package ru.majordomo.hms.personmgr.event.account.listener;

import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.feign.BizMailFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.feign.StatFeignClient;
import ru.majordomo.hms.personmgr.manager.CartManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.AccountNotificationStat;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.cart.Cart;
import ru.majordomo.hms.personmgr.model.cart.DomainCartItem;
import ru.majordomo.hms.personmgr.model.order.BitrixLicenseOrder;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.AccountNotificationStatRepository;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.service.*;
import ru.majordomo.hms.personmgr.service.order.BitrixLicenseOrderManager;
import ru.majordomo.hms.rc.user.resources.Domain;
import ru.majordomo.hms.rc.user.resources.Resource;
import ru.majordomo.hms.rc.user.resources.ResourceArchive;
import ru.majordomo.hms.rc.user.resources.ResourceArchiveType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.MailManagerMessageType.EMAIL_NEWS;
import static ru.majordomo.hms.personmgr.service.order.BitrixLicenseOrderManager.MAY_PROLONG_DAYS_BEFORE_EXPIRED;

@Component
public class AccountNotificationEventListener {
    private final static Logger logger = LoggerFactory.getLogger(AccountEventListener.class);

    private final ResourceHelper resourceHelper;
    private final AccountStatRepository accountStatRepository;
    private final PlanManager planManager;
    private final AccountNotificationHelper accountNotificationHelper;
    private final BizMailFeignClient bizMailFeignClient;
    private final PersonalAccountManager personalAccountManager;
    private final CartManager cartManager;
    private final AccountNotificationStatRepository accountNotificationStatRepository;
    private final StatFeignClient statFeignClient;
    private final BitrixLicenseOrderManager bitrixLicenseOrderManager;
    private final RcUserFeignClient rcUserFeignClient;

    @Autowired
    public AccountNotificationEventListener(
            ResourceHelper resourceHelper,
            AccountStatRepository accountStatRepository,
            PlanManager planManager,
            AccountNotificationHelper accountNotificationHelper,
            BizMailFeignClient bizMailFeignClient,
            PersonalAccountManager personalAccountManager,
            CartManager cartManager,
            AccountNotificationStatRepository accountNotificationStatRepository,
            StatFeignClient statFeignClient,
            BitrixLicenseOrderManager bitrixLicenseOrderManager,
            RcUserFeignClient rcUserFeignClient
    ) {
        this.resourceHelper = resourceHelper;
        this.accountStatRepository = accountStatRepository;
        this.planManager = planManager;
        this.accountNotificationHelper = accountNotificationHelper;
        this.bizMailFeignClient = bizMailFeignClient;
        this.personalAccountManager = personalAccountManager;
        this.cartManager = cartManager;
        this.accountNotificationStatRepository = accountNotificationStatRepository;
        this.statFeignClient = statFeignClient;
        this.bitrixLicenseOrderManager = bitrixLicenseOrderManager;
        this.rcUserFeignClient = rcUserFeignClient;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountNotificationRemainingDaysWasSentEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        AccountNotificationStat stat = new AccountNotificationStat(
                account, NotificationType.REMAINING_DAYS_MONEY_ENDS, NotificationTransportType.EMAIL, event.getApiName());

        accountNotificationStatRepository.save(stat);

        Map<String, Object> body = new HashMap<>();
        body.put(RESOURCE_ID_KEY, NotificationType.REMAINING_DAYS_MONEY_ENDS);
        body.put(NAME_KEY, event.getApiName());
        body.put(TYPE_KEY, NotificationTransportType.EMAIL);
        statFeignClient.notificatonWasSendIncrement(body);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountSendSmsNotificationRemainingDaysEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        int remainingDays = event.getRemainingDays();
        String apiName = "MajordomoRemainingDays";
        HashMap<String, String> paramsForSms = new HashMap<>();
        paramsForSms.put("remaining_days", Utils.pluralizef("остался %d день", "осталось %d дня", "осталось %d дней", remainingDays));
        paramsForSms.put("client_id", account.getAccountId());
        accountNotificationHelper.sendSms(account, apiName, 10, paramsForSms);

        AccountNotificationStat stat = new AccountNotificationStat(
                account, NotificationType.REMAINING_DAYS_MONEY_ENDS, NotificationTransportType.SMS, apiName);

        accountNotificationStatRepository.save(stat);

        Map<String, Object> body = new HashMap<>();
        body.put(RESOURCE_ID_KEY, NotificationType.REMAINING_DAYS_MONEY_ENDS);
        body.put(NAME_KEY, "MajordomoRemainingDays");
        body.put(TYPE_KEY, NotificationTransportType.SMS);
        statFeignClient.notificatonWasSendIncrement(body);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onSendTelegramNotificationRemainingDaysEvent(AccountSendTelegramNotificationRemainingDaysEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        int remainingDays = event.getRemainingDays();
        String apiName = "TelegramMajordomoRemainingDays";
        HashMap<String, String> paramsForSms = new HashMap<>();
        paramsForSms.put("remaining_days", Utils.pluralizef("остался %d день", "осталось %d дня", "осталось %d дней", remainingDays));
        paramsForSms.put("client_id", account.getAccountId());

        accountNotificationHelper.sendTelegram(account, apiName, paramsForSms);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountNotifyNotRegisteredDomainsInCart(AccountNotifyNotRegisteredDomainsInCart event){

        List<Integer> daysForNotify = Arrays.asList(1, 6, 11, 16, 21, 26);

        List<Cart> carts = cartManager.findNotEmptyCartsAtLastMonth();
        carts.forEach(cart -> {
            PersonalAccount account = personalAccountManager.findOne(cart.getPersonalAccountId());
            if (account.hasNotification(EMAIL_NEWS)) {

                int daysAfterCartLastUpdate = Utils.differenceInDays(
                        cart.getUpdateDateTime().toLocalDate(),
                        LocalDate.now()
                );

                if (daysForNotify.contains(daysAfterCartLastUpdate)) {
                    Long countDomainsInCart = cart.getItems()
                            .stream()
                            .filter(cartItem -> cartItem instanceof DomainCartItem)
                            .count();

                    if (countDomainsInCart > 0) {
                        String countDomainsForMessage = Utils.pluralizef(
                                "%d домен",
                                "%d домена",
                                "%d доменов",
                                countDomainsInCart.intValue()
                        );

                        Map<String, String> parameters = new HashMap<>();
                        parameters.put(ACC_ID_KEY, account.getName());
                        parameters.put("domains_in_cart", countDomainsForMessage);
                        parameters.put(CLIENT_ID_KEY, account.getAccountId());

                        accountNotificationHelper.sendMail(account, "HmsMajordomoForgotDomains", parameters);
                    }
                }
            }
        });
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountSendInfoMailEvent(AccountSendInfoMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        int accountAgeInDays = Utils.differenceInDays(account.getCreated().toLocalDate(), LocalDate.now());

        String apiName = null;

        if (accountAgeInDays == 30) {
            //всем, в том числе и неактивным
            apiName = "MajordomoHmsKonstructorNethouse";

        } else if (account.isActive()) {

            switch (accountAgeInDays) {
                case 1:
                    apiName = "HmsMajordomoUstanoviteCMS";
                    break;

                case 4:
                    //если не регистрировал домен у нас
                    AccountStat accountStat = accountStatRepository.findFirstByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
                            account.getId(),
                            AccountStatType.VIRTUAL_HOSTING_REGISTER_DOMAIN,
                            account.getCreated()
                    );

                    if (accountStat == null) {
                        apiName = "MajordomoHmsDomainVPodarok";
                    }
                    break;

                case 9:
                    //только для базовых тарифов
                    VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) planManager.findOne(account.getPlanId()).getPlanProperties();
                    if (!planProperties.isBusinessServices()) {
                        apiName = "MajordomoHmsCorporateTarif";
                    }
                    break;

                case 20:
                    apiName = "MajordomoHmsPromokodGoogle";
                    break;

                case 25:
                    //отправляем, если есть домены и ни один не привязан к biz.mail.ru
                    //делегирован домен на наши NS или нет - неважно
                    List<Domain> domains = resourceHelper.getDomains(account);
                    if (domains == null || domains.isEmpty()) {
                        break;
                    }
                    List<Object> bizDomains = new ArrayList<>();
                    try {
                        bizDomains = bizMailFeignClient.getDomainsFromBizmail(account.getId());
                    } catch (Exception e) {
                        logger.error("Could not get domains from bizmail with account id [" + account.getId() + "] " + e.getMessage());
                    }
                    if (bizDomains != null && bizDomains.isEmpty()) {
                        apiName = "MajordomoHmsPochtaMailRu";
                    }
                    break;

                case 35:
                    apiName = "MajordomoHmsProdvigenie";
                    break;

                case 45:
                    apiName = "MajordomoHmsPartners";
                    break;

                case 50:
                    apiName = "MajordomoEventsAdvertising";
                    break;
            }
        }
        if (apiName != null) { accountNotificationHelper.sendInfoMail(account, apiName); }
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountNotifyInactiveLongTimeEvent(AccountNotifyInactiveLongTimeEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());
        logger.debug("We got AccountNotifyInactiveLongTimeEvent\n");

        LocalDateTime deactivatedDateTime = account.getDeactivated();
        LocalDate deactivatedDate = deactivatedDateTime.toLocalDate();

        boolean needSend = Stream.of(1, 2, 3, 6, 12)
                .anyMatch(months -> deactivatedDate.isEqual(LocalDate.now().minusMonths(months)));

        if (!needSend) {
            return;
        }

        List<AccountStatType> types = new ArrayList<>();
        types.add(AccountStatType.VIRTUAL_HOSTING_ACC_OFF_NOT_ENOUGH_MONEY);
        types.add(AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE);

        AccountStat accountStat = accountStatRepository.findFirstByPersonalAccountIdAndTypeInAndCreatedAfterOrderByCreatedDesc(
                account.getId(), types, deactivatedDateTime.withHour(0).withMinute(0).withSecond(0));

        if (accountStat == null) { return; }

        //Если НЕ СОВПАДАЕТ  дата деактивации и последняя запись в статистике по причине отключения
        if (!accountStat.getCreated().toLocalDate().isEqual(deactivatedDate)) { return; }

        accountNotificationHelper.sendInfoMail(account, "MajordomoHmsReturnClient");
    }

    //Отправка писем в случае выключенного аккаунта из-за нехватки средств
    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountDeactivatedSendMailEvent(AccountDeactivatedReSendMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        logger.debug("We got AccountDeactivatedSendMailEvent\n");

        if (account.isActive() || planManager.findOne(account.getPlanId()).isAbonementOnly()) { return;}

        AccountStat accountStat = accountStatRepository.findFirstByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_ACC_OFF_NOT_ENOUGH_MONEY,
                LocalDateTime.now().minusDays(21)
        );

        if (accountStat == null) {return;}

        // Если в тот же день есть удаление абонемента, то отправлять уведомление не нужно
        // причина выключения аккаунта - истекший абонемент
        // после удаления абонемента начислилась услуга тарифа и была неудачная попытка списания,
        // после чего аккаунт был выключен
        AccountStat accountStatAbonementDelete = accountStatRepository.findFirstByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE,
                accountStat.getCreated().withHour(0).withMinute(0).withSecond(0)
        );
        if (accountStatAbonementDelete != null
                && accountStatAbonementDelete.getCreated().toLocalDate().equals(
                accountStat.getCreated().toLocalDate())) {
            return;
        }

        List<Integer> daysAgo = Arrays.asList(1, 3, 5, 10, 15, 20);
        LocalDate dateAccountDisableByNotEnoughMoney = accountStat.getCreated().toLocalDate();
        Integer daysDifferent = Utils.differenceInDays(dateAccountDisableByNotEnoughMoney, LocalDate.now());

        if (daysAgo.contains(daysDifferent)) {
            accountNotificationHelper.sendMailForDeactivatedAccount(account, dateAccountDisableByNotEnoughMoney);
        }
    }

    //Отправка sms в случае выключенного аккаунта из-за нехватки средств
    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountDeactivatedSendSmsEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        logger.debug("We got AccountDeactivatedSendSmsEvent\n");

        String smsPhone = account.getSmsPhoneNumber();

        if (account.isActive() || smsPhone == null || smsPhone.equals("")) { return;}

        String apiName = "MajordomoAccountBlocked";
        HashMap<String, String> paramsForSms = new HashMap<>();
        paramsForSms.put("acc_id", account.getName());
        paramsForSms.put("client_id", account.getAccountId());
        accountNotificationHelper.sendSms(account, apiName, 1, paramsForSms);
        accountNotificationHelper.sendTelegram(account, apiName, paramsForSms);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ResourceArchiveCreatedSendMailEvent event) {
        String archivedResourceId = event.getArchivedResourceId();
        String resourceArchiveId = event.getResourceArchiveId();
        ResourceArchiveType resourceArchiveType = event.getResourceArchiveType();

        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        Resource resource = null;
        String apiName = null;

        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());

        ResourceArchive resourceArchive = null;
        try {
            resourceArchive = rcUserFeignClient.getResourceArchive(account.getId(), resourceArchiveId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (resourceArchive == null) {
            return;
        }

        parameters.put("archive_link", resourceArchive.getFileLink());

        switch (resourceArchiveType) {
            case WEBSITE:
                try {
                    resource = rcUserFeignClient.getWebSite(account.getId(), archivedResourceId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                apiName = "MajordomoHmsArchiveSite";

                break;
            case DATABASE:

                try {
                    resource = rcUserFeignClient.getDatabase(account.getId(), archivedResourceId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                apiName = "MajordomoHmsArchiveDatabase";

                break;
        }

        if (resource == null) {
            return;
        }


        parameters.put("resource_name", resource.getName());

        accountNotificationHelper.sendMail(account, apiName, 10, parameters);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onNewAuthNotify(NewAuthNotifyEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, String> params = event.getParams();

        accountNotificationHelper.sendMail(account, "MajordomoVHNewAuthNotify", 10, params);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountQuotaAdded(AccountQuotaAddedEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountQuotaAddedEvent");

        String planName = (String) params.get(SERVICE_NAME_KEY);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("tariff", planName);

        accountNotificationHelper.sendMail(account, "MajordomoVHQuotaAdd", 10, parameters);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountQuotaDiscard(AccountQuotaDiscardEvent event) {
        PersonalAccount account = event.getSource();
        Map<String, ?> params = event.getParams();

        logger.debug("We got AccountQuotaDiscardEvent");

        String planName = (String) params.get(SERVICE_NAME_KEY);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("client_id", account.getAccountId());
        parameters.put("acc_id", account.getName());
        parameters.put("tariff", planName);
        parameters.put("domains", accountNotificationHelper.getDomainForEmail(account));

        accountNotificationHelper.sendMail(account, "MajordomoVHQuotaDiscard", 10, parameters);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(ProcessNotifyExpiringBitrixLicenseEvent event){
        Predicate predicate = bitrixLicenseOrderManager
                .getPredicate(
                        null,
                        LocalDateTime.now().minusYears(1),
                        LocalDateTime.now().minusYears(1).plusDays(MAY_PROLONG_DAYS_BEFORE_EXPIRED),
                        OrderState.FINISHED,
                        null,
                        true
                );
        List<Integer> daysForNotify = Arrays.asList(1);

        bitrixLicenseOrderManager.findAll(predicate)
                .stream()
                .filter(order -> daysForNotify.contains(
                        Utils.differenceInDays(
                                LocalDate.now(),
                                order.getUpdated().toLocalDate().plusYears(1)
                )))
                .map(BitrixLicenseOrder::getPersonalAccountId)
                .peek(logger::debug)
                .collect(Collectors.toSet())
                .forEach(personalAccountId -> {
                    PersonalAccount account = personalAccountManager.findOne(personalAccountId);
                    accountNotificationHelper.sendMail(account, "HmsVHMajordomoOkonchaniesroka1CBitrix", null);
                });

    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountDeactivatedSendMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        // Если в сегодня было удаление абонемента, то отправлять уведомление не нужно
        // причина выключения аккаунта - истекший абонемент
        // после удаления абонемента начислилась услуга тарифа и была неудачная попытка списания,
        // после чего аккаунт был выключен
        AccountStat accountStatAbonementDelete = accountStatRepository.findFirstByPersonalAccountIdAndTypeAndCreatedAfterOrderByCreatedDesc(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_ABONEMENT_DELETE,
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        );

        if (accountStatAbonementDelete != null) {
            return;
        }

        accountNotificationHelper.sendMailForDeactivatedAccount(account, LocalDate.now());
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountDeactivatedWithExpiredCreditSendMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        accountNotificationHelper.sendMailDeactivatedWithExpiredCredit(account);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountCreditJustActivatedWithHostingAbonementSendMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        accountNotificationHelper.sendMailCreditJustActivatedWithHostingAbonement(account);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountCreditExpiringWithHostingAbonementSendMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        accountNotificationHelper.sendMailCreditExpiringWithHostingAbonement(account);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountCreditExpiredWithHostingAbonementSendMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        accountNotificationHelper.sendMailCreditExpiredWithHostingAbonement(account);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountServicesDisabledWithHostingAbonementSendMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        accountNotificationHelper.sendMailServicesDisabledWithHostingAbonement(account);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountServicesExpiringWithHostingAbonementSendMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        accountNotificationHelper.sendMailServicesExpiringWithHostingAbonement(account, event.getRemainingDays());
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountCreditJustActivatedSendMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        accountNotificationHelper.sendMailCreditJustActivated(account);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountCreditExpiringSendMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        accountNotificationHelper.sendMailCreditExpiring(account);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void on(AccountServicesExpiringSendMailEvent event) {
        PersonalAccount account = personalAccountManager.findOne(event.getSource());

        accountNotificationHelper.sendMailServicesExpiring(account, event.getRemainingDays());
    }
}

