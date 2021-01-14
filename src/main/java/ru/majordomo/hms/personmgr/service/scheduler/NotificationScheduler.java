package ru.majordomo.hms.personmgr.service.scheduler;

import net.javacrumbs.shedlock.core.SchedulerLock;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.event.account.*;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.AccountStat;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_ID_PLAN_UNLIMITED_DAILY_CHARGES;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_ID_PLAN_UNLIMITED_PLUS_DAILY_CHARGES;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_ID_PLAN_BUSINESS_DAILY_CHARGES;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_ID_PLAN_BUSINESS_PLUS_DAILY_CHARGES;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class NotificationScheduler {
    private final PersonalAccountManager accountManager;
    private final AccountStatRepository accountStatRepository;
    private final AccountServiceRepository accountServiceRepository;
    private final ApplicationEventPublisher publisher;

    public NotificationScheduler(
            PersonalAccountManager accountManager,
            AccountStatRepository accountStatRepository,
            AccountServiceRepository accountServiceRepository, ApplicationEventPublisher publisher
    ) {
        this.accountManager = accountManager;
        this.accountStatRepository = accountStatRepository;
        this.accountServiceRepository = accountServiceRepository;
        this.publisher = publisher;
    }

    //Выполняем отправку писем отключенным аккаунтам в 03:00:00 каждый день
    @SchedulerLock(name = "processAccountDeactivatedSendMail")
    public void processAccountDeactivatedSendMail() {
        log.info("Started processAccountDeactivatedSendMail");
        List<String> personalAccountIds = accountManager.findAccountIdsByActiveAndNotDeleted(false);
        personalAccountIds.forEach(accountId -> publisher.publishEvent(new AccountDeactivatedReSendMailEvent(accountId)));
        log.info("Ended processAccountDeactivatedSendMail");
    }

    //Для неактивных аккаунтов отправляем письма для возврата клиентов
    @SchedulerLock(name = "processNotifyInactiveLongTime")
    public  void processNotifyInactiveLongTime() {
        log.info("Started processNotifyInactiveLongTime");
        List<String> personalAccountIds = accountManager.findAccountIdsByActiveAndDeactivatedAfterAndNotDeleted(false, LocalDateTime.now().minusMonths(13));
        personalAccountIds.forEach(accountId -> publisher.publishEvent(new AccountNotifyInactiveLongTimeEvent(accountId)));
        log.info("Ended processNotifyInactiveLongTime");
    }

    //Информационная рассылка
    @SchedulerLock(name = "processSendInfoMail")
    public  void processSendInfoMail() {
        log.info("Started processSendInfoMail");
        List<String> personalAccountIds = accountManager.findAccountIdsByActiveAndNotificationsInAndNotDeleted(MailManagerMessageType.EMAIL_NEWS);
        personalAccountIds.forEach(accountId -> publisher.publishEvent(new AccountSendInfoMailEvent(accountId)));
        log.info("Ended processSendInfoMail");
    }

    @SchedulerLock(name = "processAccountDeactivatedSendSms")
    public void processAccountDeactivatedSendSms() {
        log.info("Started processAccountDeactivatedSendSms");
        List<AccountStat> accountStats = accountStatRepository.findByTypeAndCreatedAfterOrderByCreatedDesc(
                AccountStatType.VIRTUAL_HOSTING_ACC_OFF_NOT_ENOUGH_MONEY,
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
        );
        accountStats.forEach(accountStat -> publisher.publishEvent(new AccountDeactivatedSendSmsEvent(accountStat.getPersonalAccountId())));
        log.info("Ended processAccountDeactivatedSendSms");
    }

    //Регулярное письмо для клиентов ХМС без абонементов для тарифов
    @SchedulerLock(name = "processAccountNoAbonementSendMail")
    public void processAccountNoAbonementSendMail() {
        log.info("Started processAccountNoAbonementSendMail");
        List<String> noAbonementPlanIds = Stream.of(SERVICE_ID_PLAN_UNLIMITED_DAILY_CHARGES, SERVICE_ID_PLAN_UNLIMITED_PLUS_DAILY_CHARGES, SERVICE_ID_PLAN_BUSINESS_DAILY_CHARGES, SERVICE_ID_PLAN_BUSINESS_PLUS_DAILY_CHARGES).collect(Collectors.toList());

        List<String> activeAccounts = accountManager.findByActive(true).stream().map(PersonalAccount::getAccountId).collect(Collectors.toList());

        noAbonementPlanIds.forEach((planId) -> {
            Map<String, BigDecimal> accountsIdsAndCostNoAbonement = activeAccounts.stream().map(account -> accountServiceRepository.findOneByPersonalAccountIdAndServiceId(account, planId)).filter(Objects::nonNull).collect(Collectors.toMap(AccountService::getPersonalAccountId, AccountService::getCost));
            accountsIdsAndCostNoAbonement.forEach((accountId, cost) -> {
                publisher.publishEvent(new AccountNoAbonementSendMailEvent(accountId, cost.intValue()));
            });
        });
        log.info("Ended processAccountNoAbonementSendMail");
    }
}
