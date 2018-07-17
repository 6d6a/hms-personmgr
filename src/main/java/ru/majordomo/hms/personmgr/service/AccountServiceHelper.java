package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.dto.revisium.CheckResponse;
import ru.majordomo.hms.personmgr.dto.revisium.ResultStatus;
import ru.majordomo.hms.personmgr.event.revisium.ProcessRevisiumRequestEvent;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequestService;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.repository.RevisiumRequestRepository;
import ru.majordomo.hms.personmgr.repository.ServicePlanRepository;
import ru.majordomo.hms.personmgr.service.Revisium.RevisiumApiClient;

import static ru.majordomo.hms.personmgr.common.Constants.ACCESS_TO_CONTROL_PANEL_SERVICE_OLD_ID;
import static ru.majordomo.hms.personmgr.common.Constants.REVISIUM_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.SMS_NOTIFICATIONS_29_RUB_SERVICE_ID;

@Service
public class AccountServiceHelper {
    private final AccountServiceRepository accountServiceRepository;
    private final PlanRepository planRepository;
    private final PaymentServiceRepository serviceRepository;
    private final RevisiumRequestRepository revisiumRequestRepository;
    private final RevisiumApiClient revisiumApiClient;
    private final ApplicationEventPublisher publisher;
    private final AccountHistoryManager history;
    private final AbonementManager<AccountServiceAbonement> abonementManager;
    private final ServicePlanRepository servicePlanRepository;

    @Autowired
    public AccountServiceHelper(
            AccountServiceRepository accountServiceRepository,
            PlanRepository planRepository,
            PaymentServiceRepository serviceRepository,
            RevisiumRequestRepository revisiumRequestRepository,
            RevisiumApiClient revisiumApiClient,
            ApplicationEventPublisher publisher,
            AccountHistoryManager history,
            AbonementManager<AccountServiceAbonement> abonementManager,
            ServicePlanRepository servicePlanRepository
    ) {
        this.accountServiceRepository = accountServiceRepository;
        this.planRepository = planRepository;
        this.serviceRepository = serviceRepository;
        this.revisiumRequestRepository = revisiumRequestRepository;
        this.revisiumApiClient = revisiumApiClient;
        this.publisher = publisher;
        this.history = history;
        this.abonementManager = abonementManager;
        this.servicePlanRepository = servicePlanRepository;
    }

