package ru.majordomo.hms.personmgr.service;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.AccountStatType;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.event.account.AccountCheckQuotaEvent;
import ru.majordomo.hms.personmgr.event.account.AntiSpamServiceSwitchEvent;
import ru.majordomo.hms.personmgr.event.account.DedicatedAppServiceDeleteEvent;
import ru.majordomo.hms.personmgr.event.account.RedirectWasDisabledEvent;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.model.service.RedirectAccountService;
import ru.majordomo.hms.personmgr.repository.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import static ru.majordomo.hms.personmgr.common.Constants.*;

@Service
@AllArgsConstructor
public class AccountServiceHelper {
    private final AccountServiceRepository accountServiceRepository;
    private final PlanManager planManager;
    private final PersonalAccountManager accountManager;
    private final PaymentServiceRepository serviceRepository;
    private final AbonementManager<AccountServiceAbonement> abonementManager;
    private final ServicePlanRepository servicePlanRepository;
    private final AccountPromotionManager accountPromotionManager;
    private final DiscountFactory discountFactory;
    private final ApplicationEventPublisher publisher;
    private final ResourceArchiveService resourceArchiveService;
    private final AccountHistoryManager history;
    private final AccountStatHelper accountStatHelper;
    private final AccountRedirectServiceRepository accountRedirectServiceRepository;
    private final PromocodeActionRepository promocodeActionRepository;

    public void deletePlanServiceIfExists(PersonalAccount account, Plan plan) {
        if (accountHasService(account, plan.getServiceId())) {
            deleteAccountServiceByServiceId(account, plan.getServiceId());
        }
    }

    /**
     * Удаляем старую услугу
     *
     * @param account   Аккаунт
     * @param serviceId id услуги ServiceId
     */
    public void deleteAccountServiceByServiceId(PersonalAccount account, String serviceId) {
        accountServiceRepository.deleteByPersonalAccountIdAndServiceId(account.getId(), serviceId);
    }

    /**
     * Удаляем старую услугу
     *
     * @param account   Аккаунт
     * @param accountServiceId id услуги AccountService
     */

    public void deleteAccountServiceById(@NonNull PersonalAccount account, String accountServiceId, boolean deleteOnly) throws ResourceNotFoundException {
        AccountService accountService = accountServiceRepository.findByPersonalAccountIdAndId(account.getId(), accountServiceId);
        if (accountService == null) {
            throw new ResourceNotFoundException("Не удалось найти услугу с Id: " + accountServiceId);
        }
        deleteAccountServiceById(account, accountService, deleteOnly);
    }

