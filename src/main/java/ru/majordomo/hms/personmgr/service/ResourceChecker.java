package ru.majordomo.hms.personmgr.service;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.Language;
import ru.majordomo.hms.personmgr.common.StaffServiceUtils;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.feign.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Feature;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.WebSite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static ru.majordomo.hms.personmgr.common.Constants.*;

@Component
public class ResourceChecker {
    private final RcUserFeignClient rcUserFeignClient;
    private final RcStaffFeignClient rcStaffFeignClient;
    private final PlanManager planManager;
    private final AccountServiceHelper accountServiceHelper;

    public ResourceChecker(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            AccountServiceHelper accountServiceHelper,
            PlanManager planManager
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.planManager = planManager;
        this.accountServiceHelper = accountServiceHelper;
    }

    public void checkResource(PersonalAccount account, ResourceType resourceType, Map<String, Object> resource) {
        switch (resourceType) {
            case WEB_SITE:
                checkWebSite(account, resource);

                break;
            case SSL_CERTIFICATE:
                checkSSLCertificate(account, resource);

                break;
            case MAILBOX:
                checkMailbox(account);

                break;
            case DATABASE:
                checkDatabase(account, resource);

                break;
            case DATABASE_USER:
                checkDatabaseUser(account, resource);

                break;
            case DOMAIN:
                checkDomain(account);

                break;

            case FTP_USER:
                checkFtpUser(account);

                break;
        }
    }

    private void checkWebSite(PersonalAccount account, Map<String, Object> resource) {
//        List<Service> webSiteServices;
        String webSiteServiceId;

        if (resource.get(SERVICE_ID_KEY) != null) {
            webSiteServiceId = (String) resource.get(SERVICE_ID_KEY);
        } else if (resource.get(APPLICATION_SERVICE_ID_KEY) != null) {
            webSiteServiceId = (String) resource.get(APPLICATION_SERVICE_ID_KEY);
        } else {
            WebSite webSite = getWebSite(account, resource);

            webSiteServiceId = webSite.getServiceId();
        }

        Service webSiteService = rcStaffFeignClient.getServiceById(webSiteServiceId);
        if (webSiteService == null) {
            throw new ParameterValidationException("Сервис для сайта не найден");
        }

        Plan plan = planManager.findOne(account.getPlanId());

        if (plan == null) {
            throw new ResourceNotFoundException("Тарифный план аккаунта не найден");
        }

        boolean allowDedicatedAppService = !plan.getProhibitedResourceTypes().contains(ResourceType.DEDICATED_APP_SERVICE);

        if (plan.getPlanProperties() instanceof VirtualHostingPlanProperties) {
            VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

            Map<Language, List<String>> allowedLanguages = planProperties.getAllowedLanguages();
            if (!serviceHasType(webSiteService, allowedLanguages, allowDedicatedAppService, account.getId())) {
                throw new ResourceNotFoundException("Указанный для вебсайта сервис не разрешен для вашего тарифа");
            }
        } else {
            throw new InternalApiException("Неправильный тип тарифного плана");
        }
    }

    private void checkSSLCertificate(PersonalAccount account, Map<String, Object> params) {
        Plan plan = planManager.findOne(account.getPlanId());

        boolean sslCheckProhibited = true;
        if (params.get(SSL_CHECK_PROHIBITED_KEY) instanceof Boolean) {
            sslCheckProhibited = (Boolean) params.get(SSL_CHECK_PROHIBITED_KEY);
        }

        //Если на тарифе prohibitedResourceTypes содержит SSL_CERTIFICATE, то заблокировать заказ Let's Encrypt
        //И разрешить установку пользовательских сертификатов
        //Или запретить, если это партнерский тариф
        if (sslCheckProhibited && !plan.isSslCertificateAllowed() || plan.isPartnerPlan()) {
            throw new ParameterValidationException("На вашем тарифном плане заказ SSL сертификатов недоступен");
        }
    }

