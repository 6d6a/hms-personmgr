package ru.majordomo.hms.personmgr.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.event.accountService.AccountServiceCreateEvent;
import ru.majordomo.hms.personmgr.event.accountService.AccountServiceImportEvent;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;

import static java.lang.Math.floor;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_CAPACITY;
import static ru.majordomo.hms.personmgr.common.Constants.ADDITIONAL_QUOTA_100_ID;
import static ru.majordomo.hms.personmgr.common.Constants.FREE_SERVICE_POSTFIX;
import static ru.majordomo.hms.personmgr.common.Constants.PLAN_SERVICE_PREFIX;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_PREFIX;
import static ru.majordomo.hms.personmgr.common.Constants.SMS_NOTIFICATIONS_10_RUB_ID;
import static ru.majordomo.hms.personmgr.common.Constants.SMS_NOTIFICATIONS_29_RUB_SERVICE_ID;
import static ru.majordomo.hms.personmgr.common.Constants.SMS_NOTIFICATIONS_FREE_SERVICE_ID;

/**
 * DBImportService
 */
@Service
public class AccountServicesDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountServicesDBImportService.class);

    private NamedParameterJdbcTemplate jdbcTemplate;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountServiceRepository accountServiceRepository;
    private final ApplicationEventPublisher publisher;

    private List<PaymentService> paymentServices = new ArrayList<>();
    private Map<String, PaymentService> oldServiceIdToService = new HashMap<>();

    @Autowired
    public AccountServicesDBImportService(
            NamedParameterJdbcTemplate jdbcTemplate,
            PaymentServiceRepository paymentServiceRepository,
            AccountServiceRepository accountServiceRepository,
            ApplicationEventPublisher publisher
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountServiceRepository = accountServiceRepository;
        this.publisher = publisher;

        loadPaymentServices();
    }

    private void pull() {
        String query = "SELECT id, name, plan_id FROM account ORDER BY id ASC";

        jdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new AccountServiceImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        logger.debug("[start] Searching for AccountService for acc " + accountId);

        String query = "SELECT id, name, plan_id FROM account WHERE id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        jdbcTemplate.query(query,
                namedParameters1,
                this::rowMap
        );

        logger.debug("[finish] Searching for AccountService for acc " + accountId);
    }

    private PersonalAccount rowMap(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found PersonalAccount " + rs.getString("name"));

        String oldId = PLAN_SERVICE_PREFIX + rs.getString("plan_id");
        PaymentService service = oldServiceIdToService.get(oldId);

        if (service != null) {
            AccountService accountService = new AccountService(service);
            accountService.setPersonalAccountId(rs.getString("id"));

            publisher.publishEvent(new AccountServiceCreateEvent(accountService));

            logger.debug("Added Plan service " + service.getName() + " for PersonalAccount " + rs.getString("name"));
        } else {
            logger.error("Plan PaymentService not found");
        }

        String queryExtend = "SELECT acc_id, Domain_name, usluga, cost, value, promo FROM extend WHERE acc_id = :acc_id AND usluga NOT IN (:usluga_ids)";
        SqlParameterSource namedParameters = new MapSqlParameterSource("acc_id", rs.getString("id"))
                .addValue("usluga_ids", Constants.NOT_NEEDED_ACCOUNT_SERVICE_IDS);

        jdbcTemplate.query(queryExtend,
                namedParameters,
                (rsE, rowNumE) -> {
                    AccountService accountServiceE;

                    String serviceOldId = SERVICE_PREFIX + rsE.getString("usluga")
                            + (rsE.getBigDecimal("cost").compareTo(BigDecimal.ZERO) == 0 ? FREE_SERVICE_POSTFIX : "");

                    if (rsE.getString("usluga").equals("" + SMS_NOTIFICATIONS_10_RUB_ID)
                            && rsE.getBigDecimal("cost").compareTo(BigDecimal.valueOf(10L)) != 0) {
                        if (rsE.getBigDecimal("cost").compareTo(BigDecimal.valueOf(29L)) == 0) {
                            serviceOldId = SMS_NOTIFICATIONS_29_RUB_SERVICE_ID;
                        } else {
                            serviceOldId = SMS_NOTIFICATIONS_FREE_SERVICE_ID;
                        }
                    }

                    logger.debug("Trying to find PaymentService for " + serviceOldId);

                    PaymentService serviceE = oldServiceIdToService.get(serviceOldId);

                    if (serviceE == null) {
                        logger.error("PaymentService not found for account: " +
                                rsE.getString("acc_id") + " service: " +
                                serviceOldId);
                        return null;
                    }
                    accountServiceE = new AccountService(serviceE);
                    accountServiceE.setPersonalAccountId(rsE.getString("acc_id"));
                    accountServiceE.setComment(rsE.getString("value"));

                    if (rsE.getInt("usluga") == ADDITIONAL_QUOTA_100_ID) {
                        int quantity = 1 + (int) floor(rsE.getLong("value") / ADDITIONAL_QUOTA_100_CAPACITY);
                        accountServiceE.setQuantity(quantity);
                    }

                    publisher.publishEvent(new AccountServiceCreateEvent(accountServiceE));

                    logger.debug("Added accountService for service " + serviceE.getName() + " for PersonalAccount " + rsE.getString("acc_id"));

                    return accountServiceE;
                }
        );

        return null;
    }

    public void clean() {
        accountServiceRepository.deleteAll();
    }

    public void clean(String accountId) {
        List<AccountService> accountServices = accountServiceRepository.findByPersonalAccountId(accountId);
        if (accountServices != null && !accountServices.isEmpty()) {
            accountServiceRepository.delete(accountServices);
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

    private void loadPaymentServices() {
        if (paymentServices.isEmpty()) {
            paymentServices = paymentServiceRepository.findAllPaymentServices();

            oldServiceIdToService = paymentServices
                    .stream()
                    .collect(Collectors.toMap(PaymentService::getOldId, paymentService -> paymentService))
            ;
        }
    }
}
