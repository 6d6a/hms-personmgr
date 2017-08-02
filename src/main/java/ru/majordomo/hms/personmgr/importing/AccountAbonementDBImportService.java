package ru.majordomo.hms.personmgr.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.manager.AccountAbonementManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountAbonementDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountAbonementDBImportService.class);

    private AccountAbonementManager accountAbonementManager;
    private PlanRepository planRepository;
    private PersonalAccountManager accountManager;
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public AccountAbonementDBImportService(
            NamedParameterJdbcTemplate jdbcTemplate,
            AccountAbonementManager accountAbonementManager,
            PlanRepository planRepository,
            PersonalAccountManager accountManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountAbonementManager = accountAbonementManager;
        this.planRepository = planRepository;
        this.accountManager = accountManager;
    }

    public void pull() {
        String query = "SELECT a.acc_id, a.day_buy, a.date_end, aa.auto " +
                "FROM abonement a " +
                "LEFT JOIN abt_auto_buy aa USING(acc_id) " +
                "WHERE 1 ORDER BY a.acc_id ASC";

        jdbcTemplate.query(query, this::rowMap);
    }

    public void pull(String accountId) {
        logger.debug("[start] Searching for AccountAbonement for account: " + accountId);

        String query = "SELECT a.acc_id, a.day_buy, a.date_end, aa.auto FROM abonement a LEFT JOIN abt_auto_buy aa USING(acc_id) WHERE a.acc_id = :acc_id ORDER BY a.acc_id ASC";
        SqlParameterSource namedParameters = new MapSqlParameterSource("acc_id", accountId);

        jdbcTemplate.query(query, namedParameters, this::rowMap);

        logger.debug("[finish] Searching for AccountAbonement for account: " + accountId);
    }

    private AccountAbonement rowMap(ResultSet rs, int rowNum) throws SQLException {
        AccountAbonement accountAbonement = new AccountAbonement();

        PersonalAccount account = accountManager.findByAccountId(rs.getString("acc_id"));

        if (account != null) {
            logger.debug("Found account: " + rs.getString("acc_id"));

            Plan plan = planRepository.findOne(account.getPlanId());

            if (plan != null) {
                accountAbonement.setPersonalAccountId(account.getId());

                try {
                    accountAbonement.setCreated(LocalDateTime.of(rs.getDate("day_buy").toLocalDate(), LocalTime.MAX));
                } catch (SQLException e) {
                    e.printStackTrace();
                    logger.error("Exception in accountAbonement.setCreated(LocalDateTime.of" +
                            "(rs.getDate(\"day_buy\").toLocalDate(), LocalTime.MAX)); " + e.getMessage());
                }

                try {
                    accountAbonement.setExpired(LocalDateTime.of(rs.getDate("date_end").toLocalDate(), LocalTime.MAX));
                } catch (SQLException e) {
                    e.printStackTrace();
                    logger.error("Exception in accountAbonement.setExpired(LocalDateTime.of" +
                            "(rs.getDate(\"date_end\").toLocalDate(), LocalTime.MAX)); " + e.getMessage());
                }

                accountAbonement.setAutorenew(rs.getString("auto") != null);

                accountAbonement.setAbonementId(plan.getNotInternalAbonementId());

                logger.debug("Found accountAbonement for account: " + rs.getString("acc_id") + " accountAbonement: " + accountAbonement);

                try {
                    accountAbonementManager.save(accountAbonement);
                } catch (ConstraintViolationException e) {
                    logger.debug(e.getMessage() + " with errors: " +
                            e.getConstraintViolations()
                                    .stream()
                                    .map(ConstraintViolation::getMessage)
                                    .collect(Collectors.joining())
                    );
                }
            } else {
                logger.debug("Plan not found account: " + rs.getString("acc_id") + " planId: " + account.getPlanId());
            }
        } else {
            logger.debug("Account not found account: " + rs.getString("acc_id"));
        }

        return accountAbonement;
    }

    public void clean() {
        accountAbonementManager.deleteAll();
    }

    public void clean(String accountId) {
        accountAbonementManager.deleteByPersonalAccountId(accountId);
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
