package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


import ru.majordomo.hms.personmgr.common.ImportConstants;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

import static ru.majordomo.hms.personmgr.common.StringConstants.FREE_SERVICE_POSTFIX;
import static ru.majordomo.hms.personmgr.common.StringConstants.PLAN_SERVICE_PREFIX;
import static ru.majordomo.hms.personmgr.common.StringConstants.SERVICE_PREFIX;


/**
 * DBImportService
 */
@Service
public class PersonalAccountServicesDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PersonalAccountServicesDBImportService.class);

    private NamedParameterJdbcTemplate jdbcTemplate;
    private PersonalAccountRepository personalAccountRepository;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountServiceRepository accountServiceRepository;
    private List<PersonalAccount> paymentAccounts = new ArrayList<>();

    @Autowired
    public PersonalAccountServicesDBImportService(NamedParameterJdbcTemplate jdbcTemplate, PersonalAccountRepository personalAccountRepository, PaymentServiceRepository paymentServiceRepository, AccountServiceRepository accountServiceRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.personalAccountRepository = personalAccountRepository;
        this.paymentServiceRepository = paymentServiceRepository;
        this.accountServiceRepository = accountServiceRepository;
    }

    private void pull() {
        String query = "SELECT id, name, plan_id FROM account";
        paymentAccounts = jdbcTemplate.query(query, (rs, rowNum) -> {
            PersonalAccount account = personalAccountRepository.findByName(rs.getString("name"));
            List<AccountService> accountServices = new ArrayList<>();

            PaymentService service = paymentServiceRepository.findByOldId(PLAN_SERVICE_PREFIX + rs.getString("plan_id"));

            if (service != null) {
                AccountService accountService = new AccountService(service);
                accountService.setPersonalAccountId(account.getId());

                accountServices.add(accountService);
            }

            String queryExtend = "SELECT acc_id, Domain_name, usluga, cost, value, promo FROM extend WHERE acc_id = :acc_id AND usluga NOT IN (:usluga_ids)";

            SqlParameterSource namedParameters = new MapSqlParameterSource("acc_id", rs.getString("id")).addValue("usluga_ids", ImportConstants.notNeededServiceIds);

            accountServices.addAll(jdbcTemplate.query(queryExtend,
                    namedParameters,
                    (rsE, rowNumE) -> {
                        PaymentService serviceE = paymentServiceRepository.findByOldId(PLAN_SERVICE_PREFIX + rsE.getString("usluga") + (rsE.getBigDecimal("cost").compareTo(BigDecimal.ZERO) == 0 ? FREE_SERVICE_POSTFIX: ""));

                        AccountService accountServiceE = new AccountService(serviceE);

                        accountServiceE.setPersonalAccountId(account.getId());
                        return accountServiceE;
                    }
            ));

            account.setServices(accountServices);

            accountServiceRepository.save(accountServices);

            return account;
        });
    }

    private void pull(String accountName) {
        String query = "SELECT id, name, plan_id FROM account WHERE name = :accountName";
        SqlParameterSource namedParameters = new MapSqlParameterSource("accountName", accountName);

        paymentAccounts = jdbcTemplate.query(query,
                namedParameters,
                (rs, rowNum) -> {
                    List<AccountService> accountServices = new ArrayList<>();

                    PersonalAccount account = personalAccountRepository.findByName(accountName);
                    PaymentService service = paymentServiceRepository.findByOldId(PLAN_SERVICE_PREFIX
                            + rs.getString("plan_id"));

                    if (service != null) {
                        AccountService accountService = new AccountService(service);
                        accountService.setPersonalAccountId(account.getId());

                        accountServices.add(accountService);
                    }

                    String queryExtend = "SELECT acc_id, Domain_name, usluga, cost, value, promo FROM extend WHERE acc_id = :acc_id AND usluga NOT IN (:usluga_ids)";
                    SqlParameterSource namedParametersE = new MapSqlParameterSource("acc_id", accountName.replaceAll("ac_", "")).addValue("usluga_ids", ImportConstants.notNeededServiceIds);

                    accountServices.addAll(jdbcTemplate.query(queryExtend,
                            namedParametersE,
                            (rsE, rowNumE) -> {
                                PaymentService serviceE = paymentServiceRepository.findByOldId(SERVICE_PREFIX + rsE.getString("usluga") + (rsE.getBigDecimal("cost").compareTo(BigDecimal.ZERO) == 0 ? FREE_SERVICE_POSTFIX: ""));

                                AccountService accountServiceE = new AccountService(serviceE);

                                accountServiceE.setPersonalAccountId(account.getId());
                                return accountServiceE;
                            }
                    ));

                    account.setServices(accountServices);

                    accountServiceRepository.save(accountServices);

                    return account;
                }
        );
    }

    public boolean importToMongo() {
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountName) {
        pull(accountName);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        personalAccountRepository.save(paymentAccounts);
    }
}
