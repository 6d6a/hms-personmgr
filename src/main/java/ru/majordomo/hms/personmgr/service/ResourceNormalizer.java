package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_ID_KEY;

@Component
public class ResourceNormalizer {
    private final RcUserFeignClient rcUserFeignClient;
    private final RcStaffFeignClient rcStaffFeignClient;
    private final AccountHelper accountHelper;
    private final BusinessHelper businessHelper;

    public ResourceNormalizer(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            AccountHelper accountHelper,
            BusinessHelper businessHelper
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.accountHelper = accountHelper;
        this.businessHelper = businessHelper;
    }

    public void normalizeResources(PersonalAccount account, ResourceType resourceType, Plan plan) {
        switch (resourceType) {
            case WEB_SITE:
                normalizeWebSites(account, plan);

                break;
        }
    }

    private void normalizeWebSites(PersonalAccount account, Plan plan) {
        List<WebSite> webSites = rcUserFeignClient.getWebSites(account.getId());
        List<Service> webSiteServices;

        for (WebSite webSite : webSites) {
            Server webSiteServer = rcStaffFeignClient.getServerByServiceId(webSite.getServiceId());

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

            if (plan.getPlanProperties() != null) {
                VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();

                Set<String> allowedServiceTypes = planProperties.getWebSiteAllowedServiceTypes();

                if (allowedServiceTypes != null && !allowedServiceTypes.isEmpty()) {
                    Service currentWebSiteService = webSiteServices
                            .stream()
                            .filter(service -> service.getId().equals(webSite.getServiceId()))
                            .findFirst()
                            .orElseThrow(() -> new ParameterValidationException("Текущий сервис для вебсайта не найден"));

                    if (!allowedServiceTypes.contains(currentWebSiteService.getServiceTemplate().getServiceTypeName())) {
                        //ставим какой-то, на самом деле первый ;) из доступных
                        Service selectedWebSiteService = webSiteServices
                                .stream()
                                .filter(service -> service.getServiceTemplate().getServiceTypeName().equals(allowedServiceTypes.iterator().next()))
                                .findFirst()
                                .orElseThrow(() -> new ParameterValidationException("Необходимый для вебсайта сервис не найден"));

                        SimpleServiceMessage message = new SimpleServiceMessage();
                        message.setParams(new HashMap<>());
                        message.setAccountId(account.getId());
                        message.addParam(RESOURCE_ID_KEY, webSite.getId());
                        message.addParam(SERVICE_ID_KEY, selectedWebSiteService.getId());

                        businessHelper.buildAction(BusinessActionType.WEB_SITE_UPDATE_RC, message);

                        String historyMessage = "Отправлена заявка на изменение serviceId сайта '" + webSite.getName() + "'";

                        accountHelper.saveHistoryForOperatorService(account, historyMessage);
                    }
                }
            }
        }
    }
}