    private void checkMailbox(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());

        if (!plan.isMailboxAllowed()) {
            throw new ParameterValidationException("На вашем тарифном плане добавление почтовых ящиков недоступно");
        }
    }

    private boolean hasSwitchedOnFalseOnly(@Nullable Map<String, Object> params) {
        return params != null && params.get(SWITCHED_ON_KEY) instanceof Boolean && !((Boolean) params.get(SWITCHED_ON_KEY));
    }

    private void checkDatabase(PersonalAccount account, @Nullable Map<String, Object> params) {
        Plan plan = planManager.findOne(account.getPlanId());

        if (!plan.isDatabaseAllowed()) {
            if (plan.getAllowedFeature().contains(Feature.ALLOW_USE_DATABASES)) {
                if (hasSwitchedOnFalseOnly(params)) {   // разрешить выключение баз данных оператором
                    return;
                }
                if (!accountServiceHelper.hasAllowUseDbService(account)) {
                    throw new ParameterValidationException("Поддержка баз данных недоступна, подключите дополнительную услугу");
                }
            } else {
                throw new ParameterValidationException("На вашем тарифном плане работа с базами данных недоступна");
            }
        }
    }

    private void checkDatabaseUser(PersonalAccount account, @Nullable Map<String, Object> params) {
        Plan plan = planManager.findOne(account.getPlanId());

        if (!plan.isDatabaseUserAllowed()) {
            if (plan.getAllowedFeature().contains(Feature.ALLOW_USE_DATABASES)) {
                if (hasSwitchedOnFalseOnly(params)) {
                    return;
                }
                if (!accountServiceHelper.hasAllowUseDbService(account)) {
                    throw new ParameterValidationException("Поддержка баз данных недоступна, подключите дополнительную услугу");
                }
            } else {
                throw new ParameterValidationException("На вашем тарифном плане работа с базами данных недоступна");
            }
        }
    }

    private void checkDomain(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());

        if (!plan.isDomainAllowed()) {
            throw new ParameterValidationException("На вашем тарифном плане работа с доменами недоступна");
        }
    }

    private void checkFtpUser(PersonalAccount account) {
        Plan plan = planManager.findOne(account.getPlanId());

        if (!plan.isFtpUserAllowed()) {
            throw new ParameterValidationException("На вашем тарифном плане использование FTP недоступно");
        }
    }

    private WebSite getWebSite(PersonalAccount account, Map<String, Object> resource) {
        WebSite webSite;
        try {
            webSite = rcUserFeignClient.getWebSite(account.getId(), (String) resource.get(RESOURCE_ID_KEY));
        } catch (Exception e) {
            throw new ParameterValidationException("Ошибка при поиске сайта");
        }

        if (webSite == null) {
            throw new ParameterValidationException("Сайт не найден");
        }
        return webSite;
    }

    private boolean serviceIdHasType(@Nonnull String serviceId, @Nonnull List<Service> services,
                                     @Nonnull Map<Language, List<String>> allowedLanguages, boolean allowDedicatedAppService,
                                     @Nullable String accountId) {
        Service service = services.stream().filter(s -> s.isSwitchedOn() && serviceId.equals(s.getId())).findFirst().orElse(null);
        if (service == null) {
            return false;
        }
        return serviceHasType(service, allowedLanguages, allowDedicatedAppService, accountId);
    }

    public static boolean serviceHasType(@Nullable Service service, Map<Language, List<String>> allowedLanguages, boolean allowDedicatedAppService, String accountId) {
        if (service == null || !service.isSwitchedOn()) {
            return false;
        }

        if (StringUtils.isNotEmpty(service.getAccountId())) {
            return allowDedicatedAppService && service.getAccountId().equals(accountId);
        }
        return StaffServiceUtils.isSuitableTemplate(service.getTemplate(), allowedLanguages);
    }
}
