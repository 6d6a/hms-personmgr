package ru.majordomo.hms.personmgr.importing;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

import static ru.majordomo.hms.personmgr.common.PhoneNumberManager.formatPhone;
import static ru.majordomo.hms.personmgr.common.PhoneNumberManager.phoneValid;

@Service
public class PersonalAccountDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PersonalAccountDBImportService.class);

    private NamedParameterJdbcTemplate jdbcTemplate;
    private PersonalAccountManager accountManager;
    private PlanRepository planRepository;

    @Autowired
    public PersonalAccountDBImportService(
            @Qualifier("namedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            PersonalAccountManager accountManager,
            PlanRepository planRepository
    ) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.accountManager = accountManager;
        this.planRepository = planRepository;
    }

    private void pull() {
        String query = "SELECT a.id, a.name, a.client_id, a.credit, a.plan_id, m.notify_days, " +
                "a.status, c.client_auto_bill, a.overquoted, a.overquot_addcost, e.value as sms_phone " +
                "FROM account a " +
                "LEFT JOIN Money m ON a.id = m.acc_id " +
                "LEFT JOIN client c ON a.client_id = c.Client_ID " +
                "LEFT JOIN extend e ON (a.id = e.acc_id AND e.usluga = 18) " +
                "WHERE a.client_id != 0 " +
                "ORDER BY a.id ASC";
        jdbcTemplate.query(query, this::rowMap);
    }

    private void pull(String accountId) {
        logger.debug("[start] Searching for PersonalAccount for acc " + accountId);

        String query = "SELECT a.id, a.name, a.client_id, a.credit, a.plan_id, m.notify_days, " +
                "a.status, c.client_auto_bill, a.overquoted, a.overquot_addcost, e.value as sms_phone " +
                "FROM account a " +
                "LEFT JOIN Money m ON a.id = m.acc_id " +
                "LEFT JOIN client c ON a.client_id = c.Client_ID " +
                "LEFT JOIN extend e ON (a.id = e.acc_id AND e.usluga = 18) " +
                "WHERE a.client_id != 0 AND a.id = :accountId";

        SqlParameterSource namedParametersE = new MapSqlParameterSource("accountId", accountId);

        jdbcTemplate.query(query, namedParametersE, this::rowMap);

        logger.debug("[finish] Searching for PersonalAccount for acc " + accountId);
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
            personalAccount.setName(
                    rs.getString("name").startsWith("ac_") ?
                            rs.getString("name").toUpperCase() :
                            rs.getString("name")
            );
            personalAccount.setActive(rs.getBoolean("status"));
            personalAccount.setCreated(LocalDateTime.now());

            personalAccount.setNotifyDays(rs.getInt("notify_days"));
            personalAccount.setCredit(rs.getString("credit").equals("y"));
            personalAccount.setAutoBillSending(rs.getString("client_auto_bill").equals("1"));
            personalAccount.setOverquoted(rs.getString("overquoted").equals("1"));
            personalAccount.setAddQuotaIfOverquoted(rs.getString("overquot_addcost").equals("1"));
            personalAccount.setAccountNew(false);
            personalAccount.setCreditPeriod("P14D");

            String smsPhone = rs.getString("sms_phone");

            if (smsPhone != null && !smsPhone.equals("")) {
                if (phoneValid(smsPhone)) {

                    String formattedPhone = formatPhone(smsPhone);
                    if (!formattedPhone.equals("")) {
                        personalAccount.setSmsPhoneNumber(formattedPhone);
                    }
                } else {
                    logger.error("smsPhone not valid: " + smsPhone);
                }
            }

            accountManager.save(personalAccount);
        } else {
            logger.debug("Plan not found account: " + rs.getString("acc_id") + " planId: " + rs.getString("plan_id"));
        }

        return personalAccount;
    }

    public void clean() {
        accountManager.deleteAll();
    }

    public void clean(String accountId) {
        PersonalAccount account;
        try {
            account = accountManager.findOne(accountId);
        } catch (ResourceNotFoundException e) {
            return;
        }

        if (account != null) {
            accountManager.delete(account);
        }
    }

    public boolean importToMongo() {
        clean();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        clean(accountId);
        pull(accountId);
        return true;
    }
}
