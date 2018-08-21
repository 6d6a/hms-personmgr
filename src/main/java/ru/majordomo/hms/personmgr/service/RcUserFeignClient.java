package ru.majordomo.hms.personmgr.service;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.personmgr.config.FeignConfig;
import ru.majordomo.hms.personmgr.common.Count;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@FeignClient(name = "rc-user", configuration = FeignConfig.class)
public interface RcUserFeignClient {
    @GetMapping(value = "/{accountId}/database/count", consumes = "application/json")
    Count getDatabaseCount(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/{accountId}/website/count", consumes = "application/json")
    Count getWebsiteCount(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/{accountId}/ftp-user/count", consumes = "application/json")
    Count getFtpUserCount(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/{accountId}/resource-archive/count", consumes = "application/json")
    Count getResourceArchiveCount(
            @PathVariable("accountId") String accountId,
            @RequestParam(value = "archivedResourceId", required = false) String archivedResourceId,
            @RequestParam(value = "resourceType", required = false) ResourceArchiveType resourceType
    );

    @GetMapping(value = "/{accountId}/resource-archive/{resourceArchiveId}")
    ResourceArchive getResourceArchive(@PathVariable("accountId") String accountId, @PathVariable("resourceArchiveId") String resourceArchiveId);

    @GetMapping(value = "/resource-archive")
    Collection<ResourceArchive> getResourceArchives();

    @GetMapping(value = "/{accountId}/unix-account")
    Collection<UnixAccount> getUnixAccounts(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/{accountId}/database")
    Collection<Database> getDatabases(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/{accountId}/database/{databaseId}")
    Database getDatabase(@PathVariable("accountId") String accountId, @PathVariable("databaseId") String databaseId);

    @GetMapping(value = "/{accountId}/database-user")
    List<DatabaseUser> getDatabaseUsers(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/{accountId}/database-user/{databaseUserId}")
    DatabaseUser getDatabaseUser(@PathVariable("accountId") String accountId, @PathVariable("databaseUserId") String databaseUserId);

    @GetMapping(value = "/{accountId}/mailbox")
    Collection<Mailbox> getMailboxes(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/{accountId}/website/{webSiteId}", consumes = "application/json")
    WebSite getWebSite(@PathVariable("accountId") String accountId, @PathVariable("webSiteId") String webSiteId);

    @GetMapping(value = "/{accountId}/website/find", consumes = "application/json")
    WebSite getWebSiteByDomainId(@PathVariable("accountId") String accountId, @RequestParam("domainId") String domainId);

    @GetMapping(value = "/{accountId}/website", consumes = "application/json")
    List<WebSite> getWebSites(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/{accountId}/person/{personId}", consumes = "application/json")
    Person getPerson(@PathVariable("accountId") String accountId, @PathVariable("personId") String personId);

    @GetMapping(value = "/{accountId}/person", consumes = "application/json")
    List<Person> getPersons(@PathVariable("accountId") String accountId);

    @PostMapping(value = "/{accountId}/person", consumes = "application/json")
    Person addPersonByNicHandle(@PathVariable("accountId") String accountId, @RequestBody Map<String, String> requestBody);

    @GetMapping(value = "/{accountId}/domain", consumes = "application/json")
    List<Domain> getDomains(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/{accountId}/domain/{domainId}", consumes = "application/json")
    Domain getDomain(@PathVariable("accountId") String accountId, @PathVariable("domainId") String domainId);

    @GetMapping(value = "{accountId}/redirect/{redirectId}", consumes = "application/json")
    Redirect getRedirect(@PathVariable("accountId") String accountId, @PathVariable("redirectId") String redirectId);

    @GetMapping(value = "/domain/filter", consumes = "application/json")
    List<Domain> getExpiringDomains(
            @RequestParam("paidTillStart") String paidTillStart,
            @RequestParam("paidTillEnd") String paidTillEnd
    );

    @GetMapping(value = "/{accountId}/domain/filter", consumes = "application/json")
    List<Domain> getExpiringDomainsByAccount(
            @PathVariable("accountId") String accountId,
            @RequestParam("paidTillStart") String paidTillStart,
            @RequestParam("paidTillEnd") String paidTillEnd
    );

    @GetMapping(value = "/{accountId}/ftp-user", consumes = "application/json")
    List<FTPUser> getFTPUsers(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/{accountId}/ssl-certificate", consumes = "application/json")
    Collection<SSLCertificate> getSSLCertificates(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/domain/find", consumes = "application/json")
    Domain findDomain(@RequestParam("name") String name) throws ResourceNotFoundException;

    @PostMapping(value = "/{accountId}/account-move", consumes = "application/json")
    Boolean moveAccount(@PathVariable("accountId") String accountId, @RequestBody Map<String, String> message);

    @GetMapping(value = "/{accountId}/person/find", consumes = "application/json")
    List<Person> getPersonsByAccountIdAndNicHandle (@PathVariable("accountId") String accountId, @RequestParam("nicHandle") String nicHandle);

    @GetMapping(value = "{accountId}/redirect", consumes = "application/json")
    List<Redirect> getRedirects(String accountId);

    @GetMapping(value = "/unix-account/filter", headers = {"X-HMS-Projection=pm"})
    List<UnixAccount> filterUnixAccounts(@RequestParam Map<String, String> filterParams);
}
