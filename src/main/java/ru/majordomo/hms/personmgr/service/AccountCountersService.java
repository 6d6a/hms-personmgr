package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.rc.user.resources.Database;
import ru.majordomo.hms.rc.user.resources.Mailbox;
import ru.majordomo.hms.rc.user.resources.Quotable;
import ru.majordomo.hms.rc.user.resources.UnixAccount;

import java.util.List;

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
        List<UnixAccount> unixAccounts = rcUserFeignClient.getUnixAccounts(accountId);
        Long currentQuota = 0L;
        for (Quotable item : unixAccounts) {
            Long itemQuota = item.getQuotaUsed() == null ? 0L : item.getQuotaUsed();
            currentQuota += itemQuota;
        }
        List<Database> databases = rcUserFeignClient.getDatabases(accountId);
        for (Quotable item : databases) {
            Long itemQuota = item.getQuotaUsed() == null ? 0L : item.getQuotaUsed();
            currentQuota += itemQuota;
        }
        List<Mailbox> mailboxes = rcUserFeignClient.getMailboxes(accountId);
        for (Quotable item : mailboxes) {
            Long itemQuota = item.getQuotaUsed() == null ? 0L : item.getQuotaUsed();
            currentQuota += itemQuota;
        }

        return currentQuota;
    }
}