    public void deleteAccountServicesByFeature(@NonNull PersonalAccount account, Feature feature, boolean deleteOnly) {
        if (feature == null) {
            return;
        }
        ServicePlan servicePlan = servicePlanRepository.findOneByFeature(feature); // todo обработать ситуацию когда servicePlan много
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), servicePlan.getServiceId());
        for (AccountService accountService : accountServices) {
            deleteAccountServiceById(account, accountService, deleteOnly);
        }
    }

    /**
     * Удаляем старую услугу
     *
     * @param account   Аккаунт
     * @param accountService
     */
    public void deleteAccountServiceById(@NotNull PersonalAccount account, @NotNull AccountService accountService, boolean deleteOnly) {
        if (!Objects.equals(accountService.getPersonalAccountId(), account.getId())) {
            throw new ParameterValidationException(String.format("Сервис %s не принадлежит аккаунту %s", accountService.getName(), account.getName()));
        }
        accountServiceRepository.deleteByPersonalAccountIdAndId(account.getId(), accountService.getId());
        if (deleteOnly) {
            return;
        }
        Feature feature = findFeature(accountService.getServiceId());
        if (feature == Feature.DEDICATED_APP_SERVICE) {
            publisher.publishEvent(new DedicatedAppServiceDeleteEvent(account.getId(), accountService.getId()));
        }
    }

    /**
     * Добавляем новую услугу
     *
     * @param account   Аккаунт
     * @param newPaymentServiceId PaymentService id новой услуги
     */
    public AccountService addAccountService(PersonalAccount account, String newPaymentServiceId, @Nullable String comment) {
        return addAccountService(account, newPaymentServiceId, 1, comment);
    }

    public AccountService save(AccountService accountService) {
        return accountServiceRepository.save(accountService);
    }

    /**
     * Добавляем новую услугу
     *
     * @param account   Аккаунт
     * @param newPaymentServiceId PaymentService id новой услуги
     */
    public AccountService addAccountService(PersonalAccount account, String newPaymentServiceId) {
        return addAccountService(account, newPaymentServiceId, 1, null);
    }

    public boolean hasAccountService(String accountServiceId) {
        return accountServiceRepository.existsById(accountServiceId);
    }

    /**
     * Добавляем новую услугу
     *
     * @param account   Аккаунт
     * @param newPaymentServiceId PaymentService id новой услуги
     * @param quantity кол-во услуг
     */
    private AccountService addAccountService(PersonalAccount account, String newPaymentServiceId, int quantity, @Nullable String comment) {
        AccountService service = new AccountService();
        service.setPersonalAccountId(account.getId());
        service.setServiceId(newPaymentServiceId);
        service.setQuantity(quantity);
        service.setComment(comment);

        List<String> exceptions = Arrays.asList(
                servicePlanRepository.findOneByFeature(Feature.FREEZING).getServiceId(),
                servicePlanRepository.findOneByFeature(Feature.LONG_LIFE_RESOURCE_ARCHIVE).getServiceId()
        );
        if (!exceptions.contains(newPaymentServiceId)) {
            service.setFreeze(account.isFreeze());
        }

        return accountServiceRepository.save(service);
    }

    @Nullable
    public Feature findFeature(String paymentServiceId) {
        //todo добавить поддержку тарифного плана на хостинг и абонементов, пока работает только для доп.услуг
        List<ServicePlan> servicePlans = servicePlanRepository.findByServiceId(paymentServiceId);
        return servicePlans.isEmpty() ? null : servicePlans.get(0).getFeature();
    }

    /**
     * Обновляем услугу
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     * @param quantity кол-во услуг
     */
    public void updateAccountService(PersonalAccount account, String serviceId, int quantity) {
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), serviceId);

        if (accountServices != null && !accountServices.isEmpty()) {
            AccountService accountService = accountServices.get(0);

            accountService.setQuantity(quantity);
            accountService.setEnabled(true);

            accountServiceRepository.save(accountService);
        } else {
            addAccountService(account, serviceId, quantity, null);
        }
    }

    /**
     * Заменяем старую услугу на новую
     *
     * @param account   Аккаунт
     * @param oldServiceId id текущей услуги
     * @param newServiceId id новой услуги
     */
    public void replaceAccountService(PersonalAccount account, String oldServiceId, String newServiceId) {
        if (!oldServiceId.equals(newServiceId)) {
            deleteAccountServiceByServiceId(account, oldServiceId);

            addAccountService(account, newServiceId);
        }
    }

    /**
     * Проверяем есть ли услуга на аккаунте
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public boolean accountHasService(PersonalAccount account, String serviceId) {
        return accountServiceRepository.existsByPersonalAccountIdAndServiceId(account.getId(), serviceId);
    }

    /**
     * Получить определенные услуги на аккаунте
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public List<AccountService> getAccountServices(PersonalAccount account, String serviceId) {
        return accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), serviceId);
    }

    /**
     * Включаем услугу
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public void enableAccountService(PersonalAccount account, String serviceId) {
        setEnabledAccountService(account, serviceId, true);
    }

    /**
     * Выключаем все услуги с serviceId
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public void disableAccountService(PersonalAccount account, String serviceId) {
        setEnabledAccountService(account, serviceId, false);
    }

    /**
     * Выключаем услугу
     *
     * @param accountService   услуга
     */
    public void disableAccountService(AccountService accountService) {
        setEnabledAccountService(accountService, false);
    }

    /**
     * Меняем статус услуг
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public void setEnabledAccountService(PersonalAccount account, String serviceId, boolean enabled) {
        List<AccountService> accountServices = getAccountServices(account, serviceId);

        accountServices.forEach(accountService -> accountService.setEnabled(enabled));

        accountServiceRepository.saveAll(accountServices);
    }

    /**
     * Меняем статус услуги
     *
     * @param accountService   услуга
     */
    public void setEnabledAccountService(AccountService accountService, boolean enabled) {
        accountService.setEnabled(enabled);
        accountServiceRepository.save(accountService);
    }

    /**
     * Меняем статус услуги
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public void leaveOnlyOneAccountService(PersonalAccount account, String serviceId) {
        List<AccountService> accountServices = getAccountServices(account, serviceId);

        if (accountServices.size() > 1) {
            accountServices.remove(0);
            accountServiceRepository.deleteAll(accountServices);
        }
    }

    /**
     * Меняем статус услуги
     *
     * @param account   Аккаунт
     * @param serviceId id услуги
     */
    public void setLastBilledAccountService(PersonalAccount account, String serviceId) {
        List<AccountService> accountServices = getAccountServices(account, serviceId);

        accountServices.forEach(accountService -> accountService.setLastBilled(LocalDateTime.now()));

        accountServiceRepository.saveAll(accountServices);
    }

    //получить PaymentService для услуги SMS-уведомлений
    public PaymentService getSmsPaymentServiceByPlanId(String planId) {
        Plan plan = planManager.findOne(planId);

        if (plan == null) {
            throw new ParameterValidationException("Plan with id " + planId + " not found");
        }

        String smsServiceId = plan.getSmsServiceId();

        PaymentService paymentService;

        if (smsServiceId == null) {
            smsServiceId = SMS_NOTIFICATIONS_29_RUB_SERVICE_ID;
            paymentService = serviceRepository.findByOldId(smsServiceId);
        } else {
            paymentService = serviceRepository.findById(smsServiceId).orElse(null);
        }

        if (paymentService == null) {
            throw new ParameterValidationException("paymentService with id " + smsServiceId + " not found");
        }

        return paymentService;
    }

    /**
     * Есть ли на аккаунте услуга SMS-уведомлений
     *
     * @param account   Аккаунт
     */
    public boolean hasSmsNotifications(PersonalAccount account) {
        PaymentService paymentService = this.getSmsPaymentServiceByPlanId(account.getPlanId());
        AccountService accountSmsService = accountServiceRepository.findOneByPersonalAccountIdAndServiceId(account.getId(), paymentService.getId());
        ServicePlan plan = this.getServicePlanForFeatureByAccount(Feature.SMS_NOTIFICATIONS, account);
        if (accountSmsService == null) {
            List<String> abonementIds = plan.getAbonementIds();
            List<AccountServiceAbonement> abonements = abonementManager.findByPersonalAccountIdAndAbonementIdIn(account.getId(), abonementIds);
            if (!abonements.isEmpty()) {
                return true;
            }
        }
        return (accountSmsService != null && accountSmsService.isEnabled());
    }

    /**
     * Есть ли на аккаунте услуга Расширенное резервное копирование
     *
     * @param account   Аккаунт
     */
    public boolean hasAdvancedBackup(PersonalAccount account) {
        if (!account.isActive()) return false;

        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(Feature.ADVANCED_BACKUP, true);

        AccountService accountService = accountServiceRepository.findOneByPersonalAccountIdAndServiceId(account.getId(), plan.getServiceId());

        if (accountService != null
                && accountService.isEnabled()
                && accountService.getLastBilled() != null
                && accountService.getLastBilled().isAfter(LocalDateTime.now().minusDays(1))) {
            return true;
        } else {
            List<AccountServiceAbonement> abonements = abonementManager.findByPersonalAccountIdAndAbonementIdIn(account.getId(), plan.getAbonementIds());
            return abonements != null && !abonements.isEmpty();
        }
    }

    public boolean hasAdditionalQuotaService5k(PersonalAccount account) {
        if (!account.isActive()) return false;

        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(Feature.ADDITIONAL_QUOTA_5K, true);

        List<AccountServiceAbonement> abonements = abonementManager.findByPersonalAccountIdAndAbonementIdIn(account.getId(), plan.getAbonementIds());

        if (abonements == null) {
            return false;
        }

        return abonements.stream().anyMatch(item -> {
            //Если абонемент еще действителен, то услуга активна
            if (item.getExpired().isAfter(LocalDateTime.now())) return true;

            //Если абонемент закончился, но должен быть автоматически продлен в следующее ближайшее время (01:54 + задержка),
            // то считаем услугу активной (пока абонемент либо не продлится, либо не удалится)
            LocalDateTime nextProlong = item.getExpired().plusDays(1L).with(LocalTime.of(2, 10));
            return item.isAutorenew() && nextProlong.isAfter(LocalDateTime.now());
        });
    }

    public boolean hasAllowUseDbService(PersonalAccount account) {
        ServicePlan servicePlan = servicePlanRepository.findOneByFeatureAndActive(Feature.ALLOW_USE_DATABASES, true);
        if (servicePlan == null) {
            throw new InternalApiException("Cannot find ServicePlan");
        }
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceIdAndEnabled(account.getId(), servicePlan.getServiceId(), true);
        if (!accountServices.isEmpty()) {
            return true;
        }

        List<AccountServiceAbonement> abonements = abonementManager.findByPersonalAccountIdAndAbonementIdIn(account.getId(), servicePlan.getAbonementIds());

        if (abonements == null) {
            return false;
        }

        return abonements.stream().anyMatch(item -> item.getExpired().isAfter(LocalDateTime.now()));
    }

    public AccountService getAccountService(PersonalAccount account, Feature feature) {
        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(feature, true);

        return accountServiceRepository.findOneByPersonalAccountIdAndServiceId(account.getId(), plan.getServiceId());
    }

    public List<AccountServiceAbonement> getAccountServiceAbonement(PersonalAccount account, Feature feature) {
        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(feature, true);

        return abonementManager.findByPersonalAccountIdAndAbonementIdIn(account.getId(), plan.getAbonementIds());
    }

    /**
     * @param accountService Услуга, подключенная на аккаунт
     * @return Является ли она дополнительной услугой такой как "дисковое пространство", "СМС" и т.п.
     */
    public boolean isPaymentServiceTypeAdditional(AccountService accountService) {
        if (accountService == null) return false;
        return getPaymentServiceType(accountService).equals("ADDITIONAL_SERVICE");
    }

    /**
     * @param account Аккаунт
     * @return Список всех подключенных услуг с абонементом
     */
    public List<AccountServiceAbonement> getAllAccountServiceAbonements(PersonalAccount account) {
        return abonementManager.findAllByPersonalAccountId(account.getId());
    }

    /**
     * Отключить все дополнительные услуги с ежедневным списанием
     * @param account Аккаунт
     */
    public void completeDisableAllAdditionalServices(PersonalAccount account, String reason) {
        account.getServices()
                .stream()
                .filter(this::isPaymentServiceTypeAdditional)
                .forEach(s -> this.completeDisableAdditionalService(account, s,
                        "Услуга " + s.getPaymentService().getName() + " отключена: " + reason));
    }

    /**
     * Удалить все абонементы дополнительных услуг
     * @param account Аккаунт
     */
    public void completeDisableAllAdditionalServiceAbonements(PersonalAccount account) {
        abonementManager.findAllByPersonalAccountId(account.getId())
                .forEach(ab -> this.completeDisableAdditionalServiceAbonement(account, ab));
    }

    /**
     * Отключить указанную дополнительную услугу
     *  Ставит active=false в accountService
     *  Если это доп.квота, то кидает event на пересчет квоты
     *  Остальные услуги, если требуются, нужно добавлять индивидуально
     * @param account Аккаунт
     * @param accountService Дополнительная услуга
     * @param historyMessage Кастомное сообщение в историю аккаунта
     */
    public void completeDisableAdditionalService(PersonalAccount account, AccountService accountService, String historyMessage) {
        if (accountService.getPaymentService().getPaymentType() == ServicePaymentType.ONE_TIME) {
            disableAccountService(accountService);
        } else {
            disableAccountService(account, accountService.getServiceId());
        }

        switch (accountService.getPaymentService().getOldId()) {
            case ADDITIONAL_QUOTA_100_SERVICE_ID:
                account.setAddQuotaIfOverquoted(false);
                accountManager.setAddQuotaIfOverquoted(account.getId(), false);
                publisher.publishEvent(new AccountCheckQuotaEvent(account.getId()));
                break;
            case ANTI_SPAM_SERVICE_ID:
                publisher.publishEvent(new AntiSpamServiceSwitchEvent(account.getId(), false));
                break;
            case LONG_LIFE_RESOURCE_ARCHIVE_SERVICE_ID:
                resourceArchiveService.processAccountServiceDelete(accountService);
                break;
        }

        if (historyMessage != null) {
            history.save(account, historyMessage);
        } else {
            history.save(account, "Услуга " + accountService.getPaymentService().getName() + " отключена");
        }
    }

    /**
     * Удалить указанный абонемент на дополнительную услугу
     * @param account Аккаунт
     * @param accountServiceAbonement Абонемент на услугу
     */
    public void completeDisableAdditionalServiceAbonement(PersonalAccount account, AccountServiceAbonement accountServiceAbonement) {
        abonementManager.delete(accountServiceAbonement);

        ServicePlan servicePlan = getServicePlanForFeatureByAccount(accountServiceAbonement.getAbonement().getType(), account);
        if (servicePlan == null) {
            throw new ResourceNotFoundException("[AccountServiceHelper#disableAdditionalServiceAbonement] ServicePlan not found");
        }

        switch (servicePlan.getFeature()) {
            case REDIRECT:
                RedirectAccountService redirectAccountService = accountRedirectServiceRepository.findByAccountServiceAbonementId(accountServiceAbonement.getId());
                publisher.publishEvent(new RedirectWasDisabledEvent(redirectAccountService.getPersonalAccountId(), redirectAccountService.getFullDomainName()));
                redirectAccountService.setActive(false);
                accountRedirectServiceRepository.save(redirectAccountService);
                break;
            case ANTI_SPAM:
                publisher.publishEvent(new AntiSpamServiceSwitchEvent(account.getId(), false));
                break;
        }

        Map<String, String> statData = new HashMap<>();
        statData.put("abonementId", accountServiceAbonement.getAbonementId());
        statData.put("expires", accountServiceAbonement.getExpired().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        statData.put("disabled", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        accountStatHelper.add(account.getId(), AccountStatType.VIRTUAL_HOSTING_SERVICE_ABONEMENT_DELETE, statData);
    }

    public Map<AccountService, BigDecimal> getDailyServicesToCharge(PersonalAccount account, LocalDate chargeDate) {
        return getDailyServicesToCharge(account, LocalDateTime.of(
                chargeDate,
                LocalTime.of(0, 0, 0, 0)
        ));
    }

    private Map<AccountService, BigDecimal> getDailyServicesToCharge(PersonalAccount account, LocalDateTime chargeDate) {
        Map<AccountService, BigDecimal> dailyServicesWithCost;
        List<AccountService> accountServices = account.getServices();
        if (accountServices == null || accountServices.isEmpty()) { return new HashMap<>();}

        dailyServicesWithCost = accountServices.stream()
                .filter(accountService -> accountService.isEnabled() && !accountService.isFreeze()
                        && accountService.getPaymentService() != null
                        && (isRegularAccountServiceNeedDailyCharge(accountService, chargeDate)))
                .collect(Collectors.toMap(
                        item -> item,
                        item -> this.getServiceCostDependingOnDiscount(account.getId(), item)
                )).entrySet().stream()
                .filter(item -> item.getValue().compareTo(BigDecimal.ZERO) > 0)
                //сортируем в порядке убывания paymentService.chargePriority
                //в начало попадет сервис с тарифом
                .sorted(Map.Entry.comparingByKey(AccountService.ChargePriorityComparator))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        return dailyServicesWithCost;
    }

    public void switchServicesAfterFreeze(PersonalAccount account, Boolean freezeState) {
        List<String> exceptions = Arrays.asList(
                servicePlanRepository.findOneByFeature(Feature.FREEZING).getServiceId(),
                servicePlanRepository.findOneByFeature(Feature.LONG_LIFE_RESOURCE_ARCHIVE).getServiceId()
        );

        List<AccountService> accountServices = account.getServices();
        accountServices.forEach(item -> {
            if (item.getPaymentService() != null && !exceptions.contains(item.getPaymentService().getId())) {
                item.setFreeze(freezeState);
                accountServiceRepository.save(item);
            }
        });
    }

    private Boolean isRegularAccountServiceNeedDailyCharge(AccountService accountService, LocalDateTime chargeDate) {

        return accountService.getPaymentService().getPaymentType() != ServicePaymentType.ONE_TIME
                && (accountService.getLastBilled() == null || accountService.getLastBilled().isBefore(chargeDate));
    }

    public String getPaymentServiceType(AccountService accountService) {
        if (accountService.getPaymentService() != null) {
            String oldId = accountService.getPaymentService().getOldId();
            if (oldId.startsWith("plan_")) {
                return "PLAN";
            } else {
                return "ADDITIONAL_SERVICE";
            }
        } else {
            return null;
        }
    }

    public BigDecimal getDailyCostForService(PersonalAccount account, AccountService accountService) {
        return this.getDailyCostForService(account, accountService, LocalDate.now());
    }

    public BigDecimal getDailyCostForService(PaymentService paymentService, BigDecimal serviceCost) {
        return this.getDailyCostForService(paymentService, serviceCost, LocalDate.now());
    }

    public BigDecimal getDailyCostForService(PersonalAccount account, AccountService accountService, LocalDate chargeDate) {
        return this.getDailyCostForService(accountService, this.getServiceCostDependingOnDiscount(account.getId(), accountService), chargeDate);
    }

    public BigDecimal getDailyCostForService(AccountService accountService, BigDecimal serviceCost, LocalDate chargeDate) {
        return this.getDailyCostForService(accountService.getPaymentService(), serviceCost, chargeDate);
    }

    public BigDecimal getDailyCostForService(PaymentService paymentService, BigDecimal serviceCost, LocalDate chargeDate) {
        Integer daysInCurrentMonth = chargeDate.lengthOfMonth();
        BigDecimal cost = BigDecimal.ZERO;
        switch (paymentService.getPaymentType()) {
            case MONTH:
                cost = serviceCost.divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
                break;
            case DAY:
                cost = serviceCost;
                break;
        }
        return cost;
    }

    public PaymentService getAccessToTheControlPanelService() {
        return serviceRepository.findByOldId(ACCESS_TO_CONTROL_PANEL_SERVICE_OLD_ID);
    }

    @Nullable
    public ServicePlan getServicePlanForFeatureByAccount(Feature feature, PersonalAccount account) {
        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(feature, true);

        if (feature == Feature.SMS_NOTIFICATIONS) {
            PaymentService paymentService = this.getSmsPaymentServiceByPlanId(account.getPlanId());
            plan = servicePlanRepository.findOneByFeatureAndServiceId(feature, paymentService.getId());
        }

        return plan;
    }

    public BigDecimal getServiceCostDependingOnDiscount(String accountId, AccountService accountService) {
        BigDecimal cost = this.getServiceCostDependingOnDiscount(accountId, accountService.getPaymentService());
        return cost.multiply(BigDecimal.valueOf(accountService.getQuantity()));
    }

    public BigDecimal getServiceCostDependingOnDiscount(String accountId, PaymentService paymentService) {
        BigDecimal cost = paymentService.getCost();

        AccountPromotion accountPromotion = accountPromotionManager.getServiceDiscountPromotion(accountId, paymentService);

        if (accountPromotion != null) {
            cost = discountFactory.getDiscount(accountPromotion.getAction()).getCost(cost);
        }

        Optional<PromocodeAction> bfAction = promocodeActionRepository.findById(ACTION_BLACK_FRIDAY_PROMOTION_ID);
        if (bfAction.isPresent()) {
            List serviceIds = (List) bfAction.get().getProperties().get("serviceIds");
            if (serviceIds.contains(paymentService.getId()) && accountPromotion == null) {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime startDate = LocalDateTime.parse(ACTION_BLACK_FRIDAY_START_DATE, formatter);
                LocalDateTime endDate = LocalDateTime.parse(ACTION_BLACK_FRIDAY_END_DATE, formatter);

                if (now.isAfter(startDate) && now.isBefore(endDate)) {
                    cost = discountFactory.getDiscount(bfAction.get()).getCost(cost);
                }
            }
        }

        return cost;
    }

    public BigDecimal getPlanCostDependingOnDiscount(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());
        return this.getServiceCostDependingOnDiscount(account.getId(), plan.getService());
    }
}
