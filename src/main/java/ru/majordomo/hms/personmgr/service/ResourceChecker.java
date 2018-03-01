package ru.majordomo.hms.personmgr.service;

import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.personmgr.common.Constants.APPLICATION_SERVICE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_ID_KEY;

@Component
public class ResourceChecker {
    private final RcUserFeignClient rcUserFeignClient;
    private final RcStaffFeignClient rcStaffFeignClient;
    private final PlanRepository planRepository;

    public ResourceChecker(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            PlanRepository planRepository
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.planRepository = planRepository;
    }

    public void checkResource(PersonalAccount account, ResourceType resourceType, Map<String, Object> resource) {
        switch (resourceType) {
            case WEB_SITE:
                checkWebSite(account, resource);

                break;
            case SSL_CERTIFICATE:
                checkSSLCertificate(account);

                break;
            case MAILBOX:
                checkMailbox(account);

                break;
        }
    }

    private void checkWebSite(PersonalAccount account, Map<String, Object> resource) {
        List<Service> webSiteServices;
        String webSiteServiceId;

        if (resource.get(SERVICE_ID_KEY) != null) {
            webSiteServiceId = (String) resource.get(SERVICE_ID_KEY);
        } else if (resource.get(APPLICATION_SERVICE_ID_KEY) != null) {
            webSiteServiceId = (String) resource.get(APPLICATION_SERVICE_ID_KEY);
        } else {
            WebSite webSite = getWebSite(account, resource);

            webSiteServiceId = webSite.getServiceId();
        }

        Server webSiteServer = rcStaffFeignClient.getServerByServiceId(webSiteServiceId);

        if (webSiteServer == null) {
            throw new ParameterValidationException("веб-сервер не найден");
        }

        try {
            webSiteServices = rcStaffFeignClient.getWebsiteServicesByServerId(webSiteServer.getId());
        } catch (Exception e) {
            throw new ParameterValidationException("Ошибка при получении сервисов для вебсайтов для для сервера " + webSiteServer.getId());
        }

        if (webSiteServices == null || webSiteServices.isEmpty()) {
            throw new ParameterValidationException("Список сервисов вебсайтов для сервера " + webSiteServer.getId() + " пуст");
        }

        Plan plan = planRepository.findOne(account.getPlanId());

        if (plan == null) {
            throw new ResourceNotFoundException("Тарифный план аккаунта не найден");
        }

        if (plan.getPlanProperties() != null) {
            VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

            Set<String> allowedServiceTypes = planProperties.getWebSiteAllowedServiceTypes();

            if (allowedServiceTypes != null && !allowedServiceTypes.isEmpty()) {
                boolean foundAllowedService = serviceIdHasType(webSiteServiceId, webSiteServices, allowedServiceTypes);

                if (!foundAllowedService) {
                    throw new ResourceNotFoundException("Указанный для вебсайта serviceId не разрешен для вашего тарифа");
                }
            }
        }
    }

    private void checkSSLCertificate(PersonalAccount account) {
        Plan plan = planRepository.findOne(account.getPlanId());

        if (!plan.isSslCertificateAllowed()) {
            throw new ParameterValidationException("На вашем тарифном плане заказ SSL сертификатов недоступен");
        }
    }

    private void checkMailbox(PersonalAccount account) {
        Plan plan = planRepository.findOne(account.getPlanId());

        if (!plan.isMailboxAllowed()) {
            throw new ParameterValidationException("На вашем тарифном плане добавление почтовых ящиков недоступно");
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

    private boolean serviceIdHasType(String serviceId, List<Service> services, Set<String> serviceTypes) {
        for (Service service : services) {
            if (service.getId().equals(serviceId)) {
                if (serviceHasType(service, serviceTypes)) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean serviceHasType(Service service, Set<String> serviceTypes) {
        for (String serviceType : serviceTypes) {
            if (serviceType.endsWith("*")) {
                if (service.getServiceTemplate().getServiceTypeName()
                        .startsWith(serviceType.substring(0, serviceType.length()-1))) {
                    return true;
                }
            } else {
                if (service.getServiceTemplate().getServiceTypeName().equals(serviceType)) {
                    return true;
                }
            }
        }
        return false;
    }
}
