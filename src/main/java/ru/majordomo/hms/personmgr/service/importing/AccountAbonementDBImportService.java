package ru.majordomo.hms.personmgr.service.importing;

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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.AbonementService;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountAbonementDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountAbonementDBImportService.class);

    private AccountAbonementRepository accountAbonementRepository;
    private PlanRepository planRepository;
    private PersonalAccountManager accountManager;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private List<AccountAbonement> accountAbonements = new ArrayList<>();
    private AbonementService abonementService;

    @Autowired
    public AccountAbonementDBImportService(
            NamedParameterJdbcTemplate jdbcTemplate,
            AccountAbonementRepository accountAbonementRepository,
            PlanRepository planRepository,
            PersonalAccountManager accountManager,
            AbonementService abonementService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountAbonementRepository = accountAbonementRepository;
        this.planRepository = planRepository;
        this.accountManager = accountManager;
        this.abonementService = abonementService;
    }

    public void pull() {
        accountAbonementRepository.deleteAll();

        String query = "SELECT a.acc_id, a.day_buy, a.date_end, aa.auto " +
                "FROM abonement a " +
                "LEFT JOIN abt_auto_buy aa USING(acc_id) " +
                "WHERE 1 ORDER BY a.acc_id ASC";

        jdbcTemplate.query(query, this::rowMap);
    }

    public void pull(String accountId) {
        accountAbonementRepository.deleteAll();

        String query = "SELECT a.acc_id, a.day_buy, a.date_end, aa.auto FROM abonement a LEFT JOIN abt_auto_buy aa USING(acc_id) WHERE a.acc_id = :acc_id ORDER BY a.acc_id ASC";
        SqlParameterSource namedParameters = new MapSqlParameterSource("acc_id", accountId);

        jdbcTemplate.query(query, namedParameters, this::rowMap);
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
                    accountAbonement.setExpired(LocalDateTime.of(rs.getDate("date_end").toLocalDate(), LocalTime.MAX));
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                accountAbonement.setAutorenew(rs.getString("auto") != null);

                accountAbonement.setAbonementId(plan.getNotInternalAbonementId());

                logger.debug("Found accountAbonement for account: " + rs.getString("acc_id") + " accountAbonement: " + accountAbonement);

                accountAbonements.add(accountAbonement);
            } else {
                logger.debug("Plan not found account: " + rs.getString("acc_id") + " planId: " + account.getPlanId());
            }
        } else {
            logger.debug("Account not found account: " + rs.getString("acc_id"));
        }

        return accountAbonement;
    }

    public boolean importToMongo() {
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountId) {
        PersonalAccount account = accountManager.findByAccountId(accountId);

        if (account != null) {
            AccountAbonement foundAccountAbonement = accountAbonementRepository.findByPersonalAccountId(account.getId());

            if (foundAccountAbonement != null) {
                accountAbonementRepository.delete(foundAccountAbonement);
            }
        }

        pull(accountId);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        try {
            accountAbonementRepository.save(accountAbonements);
        } catch (ConstraintViolationException e) {
            logger.debug(e.getMessage() + " with errors: " + StreamSupport.stream(e.getConstraintViolations().spliterator(), false).map(ConstraintViolation::getMessage).collect(Collectors.joining()));
        }
    }
}
