package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.resources.Quotable;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class AccountCountersService {
    private final RcUserFeignClient rcUserFeignClient;

    @Autowired
    public AccountCountersService(RcUserFeignClient rcUserFeignClient) {
        this.rcUserFeignClient = rcUserFeignClient;
    }

    public Long getCurrentDatabaseCount(String accountId) {
        return rcUserFeignClient.getDatabaseCount(accountId).getCount();
    }

    public Long getCurrentFtpUserCount(String accountId) {
        return rcUserFeignClient.getFtpUserCount(accountId).getCount();
    }

    public Long getCurrentWebSiteCount(String accountId) {
        return rcUserFeignClient.getWebsiteCount(accountId).getCount();
    }

    public Long getCurrentQuotaUsed(String accountId) {
        Long currentQuota = 0L;

        Collection<Quotable> quotables = new ArrayList<>();

        quotables.addAll(rcUserFeignClient.getUnixAccounts(accountId));
        quotables.addAll(rcUserFeignClient.getDatabases(accountId));
        quotables.addAll(rcUserFeignClient.getMailboxes(accountId));

        for (Quotable item : quotables) {
            Long itemQuota = item.getQuotaUsed() == null ? 0L : item.getQuotaUsed();
            currentQuota += itemQuota;
        }

        return currentQuota;
    }
}
