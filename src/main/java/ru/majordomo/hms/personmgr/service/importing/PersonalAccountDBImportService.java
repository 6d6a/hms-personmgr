package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_SETTING_ADD_QUOTA_IF_OVERQUOTED;
import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_SETTING_AUTO_BILL_SENDING;
import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_SETTING_CREDIT;
import static ru.majordomo.hms.personmgr.common.Constants.ACCOUNT_SETTING_NOTIFY_DAYS;

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
        String query = "SELECT a.id, a.name, a.client_id, a.credit, a.plan_id, m.notify_days, a.status, c.client_auto_bill, a.overquoted, a.overquot_addcost FROM account a LEFT JOIN Money m ON a.id = m.acc_id LEFT JOIN client c ON a.client_id = c.Client_ID ORDER BY a.id ASC";
        jdbcTemplate.query(query, this::rowMap);
    }

    private void pull(String accountId) {
        String query = "SELECT a.id, a.name, a.client_id, a.credit, a.plan_id, m.notify_days, a.status, c.client_auto_bill, a.overquoted, a.overquot_addcost FROM account a LEFT JOIN Money m ON a.id = m.acc_id LEFT JOIN client c ON a.client_id = c.Client_ID WHERE id = ?";
        jdbcTemplate.query(query,
                new Object[] { accountId },
                this::rowMap
        );
    }

    private PersonalAccount rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found PersonalAccount " + rs.getString("name"));

        PersonalAccount personalAccount = new PersonalAccount();

        Plan plan = planRepository.findByOldId(rs.getString("plan_id"));

        if (plan != null) {
            //Создаем PersonalAccount
            personalAccount = new PersonalAccount();
            personalAccount.setId(rs.getString("id"));
            personalAccount.setAccountType(AccountType.VIRTUAL_HOSTING);
            personalAccount.setPlanId(plan.getId());
            personalAccount.setAccountId(rs.getString("id"));
            personalAccount.setClientId(rs.getString("client_id"));
            personalAccount.setName(rs.getString("name"));
            personalAccount.setActive(rs.getBoolean("status"));
            personalAccount.setCreated(LocalDateTime.now());

            personalAccount.setSetting(ACCOUNT_SETTING_NOTIFY_DAYS, rs.getString("notify_days"));
            personalAccount.setSetting(ACCOUNT_SETTING_CREDIT, rs.getString("credit").equals("y") ? "1" : "0");
            personalAccount.setSetting(ACCOUNT_SETTING_AUTO_BILL_SENDING, rs.getString("client_auto_bill"));
            personalAccount.setOverquoted(rs.getString("overquoted").equals("1"));
            personalAccount.setAddQuotaIfOverquoted(rs.getString("overquot_addcost").equals("1"));

            personalAccounts.add(personalAccount);
        } else {
            logger.debug("Plan not found account: " + rs.getString("acc_id") + " planId: " + rs.getString("plan_id"));
        }

        return personalAccount;
    }

    public boolean importToMongo() {
        personalAccountRepository.deleteAll();
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountId) {
        PersonalAccount account = null;
        try {
            account = personalAccountRepository.findByAccountId(accountId);

            if (account != null) {
                personalAccountRepository.delete(account);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        pull(accountId);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        logger.debug("pushToMongo personalAccounts");

        personalAccountRepository.save(personalAccounts);
    }
}
