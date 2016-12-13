package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.AbonementType;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.DBType;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanPropertyLimit;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

import static ru.majordomo.hms.personmgr.common.Constants.PLAN_BUSINESS_ID;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_BUSINESS_PLUS_ID;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_PARKING_DOMAINS_ID;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_PARKING_ID;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_PARKING_PLUS_ID;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_PROPERTY_LIMIT_UNLIMITED;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_SERVICE_ABONEMENT_PREFIX;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_SERVICE_PREFIX;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_START_ID;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_UNLIMITED_ID;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_UNLIMITED_PLUS_ID;

/**
 * PlanDBImportService
 */
@Service
public class PlanDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PlanDBImportService.class);

    private JdbcTemplate jdbcTemplate;
    private PlanRepository planRepository;
    private AbonementRepository abonementRepository;
    private PaymentServiceRepository paymentServiceRepository;
    private List<Plan> plans = new ArrayList<>();
    private List<Abonement> abonements = new ArrayList<>();

    @Autowired
    public PlanDBImportService(
            JdbcTemplate jdbcTemplate,
            PlanRepository planRepository,
            AbonementRepository abonementRepository,
            PaymentServiceRepository paymentServiceRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.planRepository = planRepository;
        this.abonementRepository = abonementRepository;
        this.paymentServiceRepository = paymentServiceRepository;
    }

    public void pull() {
        String query = "SELECT p.Plan_ID, p.name, p.cost, p.cost_disc, p.QuotaKB, p.db, p.apache, " +
                "p.active, p.username, p.sites, p.web_cpu_limit, p.db_cpu_limit FROM plan p " +
                "LEFT JOIN account a ON p.Plan_ID=a.plan_id WHERE a.id IS NOT NULL GROUP BY p.Plan_ID";
        plans = jdbcTemplate.query(query, this::rowMap);
    }

    public void pull(String planId) {
        String query = "SELECT p.Plan_ID, p.name, p.cost, p.cost_disc, p.QuotaKB, p.db, p.apache, " +
                "p.active, p.username, p.sites, p.web_cpu_limit, p.db_cpu_limit FROM plan p " +
                "LEFT JOIN account a ON p.Plan_ID=a.plan_id WHERE p.Plan_ID = ? AND a.id IS NOT NULL GROUP BY p.Plan_ID";
        plans = jdbcTemplate.query(query,
                new Object[]{planId},
                this::rowMap);
    }

    private Plan rowMap(ResultSet rs, int rowNum) throws SQLException {
        VirtualHostingPlanProperties planProperties = new VirtualHostingPlanProperties();
        if (rs.getInt("Plan_ID") == PLAN_UNLIMITED_ID
                || rs.getInt("Plan_ID") == PLAN_START_ID
                || rs.getInt("Plan_ID") == PLAN_UNLIMITED_PLUS_ID
                || rs.getInt("Plan_ID") == PLAN_BUSINESS_ID
                || rs.getInt("Plan_ID") == PLAN_BUSINESS_PLUS_ID) {
            planProperties.setFtpLimit(new PlanPropertyLimit(5, PLAN_PROPERTY_LIMIT_UNLIMITED));
        } else {
            planProperties.setFtpLimit(new PlanPropertyLimit(PLAN_PROPERTY_LIMIT_UNLIMITED));
        }

        planProperties.setWebCpuLimit(new PlanPropertyLimit(rs.getInt("web_cpu_limit")));
        planProperties.setDbCpuLimit(new PlanPropertyLimit(rs.getInt("db_cpu_limit")));
        planProperties.setQuotaKBLimit(new PlanPropertyLimit(rs.getInt("QuotaKB")));
        planProperties.setSitesLimit(new PlanPropertyLimit(
                rs.getInt("sites") == 1000 ?
                        PLAN_PROPERTY_LIMIT_UNLIMITED :
                        rs.getInt("sites"))
        );
        planProperties.setSshLimit(new PlanPropertyLimit(PLAN_PROPERTY_LIMIT_UNLIMITED));

        //TODO serviceTemplateIds rs.getBoolean("apache") включен ли php (Для Старт только perl)

        if (rs.getInt("Plan_ID") == PLAN_BUSINESS_ID
                || rs.getInt("Plan_ID") == PLAN_BUSINESS_PLUS_ID) {
            planProperties.setBusinessServices(true);
        }

        Map<DBType, PlanPropertyLimit> dbList = new HashMap<>();
        dbList.put(DBType.MYSQL, new PlanPropertyLimit(
                rs.getInt("db") == 1000 ?
                        PLAN_PROPERTY_LIMIT_UNLIMITED :
                        rs.getInt("db"))
        );

        planProperties.setDb(dbList);

        PaymentService paymentService = new PaymentService();
        paymentService.setPaymentType(ServicePaymentType.MONTH);
        paymentService.setAccountType(AccountType.VIRTUAL_HOSTING);
        paymentService.setActive(rs.getBoolean("active"));
        paymentService.setCost(rs.getBigDecimal("cost"));
        paymentService.setLimit(1);
        paymentService.setOldId(PLAN_SERVICE_PREFIX + rs.getString("Plan_ID"));
        paymentService.setName(rs.getString("username"));

        paymentServiceRepository.save(paymentService);

        logger.info("paymentService " + paymentService.toString());

        String finServiceId = paymentService.getId();

        String abonementName = rs.getString("username") + " (годовой абонемент)";

        BigDecimal abonementCost = rs.getBigDecimal("cost_disc").multiply(BigDecimal.valueOf(12L)).setScale(0, BigDecimal.ROUND_FLOOR);

        if (abonementCost.compareTo(BigDecimal.ZERO) == 0) {
            abonementCost = rs.getBigDecimal("cost").multiply(BigDecimal.valueOf(12L)).setScale(0, BigDecimal.ROUND_FLOOR);
        }

        paymentService = new PaymentService();
        paymentService.setPaymentType(ServicePaymentType.ONE_TIME);
        paymentService.setAccountType(AccountType.VIRTUAL_HOSTING);
        paymentService.setActive(rs.getBoolean("active"));
        paymentService.setCost(abonementCost);
        paymentService.setLimit(1);
        paymentService.setOldId(PLAN_SERVICE_ABONEMENT_PREFIX + rs.getString("Plan_ID"));
        paymentService.setName(abonementName);

        paymentServiceRepository.save(paymentService);

        logger.info("AbonementFinService " + paymentService.toString());

        String AbonementFinServiceId = paymentService.getId();

        Abonement abonement = new Abonement();
        abonement.setServiceId(AbonementFinServiceId);
        abonement.setName(abonementName);
        abonement.setPeriod("P1Y");
        abonement.setType(AbonementType.VIRTUAL_HOSTING_PLAN);

        abonementRepository.save(abonement);

        boolean abonementOnly = false;

        if (rs.getInt("Plan_ID") == PLAN_PARKING_DOMAINS_ID ||
                rs.getInt("Plan_ID") == PLAN_PARKING_ID ||
                rs.getInt("Plan_ID") == PLAN_PARKING_PLUS_ID) {
            abonementOnly = true;
        }

        Plan plan = new Plan();
        plan.setName(rs.getString("username"));
        plan.setInternalName(rs.getString("name"));
        plan.setServiceId(finServiceId);
        plan.setOldId(rs.getString("Plan_ID"));
        plan.setAccountType(AccountType.VIRTUAL_HOSTING);
        plan.setActive(rs.getBoolean("active"));
        plan.setPlanProperties(planProperties);
        plan.setAbonementIds(Collections.singletonList(abonement.getId()));
        plan.setAbonementOnly(abonementOnly);

        return plan;
    }

    public boolean importToMongo() {
        planRepository.deleteAll();
        abonementRepository.deleteAll();
        try (Stream<PaymentService> paymentServiceStream = paymentServiceRepository.findByOldIdRegex(PLAN_SERVICE_PREFIX + ".*")) {
            paymentServiceStream.forEach(
                    paymentServiceRepository::delete
            );
        }
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String planId) {
        Plan plan = planRepository.findOne(planId);

        if (plan != null) {
            for (Abonement abonement : plan.getAbonements()) {
                abonementRepository.delete(abonement);
            }
            planRepository.delete(plan);
        }

        try (Stream<PaymentService> paymentServiceStream = paymentServiceRepository.findByOldIdRegex(planId)) {
            paymentServiceStream.forEach(
                    paymentServiceRepository::delete
            );
        }

        pull(planId);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        planRepository.save(plans);
    }
}
