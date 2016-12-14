package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.model.plan.Plan;

@Service
public class PlanCheckerService {
    private final static Logger logger = LoggerFactory.getLogger(PlanCheckerService.class);

    private final RcUserFeignClient rcUserFeignClient;

    private final PlanLimitsService planLimitsService;

    @Autowired
    public PlanCheckerService(RcUserFeignClient rcUserFeignClient, PlanLimitsService planLimitsService) {
        this.rcUserFeignClient = rcUserFeignClient;
        this.planLimitsService = planLimitsService;
    }

    public boolean canAddDatabase(String accountId) {
        Long currentDatabaseCount = getCurrentDatabaseCount(accountId);
        Long planDatabaseCount = planLimitsService.getDatabaseLimit(accountId);

        logger.info("Checking limit for databases. currentDatabaseCount " + currentDatabaseCount + " planDatabaseCount " + planDatabaseCount);

        return planDatabaseCount.compareTo(-1L) == 0 || currentDatabaseCount.compareTo(planDatabaseCount) < 0;
    }

    public boolean canAddFtpUser(String accountId) {
        Long currentFtpUserCount = getCurrentFtpUserCount(accountId);
        Long planFtpUserCount = planLimitsService.getFtpUserLimit(accountId);

        logger.info("Checking FtpUser limit. currentFtpUserCount " + currentFtpUserCount + " planFtpUserCount " + planFtpUserCount);

        return planFtpUserCount.compareTo(-1L) == 0 || currentFtpUserCount.compareTo(planFtpUserCount) < 0;
    }

    public boolean canAddWebSite(String accountId) {
        Long currentWebsiteCount = getCurrentWebSiteCount(accountId);
        Long planWebsiteCount = planLimitsService.getWebsiteLimit(accountId);

        logger.info("Checking WebSite limit. currentWebsiteCount " + currentWebsiteCount + " planWebsiteCount " + planWebsiteCount);

        return planWebsiteCount.compareTo(-1L) == 0 || currentWebsiteCount.compareTo(planWebsiteCount) < 0;
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

    public Long getPlanDatabaseLimit(Plan plan) {
        return planLimitsService.getDatabaseLimit(plan);
    }

    public Long getPlanFtpUserLimit(Plan plan) {
        return planLimitsService.getFtpUserLimit(plan);
    }

    public Long getPlanWebSiteLimit(Plan plan) {
        return planLimitsService.getWebsiteLimit(plan);
    }
}
