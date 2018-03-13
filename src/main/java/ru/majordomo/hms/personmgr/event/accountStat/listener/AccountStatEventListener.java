package ru.majordomo.hms.personmgr.event.accountStat.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.NotificationType;
import ru.majordomo.hms.personmgr.common.Utils;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.PaymentWasReceivedEvent;
import ru.majordomo.hms.personmgr.event.accountStat.AccountStatDomainUpdateEvent;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.repository.AccountNotificationStatRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.service.AccountStatHelper;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.Domain;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_AUTO_RENEW_DOMAIN;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.NotificationTransportType.EMAIL;
import static ru.majordomo.hms.personmgr.common.NotificationTransportType.SMS;

@Component
public class AccountStatEventListener {
    private final RcUserFeignClient rcUserFeignClient;
    private final AccountStatHelper accountStatHelper;
    private final PersonalAccountManager accountManager;
    private final AccountNotificationStatRepository accountNotificationStatRepository;
    private final ProcessingBusinessActionRepository processingBusinessActionRepository;

    public AccountStatEventListener(
            RcUserFeignClient rcUserFeignClient,
            AccountStatHelper accountStatHelper,
            PersonalAccountManager accountManager,
            AccountNotificationStatRepository accountNotificationStatRepository,
            ProcessingBusinessActionRepository processingBusinessActionRepository
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.accountStatHelper = accountStatHelper;
        this.accountManager = accountManager;
        this.accountNotificationStatRepository = accountNotificationStatRepository;
        this.processingBusinessActionRepository = processingBusinessActionRepository;
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void onAccountStatDomainUpdateEvent(AccountStatDomainUpdateEvent event) {
        SimpleServiceMessage message = event.getSource();
        String domainName = (String) message.getParam(NAME_KEY);
        String accountId = message.getAccountId();
        boolean statDataAutoRenew = (Boolean) message.getParam(AUTO_RENEW_KEY);

        if (domainName == null) {
            ProcessingBusinessAction businessAction = processingBusinessActionRepository.findOne(message.getActionIdentity());
            Domain domain = rcUserFeignClient.getDomain(accountId, (String) businessAction.getParam(RESOURCE_ID_KEY));
            if (domain != null) { domainName = domain.getName(); }
        }

        Map<String, String> statData = new HashMap<>();
        statData.put(ACCOUNT_ID_KEY, accountId);
        statData.put(DOMAIN_NAME_KEY, domainName);

        accountStatHelper.add(
                accountId,
                statDataAutoRenew ? VIRTUAL_HOSTING_AUTO_RENEW_DOMAIN : AccountStatType.VIRTUAL_HOSTING_MANUAL_RENEW_DOMAIN,
                statData);
    }

    @EventListener
    @Async("threadPoolTaskExecutor")
    public void checkFirstRealPayment(PaymentWasReceivedEvent event) {

        SimpleServiceMessage message = event.getSource();

        // только если платеж реальных средств сохраняем в accountStat
        if (!message.getParam("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {
            return;
        }

        String accountId = message.getAccountId();

        //Если запись уже существует, то ничего не делаем, иначе сохраним инфу по первому платежу
        if (accountStatHelper.exist(accountId, AccountStatType.VIRTUAL_HOSTING_FIRST_REAL_PAYMENT)) {
            return;
        }

        HashMap<String, String> statData = new HashMap<>();
        statData.put(ACCOUNT_ID_KEY, accountId);
        statData.put(AMOUNT_KEY, Utils.getBigDecimalFromUnexpectedInput(message.getParam(AMOUNT_KEY)).toString());
        statData.put("paymentTypeId", (String) message.getParam("paymentTypeId"));

        accountStatHelper.add(
                accountId,
                AccountStatType.VIRTUAL_HOSTING_FIRST_REAL_PAYMENT,
                statData);

    }

    /**
     * @param event должен содержать SimpleServiceMessage с информацией о платеже
     *
     * Сохранение статистики для платежей после уведомлений о том, что деньги на аккаунте заканчиваются
     *
     * Условия:
     *              Обрабатываются только реальные платежи
     *              Последнее уведомление о нехватке средств отправлялось не более 3 дней назад
     */
    @EventListener
    @Async("vipThreadPoolTaskExecutor")
    public void saveStatIfPaymentAfterNotification(PaymentWasReceivedEvent event) {
        SimpleServiceMessage message = event.getSource();

        if (!message.getParam("paymentTypeKind").equals(REAL_PAYMENT_TYPE_KIND)) {
            return;
        }

        PersonalAccount account = accountManager.findOne(message.getAccountId());
        if (account == null) {
            return;
        }

        boolean paymentWasRecievedAfterNotificaton = accountNotificationStatRepository
                .existsByAccountTypeAndTransportTypeInAndPersonalAccountIdAndCreatedAfterAndNotificationType(
                        AccountType.VIRTUAL_HOSTING,
                        Arrays.asList(EMAIL, SMS),
                        account.getId(),
                        LocalDateTime.now().minusDays(3),
                        NotificationType.REMAINING_DAYS_MONEY_ENDS
                );

        if (!paymentWasRecievedAfterNotificaton) {
            return;
        }

//        statFeignClein
    }
}
