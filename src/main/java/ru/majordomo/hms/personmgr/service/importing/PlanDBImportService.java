package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.AbonementType;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.DBType;
import ru.majordomo.hms.personmgr.common.FinService;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanPropertyLimit;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

import static ru.majordomo.hms.personmgr.common.StringConstants.PLAN_SERVICE_ABONEMENT_PREFIX;
import static ru.majordomo.hms.personmgr.common.StringConstants.PLAN_SERVICE_PREFIX;

/**
 * PlanDBImportService
 */
@Service
public class PlanDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PlanDBImportService.class);

    private JdbcTemplate jdbcTemplate;
    private PlanRepository planRepository;
    private AbonementRepository abonementRepository;
    private FinFeignClient finFeignClient;
    private List<Plan> planList = new ArrayList<>();

    @Autowired
    public PlanDBImportService(JdbcTemplate jdbcTemplate, PlanRepository planRepository, AbonementRepository abonementRepository, FinFeignClient finFeignClient) {
        this.jdbcTemplate = jdbcTemplate;
        this.planRepository = planRepository;
        this.abonementRepository = abonementRepository;
        this.finFeignClient = finFeignClient;
    }

    public void pull() {
        String query = "SELECT p.Plan_ID, p.name, p.cost, p.cost_disc, p.QuotaKB, p.db, p.apache, p.active, p.username, p.sites, p.web_cpu_limit, p.db_cpu_limit FROM plan p LEFT JOIN account a ON p.Plan_ID=a.plan_id WHERE a.id IS NOT NULL GROUP BY p.Plan_ID";
        planList = jdbcTemplate.query(query, this::rowMap);
    }

    public void pull(String planId) {
        String query = "SELECT p.Plan_ID, p.name, p.cost, p.cost_disc, p.QuotaKB, p.db, p.apache, p.active, p.username, p.sites, p.web_cpu_limit, p.db_cpu_limit FROM plan p LEFT JOIN account a ON p.Plan_ID=a.plan_id WHERE p.Plan_ID = ? AND a.id IS NOT NULL GROUP BY p.Plan_ID";
        planList = jdbcTemplate.query(query,
                new Object[]{planId},
                this::rowMap);
    }

    private Plan rowMap(ResultSet rs, int rowNum) throws SQLException {
        VirtualHostingPlanProperties planProperties = new VirtualHostingPlanProperties();
        if (rs.getInt("Plan_ID") == 9802
                || rs.getInt("Plan_ID") == 9804
                || rs.getInt("Plan_ID") == 9805
                || rs.getInt("Plan_ID") == 9806
                || rs.getInt("Plan_ID") == 9807) {
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

        logger.info("finService " + finService.toString());

        String finServiceId = finService.getId();

        String abonementName = rs.getString("username") + " (годовой абонемент)";

        BigDecimal abonementCost = rs.getBigDecimal("cost_disc").multiply(BigDecimal.valueOf(12L)).setScale(0, BigDecimal.ROUND_FLOOR);

        if (abonementCost.compareTo(BigDecimal.ZERO) == 0) {
            abonementCost = rs.getBigDecimal("cost").multiply(BigDecimal.valueOf(12L)).setScale(0, BigDecimal.ROUND_FLOOR);
        }

        finService = new FinService();
        finService.setPaymentType(ServicePaymentType.ONE_TIME);
        finService.setAccountType(AccountType.VIRTUAL_HOSTING);
        finService.setActive(rs.getBoolean("active"));
        finService.setCost(abonementCost);
        finService.setLimit(1);
        finService.setOldId(PLAN_SERVICE_ABONEMENT_PREFIX + rs.getString("Plan_ID"));
        finService.setName(abonementName);

        finService = finFeignClient.createService(finService);

        logger.info("AbonementFinService " + finService.toString());

        String AbonementFinServiceId = finService.getId();

        Abonement abonement = new Abonement();
        abonement.setFinServiceId(AbonementFinServiceId);
        abonement.setName(abonementName);
        abonement.setPeriod("P1Y");
        abonement.setType(AbonementType.VIRTUAL_HOSTING_PLAN);

        abonementRepository.save(abonement);

        return new Plan(rs.getString("username"),
                rs.getString("name"),
                finServiceId,
                rs.getString("Plan_ID"),
                AccountType.VIRTUAL_HOSTING,
                rs.getBoolean("active"),
                planProperties,
                Collections.singletonList(abonement.getId())
        );
    }

    public boolean importToMongo() {
        planRepository.deleteAll();
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String planId) {
        Plan plan = planRepository.findOne(planId);

        if (plan != null) {
            planRepository.delete(plan);
        }

        pull(planId);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        planRepository.save(planList);
    }
}
