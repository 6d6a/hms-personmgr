package ru.majordomo.hms.personmgr.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;

import ru.majordomo.hms.rc.staff.resources.Server;
import ru.majordomo.hms.rc.staff.resources.Service;
import ru.majordomo.hms.rc.staff.resources.Storage;
import ru.majordomo.hms.personmgr.config.FeignConfig;
import ru.majordomo.hms.rc.staff.resources.template.Template;

@FeignClient(name = "RC-STAFF", configuration = FeignConfig.class)
public interface RcStaffFeignClient {
    @RequestMapping(method = RequestMethod.GET, value = "/server/filter?server-role=shared-hosting&state=active", consumes = "application/json;utf8")
    Server getActiveHostingServer();

    @RequestMapping(method = RequestMethod.GET, value = "/server-ip-info?serverId={serverId}")
    Map<String, String> getServerIpInfoByServerId(@PathVariable("serverId") String serverId);

    @RequestMapping(method = RequestMethod.GET, value = "/server/filter?server-role=mysql-database-server&state=active", consumes = "application/json;utf8")
    Server getActiveDatabaseServer();

    @RequestMapping(method = RequestMethod.GET, value = "/server/filter?server-role=mail-storage&state=active", consumes = "application/json;utf8")
    Server getActiveMailboxServer();

    @RequestMapping(method = RequestMethod.GET, value = "/server/{serverId}/active-storage", consumes = "application/json;utf8")
    Storage getActiveMailboxStorageByServerId(@PathVariable("serverId") String serverId);

    @RequestMapping(method = RequestMethod.GET, value = "/server/{serverId}", consumes = "application/json;utf8")
    Server getServerById(@PathVariable("serverId") String serverId);

    @RequestMapping(method = RequestMethod.GET, value = "/server/filter?service-id={serviceId}")
    Server getServerByServiceId(@PathVariable("serviceId") String serviceId);

    @RequestMapping(method = RequestMethod.GET, value = "/server/{serverId}/services?service-type=WEBSITE")
    List<Service> getWebsiteServicesByServerId(@PathVariable("serverId") String serverId);

    @RequestMapping(method = RequestMethod.GET, value = "/server/{serverId}/services?service-type=DATABASE")
    List<Service> getDatabaseServicesByServerId(@PathVariable("serverId") String serverId);

    @RequestMapping(method = RequestMethod.GET, value = "/server/{serverId}/services?service-type=STAFF_NGINX")
    List<Service> getNginxServicesByServerId(@PathVariable("serverId") String serverId);

    @RequestMapping(method = RequestMethod.GET, value = "/server/{serverId}/services?service-type={serviceType}")
    List<Service> getServicesByServerIdAndServiceType(@PathVariable("serverId") String serverId, @PathVariable("serviceType") String serviceType);

    @RequestMapping(method = RequestMethod.GET, value = "/service")
    List<Service> getServices();

    @GetMapping(value = "/service", headers = "X-HMS-Projection=OnlyIdAndName")
    List<Service> getServicesOnlyIdAndName();

    @GetMapping(value = "/server", headers = "X-HMS-Projection=OnlyIdAndName")
    List<Server> getServersOnlyIdAndName();

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/template")
    List<Template> getTemplatesAvailableToAccounts(@PathVariable String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/template/{templateId}")
    Template getTemplateAvailableToAccountsById(@PathVariable String accountId, @PathVariable String templateId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/server/{serverId}/services?service-type=WEBSITE")
    List<Service> getWebsiteServicesByAccountIdAndServerId(@PathVariable String accountId, @PathVariable String serverId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/server/{serverId}/services?service-type=DATABASE")
    List<Service> getDatabaseServicesByAccountIdAndServerId(@PathVariable String accountId, @PathVariable String serverId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/server/{serverId}/services?service-type=STAFF_NGINX")
    List<Service> getNginxServicesByAccountIdAndServerId(@PathVariable String accountId, @PathVariable String serverId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/server/{serverId}/services?service-type={serviceType}")
    List<Service> getServicesByAccountIdAndServerIdAndServiceType(
            @PathVariable String accountId,
            @PathVariable String serverId,
            @PathVariable String serviceType
    );

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/server/{serverId}/services?templateId={templateId}")
    List<Service> getServicesByAccountIdAndServerIdAndTemplateId(
            @PathVariable String accountId,
            @PathVariable String serverId,
            @PathVariable String templateId
    );

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/service?templateId={templateId}")
    List<Service> getServicesByAccountIdAndTemplateId(
            @PathVariable String accountId,
            @PathVariable String templateId
    );

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/service/{serviceId}")
    Service getServiceByAccountIdAndId(@PathVariable String accountId, @PathVariable String serviceId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/server/{serverId}/services?onlyDedicated=true")
    List<Service> getServiceByAccountIdAndServerId(@PathVariable String accountId, @PathVariable String serverId);
}
