package ru.majordomo.hms.personmgr.feign;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import ru.majordomo.hms.personmgr.config.FeignConfig;
import ru.majordomo.hms.personmgr.common.Count;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.rc.user.resources.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    @GetMapping(value = "/{accountId}/unix-account")
    List<UnixAccount> getUnixAccountList(@PathVariable("accountId") String accountId);

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

    @GetMapping(value = "/{accountId}/mailbox")
    List<Mailbox> getMailboxList(@PathVariable("accountId") String accountId);

    /**
     * @throws ResourceNotFoundException ???????? ?????????? ??????
     * @throws FeignException ???????????????????????? ?????????????????? ?????????????? ?????? ???? ?????????????? ?????????????????? ????????????
     */
    @GetMapping(value = "/{accountId}/website/{webSiteId}", consumes = "application/json")
    WebSite getWebSite(@Nonnull @PathVariable("accountId") String accountId, @Nonnull @PathVariable("webSiteId") String webSiteId) throws ResourceNotFoundException, FeignException;

    @GetMapping(value = "/{accountId}/website/find", consumes = "application/json")
    WebSite getWebSiteByDomainId(@PathVariable("accountId") String accountId, @RequestParam("domainId") String domainId);

    @GetMapping(value = "/{accountId}/website", consumes = "application/json")
    List<WebSite> getWebSites(@PathVariable("accountId") String accountId);

    /**
     * ???????????? ???????????? ?????????????? WebSite, ?????? Transient ???????????????? ?? ??????. ???? ?????????????? ???????????????????? ?? ???????????? ???????????????????? ????????????, unix-???????????????? ?? ??.??.
     * @param accountId - PersonalAccount
     * @return - ???????????? ????????????
     */
    @GetMapping(value = "/{accountId}/website?withoutBuiltIn=true", consumes = "application/json")
    List<WebSite> getWebSitesOnly(@PathVariable("accountId") String accountId);

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

    @GetMapping(value = "/{accountId}/redirect/{redirectId}", consumes = "application/json")
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

    @GetMapping(value = "/{accountId}/redirect", consumes = "application/json")
    List<Redirect> getRedirects(@PathVariable("accountId") String accountId);

    @GetMapping(value = "/unix-account/filter", headers = {"X-HMS-Projection=pm"})
    List<UnixAccount> filterUnixAccounts(@RequestParam Map<String, String> filterParams);

    @GetMapping(value = "/stat/account-id-and-field/{resource}/{fieldName}", consumes = "application/json")
    Map<String, String> getAccountIdAndField(@PathVariable("resource") String resource, @PathVariable("fieldName") String fieldName);

    @GetMapping(value = "/{accountId}/domain/get-dns-record/{recordId}", consumes = "application/json")
    DNSResourceRecord getRecord(@PathVariable("accountId") String accountId, @PathVariable("recordId") String recordId);

    @PostMapping(value = "import/{accountId}", consumes = "application/json")
    void importToMongo(@PathVariable String accountId, @RequestParam("accountEnabled") boolean accountEnabled, @RequestParam("allowAntispam") boolean allowAntispam);
}
