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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.event.account.AccountNotifySupportOnChangePlanEvent;
import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.AccountStat;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanChangeAgreement;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.repository.AccountStatRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

import static java.lang.Math.floor;
import static java.time.temporal.ChronoUnit.DAYS;
import static ru.majordomo.hms.personmgr.common.AccountSetting.CREDIT_ACTIVATION_DATE;
import static ru.majordomo.hms.personmgr.common.Constants.*;
import static ru.majordomo.hms.personmgr.common.Utils.planChangeComparator;

@Service
public class PlanChangeService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FinFeignClient finFeignClient;
    private final PlanRepository planRepository;
    private final AccountAbonementRepository accountAbonementRepository;
    private final AccountStatRepository accountStatRepository;
    private final AccountHistoryService accountHistoryService;
    private final PersonalAccountRepository personalAccountRepository;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountCountersService accountCountersService;
    private final PlanLimitsService planLimitsService;
    private final AccountQuotaService accountQuotaService;
    private final AccountServiceHelper accountServiceHelper;
    private final AccountHelper accountHelper;
    private final ApplicationEventPublisher publisher;
    private final AbonementService abonementService;

    @Autowired
    public PlanChangeService(
            FinFeignClient finFeignClient,
            PlanRepository planRepository,
            AccountAbonementRepository accountAbonementRepository,
            AccountStatRepository accountStatRepository,
            AccountHistoryService accountHistoryService,
            PersonalAccountRepository personalAccountRepository,
            PaymentServiceRepository paymentServiceRepository,
            AccountCountersService accountCountersService,
            PlanLimitsService planLimitsService,
            AccountQuotaService accountQuotaService,
            AccountServiceHelper accountServiceHelper,
            AccountHelper accountHelper,
            ApplicationEventPublisher publisher,
            AbonementService abonementService
    ) {
        this.finFeignClient = finFeignClient;
        this.planRepository = planRepository;
        this.accountAbonementRepository = accountAbonementRepository;
        this.accountStatRepository = accountStatRepository;
        this.accountHistoryService = accountHistoryService;
        this.personalAccountRepository = personalAccountRepository;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountCountersService = accountCountersService;
        this.planLimitsService = planLimitsService;
        this.accountQuotaService = accountQuotaService;
        this.accountServiceHelper = accountServiceHelper;
        this.accountHelper = accountHelper;
        this.publisher = publisher;
        this.abonementService = abonementService;
    }

    /**
     * Изменение тарифного плана
     *
     * @param account   Аккаунт
     * @param newPlanId ID нового тарифа
     */
    public PlanChangeAgreement changePlan(PersonalAccount account, String newPlanId, PlanChangeAgreement requestAgreement) {

        PlanChangeAgreement planChangeAgreement = new PlanChangeAgreement();
        planChangeAgreement.setBalance(accountHelper.getBalance(account));
        planChangeAgreement.setDelta(BigDecimal.ZERO);
        planChangeAgreement.setNeedToFeelBalance(BigDecimal.ZERO);
        planChangeAgreement.setBalanceChanges(false);

        String currentPlanId = account.getPlanId();

        Plan currentPlan = planRepository.findOne(currentPlanId);

        if (currentPlan == null) {
            throw new ResourceNotFoundException("Текущий тарифный план не найден");
        }

        Plan newPlan = planRepository.findOne(newPlanId);

        if (newPlan == null) {
            throw new ParameterValidationException("Новый тарифный план не найден");
        }

        if (currentPlanId.equals(newPlanId)) {
            throw new ParameterValidationException("Текущий тарифный план совпадает с выбранным");
        }

        AccountAbonement accountAbonement = accountAbonementRepository.findByPersonalAccountIdAndPreordered(account.getId(), false);
        AccountAbonement preorderdAccountAbonement = accountAbonementRepository.findByPersonalAccountIdAndPreordered(account.getId(), true);

        Boolean accountHasFree14DaysAbonement = false;

        if (accountAbonement != null) {
            accountHasFree14DaysAbonement = accountAbonement.getAbonement().getPeriod().equals("P14D");
        }

        BigDecimal balance = accountHelper.getBalance(account);

        //Проверим, можно ли менять тариф
        canChangePlan(account, currentPlan, newPlan, accountHasFree14DaysAbonement);

        // Если текущий тариф НЕ isAbonementOnly (например "Парковка") и имеет абонемент - необходимо сменить тариф с пересчётом баланса
        // И абонемент не бесплатный на 14 дней
        if (accountAbonement != null && !currentPlan.isAbonementOnly() && !accountHasFree14DaysAbonement) {
            if (newPlan.getService().getCost().compareTo(currentPlan.getService().getCost()) < 0) {
                throw new ParameterValidationException("Переход на тарифный план с меньшей стоимостью при активном абонементе невозможен");
            }

            //Только перерасчёт и валидация без сохранения
            planChangeAgreement = this.calculateDeclineAbonementValues(account, planChangeAgreement);
            BigDecimal newBalanceAfterDecline = balance.add(planChangeAgreement.getDelta());

            if (newBalanceAfterDecline.compareTo(newPlan.getNotInternalAbonement().getService().getCost()) < 0) { // Денег на новый абонемент не хватает
                planChangeAgreement.setNeedToFeelBalance(newPlan.getNotInternalAbonement().getService().getCost().subtract(newBalanceAfterDecline));

                if (requestAgreement != null) {
                    throw new ParameterValidationException("Недостаточно средств для смены тарифного плана при активном абонементе");
                }
            }
        } else if (newPlan.isAbonementOnly()) {
            //Необходимо проверить что чуваку хватает денег на покупку абонемента нового тарифа
            //Только перерасчёт и валидация без сохранения
            planChangeAgreement.setBalanceChanges(true);
            if (balance.compareTo(newPlan.getNotInternalAbonement().getService().getCost()) < 0) {
                planChangeAgreement.setNeedToFeelBalance(newPlan.getNotInternalAbonement().getService().getCost().subtract(balance));

                if (requestAgreement != null) {
                    throw new ParameterValidationException("Недостаточно средств для покупки абонемента на выбранном тарифе");
                }
            }
        }

        if (requestAgreement != null) {

            if (!planChangeAgreement.equals(requestAgreement)) {
                logger.error("planChangeAgreements are not equals. What we got: " + requestAgreement.toString() + " What we expected (newly calculated): " + planChangeAgreement.toString());
                throw new ParameterValidationException("Произошла ошибка");
            }

            if (preorderdAccountAbonement != null) {
                accountAbonementRepository.delete(preorderdAccountAbonement.getId());
            }

            // Процессим абонементы (списываем деньги\начисляем деньги\покупаем абонементы\удаляем абонементы)
            if (accountAbonement != null) {
                if (!accountHasFree14DaysAbonement) {
                    processNotAbonementOnlyPlans(account, currentPlan, newPlan, planChangeAgreement);
                } else {
                    replaceFree14DaysAbonement(account, currentPlan, newPlan, accountAbonement.getExpired());
                }
            }
            processAbonementOnlyPlans(account, currentPlan, newPlan);

            //Произведем нужные действия со всеми услугами
            processServices(account, currentPlan, newPlan);

            //Укажем новый тариф
            account.setPlanId(newPlan.getId());

            if (newPlan.isAbonementOnly()) {
                if (account.isCredit()) {
                    account.removeSettingByName(CREDIT_ACTIVATION_DATE);
                    account.setCredit(false);

                    //Запишем в историю клиента
                    Map<String, String> historyParams = new HashMap<>();
                    historyParams.put(HISTORY_MESSAGE_KEY, "Для аккаунта отключен кредит в связи с переходом на тариф с обязательным абонементом");
                    historyParams.put(OPERATOR_KEY, "service");

                    publisher.publishEvent(new AccountHistoryEvent(account.getId(), historyParams));
                }
            }

            if (!newPlan.isSslCertificateAllowed()) {

                accountHelper.disableAllSslCertificates(account);

                //Запишем в историю клиента
                Map<String, String> historyParams = new HashMap<>();
                historyParams.put(HISTORY_MESSAGE_KEY, "Для аккаунта отключны SSL сертификаты в соответствии с тарифным планом");
                historyParams.put(OPERATOR_KEY, "service");

                publisher.publishEvent(new AccountHistoryEvent(account.getId(), historyParams));
            }

            personalAccountRepository.save(account);

            if (isFromRegularToBusiness(currentPlan, newPlan)) {
                publisher.publishEvent(new AccountNotifySupportOnChangePlanEvent(account));
            }

            //Сохраним статистику смены тарифа
            saveStat(account, newPlanId);

            //Сохраним историю аккаунта
            saveHistory(account, currentPlan, newPlan);
        }

        return planChangeAgreement;
    }

    // Для тарифов, которые НЕ isAbonementOnly
    private PlanChangeAgreement calculateDeclineAbonementValues(PersonalAccount account, PlanChangeAgreement planChangeAgreement) {
        AccountAbonement accountAbonement = accountAbonementRepository.findByPersonalAccountIdAndPreordered(account.getId(), false);

        if (planRepository.findOne(account.getPlanId()).isAbonementOnly()) {
            throw new ParameterValidationException("Смена тарифного плана невозможна");
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal delta;
        BigDecimal currentPlanCost = planRepository.findOne(account.getPlanId()).getService().getCost();

        if (accountAbonement.getExpired() != null) {
            LocalDateTime nextDate = accountAbonement.getExpired().minus(Period.parse(accountAbonement.getAbonement().getPeriod())); // первая дата для начала пересчета АБ
            LocalDateTime stopDate = LocalDateTime.now(); // дата окончания пересчета абонемента
            while (stopDate.isAfter(nextDate)) {
                Integer daysInMonth = nextDate.toLocalDate().lengthOfMonth();
                total = total.add(currentPlanCost.divide(BigDecimal.valueOf(daysInMonth), 4, BigDecimal.ROUND_HALF_UP));
                nextDate = nextDate.plusDays(1L);
            }
        } else {
            throw new ParameterValidationException("Абонемент не активирован");
        }

        delta = (accountAbonement.getAbonement().getService().getCost()).subtract(total);

        // delta может быть как отрицательной (будет списано), так и положительной (будет начислено)
        planChangeAgreement.setDelta(delta);
        planChangeAgreement.setBalanceChanges(true);
        
        return planChangeAgreement;
    }

    /**
     * Проверим было ли изменение тарифного плана за последний месяц
     *
     * @param account Аккаунт
     */
    private void checkLastMonthPlanChange(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        List<AccountStat> accountStats = accountStatRepository.findByPersonalAccountIdAndTypeAndCreatedAfter(
                account.getId(),
                AccountStatType.VIRTUAL_HOSTING_PLAN_CHANGE,
                LocalDateTime.now().minusMonths(1)
        );

        if (accountStats != null && !accountStats.isEmpty()) {
            if (currentPlan.getService().getCost().compareTo(newPlan.getService().getCost()) > 0) {
                throw new ParameterValidationException("Смена тарифного плана на меньший по стоимости возможна не чаще 1 раза в месяц");
            }

        }
    }

    /**
     * Сохраним в статистику об изменении тарифного плана
     *
     * @param account   Аккаунт
     * @param newPlanId ID нового тарифа
     */
    private void saveStat(PersonalAccount account, String newPlanId) {
        AccountStat accountStat = new AccountStat();
        accountStat.setPersonalAccountId(account.getId());
        accountStat.setCreated(LocalDateTime.now());
        accountStat.setType(AccountStatType.VIRTUAL_HOSTING_PLAN_CHANGE);

        Map<String, String> data = new HashMap<>();
        data.put("oldPlanId", account.getPlanId());
        data.put("newPlanId", newPlanId);

        accountStat.setData(data);

        accountStatRepository.save(accountStat);
    }

    /**
     * Сохраним в историю запись об изменении тарифного плана
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void saveHistory(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        accountHistoryService.addMessage(account.getId(), "Произведена смена тарифа с " + currentPlan.getName() + " на " + newPlan.getName(), "operator");
    }


    /**
     * Работа с услугами привязанными к аккаунту
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void replacePlanService(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        accountServiceHelper.replaceAccountService(account, currentPlan.getServiceId(), newPlan.getServiceId());
    }

    private void deletePlanService(PersonalAccount account, Plan currentPlan) {
        accountServiceHelper.deleteAccountServiceById(account, currentPlan.getServiceId());
    }

    /**
     * Может ли быть произведена смена тарифа
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void canChangePlan(PersonalAccount account, Plan currentPlan, Plan newPlan, Boolean accountHaveFree14DaysAbonement) {

        if (!newPlan.isActive()) {
            throw new ParameterValidationException("Переход на данный тарифный план невозможен");
        }

        if (!accountHaveFree14DaysAbonement) {

            //Проверим не менялся ли тариф в последний месяц
            checkLastMonthPlanChange(account, currentPlan, newPlan);

            //Проверим баланс
            accountHelper.checkBalance(account);

            //Проверим возможность перехода с бизнес тарифа
            checkBusinessPlan(currentPlan, newPlan);

            //Проверка на активные бонусные абонементы
            checkBonusAbonements(account);

        }

        //Проверим лимиты нового тарифа
        checkAccountLimits(account, newPlan);
    }

    /**
     * Проверка наличия бонусных абонементов
     *
     * @param account Аккаунт
     */
    private void checkBonusAbonements(PersonalAccount account) {
        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountId(account.getId());
        for (AccountAbonement accountAbonement :accountAbonements) {
            if (accountAbonement.getAbonement().isInternal()) {
                throw new ParameterValidationException("Для смены тарифного плана вам необходимо приобрести абонемент на " +
                        "текущий тарифный план сроком на 1 год или дождаться окончания бесплатного абонемента");
            }
        }
    }

    /**
     * Произведем нужные действия с абонементами
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void processAbonementOnlyPlans(PersonalAccount account, Plan currentPlan, Plan newPlan) {

        if (currentPlan.isAbonementOnly()) {
            //Если старый тариф был только абонементным, то нужно удалить абонемент и вернуть неизрасходованные средства
            processCurrentAccountAbonement(account, currentPlan);
        }

        if (newPlan.isAbonementOnly()) {
            //Если новый тариф только абонементный, то нужно сразу купить абонемент и списать средства
            processNewAccountAbonement(account, newPlan);
        }
    }

    private void processNotAbonementOnlyPlans(PersonalAccount account, Plan currentPlan, Plan newPlan, PlanChangeAgreement planChangeAgreement) {

        if (!currentPlan.isAbonementOnly()) {
            //Начислить деньги
            if (planChangeAgreement.getDelta().compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> payment = new HashMap<>();
                payment.put("accountId", account.getName());
                payment.put("paymentTypeId", BONUS_PAYMENT_TYPE_ID);
                payment.put("amount", planChangeAgreement.getDelta());
                payment.put("message", "Возврат средств при отказе от абонемента при смене тарифного плана");

                try {
                    finFeignClient.addPayment(payment);
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Exception in ru.majordomo.hms.personmgr.service.PlanChangeService.processNotAbonementOnlyPlans #1 " + e.getMessage());
                }
            }

            //Снять деньги
            if (planChangeAgreement.getDelta().compareTo(BigDecimal.ZERO) < 0) {
                accountHelper.charge(account, currentPlan.getService(), planChangeAgreement.getDelta().abs());
            }

            deleteAccountAbonement(account, currentPlan);
        }

        if (!currentPlan.isAbonementOnly() && !newPlan.isAbonementOnly()) {
            processNewAccountAbonement(account, newPlan);
        }
    }

    private void replaceFree14DaysAbonement(PersonalAccount account, Plan currentPlan, Plan newPlan, LocalDateTime expiredFree14DaysAbonementDate) {

        deleteAccount14DaysFreeAbonement(account, currentPlan);

        if (!newPlan.isAbonementOnly() && expiredFree14DaysAbonementDate.isAfter(LocalDateTime.now())) {
            Abonement abonement = newPlan.getFree14DaysAbonement();
            if (abonement != null) {
                AccountAbonement newFree14DaysAbonement = addAccountAbonement(account, abonement);
                newFree14DaysAbonement.setExpired(expiredFree14DaysAbonementDate);
                accountAbonementRepository.save(newFree14DaysAbonement);
            }
        }

    }

    /**
     * Добавляем при необходимости абонемент на тариф
     *
     * @param account   Аккаунт
     * @param currentPlan текущий тариф
     */
    private void processCurrentAccountAbonement(PersonalAccount account, Plan currentPlan) {
        if (currentPlan.isAbonementOnly()) {
            addRemainingAccountAbonementCost(account, currentPlan);

            deleteAccountAbonement(account, currentPlan);
        }
    }

    /**
     * Возвращаем неизрасходованные средства за старый абонемент
     * Только для тарифов, котороые isAbonementOnly
     *
     * @param account   Аккаунт
     * @param currentPlan текущий тариф
     */
    private void addRemainingAccountAbonementCost(PersonalAccount account, Plan currentPlan) {
        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountIdAndAbonementId(
                account.getId(),
                currentPlan.getNotInternalAbonementId()
        );

        if (!planRepository.findOne(account.getPlanId()).isAbonementOnly()) {
            throw new ParameterValidationException("Account plan is not abonement only");
        }

        if (accountAbonements != null && !accountAbonements.isEmpty()) {
            AccountAbonement accountAbonement = accountAbonements.get(0);
            Abonement abonement = accountAbonement.getAbonement();

            if (accountAbonement.getExpired().isAfter(LocalDateTime.now())) {
                long remainingDays = DAYS.between(LocalDateTime.now(), accountAbonement.getExpired());
                BigDecimal remainedServiceCost = (BigDecimal.valueOf(remainingDays)).multiply(abonement.getService().getCost().divide(BigDecimal.valueOf(365L), 2, BigDecimal.ROUND_DOWN));

                if (remainedServiceCost.compareTo(BigDecimal.ZERO) > 0) {
                    Map<String, Object> payment = new HashMap<>();
                    payment.put("accountId", account.getName());
                    payment.put("paymentTypeId", BONUS_PAYMENT_TYPE_ID);
                    payment.put("amount", remainedServiceCost);
                    payment.put("message", "Возврат неиспользованных средств при отказе от абонемента");

                    try {
                        finFeignClient.addPayment(payment);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("Exception in ru.majordomo.hms.personmgr.service.PlanChangeService.addRemainingAccountAbonementCost " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Удаляем старый абонемент
     *
     * @param account   Аккаунт
     * @param currentPlan текущий тариф
     */
    private void deleteAccountAbonement(PersonalAccount account, Plan currentPlan) {
        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountIdAndAbonementId(
                account.getId(),
                currentPlan.getNotInternalAbonementId()
        );

        if (accountAbonements != null && !accountAbonements.isEmpty()) {
            accountAbonementRepository.delete(accountAbonements);
        }
    }

    private void deleteAccount14DaysFreeAbonement(PersonalAccount account, Plan currentPlan) {
        List<AccountAbonement> accountAbonements = accountAbonementRepository.findByPersonalAccountIdAndAbonementId(
                account.getId(),
                currentPlan.getFree14DaysAbonement().getId()
        );

        if (accountAbonements != null && !accountAbonements.isEmpty()) {
            accountAbonementRepository.delete(accountAbonements);
        }
    }

    /**
     * Добавляем при необходимости абонемент на тариф
     *
     * @param account   Аккаунт
     * @param newPlan новый тариф
     */
    private void processNewAccountAbonement(PersonalAccount account, Plan newPlan) {
        Abonement abonement = newPlan.getNotInternalAbonement();
        addAccountAbonement(account, abonement);

        accountHelper.charge(account, abonement.getService());
    }

    /**
     * Добавляем абонемент на тариф
     *
     * @param account   Аккаунт
     * @param abonement новый абонемент
     */
    private AccountAbonement addAccountAbonement(PersonalAccount account, Abonement abonement) {
        AccountAbonement accountAbonement = new AccountAbonement();
        accountAbonement.setAbonementId(abonement.getId());
        accountAbonement.setPersonalAccountId(account.getId());
        accountAbonement.setCreated(LocalDateTime.now());
        accountAbonement.setExpired(LocalDateTime.now().plus(Period.parse(abonement.getPeriod())));
        accountAbonement.setAutorenew(false);
        accountAbonement.setPreordered(false);

        accountAbonementRepository.save(accountAbonement);

        return accountAbonement;
    }

    /**
     * Произвести все действия с услугами
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void processServices(PersonalAccount account, Plan currentPlan, Plan newPlan) {

        AccountAbonement accountAbonementAfterProcessing = accountAbonementRepository.findByPersonalAccountIdAndPreordered(account.getId(), false);

        //Если нет абонемента
        if (accountAbonementAfterProcessing == null) {
            //Удалим старую услугу тарифа и добавим новую
            replacePlanService(account, currentPlan, newPlan);
        } else {
            //Удалим старую услугу тарифа
            deletePlanService(account, currentPlan);
        }

        //Удалим старую услугу смс-уведомлений и добавим новую
        replaceSmsNotificationsService(account, currentPlan, newPlan);

        //Обработаем услуги Доп.FTP
        processFtpUserService(account, newPlan);

        //Обработаем услуги Доп.Сайт
        processWebSiteService(account, newPlan);

        //Обработаем услуги Доп.место
        accountQuotaService.processQuotaService(account, newPlan);
    }

    /**
     * Может ли быть произведена смена тарифа (с Бизнес можно только на Бизнес)
     *
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void checkBusinessPlan(Plan currentPlan, Plan newPlan) {
        VirtualHostingPlanProperties currentPlanProperties = (VirtualHostingPlanProperties) currentPlan.getPlanProperties();
        VirtualHostingPlanProperties newPlanProperties = (VirtualHostingPlanProperties) newPlan.getPlanProperties();
        if (currentPlanProperties.isBusinessServices() && !newPlanProperties.isBusinessServices()) {
            throw new ParameterValidationException("Вы можете выбрать только другой корпоративный тарифный план");
        }
    }

    /**
     * Является ли это переходом с обычного тарифа на бизнес
     *
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private boolean isFromRegularToBusiness(Plan currentPlan, Plan newPlan) {
        VirtualHostingPlanProperties currentPlanProperties = (VirtualHostingPlanProperties) currentPlan.getPlanProperties();
        VirtualHostingPlanProperties newPlanProperties = (VirtualHostingPlanProperties) newPlan.getPlanProperties();
        return !currentPlanProperties.isBusinessServices() && newPlanProperties.isBusinessServices();
    }

    /**
     * Проверить счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountLimits(PersonalAccount account, Plan newPlan) {
        //Database
        checkAccountDatabaseLimits(account, newPlan);

        //FtpUser
        checkAccountFtpUserLimits(account, newPlan);

        //WebSite
        checkAccountWebSiteLimits(account, newPlan);

        //Quota
        checkAccountQuotaLimits(account, newPlan);
    }

    /**
     * Проверить DB счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountDatabaseLimits(PersonalAccount account, Plan newPlan) {
        Long count = accountCountersService.getCurrentDatabaseCount(account.getId());
        Long freeLimit = planLimitsService.getDatabaseFreeLimit(newPlan);
        if (planChangeComparator(count, freeLimit) > 0) {
            throw new ParameterValidationException("Текущее количество баз данных больше, чем лимит на новом тарифном плане. " +
                    "Текущее количество: " + count + " Лимит: " + freeLimit);
        }
    }

    /**
     * Проверить FtpUser счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountFtpUserLimits(PersonalAccount account, Plan newPlan) {
        Long count = accountCountersService.getCurrentFtpUserCount(account.getId());
        Long freeLimit = planLimitsService.getFtpUserFreeLimit(newPlan);
        if (planChangeComparator(count, freeLimit) > 0) {
            throw new ParameterValidationException("Текущее количество FTP-пользователей больше, чем лимит на новом тарифном плане. "  +
                    "Текущее количество: " + count + " Лимит: " + freeLimit);
        }
    }

    /**
     * Проверить WebSite счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountWebSiteLimits(PersonalAccount account, Plan newPlan) {
        Long count = accountCountersService.getCurrentWebSiteCount(account.getId());
        Long freeLimit = planLimitsService.getWebsiteFreeLimit(newPlan);
        if (planChangeComparator(count, freeLimit) > 0) {
            throw new ParameterValidationException("Текущее количество сайтов больше, чем лимит на новом тарифном плане. "  +
                    "Текущее количество: " + count + " Лимит: " + freeLimit);
        }
    }

    /**
     * Проверить Quota счетчики аккаунта и соответствие их новому тарифу
     *
     * @param account Аккаунт
     * @param newPlan новый тариф
     */
    private void checkAccountQuotaLimits(PersonalAccount account, Plan newPlan) {
        Long count = accountCountersService.getCurrentQuotaUsed(account.getId());
        Long freeLimit = planLimitsService.getQuotaKBFreeLimit(newPlan);
        if (planChangeComparator(count, freeLimit * 1024) > 0) {
            throw new ParameterValidationException("Использованная квота превышает лимит на новом тарифном плане. "  +
                    "Текущая квота: " + count + " Лимит: " + freeLimit);
        }
    }

    /**
     * Обновляем услугу смс-уведомлений (она могла быть на старом тарифе с другой стоимостью или бесплатной)
     *
     * @param account     Аккаунт
     * @param currentPlan текущий тариф
     * @param newPlan     новый тариф
     */
    private void replaceSmsNotificationsService(PersonalAccount account, Plan currentPlan, Plan newPlan) {
        if (currentPlan.getSmsServiceId() != null && newPlan.getSmsServiceId() != null) {
            accountServiceHelper.replaceAccountService(account, currentPlan.getSmsServiceId(), newPlan.getSmsServiceId());
        }
    }

    /**
     * Обрабатываем услуги Доп.FTP в соответствии с новым тарифом
     *
     * @param account     Аккаунт
     * @param newPlan     новый тариф
     */
    private void processFtpUserService(PersonalAccount account, Plan newPlan) {
        Long currentFtpUserCount = accountCountersService.getCurrentFtpUserCount(account.getId());
        Long planFtpUserFreeLimit = planLimitsService.getFtpUserFreeLimit(newPlan);

        String ftpServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_FTP_SERVICE_ID).getId();

        deleteOrAddAccountService(account, ftpServiceId, currentFtpUserCount, planFtpUserFreeLimit);
    }

    /**
     * Обрабатываем услуги Доп.Сайт в соответствии с новым тарифом
     *
     * @param account     Аккаунт
     * @param newPlan     новый тариф
     */
    private void processWebSiteService(PersonalAccount account, Plan newPlan) {
        Long currentWebSiteCount = accountCountersService.getCurrentWebSiteCount(account.getId());
        Long planWebSiteFreeLimit = planLimitsService.getWebsiteFreeLimit(newPlan);

        String webSiteServiceId = paymentServiceRepository.findByOldId(ADDITIONAL_WEB_SITE_SERVICE_ID).getId();

        deleteOrAddAccountService(account, webSiteServiceId, currentWebSiteCount, planWebSiteFreeLimit);
    }

    /**
     * Удаляем или добавляем услуги в зависимости от счетчиков
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     * @param currentCount текущее кол-во услуг
     * @param planFreeLimit бесплатно по тарифу
     */
    public void deleteOrAddAccountService(PersonalAccount account, String serviceId, Long currentCount, Long planFreeLimit) {
        if (planChangeComparator(currentCount, planFreeLimit) <= 0) {
            accountServiceHelper.deleteAccountServiceByServiceId(account, serviceId);
        } else {
            int notFreeServiceCount = (int) floor(currentCount - planFreeLimit);
            accountServiceHelper.addAccountService(account, serviceId, notFreeServiceCount);
        }
    }
}
