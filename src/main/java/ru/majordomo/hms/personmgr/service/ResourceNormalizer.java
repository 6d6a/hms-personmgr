package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.Language;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.feign.RcStaffFeignClient;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.comparator.StaffServiceComparator;
import ru.majordomo.hms.rc.user.resources.WebSite;

import static ru.majordomo.hms.personmgr.common.Constants.RESOURCE_ID_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_ID_KEY;

@Component
public class ResourceNormalizer {
    private final RcUserFeignClient rcUserFeignClient;
    private final RcStaffFeignClient rcStaffFeignClient;
    private final BusinessHelper businessHelper;
    private final AccountHistoryManager history;

    public ResourceNormalizer(
            RcUserFeignClient rcUserFeignClient,
            RcStaffFeignClient rcStaffFeignClient,
            BusinessHelper businessHelper,
            AccountHistoryManager history
    ) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcStaffFeignClient = rcStaffFeignClient;
        this.businessHelper = businessHelper;
        this.history = history;
    }

    public void normalizeResources(PersonalAccount account, ResourceType resourceType, Plan plan, boolean applyChanges) {
        switch (resourceType) {
            case WEB_SITE:
                normalizeWebSites(account, plan, applyChanges);

                break;
        }
    }

    private void normalizeWebSites(PersonalAccount account, Plan plan, boolean applyChanges) {
        List<WebSite> webSites = rcUserFeignClient.getWebSites(account.getId());
        List<Service> webSiteServices;

        for (WebSite webSite : webSites) {
            Server webSiteServer = rcStaffFeignClient.getServerByServiceId(webSite.getServiceId());

            if (webSiteServer == null) {
                throw new ParameterValidationException("веб-сервер не найден");
            }

            try {
                webSiteServices = rcStaffFeignClient.getWebsiteServicesByAccountIdAndServerId(account.getId(), webSiteServer.getId());
            } catch (Exception e) {
                e.printStackTrace();
                throw new ParameterValidationException("Ошибка при получении сервисов для вебсайтов для для сервера " + webSiteServer.getId());
            }

            if (webSiteServices == null || webSiteServices.isEmpty()) {
                throw new ParameterValidationException("Список сервисов вебсайтов для сервера " + webSiteServer.getId() + " пуст");
            }

            if (!(plan.getPlanProperties() instanceof VirtualHostingPlanProperties)) {
                throw new ParameterValidationException("Необходимый для вебсайта сервис не найден");
            }
            VirtualHostingPlanProperties planProperties = (VirtualHostingPlanProperties) plan.getPlanProperties();
            Map<Language, List<String>> allowedLanguages = planProperties.getAllowedLanguages();

            boolean allowDedicatedAppService = account.getProperties().getAllowDedicatedApps() != null &&
                    account.getProperties().getAllowDedicatedApps() ||
                    !plan.getProhibitedResourceTypes().contains(ResourceType.DEDICATED_APP_SERVICE);

            Service currentWebSiteService = webSiteServices
                    .stream()
                    .filter(service -> service.getId().equals(webSite.getServiceId()))
                    .findFirst()
                    .orElseThrow(() -> new ParameterValidationException("Текущий сервис для вебсайта не найден"));

            boolean allowedCurrentWebSiteService = ResourceChecker.serviceHasType(currentWebSiteService, allowedLanguages, allowDedicatedAppService, account.getId());

            if (!allowedCurrentWebSiteService) {
                //ставим какой-то, на самом деле с самой большой версией
                Service selectedWebSiteService = webSiteServices.stream()
                        .filter(service -> ResourceChecker.serviceHasType(service, allowedLanguages, allowDedicatedAppService, account.getId()))
                        .min(new StaffServiceComparator())
                        .orElseThrow(() -> new ParameterValidationException("Необходимый для вебсайта сервис не найден"));

                if (applyChanges) {
                    SimpleServiceMessage message = new SimpleServiceMessage();
                    message.setParams(new HashMap<>());
                    message.setAccountId(account.getId());
                    message.addParam(RESOURCE_ID_KEY, webSite.getId());
                    message.addParam(SERVICE_ID_KEY, selectedWebSiteService.getId());

                    businessHelper.buildAction(BusinessActionType.WEB_SITE_UPDATE_RC, message);

                    String historyMessage = "Отправлена заявка на изменение serviceId сайта '" + webSite.getName() + "'";

                    history.saveForOperatorService(account, historyMessage);
                }
            }
        }
    }
}
