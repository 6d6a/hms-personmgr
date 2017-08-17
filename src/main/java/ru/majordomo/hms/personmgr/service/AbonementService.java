package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;

import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.event.account.AccountSendEmailWithExpiredAbonementEvent;
import ru.majordomo.hms.personmgr.event.account.AccountSetSettingEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.event.mailManager.SendMailEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.rc.user.resources.Domain;

import static java.time.temporal.ChronoUnit.DAYS;
import static ru.majordomo.hms.personmgr.common.AccountStatType.*;
import static ru.majordomo.hms.personmgr.common.Constants.DAYS_FOR_ABONEMENT_EXPIRED_MESSAGE_SEND;
import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;
import static ru.majordomo.hms.personmgr.common.Utils.formatBigDecimalWithCurrency;

@Service
public class AbonementService {
    private final static Logger logger = LoggerFactory.getLogger(AbonementService.class);

    private final PlanRepository planRepository;
    private final AccountAbonementManager accountAbonementManager;
    private final AccountHelper accountHelper;
    private final AccountServiceHelper accountServiceHelper;
    private final ApplicationEventPublisher publisher;
    private final AccountStatHelper accountStatHelper;
    private final FinFeignClient finFeignClient;
    private final AccountNotificationHelper accountNotificationHelper;

    private static TemporalAdjuster FOURTEEN_DAYS_AFTER = TemporalAdjusters.ofDateAdjuster(date -> date.plusDays(14));

    @Autowired
    public AbonementService(
            PlanRepository planRepository,
            AccountAbonementManager accountAbonementManager,
            AccountHelper accountHelper,
            AccountServiceHelper accountServiceHelper,
            ApplicationEventPublisher publisher,
            AccountStatHelper accountStatHelper,
            FinFeignClient finFeignClient,
            AccountNotificationHelper accountNotificationHelper
    ) {
        this.planRepository = planRepository;
        this.accountAbonementManager = accountAbonementManager;
        this.accountHelper = accountHelper;
        this.accountServiceHelper = accountServiceHelper;
        this.publisher = publisher;
        this.accountStatHelper = accountStatHelper;
        this.finFeignClient = finFeignClient;
        this.accountNotificationHelper = accountNotificationHelper;
    }

    /**
     * Покупка абонемента
     *
     * @param account Аккаунт
     * @param abonementId id абонемента
     * @param autorenew автопродление абонемента
     */
    public void addAbonement(PersonalAccount account, String abonementId, Boolean autorenew) {
        Plan plan = getAccountPlan(account);

        Boolean accountHasFree14DaysAbonement = false;

        AccountAbonement currentAccountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());

        if (currentAccountAbonement != null) {
            accountHasFree14DaysAbonement = currentAccountAbonement.getAbonement().getPeriod().equals("P14D");
        }

        Abonement abonement = checkAbonementAllownes(account, plan, abonementId, accountHasFree14DaysAbonement);

        accountHelper.charge(account, abonement.getService());

        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setAbonementId(abonementId);
        accountAbonement.setPersonalAccountId(account.getId());
        accountAbonement.setCreated(LocalDateTime.now());
        if (accountHasFree14DaysAbonement) {
            accountAbonementManager.delete(currentAccountAbonement);
        }
        accountAbonement.setExpired(LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
        accountAbonement.setAutorenew(autorenew);

        accountAbonementManager.insert(accountAbonement);

        if (accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
        }
    }

