package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static ru.majordomo.hms.personmgr.common.Utils.planLimitsComparator;

@Service
public class PlanCheckerService {
    private final static Logger logger = LoggerFactory.getLogger(PlanCheckerService.class);

    private final PlanLimitsService planLimitsService;

    private final AccountCountersService accountCountersService;
    @Autowired
    public PlanCheckerService(
            PlanLimitsService planLimitsService,
            AccountCountersService accountCountersService
    ) {
        this.planLimitsService = planLimitsService;
        this.accountCountersService = accountCountersService;
    }

    public boolean canAddDatabase(String accountId) {
        Long currentDatabaseCount = accountCountersService.getCurrentDatabaseCount(accountId);
        Long planDatabaseCount = planLimitsService.getDatabaseFreeLimit(accountId);

        logger.debug("Checking limit for databases. currentDatabaseCount " + currentDatabaseCount + " planDatabaseCount " + planDatabaseCount);

        return planLimitsComparator(currentDatabaseCount, planDatabaseCount) < 0;
    }

    public boolean canAddFtpUser(String accountId) {
        Long currentFtpUserCount = accountCountersService.getCurrentFtpUserCount(accountId);
        Long planFtpUserCount = planLimitsService.getFtpUserLimit(accountId);

        logger.debug("Checking FtpUser limit. currentFtpUserCount " + currentFtpUserCount + " planFtpUserCount " + planFtpUserCount);

        return planLimitsComparator(currentFtpUserCount, planFtpUserCount) < 0;
    }

    public boolean canAddWebSite(String accountId) {
        Long currentWebsiteCount = accountCountersService.getCurrentWebSiteCount(accountId);
        Long planWebsiteCount = planLimitsService.getWebsiteFreeLimit(accountId);

        logger.debug("Checking WebSite limit. currentWebsiteCount " + currentWebsiteCount + " planWebsiteCount " + planWebsiteCount);

        return planLimitsComparator(currentWebsiteCount, planWebsiteCount) < 0;
    }
}
