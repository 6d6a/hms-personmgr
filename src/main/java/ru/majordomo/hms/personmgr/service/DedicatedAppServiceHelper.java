package ru.majordomo.hms.personmgr.service;

import feign.FeignException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.*;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.DedicatedAppServiceDto;
import ru.majordomo.hms.personmgr.exception.*;
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
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.DedicatedAppServiceRepository;
import ru.majordomo.hms.personmgr.repository.ProcessingBusinessOperationRepository;
import ru.majordomo.hms.personmgr.repository.ServicePlanRepository;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.template.ApplicationServer;
import ru.majordomo.hms.rc.staff.resources.template.Template;
import ru.majordomo.hms.rc.user.resources.UnixAccount;
import ru.majordomo.hms.rc.user.resources.WebSite;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * ?????????? ???????????????? ?? ???????? ?????? ???????????? ???????????? ?? ?????????????????????? ??????????????????
 */
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
            throw new ResourceNotFoundException("???? ?????????????? ?????????? ???????????? ???? ?????????????? ?????????????????? ??????????????");
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

    public ProcessingBusinessAction restartDedicatedAppService(String accountId, SimpleServiceMessage userMessage) {
        PersonalAccount account = accountManager.findOne(accountId);
        assertAccountIsActive(account);
        assertAccountIsFreezed(account);

        assertPlanAllowedDedicatedApp(account);

        String templateId = MapUtils.getString(userMessage.getParams(), Constants.TEMPLATE_ID_KEY, "");
        String staffServiceId = MapUtils.getString(userMessage.getParams(), Constants.RESOURCE_ID_KEY, "");

        Service staffService;
        if (!staffServiceId.isEmpty()) {
            try {
                staffService = rcStaffFeignClient.getServiceByAccountIdAndId(accountId, staffServiceId);
                templateId = staffService.getTemplateId();
            } catch (BaseException | FeignException ex) {
                ex.printStackTrace();
                logger.error(String.format("Got exception when get service: %s for account: %s", staffServiceId, accountId));
                throw new InternalApiException("???? ?????????????? ???????????????? ????????????");
            }
        } else if (!templateId.isEmpty()) {
            String serverId = getServerId(accountId);
            if (StringUtils.isEmpty(serverId)) {
                throw new InternalApiException("???? ?????????????? ???????????????? ???????????? ???? ?????????????? ?????????????????? ??????????????");
            }
            try {
                List<Service> staffServices = rcStaffFeignClient.getServicesByAccountIdAndServerIdAndTemplateId(accountId, serverId, templateId);
                if (CollectionUtils.isNotEmpty(staffServices)) {
                    staffService = staffServices.get(0);
                    staffServiceId = staffService.getId();
                } else {
                    throw new InternalApiException("???? ?????????????? ???????????????? ????????????");
                }
            } catch (InternalApiException ex) {
                throw ex;
            } catch (BaseException | FeignException ex) {
                ex.printStackTrace();
                logger.error(String.format("Got exception when get for template: %s for account: %s and server %s", staffServiceId, accountId, serverId));
                throw new InternalApiException("???? ?????????????? ???????????????? ????????????");
            }
        } else {
            throw new ParameterValidationException("???????????????????????? ???????????? ???? ???????????????????? ??????????????");
        }

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        message.addParam(Constants.TEMPLATE_ID_KEY, templateId);
        message.addParam(Constants.RESOURCE_ID_KEY, staffServiceId);
        message.addParam("restart", true);

        ProcessingBusinessOperation operation = businessHelper.buildOperationAtomic(BusinessOperationType.DEDICATED_APP_SERVICE_UPDATE, message);
        if (operation == null) {
            throw new ResourceIsLockedException("???????????? ??????????, ?????????????????? ???????????????????? ?????????????????????? ????????????????");
        }

        return businessHelper.buildActionByOperation(
                BusinessActionType.DEDICATED_APP_SERVICE_UPDATE_RC_STAFF, message, operation
        );
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
            throw new ResourceNotFoundException("???? ?????????????? ?????????? ???????????? ???? ?????????????? ?????????????????? ??????????????");
        }
        List<Service> services = rcStaffFeignClient.getServicesByAccountIdAndServerIdAndTemplateId(dedicatedAppService.getPersonalAccountId(), serverId, dedicatedAppService.getTemplateId());
        if (CollectionUtils.isNotEmpty(services)) {
            dedicatedAppService.setService(services.get(0));
        }
        return dedicatedAppService;
    }

    private void assertLockedResource(String accountId, @Nullable String resourceId, @Nullable String templateId, String message) throws ResourceIsLockedException {
        List<ProcessingBusinessOperation> operations = processingBusinessOperationRepository.findAllByPersonalAccountIdAndTypeInAndStateIn(
                accountId,
                DEDICATED_APP_SERVICE_OPERATIONS,
                BLOCKED_OPERATIONS_STATES);
        if (operations.stream().anyMatch(operation ->
                (StringUtils.isNotEmpty(resourceId) && resourceId.equals(operation.getParam(Constants.RESOURCE_ID_KEY)) ||
                        (StringUtils.isNotEmpty(templateId) && templateId.equals(operation.getParam(Constants.TEMPLATE_ID_KEY)))))) {
            throw StringUtils.isEmpty(message) ? new ResourceIsLockedException() : new ResourceIsLockedException(message);
        }
    }

    public void processAmqpEventCreateRcStaffFinish(ProcessingBusinessOperation processingBusinessOperation, String staffServiceId) {
        finishUpdateAmqpEvent(processingBusinessOperation.getPersonalAccountId(), staffServiceId, BusinessActionType.DEDICATED_APP_SERVICE_CREATE_RC_STAFF, processingBusinessOperation.getParams());
    }

    public void processAmqpEventUpdateRcStaffFinish(ProcessingBusinessAction action, String staffServiceId) {
        finishUpdateAmqpEvent(
                action.getPersonalAccountId(),
                staffServiceId,
                action.getBusinessActionType(),
                action.getParams()
        );
    }

    public void finishUpdateAmqpEvent(
            String accountId,
            String staffServiceId,
            @Nullable BusinessActionType businessActionType,
            @Nullable Map<String, Object> params
    ) {
        if (params == null) {
            params = Collections.emptyMap();
        }
        Boolean switchedOn = null;
        String templateId = null;
        try {
            switchedOn = (Boolean) params.get(Constants.SWITCHED_ON_KEY);
            templateId = (String) params.get(Constants.TEMPLATE_ID_KEY);
        } catch (ClassCastException ignore) { }
        DedicatedAppService dedicatedAppService;
        if (templateId != null) {
            dedicatedAppService = dedicatedAppServiceRepository.findByPersonalAccountIdAndTemplateId(accountId, templateId);
        } else {
            dedicatedAppService = dedicatedAppServiceRepository.findByStaffServiceId(staffServiceId);
        }

        if (dedicatedAppService == null) {
            return;
        }

        if (dedicatedAppService.getAccountService() != null) {
            if (!dedicatedAppService.getAccountService().isEnabled()
                    && (BusinessActionType.DEDICATED_APP_SERVICE_CREATE_RC_STAFF == businessActionType
                    || Boolean.TRUE.equals(switchedOn))) {
                accountServiceHelper.setEnabledAccountService(dedicatedAppService.getAccountService(), true);
            } else if (dedicatedAppService.getAccountService().isEnabled() && Boolean.FALSE.equals(switchedOn)) {
                accountServiceHelper.setEnabledAccountService(dedicatedAppService.getAccountService(), false);
            }
        }
        if (dedicatedAppService.getStaffServiceId() == null) {
            dedicatedAppService.setStaffServiceId(staffServiceId);
        }
        dedicatedAppServiceRepository.save(dedicatedAppService);
    }

    @Nullable
    public ProcessingBusinessAction create(@NonNull PersonalAccount account, @NonNull String templateId) throws ParameterValidationException, ResourceNotFoundException, ResourceIsLockedException {

        assertAccountIsActive(account);
        assertAccountIsFreezed(account);

        assertPlanAllowedDedicatedApp(account);

        accountHasThisService(account.getId(), templateId);

        Template template = null;
        try {
            template = rcStaffFeignClient.getTemplateAvailableToAccountsById(account.getId(), templateId);
        } catch (Exception ignored) {
            logger.debug("Exception when found Template with id:" + templateId);
        }

        if (template == null) {
            throw new ParameterValidationException("???? ???????????? Template");
        }

        UnixAccount unixAccount = getUnixAccount(account.getId());
        if (unixAccount == null) {
            throw new InternalApiException("???? ?????????????? ???????????????? ???????????? ???? ?????????????? ?????????????????? ??????????????");
        }
        String serverId = unixAccount.getServerId();

        ServicePlan servicePlan = servicePlanRepository.findOneByFeatureAndActive(Feature.DEDICATED_APP_SERVICE, true);
        if (servicePlan == null) {
            logger.error("Could not find ServicePlan for DEDICATED_APP_SERVICE. Database is wrong");
            throw new InternalApiException();
        }

        List<Service> services = rcStaffFeignClient.getServicesByAccountIdAndServerIdAndTemplateId(account.getId(), serverId, templateId);
        Optional<Service> existsService = services.stream().filter(service -> serverId.equals(service.getServerId())).findFirst();//.collect(Collectors.toList());

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(account.getId());
        message.addParam(Constants.SERVER_ID_KEY, serverId);
        message.addParam(Constants.UNIX_ACCOUNT_HOMEDIR_KEY, unixAccount.getHomeDir());

        ProcessingBusinessAction action;

        if (!existsService.isPresent()) {
            message.addParam(Constants.TEMPLATE_ID_KEY, template.getId());

            action = businessHelper.buildActionAndOperation(
                    BusinessOperationType.DEDICATED_APP_SERVICE_CREATE,
                    BusinessActionType.DEDICATED_APP_SERVICE_CREATE_RC_STAFF,
                    message,
                    template.getId(),
                    Service.class
            ).getFirst();

            history.save(account, "?????????????????? ???????????? ???? ???????????????? ?????????????????????? ?????????????? ?? templateId: " + template.getId());
        } else {
            Service staffService = existsService.get();
            if (!staffService.getSwitchedOn()){
                action = switchStaffService(account.getId(),  staffService, true, null);
            } else {
                action = null;
            }
        }

        AccountService accountService = createAccountService(account, template, servicePlan.getService());

        addService(account, template.getId(), accountService);

        history.save(account, "???????????????? ???????????? ???????????????????? ???????????? ?? templateId: " + template.getId());

        return action;
    }

    private AccountService createAccountService(PersonalAccount account, Template template, PaymentService paymentService) {
        String comment;
        if (template instanceof ApplicationServer) {
            ApplicationServer as = (ApplicationServer) template;
            String language = as.getLanguage() == ApplicationServer.Language.JAVASCRIPT ? "JS" : as.getLanguage().name();
            comment = String.format("%s %s", language, as. getVersion());
        } else {
            comment = template.getName();
        }
        AccountService accountService = new AccountService();
        accountService.setEnabled(false);
        accountService.setComment(comment);
        accountService.setPersonalAccountId(account.getId());
        accountService.setQuantity(1);
        accountService.setPaymentService(paymentService);
        accountService.setServiceId(paymentService.getId());
        accountService.setLastBilled(LocalDateTime.now());
        accountService.setFreeze(account.isFreeze());
        return accountServiceHelper.save(accountService);
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
                throw new ParameterValidationException("???????????????? ?????????????????? ?????????????? ???? ???????????????? ?????????????????????? ??????????????");
            }
            PersonalAccount account = accountManager.findOne(accountId);
            if (account == null) {
                throw new ResourceNotFoundException("???? ???????????? ?????????????? " + accountId);
            }
            cancelDedicatedAppService(account, dedicatedAppService);
        } catch (ResourceNotFoundException | ParameterValidationException | FeignException ex) {
            logger.error(String.format("Cannot delete dedicated service with account service %s", accountServiceId), ex);
            throw ex;
        }
    }

    @Nullable
    public ProcessingBusinessAction cancelDedicatedAppService(String accountId, String dedicatedServiceId) {
        DedicatedAppService dedicatedAppService = getService(dedicatedServiceId);
        if (dedicatedAppService == null) {
            throw new ResourceNotFoundException("???? ???????????? ???????????????????? ???????????? ?? id: " + dedicatedServiceId);
        }
        assertAccountIsOwnerOfDedicatedService(accountId, dedicatedAppService);
        PersonalAccount account = accountManager.findOne(accountId);
        if (account == null) {
            throw new ResourceNotFoundException("???? ???????????? ?????????????? " + accountId);
        }
        return cancelDedicatedAppService(account, dedicatedAppService);
    }

    public void switchAllDedicatedAppService(String accountId, boolean state, @Nullable String operationId, @Nullable BiConsumer<RuntimeException, Service> exceptionCallback) throws BaseException, FeignException {

        String serverId = getServerId(accountId);
        if (StringUtils.isEmpty(serverId)) {
            throw new InternalApiException("???? ?????????????? ???????????????????? web-????????????");
        }
        List<Service> staffServices = rcStaffFeignClient.getServiceByAccountIdAndServerId(accountId, serverId).stream()
                .filter(staffService -> accountId.equals(staffService.getAccountId())).collect(Collectors.toList()); //todo ?????????????????????? ???????????? ???????????????????? ?????????????? ?? ???? ?????? ????????????

        List<DedicatedAppService> services = getServices(accountId);

        for (DedicatedAppService appService : services) {
            appService.setActive(state);
            dedicatedAppServiceRepository.save(appService);
            Service staffService = staffServices.stream().filter(ss -> ss.getTemplateId()
                    .equals(appService.getTemplateId())).findFirst().orElse(null);
            if (staffService == null) {
                continue;
            }
            try {
                switchStaffService(accountId, staffService, state, operationId);
            } catch (ResourceIsLockedException e) {
                logger.debug(String.format(
                        "A dedicated service with rc-staff's Service id: %s is locked, templateId: %s, accountId: %s",
                        staffService.getId(),
                        staffService.getTemplateId(),
                        accountId
                ), e);
                if (exceptionCallback != null) {
                    exceptionCallback.accept(e, staffService);
                }
            } catch (RuntimeException e) {
                if (exceptionCallback == null) {
                    throw e;
                } else {
                    exceptionCallback.accept(e, staffService);
                }
            }
        }


    }

    /**
     * @param operationId null ?????????????? ?????????? {@link ProcessingBusinessOperation}, ???????? ???????????? ???????????????????? ????????????????????????.
     * @throws ResourceIsLockedException
     * @throws ParameterValidationException
     */
    private ProcessingBusinessAction switchStaffService(
            String accountId,
            Service staffService,
            boolean state,
            @Nullable String operationId
    ) throws ResourceIsLockedException, ParameterValidationException {

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam(Constants.RESOURCE_ID_KEY, staffService.getId());
        message.addParam(Constants.SWITCHED_ON_KEY, state);
        message.addParam(Constants.TEMPLATE_ID_KEY, staffService.getTemplateId()); // need for assertLockedResource and buildOperationAtomic

        ProcessingBusinessAction action;
        if (StringUtils.isEmpty(operationId)) {
            action = businessHelper.buildActionAndOperation(
                    BusinessOperationType.DEDICATED_APP_SERVICE_UPDATE,
                    BusinessActionType.DEDICATED_APP_SERVICE_UPDATE_RC_STAFF,
                    message,
                    staffService.getTemplateId(),
                    Service.class
            ).getFirst();
        } else {
            action = businessHelper.buildAction(
                    BusinessActionType.DEDICATED_APP_SERVICE_UPDATE_RC_STAFF,
                    message,
                    operationId,
                    staffService.getTemplateId(),
                    Service.class
            );
        }

        history.save(accountId, String.format("?????????????????? ???????????? ???? %s ?????????????????????? ?????????????? ?? id: %s", state ? "??????????????????" : "????????????????????", staffService.getId()));

        return action;
    }

    /**
     * ???????????????? ?????????????? ???????????????????? ???????? ???????????????????????? ???????????????????????? ???? ?????????????????????? ??????????????. ?????????????? DedicatedAppService, AccountService ?? ???????????? ???? rc-staff ???????????? ??????????????????
     * @param account
     * @param dedicatedAppService
     * @throws ParameterValidationException
     * @throws ResourceNotFoundException
     * @throws ResourceIsLockedException
     */
    @Nullable
    private ProcessingBusinessAction cancelDedicatedAppService(@NonNull PersonalAccount account, @NonNull DedicatedAppService dedicatedAppService) throws ParameterValidationException, ResourceNotFoundException, ResourceIsLockedException {
        String serverId = getServerId(account.getId());
        if (StringUtils.isEmpty(serverId)) {
            throw new InternalApiException();
        }
        List<Service> staffServices = rcStaffFeignClient.getServicesByAccountIdAndServerIdAndTemplateId(account.getId(), serverId, dedicatedAppService.getTemplateId());
        staffServices =  staffServices.stream().filter(service -> serverId.equals(service.getServerId())).collect(Collectors.toList()); // ???? ???????????? ????????????

        ProcessingBusinessAction action = null;

        if (staffServices.size() > 0) {
            Service staffService = staffServices.get(0);
            assertServiceIsnotUsed(account.getId(), staffService.getId());
            action = switchStaffService(account.getId(),  staffService, false, null);
        } else {
            assertLockedResource(account.getId(), null, dedicatedAppService.getTemplateId(), null);
            history.save(account, "?????????????????? ???????????? ???? ???????????????????? ?????????????????????? ??????????????. ???????????????? ???? ??????????????, ?????????????? ????????????");
        }
        if (staffServices.size() <= 1) {
            dedicatedAppServiceRepository.deleteById(dedicatedAppService.getId());
            if (accountServiceHelper.hasAccountService(dedicatedAppService.getAccountServiceId())) {
                accountServiceHelper.deleteAccountServiceById(account, dedicatedAppService.getAccountServiceId(), true);
            }
        }

        return action;
    }

    /**
     * ???????????????? ???????????? ???? ?????????????? ?????????????????? ??????????????
     * @param accountId - ??????????????
     * @return - id-?????????????? ?????? ???????????? ???????????? ???????? ???????????? ???? ?????????? ?????? ???? ???????????? ??????????????????????
     */
    private String getServerId(String accountId) {
        Collection<UnixAccount> unixAccounts = null;
        try {
            unixAccounts = rcUserFeignClient.getUnixAccounts(accountId);
        } catch (RuntimeException ex) {
            logger.error("Got exception when attempt get unix-account for account: " + accountId, ex);
        }

        if (CollectionUtils.isEmpty(unixAccounts)) {
            return "";
        } else {
            return unixAccounts.iterator().next().getServerId();
        }
    }

    @Nullable
    private UnixAccount getUnixAccount(String accountId) {
        try {
            Collection<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(accountId);
            return CollectionUtils.isEmpty(unixAccounts) ? null : unixAccounts.iterator().next();
        } catch (Exception ex) {
            logger.error("An exception occurred while loading unix-accounts for accountId: " + accountId, ex);
            return null;
        }
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

    private void assertAccountIsOwnerOfResource(@NonNull String accountId, @NonNull Service staffService) {
        if (!accountId.equals(staffService.getAccountId())) {
            throw new ParameterValidationException("?????????????? ???? ???????????????? ???????????????????? ??????????????");
        }
    }

    private void assertAccountIsOwnerOfResource(String accountId, String resourceId) {
        Service service = null;
        try {
            service = rcStaffFeignClient.getServiceByAccountIdAndId(accountId, resourceId);
        } catch (ResourceNotFoundException ignored) {}
        if (service != null) {
            assertAccountIsOwnerOfResource(accountId, service);
        }
    }

    private void accountHasThisService(String accountId, String templateId) {
        if (findByPersonalAccountIdAndTemplateId(accountId, templateId) != null) {
            throw new ParameterValidationException("???????????????????? ???????????? ?????? ?????????????????? ???? ??????????????");
        }
    }

    private void assertAccountIsOwnerOfDedicatedService(String accountId, DedicatedAppService dedicatedService) {
        if (!Objects.equals(dedicatedService.getPersonalAccountId(), accountId)) {
            throw new ParameterValidationException("?????????????? ???? ???????????????? ???????????????????? ???????????????????? ??????????????");
        }
    }

    private void assertServiceIsnotUsed(String accountId, String staffServiceId) {
        List<WebSite> webSites = rcUserFeignClient.getWebSites(accountId);
        webSites.stream().filter(webSite -> Objects.equals(webSite.getServiceId(), staffServiceId)).findFirst()
                .ifPresent(webSite -> {
                    throw new ParameterValidationException("????????????. ???????????? ???????????????????????? ???? ??????????: " + webSite.getName());
                });
    }

    private void assertAccountIsActive(PersonalAccount account) {
        if (!account.isActive() || account.isPreorder()) {
            throw new ParameterValidationException("?????????????? ???? ??????????????");
        }
    }

    private void assertAccountIsFreezed(PersonalAccount account) {
        if (account.isFreeze()) {
            throw new ParameterValidationException("?????????????? ??????????????????");
        }
    }

    private void assertPlanAllowedDedicatedApp(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());
        if (plan == null) {
            throw new ResourceNotFoundException("???? ?????????????? ?????????? ???????????????? ????????: " + account.getPlanId());
        }

        if (account.getProperties().getAllowDedicatedApps() != null && account.getProperties().getAllowDedicatedApps()) {
            return;
        }

        if (plan.getProhibitedResourceTypes().contains(ResourceType.DEDICATED_APP_SERVICE)) {
            throw new ParameterValidationException("?????????????????????? ?????????????????????? ?????????????? ?????????????????? ?????? ???????????? " + plan.getName());
        }
    }

}
