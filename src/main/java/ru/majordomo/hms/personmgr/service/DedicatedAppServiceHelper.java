package ru.majordomo.hms.personmgr.service;

import feign.FeignException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.DedicatedAppServiceDto;
import ru.majordomo.hms.personmgr.event.account.DedicatedAppServiceEnabledEvent;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceIsLockedException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.feign.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.ServicePlan;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.DedicatedAppService;
import ru.majordomo.hms.personmgr.repository.DedicatedAppServiceRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessActionRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.repository.ServicePlanRepository;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.template.Template;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DedicatedAppServiceHelper {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RcStaffFeignClient rcStaffFeignClient;
    private final RcUserFeignClient rcUserFeignClient;
    private final ServicePlanRepository servicePlanRepository;
    private final PlanManager planManager;
    private final AccountServiceHelper accountServiceHelper;
    private final DedicatedAppServiceRepository dedicatedAppServiceRepository;
    private final BusinessHelper businessHelper;
    private final PersonalAccountManager accountManager;
    private final AccountHistoryManager history;
    private final ProcessingBusinessOperationRepository processingBusinessOperationRepository;

    private final static List<BusinessOperationType> DEDICATED_APP_SERVICE_OPERATIONS = Collections.unmodifiableList(Arrays.asList(
            BusinessOperationType.DEDICATED_APP_SERVICE_CREATE,
            BusinessOperationType.DEDICATED_APP_SERVICE_DELETE,
            BusinessOperationType.DEDICATED_APP_SERVICE_UPDATE)
    );

    private final static Set<State> BLOCKED_OPERATIONS_STATES = EnumSet.of(State.PROCESSING, State.NEED_TO_PROCESS);

    public List<DedicatedAppServiceDto> getServicesWithStaffService(@NonNull String accountId) {
        List<DedicatedAppService> dedicatedAppServicesDb =  dedicatedAppServiceRepository.findByPersonalAccountId(accountId);
        if (CollectionUtils.isEmpty(dedicatedAppServicesDb)) {
            return Collections.emptyList();
        }
        List<DedicatedAppServiceDto> dedicatedAppServices = dedicatedAppServicesDb.stream()
                .map(DedicatedAppServiceDto::new).collect(Collectors.toList());
        String serverId = getServerId(accountId);
        if (StringUtils.isEmpty(serverId)) {
            throw new ResourceNotFoundException("Не удалось найти сервер на котором находится аккаунт");
        }
        List<Service> services = rcStaffFeignClient.getServiceByAccountIdAndServerId(accountId, serverId);
        if (CollectionUtils.isNotEmpty(services)) {
            dedicatedAppServices.forEach(dedicatedAppServiceDto -> dedicatedAppServiceDto.setService(
                    services.stream().filter(service -> StringUtils.equals(
                            service.getTemplateId(), dedicatedAppServiceDto.getTemplateId()
                    )).findFirst().orElse(null)
            ));
        }
        return dedicatedAppServices;
    }

    public List<DedicatedAppService> getServices(@NonNull String accountId) {
        return dedicatedAppServiceRepository.findByPersonalAccountId(accountId);
    }

    @Nullable
    public DedicatedAppService getService( @NonNull String dedicatedAppServiceId, String personalAccountId) {
        return dedicatedAppServiceRepository.findByIdAndPersonalAccountId(dedicatedAppServiceId, personalAccountId);
    }

    @Nullable
    public DedicatedAppService getService(@NonNull String dedicatedAppServiceId) {
        return dedicatedAppServiceRepository.findById(dedicatedAppServiceId).orElse(null);
    }

    @Nullable
    public DedicatedAppServiceDto getServiceWithStaffService(@NonNull String dedicatedAppServiceId) throws ParameterValidationException, ResourceNotFoundException {
        DedicatedAppService dedicatedAppServiceDb =  dedicatedAppServiceRepository.findById(dedicatedAppServiceId).orElse(null);
        if (dedicatedAppServiceDb == null) {
            return null;
        }
        DedicatedAppServiceDto dedicatedAppService = new DedicatedAppServiceDto(dedicatedAppServiceDb);
        String serverId = getServerId(dedicatedAppService.getPersonalAccountId());
        if (StringUtils.isEmpty(serverId)) {
            throw new ResourceNotFoundException("Не удалось найти сервер на котором находится аккаунт");
        }
        List<Service> services = rcStaffFeignClient.getServicesByAccountIdAndServerIdAndTemplateId(dedicatedAppService.getPersonalAccountId(), serverId, dedicatedAppService.getTemplateId());
        if (CollectionUtils.isNotEmpty(services)) {
            dedicatedAppService.setService(services.get(0));
        }
        return dedicatedAppService;
    }

    private void assertLockedResource(String accountId, @Nullable String resourceId, @Nullable String templateId) {
        List<ProcessingBusinessOperation> operations = processingBusinessOperationRepository.findAllByPersonalAccountIdAndTypeInAndStateIn(
                accountId,
                DEDICATED_APP_SERVICE_OPERATIONS,
                BLOCKED_OPERATIONS_STATES);
        if (operations.stream().anyMatch(operation ->
                (StringUtils.isNotEmpty(resourceId) && resourceId.equals(operation.getParam(Constants.RESOURCE_ID_KEY)) ||
                        (StringUtils.isNotEmpty(templateId) && templateId.equals(operation.getParam(Constants.TEMPLATE_ID_KEY)))))) {
            throw new ResourceIsLockedException();
        }
    }

    public ProcessingBusinessAction create(@NonNull PersonalAccount account, @NonNull String templateId) throws ParameterValidationException, ResourceNotFoundException {

        assertAccountIsActive(account);

        assertPlanAllowedDedicatedApp(account.getPlanId());

        accountHasThisService(account.getId(), templateId);

        Template template = null;
        try {
            template = rcStaffFeignClient.getTemplateAvailableToAccountsById(account.getId(), templateId);
        } catch (Exception ignored) {
            logger.debug("Exception when found Template with id:" + templateId);
        }

        if (template == null) {
            throw new ParameterValidationException("Не найден Template");
        }

        assertLockedResource(account.getId(), null, templateId);

        String serverId = getServerId(account.getId());

        List<Service> services = rcStaffFeignClient.getServicesByAccountIdAndServerIdAndTemplateId(account.getId(), serverId, templateId);

        DedicatedAppService dedicatedAppService;

        ServicePlan servicePlan = servicePlanRepository.findOneByFeatureAndActive(Feature.DEDICATED_APP_SERVICE, true);
        if (servicePlan == null) {
            logger.error("Cannot found ServicePlan for DEDICATED_APP_SERVICE, database is wrong");
            throw new InternalApiException();
        }


        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        message.addParam("serverId", serverId);

        ProcessingBusinessAction action;

        if (CollectionUtils.isEmpty(services)) {
            message.addParam(Constants.TEMPLATE_ID_KEY, template.getId());
            action = businessHelper.buildActionAndOperation(
                    BusinessOperationType.DEDICATED_APP_SERVICE_CREATE,
                    BusinessActionType.DEDICATED_APP_SERVICE_CREATE_RC_STAFF,
                    message
            );
            history.save(account, "Поступила заявка на создание выделенного сервиса с templateId: " + template.getId());
        } else {
            Service staffService = services.get(0);
            assertLockedResource(account.getId(), staffService.getId(), staffService.getTemplateId());
            if (!staffService.getSwitchedOn()){
                message.addParam(Constants.RESOURCE_ID_KEY, staffService.getId());
                message.addParam(Constants.SWITCHED_ON_KEY, true);
                message.addParam(Constants.TEMPLATE_ID_KEY, staffService.getTemplateId()); // need for assertLockedResource
                action = businessHelper.buildActionAndOperation(
                        BusinessOperationType.DEDICATED_APP_SERVICE_UPDATE,
                        BusinessActionType.DEDICATED_APP_SERVICE_UPDATE_RC_STAFF,
                        message
                );
                history.save(account, "Поступила заявка на включение выделенного сервиса с templateId: " + template.getId());
            } else {
                action = null;
            }
        }

        AccountService accountService = accountServiceHelper.addAccountService(account, servicePlan.getServiceId());

        dedicatedAppService = addService(account, template.getId(), accountService);

        history.save(account, "Заказана услуга выделенный сервис с templateId: " + template.getId());

        return action;
    }

    public DedicatedAppService findByAccountService(String personalAccountId, String accountServiceId) {
        return dedicatedAppServiceRepository.findByPersonalAccountIdAndAccountServiceId(personalAccountId, accountServiceId);
    };

    public DedicatedAppService findByPersonalAccountIdAndTemplateId(String personalAccountId, String templateId) {
        return dedicatedAppServiceRepository.findByPersonalAccountIdAndTemplateId(personalAccountId, templateId);
    }

    public void deleteDedicatedAppServiceAndAccountService(String accountId, String accountServiceId) {
        try {
            DedicatedAppService dedicatedAppService = findByAccountService(accountId, accountServiceId);
            if (dedicatedAppService == null) {
                throw new ParameterValidationException("Неверные параметры запроса на удаление выделенного сервиса");
            }
            PersonalAccount account = accountManager.findOne(accountId);
            if (account == null) {
                throw new ResourceNotFoundException("Не найден аккаунт " + accountId);
            }
            disableDedicatedAppService(account, dedicatedAppService);
        } catch (ResourceNotFoundException | ParameterValidationException | FeignException ex) {
            logger.error(String.format("Cannot delete dedicated service with account service %s", accountServiceId), ex);
            throw ex;
        }
    }

    @Nullable
    public ProcessingBusinessAction disableDedicatedAppService(String accountId, String dedicatedServiceId) {
        DedicatedAppService dedicatedAppService = getService(dedicatedServiceId);
        if (dedicatedAppService == null) {
            throw new ResourceNotFoundException("Не найден выделенный сервис с id: " + dedicatedServiceId);
        }
        assertAccountIsOwnerOfDedicatedService(accountId, dedicatedAppService);
        PersonalAccount account = accountManager.findOne(accountId);
        if (account == null) {
            throw new ResourceNotFoundException("Не найден аккаунт " + accountId);
        }
        return disableDedicatedAppService(account, dedicatedAppService);
    }

    /**
     * Удаляет DedicatedAppService, AccountService и сервис на rc-staff
     * @param account
     * @param dedicatedAppService
     * @throws ParameterValidationException
     * @throws ResourceNotFoundException
     */
    @Nullable
    private ProcessingBusinessAction disableDedicatedAppService(@NonNull PersonalAccount account, @NonNull DedicatedAppService dedicatedAppService) throws ParameterValidationException, ResourceNotFoundException {

        List<ru.majordomo.hms.rc.staff.resources.Service> staffServices =
                rcStaffFeignClient.getServicesByAccountIdAndTemplateId(
                        account.getId(),
                        dedicatedAppService.getTemplateId()
                );

        ProcessingBusinessAction action = null;

        if (staffServices.size() > 0) {
            Service staffService = staffServices.get(0);
            assertServiceIsnotUser(account.getId(), staffService.getId());
            assertLockedResource(account.getId(), staffService.getId(), staffService.getTemplateId());

            SimpleServiceMessage message = new SimpleServiceMessage();
            message.setAccountId(dedicatedAppService.getPersonalAccountId());
            message.addParam(Constants.RESOURCE_ID_KEY, staffService.getId());
            message.addParam(Constants.SWITCHED_ON_KEY, false);
            message.addParam(Constants.TEMPLATE_ID_KEY, staffService.getTemplateId()); // need for assertLockedResource

            action = businessHelper.buildActionAndOperation(
                    BusinessOperationType.DEDICATED_APP_SERVICE_UPDATE,
                    BusinessActionType.DEDICATED_APP_SERVICE_UPDATE_RC_STAFF,
                    message
            );
            history.save(account, "Поступила заявка на отключение выделенного сервиса с id: " + staffServices.get(0).getId());
        } else {
            assertLockedResource(account.getId(), null, dedicatedAppService.getTemplateId());
            history.save(account, "Поступила заявка на отключение выделенного сервиса. Сервисов не найдено, удалена услуга");
        }
        if (staffServices.size() <= 1) {
            dedicatedAppServiceRepository.deleteById(dedicatedAppService.getId());
            if (accountServiceHelper.hasAccountService(dedicatedAppService.getAccountServiceId())) {
                accountServiceHelper.deleteAccountServiceById(account, dedicatedAppService.getAccountServiceId(), true);
            }
        }

        return action;
    }

    private String getServerId(String accountId) throws ParameterValidationException {
        List<UnixAccount> unixAccounts = null;
        try {
            unixAccounts = (List<UnixAccount>) rcUserFeignClient.getUnixAccounts(accountId);
        } catch (Exception ignored) {}

        if (unixAccounts == null || unixAccounts.isEmpty()) {
            throw new ParameterValidationException("UnixAccount не найден");
        }

        return unixAccounts.get(0).getServerId();
    }

    private DedicatedAppService addService(PersonalAccount account, String templateId, AccountService accountService) {
        DedicatedAppService dedicatedAppService = new DedicatedAppService();
        dedicatedAppService.setTemplateId(templateId);
        dedicatedAppService.setPersonalAccountId(account.getId());
        dedicatedAppService.setCreatedDate(LocalDate.now());
        dedicatedAppService.setAccountServiceId(accountService.getId());
        dedicatedAppService.setAccountService(accountService);
        dedicatedAppService.setActive(true);

        return dedicatedAppServiceRepository.insert(dedicatedAppService);
    }

    private void assertAccountIsOwnerOfResource(@NonNull String accountId, @NonNull ru.majordomo.hms.rc.staff.resources.Service staffService) {
        if (!accountId.equals(staffService.getAccountId())) {
            throw new ParameterValidationException("Аккаунт не является владельцем ресурса");
        }
    }

    private void assertAccountIsOwnerOfResource(String accountId, String resourceId) {
        ru.majordomo.hms.rc.staff.resources.Service service = null;
        try {
            service = rcStaffFeignClient.getServiceByAccountIdAndId(accountId, resourceId);
        } catch (ResourceNotFoundException ignored) {}
        if (service != null) {
            assertAccountIsOwnerOfResource(accountId, service);
        }
    }

    private void accountHasThisService(String accountId, String templateId) {
        if (findByPersonalAccountIdAndTemplateId(accountId, templateId) != null) {
            throw new ParameterValidationException("Выделенный сервис уже подключен на аккаунт");
        }
    }

    private void assertAccountIsOwnerOfDedicatedService(String accountId, DedicatedAppService dedicatedService) {
        if (!Objects.equals(dedicatedService.getPersonalAccountId(), accountId)) {
            throw new ParameterValidationException("Аккаунт не является владельцем выделеного сервиса");
        }
    }

    private void assertServiceIsnotUser(String accountId, String staffServiceId) {
        List<WebSite> webSites = rcUserFeignClient.getWebSites(accountId);
        webSites.stream().filter(webSite -> Objects.equals(webSite.getServiceId(), staffServiceId)).findFirst()
                .ifPresent(webSite -> {
                    throw new ParameterValidationException("Ошибка. Сервис используется на сайте: " + webSite.getName());
                });
    }

    private void assertAccountIsActive(PersonalAccount account) {
        if (!account.isActive()) {
            throw new ParameterValidationException("Аккаунт не активен");
        }
    }

    private void assertPlanAllowedDedicatedApp(String planId) {
        Plan plan = planManager.findOne(planId);
        if (plan == null) {
            throw new ResourceNotFoundException("Не удалось найти тарифный план: " + planId);
        }

        if (plan.getProhibitedResourceTypes().contains(ResourceType.DEDICATED_APP_SERVICE)) {
            throw new ParameterValidationException("Подключение выделенного сервиса запрещено для тарифа " + plan.getName());
        }
    }

}
