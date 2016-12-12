package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.common.Count;

@Service
public class PlanCheckerService {
    private final static Logger logger = LoggerFactory.getLogger(PlanCheckerService.class);

    private final RcUserFeignClient rcUserFeignClient;

    private final RcUserFeignClientFallback rcUserFeignClientFallback;

    @Autowired
    public PlanCheckerService(RcUserFeignClient rcUserFeignClient, RcUserFeignClientFallback rcUserFeignClientFallback) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.rcUserFeignClientFallback = rcUserFeignClientFallback;
    }

    public boolean canAddDatabase(String accountId) {
        Count currentDatabaseCount = rcUserFeignClient.getDatabaseCount(accountId);
        Count planDatabaseCount = rcUserFeignClientFallback.getDatabaseCount(accountId);

        logger.info("Checking limit for databases. currentDatabaseCount " + currentDatabaseCount + " planDatabaseCount " + planDatabaseCount);

        return planDatabaseCount.compareTo(new Count(-1)) != 0 || currentDatabaseCount.compareTo(planDatabaseCount) < 0;
    }

    public boolean canAddFtpUser(String accountId) {
        Count currentFtpUserCount = rcUserFeignClient.getFtpUserCount(accountId);
        Count planFtpUserCount = rcUserFeignClientFallback.getFtpUserCount(accountId);

        logger.info("Checking FtpUser limit. currentFtpUserCount " + currentFtpUserCount + " planFtpUserCount " + planFtpUserCount);

        return planFtpUserCount.compareTo(new Count(-1)) != 0 || currentFtpUserCount.compareTo(planFtpUserCount) < 0;
    }

    public boolean canAddWebSite(String accountId) {
        Count currentWebsiteCount = rcUserFeignClient.getWebsiteCount(accountId);
        Count planWebsiteCount = rcUserFeignClientFallback.getWebsiteCount(accountId);

        logger.info("Checking WebSite limit. currentWebsiteCount " + currentWebsiteCount + " planWebsiteCount " + planWebsiteCount);

        return planWebsiteCount.compareTo(new Count(-1)) != 0 || currentWebsiteCount.compareTo(planWebsiteCount) < 0;
    }

    public Long getCurrentDatabaseCount(String accountId) {
        return rcUserFeignClient.getDatabaseCount(accountId).getCount();
    }
}