    /**
     * Удаляем старую услугу
     *
     * @param account   Аккаунт
     * @param serviceId id услуги ServiceId
     */
    public void deleteAccountServiceByServiceId(PersonalAccount account, String serviceId) {
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), serviceId);

        if (accountServices != null && !accountServices.isEmpty()) {
            accountServiceRepository.delete(accountServices);
        }
    }

    /**
     * Удаляем старую услугу
     *
     * @param account   Аккаунт
     * @param accountServiceId id услуги AccountService
     */
    public void deleteAccountServiceById(PersonalAccount account, String accountServiceId) {
        AccountService accountService = accountServiceRepository.findByPersonalAccountIdAndId(account.getId(), accountServiceId);

        if (accountService != null) {
            accountServiceRepository.delete(accountService);
        }
    }

    /**
     * Добавляем новую услугу
     *
     * @param account   Аккаунт
     * @param newServiceId id новой услуги
     */
    public AccountService addAccountService(PersonalAccount account, String newServiceId) {
        AccountService service = new AccountService();
        service.setPersonalAccountId(account.getId());
        service.setServiceId(newServiceId);

        return accountServiceRepository.save(service);
    }

    /**
     * Добавляем новую услугу
     *
     * @param account   Аккаунт
     * @param newServiceId id новой услуги
     * @param quantity кол-во услуг
     */
    public void addAccountService(PersonalAccount account, String newServiceId, int quantity) {
        AccountService service = new AccountService();
        service.setPersonalAccountId(account.getId());
        service.setServiceId(newServiceId);
        service.setQuantity(quantity);

        accountServiceRepository.save(service);
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
            addAccountService(account, serviceId, quantity);
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
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountIdAndServiceId(account.getId(), serviceId);

        return accountServices != null && !accountServices.isEmpty();
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

        accountServiceRepository.save(accountServices);
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
            accountServiceRepository.delete(accountServices);
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

        accountServiceRepository.save(accountServices);
    }

    public PaymentService getRevisiumPaymentService() {

        PaymentService paymentService = serviceRepository.findByOldId(REVISIUM_SERVICE_ID);

        if (paymentService == null) {
            throw new ParameterValidationException("paymentService with id '" + REVISIUM_SERVICE_ID + "' not found");
        }

        return paymentService;
    }

    public RevisiumRequest revisiumCheckRequest(PersonalAccount account, RevisiumRequestService revisiumRequestService) {
        //Запрос в Ревизиум и обработка
        RevisiumRequest revisiumRequest = new RevisiumRequest();
        revisiumRequest.setRevisiumRequestServiceId(revisiumRequestService.getId());
        revisiumRequest.setPersonalAccountId(revisiumRequestService.getPersonalAccountId());
        revisiumRequest.setCreated(LocalDateTime.now());
        revisiumRequestRepository.save(revisiumRequest);

        try {

            CheckResponse checkResponse = revisiumApiClient.check(revisiumRequestService.getSiteUrl());

            switch (ResultStatus.valueOf(checkResponse.getStatus().toUpperCase())) {
                case COMPLETE:
                case INCOMPLETE:
                    revisiumRequest.setRequestId(checkResponse.getRequestId());
                    revisiumRequest.setSuccessCheck(true);
                    revisiumRequest = revisiumRequestRepository.save(revisiumRequest);
                    publisher.publishEvent(new ProcessRevisiumRequestEvent(revisiumRequest));
                    break;
                case CANCELED:
                case FAILED:
                default:
                    revisiumRequest.setSuccessCheck(false);
                    revisiumRequestRepository.save(revisiumRequest);
                    history.save(
                            account,
                            "Ошибка при запросе проверки (check) сайта '" + revisiumRequestService.getSiteUrl() + "' в Ревизиум. Текст ошибки: '" + checkResponse.getErrorMessage() + "'",
                            "service");
                    break;
            }

        } catch (Exception e) {
            history.save(
                    account,
                    "Ошибка при запросе проверки (check) сайта: '" + revisiumRequestService.getSiteUrl() + "' в Ревизиум.",
                    "service");
            e.printStackTrace();
        }

        return revisiumRequest;
    }

    //получить PaymentService для услуги SMS-уведомлений
    public PaymentService getSmsPaymentServiceByPlanId(String planId) {
        Plan plan = planRepository.findOne(planId);

        if (plan == null) {
            throw new ParameterValidationException("Plan with id " + planId + " not found");
        }

        String smsServiceId = plan.getSmsServiceId();

        PaymentService paymentService;

        if (smsServiceId == null) {
            smsServiceId = SMS_NOTIFICATIONS_29_RUB_SERVICE_ID;
            paymentService = serviceRepository.findByOldId(smsServiceId);
        } else {
            paymentService = serviceRepository.findOne(smsServiceId);
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
        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(Feature.SMS_NOTIFICATIONS, true);
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
        ServicePlan plan = servicePlanRepository.findOneByFeatureAndActive(Feature.ADVANCED_BACKUP, true);

        AccountService accountService = accountServiceRepository.findOneByPersonalAccountIdAndServiceId(account.getId(), plan.getServiceId());

        if (accountService == null) {
            List<AccountServiceAbonement> abonements = abonementManager.findByPersonalAccountIdAndAbonementIdIn(account.getId(), plan.getAbonementIds());
            if (abonements != null && !abonements.isEmpty()) {
                return true;
            }
        }

        return accountService != null
                && accountService.isEnabled()
                && accountService.getLastBilled() != null
                && accountService.getLastBilled().isAfter(LocalDateTime.now());
    }


    public List<AccountService> getDailyServicesToCharge(PersonalAccount account, LocalDate chargeDate) {
        return getDailyServicesToCharge(account, LocalDateTime.of(
                chargeDate,
                LocalTime.of(0, 0, 0, 0)
                )
        );
    }
    public List<AccountService> getDailyServicesToCharge(PersonalAccount account, LocalDateTime chargeDate) {
        List<AccountService> dailyServices = new ArrayList<>();
        List<AccountService> accountServices = account.getServices();
        if (accountServices == null || accountServices.isEmpty()) { return dailyServices;}
        dailyServices = accountServices.stream().filter(accountService ->
                accountService.isEnabled()
                        && accountService.getPaymentService() != null
                        && accountService.getPaymentService().getCost().compareTo(BigDecimal.ZERO) > 0
                        && (isRegularAccountServiceNeedDailyCharge(accountService, chargeDate)))
                .collect(Collectors.toList());

        //сортируем в порядке убывания paymentService.chargePriority
        //в начало попадет сервис с тарифом
        accountServices.sort(AccountService.ChargePriorityComparator);

        return dailyServices;
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

    public BigDecimal getDailyCostForService(AccountService accountService) {
        return this.getDailyCostForService(accountService, LocalDate.now());
    }

    public BigDecimal getDailyCostForService(AccountService accountService, LocalDate chargeDate) {
        Integer daysInCurrentMonth = chargeDate.lengthOfMonth();
        BigDecimal cost = BigDecimal.ZERO;
        switch (accountService.getPaymentService().getPaymentType()) {
            case MONTH:
                cost = accountService.getCost().divide(BigDecimal.valueOf(daysInCurrentMonth), 4, BigDecimal.ROUND_HALF_UP);
                break;
            case DAY:
                cost = accountService.getCost();
                break;
        }
        return cost;
    }

    public PaymentService getAccessToTheControlPanelService() {
        return serviceRepository.findByOldId(ACCESS_TO_CONTROL_PANEL_SERVICE_OLD_ID);
    }
}
