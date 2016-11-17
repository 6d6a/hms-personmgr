package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.DBType;
import ru.majordomo.hms.personmgr.common.FinService;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.common.message.ImportMessage;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanPropertyLimit;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

import static ru.majordomo.hms.personmgr.common.StringConstants.PLAN_SERVICE_PREFIX;

/**
 * PlanDBImportService
 */
@Service
public class PlanDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PlanDBImportService.class);

    private JdbcTemplate jdbcTemplate;
    private PlanRepository planRepository;
    private FinFeignClient finFeignClient;
    private List<Plan> planList = new ArrayList<>();

    @Autowired
    public PlanDBImportService(JdbcTemplate jdbcTemplate, PlanRepository planRepository, FinFeignClient finFeignClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.planRepository = planRepository;
        this.finFeignClient = finFeignClient;
    }

    public void pull() {
        String query = "SELECT Plan_ID, name, cost, QuotaKB, db, apache, active, username, sites, web_cpu_limit, db_cpu_limit FROM plan";
        planList = jdbcTemplate.query(query, (rs, rowNum) -> {
            VirtualHostingPlanProperties planProperties = new VirtualHostingPlanProperties();
            if (rs.getInt("Plan_ID") == 9802) {
                planProperties.setFtpLimit(new PlanPropertyLimit(5, -1));
            } else {
                planProperties.setFtpLimit(new PlanPropertyLimit(-1, -1));
            }

            planProperties.setWebCpuLimit(new PlanPropertyLimit(rs.getInt("web_cpu_limit")));
            planProperties.setDbCpuLimit(new PlanPropertyLimit(rs.getInt("db_cpu_limit")));
            planProperties.setQuotaKBLimit(new PlanPropertyLimit(rs.getInt("QuotaKB")));
            planProperties.setSitesLimit(new PlanPropertyLimit(rs.getInt("sites")));
            planProperties.setSshLimit(new PlanPropertyLimit(-1));
            planProperties.setPhpEnabled(rs.getBoolean("apache"));

            if (rs.getInt("Plan_ID") == 9806 || rs.getInt("Plan_ID") == 9807) {
                planProperties.setBusinessServices(true);
            }

            Map<DBType, PlanPropertyLimit> dbList = new HashMap<>();
            dbList.put(DBType.MYSQL, new PlanPropertyLimit(rs.getInt("db")));

            planProperties.setDb(dbList);

            FinService finService = new FinService();
            finService.setPaymentType(ServicePaymentType.MONTH);
            finService.setAccountType(AccountType.VIRTUAL_HOSTING);
            finService.setActive(rs.getBoolean("active"));
            finService.setCost(rs.getBigDecimal("cost"));
            finService.setLimit(1);
            finService.setOldId(PLAN_SERVICE_PREFIX + rs.getString("Plan_ID"));
            finService.setName(rs.getString("username"));

            finService = finFeignClient.createService(finService);

            logger.info(finService.toString());

            return new Plan(rs.getString("username"), rs.getString("name"), finService.getId(), rs.getString("Plan_ID"), AccountType.VIRTUAL_HOSTING, rs.getBoolean("active"), planProperties);
        });
    }

    public void pull(String planId, String finServiceId) {
        String query = "SELECT Plan_ID, name, cost, QuotaKB, db, apache, active, username, sites, web_cpu_limit, db_cpu_limit FROM plan WHERE Plan_ID = ?";
        planList = jdbcTemplate.query(query,
                new Object[]{planId},
                (rs, rowNum) -> {
                    VirtualHostingPlanProperties planProperties = new VirtualHostingPlanProperties();
                    if (rs.getInt("Plan_ID") == 9802) {
                        planProperties.setFtpLimit(new PlanPropertyLimit(5, -1));
                    } else {
                        planProperties.setFtpLimit(new PlanPropertyLimit(-1, -1));
                    }

                    planProperties.setWebCpuLimit(new PlanPropertyLimit(rs.getInt("web_cpu_limit")));
                    planProperties.setDbCpuLimit(new PlanPropertyLimit(rs.getInt("db_cpu_limit")));
                    planProperties.setQuotaKBLimit(new PlanPropertyLimit(rs.getInt("QuotaKB")));
                    planProperties.setSitesLimit(new PlanPropertyLimit(rs.getInt("sites")));
                    planProperties.setSshLimit(new PlanPropertyLimit(-1));
                    planProperties.setPhpEnabled(rs.getBoolean("apache"));

                    if (rs.getInt("Plan_ID") == 9806 || rs.getInt("Plan_ID") == 9807) {
                        planProperties.setBusinessServices(true);
                    }

                    Map<DBType, PlanPropertyLimit> dbList = new HashMap<>();
                    dbList.put(DBType.MYSQL, new PlanPropertyLimit(rs.getInt("db")));

                    planProperties.setDb(dbList);

                    return new Plan(rs.getString("username"), rs.getString("name"), finServiceId, rs.getString("Plan_ID"), AccountType.VIRTUAL_HOSTING, rs.getBoolean("active"), planProperties);
                });
    }

    public boolean importToMongo() {
        planRepository.deleteAll();
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String planId, String finServiceId) {
        planRepository.deleteAll();
        pull(planId, finServiceId);
        pushToMongo();
        return true;
    }

    public boolean processImportMessage(ImportMessage message) {
        String planId, finServiceId;

        planId = message.getParams().getImportValues().get("planId").replaceAll("^plan_", "");
        finServiceId = message.getParams().getImportValues().get("finServiceId");

        pull(planId, finServiceId);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        planRepository.save(planList);
    }
}