    /**
     * Продление абонемента
     *
     * @param account Аккаунт
     * @param accountAbonement абонемент аккаунта
     */
    public void renewAbonement(PersonalAccount account, AccountAbonement accountAbonement) {
        Plan plan = getAccountPlan(account);

        accountHelper.charge(account, accountAbonement.getAbonement().getService());

        accountAbonementManager.setExpired(
                accountAbonement.getId(),
                LocalDateTime.now()
                        .plus(Period.parse(accountAbonement.getAbonement().getPeriod()))
        );

        if (accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, plan.getServiceId());
        }
    }

    /**
     * Удаление абонемента
     *
     * @param account Аккаунт
     * @param accountAbonementId id абонемента на аккаунте
     */
    public void deleteAbonement(PersonalAccount account, String accountAbonementId) {
        AccountAbonement accountAbonement = accountAbonementManager.findByIdAndPersonalAccountId(accountAbonementId, account.getId());

        processAccountAbonementDelete(account, accountAbonement, VIRTUAL_HOSTING_USER_DELETE_ABONEMENT);
    }

    public void processExpiringAbonementsByAccount(PersonalAccount account) {
        //В итоге нам нужно получить абонементы которые заканчиваются через 14 дней и раньше
        LocalDateTime expireEnd = LocalDateTime.now().with(FOURTEEN_DAYS_AFTER);

        logger.debug("Trying to find all expiring abonements before expireEnd: "
                + expireEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<AccountAbonement> accountAbonements  = accountAbonementManager.findByPersonalAccountIdAndExpiredBefore(account.getId(), expireEnd);

        //Ну вообще-то должен быть только один Абонемент)
        accountAbonements.forEach(accountAbonement -> {

            BigDecimal abonementCost = accountAbonement.getAbonement().getService().getCost();

            if (accountAbonements.isEmpty()) {
                logger.debug("Not found expiring abonements for accountId: " + account.getId());
            }

            logger.debug("We found expiring abonement: " + accountAbonement);

            BigDecimal balance = accountHelper.getBalance(account);

            Boolean sendMessage = false;

            // Высчитываем предполагаемую месячную стоимость аккаунта
            Integer daysInCurrentMonth =  LocalDateTime.now().toLocalDate().lengthOfMonth();
            Plan plan = planRepository.findOne(account.getPlanId());
            PaymentService planAccountService = plan.getService();
            BigDecimal monthCost = planAccountService.getCost();

            List<AccountService> accountServices = account.getServices();
            for (AccountService accountService : accountServices) {
                if (accountService.isEnabled() && accountService.getPaymentService() != null
                        && !accountService.getPaymentService().getId().equals(planAccountService.getId())) {
                    switch (accountService.getPaymentService().getPaymentType()) {
                        case MONTH:
                            monthCost = monthCost.add(accountService.getCost());
                            break;
                        case DAY:
                            monthCost = monthCost.add(accountService.getCost().multiply(BigDecimal.valueOf(daysInCurrentMonth)));
                            break;
                    }
                }
            }

            if (!plan.isAbonementOnly() && balance.compareTo(monthCost) < 0) {
                if (balance.compareTo(abonementCost) < 0) {
                    sendMessage = true;
                }
            } else if (plan.isAbonementOnly() && balance.compareTo(abonementCost) < 0) {
                sendMessage = true;
            }

            long daysToExpired = DAYS.between(LocalDateTime.now(), accountAbonement.getExpired());

            if (sendMessage && Arrays.asList(DAYS_FOR_ABONEMENT_EXPIRED_MESSAGE_SEND).contains(daysToExpired)) {
                logger.debug("Account balance is too low to buy new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                List<Domain> domains = accountHelper.getDomains(account);
                List<String> domainNames = new ArrayList<>();
                for (Domain domain: domains) {
                    domainNames.add(domain.getName());
                }

                //Отправим письмо
                HashMap<String, String> parameters = new HashMap<>();
                parameters.put("client_id", account.getAccountId());
                parameters.put("acc_id", account.getName());
                parameters.put("domains", String.join("<br>", domainNames));
                parameters.put("balance", formatBigDecimalWithCurrency(balance));
                parameters.put("cost", formatBigDecimalWithCurrency(abonementCost)); //Этот параметр передаётся, но не используется
                parameters.put("date_finish", accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                parameters.put("auto_renew", "включено");
                parameters.put("from", "noreply@majordomo.ru");

                accountNotificationHelper.sendMail(account, "MajordomoVHAbNoMoneyProlong", 1, parameters);
            }
        });
    }

    public void processAbonementsAutoRenewByAccount(PersonalAccount account) {
        //В итоге нам нужно получить абонементы которые закончились сегодня и раньше
        LocalDateTime expireEnd = LocalDateTime.now();

        logger.debug("Trying to find all expired abonements before expireEnd: "
                + expireEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );

        List<AccountAbonement> accountAbonements  = accountAbonementManager.findByPersonalAccountIdAndExpiredBefore(account.getId(), expireEnd);

        if (accountAbonements.isEmpty()) {
            logger.debug("Not found expired abonements for accountId: " + account.getId());
        }

        //Ну вообще-то должен быть только один Абонемент)
        accountAbonements.forEach(accountAbonement -> {
            logger.debug("We found expired abonement: " + accountAbonement);

            BigDecimal balance = accountHelper.getBalance(account);
            BigDecimal abonementCost = accountAbonement.getAbonement().getService().getCost();
            String currentExpired = accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            // Если абонемент не бонусный (internal)
            if (!accountAbonement.getAbonement().isInternal()) {
                    logger.debug("Abonement has autorenew option enabled");

                    if (balance.compareTo(abonementCost) >= 0) {
                        logger.debug("Buying new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                        renewAbonement(account, accountAbonement);

                        //Запишем инфу о произведенном продлении в историю клиента
                        Map<String, String> params = new HashMap<>();
                        params.put(HISTORY_MESSAGE_KEY, "Автоматическое продление абонемента. Со счета аккаунта списано " +
                                formatBigDecimalWithCurrency(abonementCost) +
                                " Новый срок окончания: " + accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        );
                        params.put(OPERATOR_KEY, "service");

                        publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));
                    } else {
                        logger.debug("Account balance is too low to buy new abonement. Balance: " + balance + " abonementCost: " + abonementCost);

                        //Удаляем абонемент и включаем услуги хостинга по тарифу
                        processAccountAbonementDelete(account, accountAbonement);

                        //Запишем в историю клиента
                        Map<String, String> params = new HashMap<>();
                        params.put(HISTORY_MESSAGE_KEY, "Абонемент удален, так как средств на счету аккаунта не достаточно. Стоимость абонемента: " +
                                formatBigDecimalWithCurrency(abonementCost) +
                                " Баланс: " + formatBigDecimalWithCurrency(balance) +
                                " Дата окончания: " + currentExpired
                        );
                        params.put(OPERATOR_KEY, "service");

                        publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));

                        //Сохраним "на будущее" установку автопокупки абонемента
                        publisher.publishEvent(new AccountSetSettingEvent(account, AccountSetting.ABONEMENT_AUTO_RENEW, true));
                    }
            } else {
                // Если абонемент бонусный (internal)

                //Удаляем абонемент и включаем услуги хостинга по тарифу
                processAccountAbonementDelete(account, accountAbonement);

                //Запишем в историю клиента
                Map<String, String> params = new HashMap<>();
                params.put(HISTORY_MESSAGE_KEY, "Бонусный абонемент удален. Обычный абонемент не был предзаказн. Дата окончания: " + currentExpired);
                params.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));

            }
        });
    }

    private Plan getAccountPlan(PersonalAccount account) {
        Plan plan = planRepository.findOne(account.getPlanId());

        if(plan == null){
            throw new ResourceNotFoundException("Account plan not found");
        }

        return plan;
    }

    private Abonement checkAbonementAllownes(PersonalAccount account, Plan plan, String abonementId, Boolean accountHasFree14DaysAbonement) {

        if (!plan.getAbonementIds().contains(abonementId)) {
            throw new ParameterValidationException("Current account plan does not have abonement with specified abonementId");
        }

        Optional<Abonement> newAbonement = plan.getAbonements().stream().filter(abonement1 -> abonement1.getId().equals(abonementId)).findFirst();

        if (!newAbonement.isPresent()) {
            throw new ParameterValidationException("Current account plan does not have abonement with specified abonementId (not found in abonements)");
        }

        Abonement abonement = newAbonement.get();

        AccountAbonement accountAbonement = accountAbonementManager.findByPersonalAccountId(account.getId());

        if (accountAbonement != null && !accountHasFree14DaysAbonement) {
            throw new ParameterValidationException("Account already has abonement");
        }

        return abonement;
    }

    /**
     * Удаление абонемента
     *
     * @param account Аккаунт
     * @param accountAbonement абонемента на аккаунте
     */
    private void processAccountAbonementDelete(PersonalAccount account, AccountAbonement accountAbonement, AccountStatType reason) {

        accountAbonementManager.delete(accountAbonement);

        Plan plan = planRepository.findOne(account.getPlanId());
        boolean needToSendMail = false;
        if (!plan.isAbonementOnly()) {
            BigDecimal balance = accountHelper.getBalance(account);
            BigDecimal costForOneMonth = plan.getService().getCost();
            needToSendMail = balance.compareTo(costForOneMonth) < 0;
        } else {
            needToSendMail = true;
        }
        if (needToSendMail) {
            publisher.publishEvent(new AccountSendEmailWithExpiredAbonementEvent(account));
        }

        Map<String, String> data = new HashMap<>();
        data.put("expireEnd", accountAbonement.getExpired().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        data.put("abonementId", accountAbonement.getAbonementId());
        accountStatHelper.add(account, reason, data);

        //Создаем AccountService с выбранным тарифом
        addPlanServicesAfterAbonementExpire(account);

        if (planRepository.findOne(account.getPlanId()).isAbonementOnly()) {
            accountHelper.switchAccountResources(account, false);
        }
    }

    private void processAccountAbonementDelete(PersonalAccount account, AccountAbonement accountAbonement) {
        this.processAccountAbonementDelete(account, accountAbonement, VIRTUAL_HOSTING_ABONEMENT_DELETE);
    }

    /**
     * Добавление(проверка наличия) услуг хостинга по тарифу после окончания абонемента
     *
     * @param account Аккаунт
     */
    private void addPlanServicesAfterAbonementExpire(PersonalAccount account) {
        Plan plan = getAccountPlan(account);

        if (!accountServiceHelper.accountHasService(account, plan.getServiceId())) {
            accountServiceHelper.addAccountService(account, plan.getServiceId());
        }
    }

    public void addFree14DaysAbonement(PersonalAccount account) {
        Plan plan = planRepository.findOne(account.getPlanId());
        Abonement abonement = plan.getFree14DaysAbonement();

        if (abonement != null) {
            addAbonement(account, abonement.getId(), false);

            //Save history
            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "Добавлен абонемент на тестовый период (14 дней)");
            params.put(OPERATOR_KEY, "service");

            publisher.publishEvent(new AccountHistoryEvent(account.getId(), params));
        }
    }
}
