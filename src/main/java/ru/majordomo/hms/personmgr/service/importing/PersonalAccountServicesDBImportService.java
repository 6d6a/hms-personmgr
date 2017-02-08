package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

import static java.lang.Math.floor;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_CAPACITY;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_ID;
import static ru.majordomo.hms.personmgr.common.Constants.FREE_SERVICE_POSTFIX;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_SERVICE_PREFIX;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_PREFIX;


/**
 * DBImportService
 */
@Service
public class PersonalAccountServicesDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PersonalAccountServicesDBImportService.class);

    private NamedParameterJdbcTemplate jdbcTemplate;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountServiceRepository accountServiceRepository;

    @Autowired
    public PersonalAccountServicesDBImportService(
            NamedParameterJdbcTemplate jdbcTemplate,
            PaymentServiceRepository paymentServiceRepository,
            AccountServiceRepository accountServiceRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountServiceRepository = accountServiceRepository;
    }

    private void pull() {
        String query = "SELECT id, name, plan_id FROM account ORDER BY id ASC";
        jdbcTemplate.query(query, this::rowMap);
    }

    private void pull(String accountId) {
        String query = "SELECT id, name, plan_id FROM account WHERE id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        jdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );
    }

    private PersonalAccount rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found PersonalAccount " + rs.getString("name"));

        List<AccountService> accountServices = new ArrayList<>();

        PaymentService service = paymentServiceRepository.findByOldId(PLAN_SERVICE_PREFIX
                + rs.getString("plan_id"));

        if (service != null) {
            AccountService accountService = new AccountService(service);
            accountService.setPersonalAccountId(rs.getString("id"));

            accountServices.add(accountService);

            logger.debug("Added Plan service " + service.getId() + " for PersonalAccount " + rs.getString("name"));
        }

        String queryExtend = "SELECT acc_id, Domain_name, usluga, cost, value, promo FROM extend WHERE acc_id = :acc_id AND usluga NOT IN (:usluga_ids)";
        SqlParameterSource namedParameters = new MapSqlParameterSource("acc_id", rs.getString("id"))
                .addValue("usluga_ids", Constants.NOT_NEEDED_ACCOUNT_SERVICE_IDS);

        jdbcTemplate.query(queryExtend,
                namedParameters,
                (rsE, rowNumE) -> {
                    AccountService accountServiceE;
                    PaymentService serviceE = paymentServiceRepository.findByOldId(SERVICE_PREFIX + rsE.getString("usluga")
                            + (rsE.getBigDecimal("cost").compareTo(BigDecimal.ZERO) == 0 ? FREE_SERVICE_POSTFIX: ""));

                    accountServiceE = new AccountService(serviceE);
                    accountServiceE.setPersonalAccountId(rsE.getString("acc_id"));

                    if (rsE.getInt("usluga") == ADDITIONAL_QUOTA_100_ID) {
                        int quantity = 1 + (int) floor(rsE.getLong("value") / ADDITIONAL_QUOTA_100_CAPACITY);
                        accountServiceE.setQuantity(quantity);
                    }

                    accountServices.add(accountServiceE);

                    logger.debug("Added accountService for service " + serviceE.getId() + " for PersonalAccount " + rsE.getString("acc_id"));

                    return accountServiceE;
                }
        );

        try {
            accountServiceRepository.save(accountServices);
        } catch (ConstraintViolationException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean importToMongo() {
        accountServiceRepository.deleteAll();
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<AccountService> services = accountServiceRepository.findByPersonalAccountId(accountId);

        if (services != null && !services.isEmpty()) {
            accountServiceRepository.delete(services);
        }

        pull(accountId);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
//        personalAccountRepository.save(paymentAccounts);
    }
}
