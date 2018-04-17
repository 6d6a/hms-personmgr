package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ru.majordomo.hms.personmgr.config.FeignConfig;
import ru.majordomo.hms.personmgr.common.Count;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@FeignClient(name = "rc-user", configuration = FeignConfig.class)
public interface RcUserFeignClient {
    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/database/count", consumes = "application/json")
    Count getDatabaseCount(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/website/count", consumes = "application/json")
    Count getWebsiteCount(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/ftp-user/count", consumes = "application/json")
    Count getFtpUserCount(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/unix-account")
    Collection<UnixAccount> getUnixAccounts(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/database")
    Collection<Database> getDatabases(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/database/{databaseId}")
    Database getDatabase(@PathVariable("accountId") String accountId, @PathVariable("databaseId") String databaseId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/database-user")
    List<DatabaseUser> getDatabaseUsers(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/database-user/{databaseUserId}")
    DatabaseUser getDatabaseUser(@PathVariable("accountId") String accountId, @PathVariable("databaseUserId") String databaseUserId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/mailbox")
    Collection<Mailbox> getMailboxes(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/website/{webSiteId}", consumes = "application/json")
    WebSite getWebSite(@PathVariable("accountId") String accountId, @PathVariable("webSiteId") String webSiteId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/website", consumes = "application/json")
    List<WebSite> getWebSites(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/person/{personId}", consumes = "application/json")
    Person getPerson(@PathVariable("accountId") String accountId, @PathVariable("personId") String personId);

    @RequestMapping(method = RequestMethod.POST, value = "/{accountId}/person", consumes = "application/json")
    Person addPersonByNicHandle(@PathVariable("accountId") String accountId, @RequestBody Map<String, String> requestBody);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/domain", consumes = "application/json")
    List<Domain> getDomains(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/domain/{domainId}", consumes = "application/json")
    Domain getDomain(@PathVariable("accountId") String accountId, @PathVariable("domainId") String domainId);

    @RequestMapping(method = RequestMethod.GET, value = "/domain/filter", consumes = "application/json")
    List<Domain> getExpiringDomains(
            @RequestParam("paidTillStart") String paidTillStart,
            @RequestParam("paidTillEnd") String paidTillEnd
    );

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/domain/filter", consumes = "application/json")
    List<Domain> getExpiringDomainsByAccount(
            @PathVariable("accountId") String accountId,
            @RequestParam("paidTillStart") String paidTillStart,
            @RequestParam("paidTillEnd") String paidTillEnd
    );

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/ftp-user", consumes = "application/json")
    List<FTPUser> getFTPUsers(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/{accountId}/ssl-certificate", consumes = "application/json")
    Collection<SSLCertificate> getSSLCertificates(@PathVariable("accountId") String accountId);

    @RequestMapping(method = RequestMethod.GET, value = "/domain/find", consumes = "application/json")
    Domain findDomain(@RequestParam("name") String name) throws ResourceNotFoundException;

    @RequestMapping(method = RequestMethod.POST, value = "/{accountId}/account-move", consumes = "application/json")
    Boolean moveAccount(@PathVariable("accountId") String accountId, @RequestBody Map<String, String> message);
}
