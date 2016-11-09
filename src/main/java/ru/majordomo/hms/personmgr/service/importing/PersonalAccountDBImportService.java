package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

/**
 * DBImportService
 */
@Service
public class PersonalAccountDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PersonalAccountDBImportService.class);

    private JdbcTemplate jdbcTemplate;
    private PersonalAccountRepository personalAccountRepository;
    private PlanRepository planRepository;
    private List<PersonalAccount> personalAccounts = new ArrayList<>();

    @Autowired
    public PersonalAccountDBImportService(JdbcTemplate jdbcTemplate, PersonalAccountRepository personalAccountRepository, PlanRepository planRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.personalAccountRepository = personalAccountRepository;
        this.planRepository = planRepository;
    }

    private void pull() {
        String query = "SELECT a.id, a.name, a.client_id, a.credit, a.plan_id, m.notify_days FROM account a LEFT JOIN Money m ON a.id = m.acc_id";
        personalAccounts = jdbcTemplate.query(query, this::rowMap);
    }

    private void pull(String accountName) {
        String query = "SELECT a.id, a.name, a.client_id, a.credit, a.plan_id, m.notify_days FROM account a LEFT JOIN Money m ON a.id = m.acc_id WHERE name = ?";
        personalAccounts = jdbcTemplate.query(query,
                new Object[] { accountName },
                this::rowMap
        );
    }

    private PersonalAccount rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.info("Found PersonalAccount " + rs.getString("name"));

        Plan plan = planRepository.findByOldId(rs.getString("plan_id"));
        String planId = plan != null ? plan.getId() : null;
        PersonalAccount personalAccount = new PersonalAccount(rs.getString("id"), rs.getString("client_id"), planId, rs.getString("name"), AccountType.VIRTUAL_HOSTING);
        personalAccount.setSetting("notify_days", rs.getString("notify_days"));
        personalAccount.setSetting("credit", rs.getString("credit").equals("y") ? "1" : "0");
        return personalAccount;
    }

    public boolean importToMongo() {
        personalAccountRepository.deleteAll();
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountName) {
        personalAccountRepository.deleteAll();
        pull(accountName);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        logger.info("pushToMongo personalAccounts");

        personalAccountRepository.save(personalAccounts);
    }
}
