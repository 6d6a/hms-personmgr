package ru.majordomo.hms.personmgr.service.importing;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.message.ImportMessage;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanPropertyDB;
import ru.majordomo.hms.personmgr.common.DBType;
import ru.majordomo.hms.personmgr.model.plan.PlanPropertyLimit;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

/**
 * PlanDBImportService
 */
@Service
public class PlanDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PlanDBImportService.class);

    private JdbcTemplate jdbcTemplate;
    private PlanRepository planRepository;
    private List<Plan> planList = new ArrayList<>();

    @Autowired
    public PlanDBImportService(JdbcTemplate jdbcTemplate, PlanRepository planRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.planRepository = planRepository;
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

            PlanPropertyDB db = new PlanPropertyDB(rs.getInt("db"), DBType.MYSQL);

            List<PlanPropertyDB> dbList = new ArrayList<>();
            dbList.add(db);

            planProperties.setDb(dbList);

            String finServiceId = ObjectId.get().toHexString();

            return new Plan(rs.getString("username"), rs.getString("name"), finServiceId, rs.getString("Plan_ID"), AccountType.VIRTUAL_HOSTING, rs.getBoolean("active"), rs.getBigDecimal("cost"),  planProperties);
        });
    }

    public void pull(String planId, String finServiceId) {
        String query = "SELECT Plan_ID, name, cost, QuotaKB, db, apache, active, username, sites, web_cpu_limit, db_cpu_limit FROM plan WHERE Plan_ID = ?";
        planList = jdbcTemplate.query(query,
                new Object[] { planId },
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

            PlanPropertyDB db = new PlanPropertyDB(rs.getInt("db"), DBType.MYSQL);

            List<PlanPropertyDB> dbList = new ArrayList<>();
            dbList.add(db);

            planProperties.setDb(dbList);

            return new Plan(rs.getString("username"), rs.getString("name"), finServiceId, rs.getString("Plan_ID"), AccountType.VIRTUAL_HOSTING, rs.getBoolean("active"), rs.getBigDecimal("cost"),  planProperties);
        });
    }

    public boolean importToMongo() {
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String planId, String finServiceId) {
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
