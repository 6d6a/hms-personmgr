package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.domain.AccountDomain;
import ru.majordomo.hms.personmgr.repository.AccountDomainRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

import static ru.majordomo.hms.personmgr.common.ImportConstants.DOMAIN_REGISTRATOR_MAP;
import static ru.majordomo.hms.personmgr.common.ImportConstants.DOMAIN_REGISTRATOR_STRING_MAP;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountDomainDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountDomainDBImportService.class);

    private AccountDomainRepository accountDomainRepository;
    private PersonalAccountRepository personalAccountRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private List<AccountDomain> accountDomains = new ArrayList<>();

    @Autowired
    public AccountDomainDBImportService(NamedParameterJdbcTemplate jdbcTemplate, AccountDomainRepository accountDomainRepository, PersonalAccountRepository personalAccountRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountDomainRepository = accountDomainRepository;
        this.personalAccountRepository = personalAccountRepository;
    }

    public void pull() {
        accountDomainRepository.deleteAll();

        String query = "SELECT domain.Domain_name, domain.acc_id, domain_auto_renew.status, domain_reg.source FROM domain LEFT JOIN domain_auto_renew ON domain.Domain_name = domain_auto_renew.domain LEFT JOIN domain_reg ON domain.Domain_name = domain_reg.domain";

        jdbcTemplate.query(query, (rs, rowNum) -> {
            AccountDomain accountDomain = new AccountDomain();

            PersonalAccount account = personalAccountRepository.findByAccountId(rs.getString("acc_id"));

            logger.info("rs.getString(\"acc_id\") " + rs.getString("acc_id") + " rs.getString(\"source\") " + rs.getString("source") + " rs.getString(\"Domain_name\") " + rs.getString("Domain_name"));

            if (account != null) {
                accountDomain.setPersonalAccountId(account.getId());

                accountDomain.setRegistrator(DOMAIN_REGISTRATOR_STRING_MAP.get(rs.getString("source")));
                accountDomain.setName(rs.getString("Domain_name"));
                accountDomain.setAutorenew(rs.getString("status") != null);

                accountDomains.add(accountDomain);
            }

            return accountDomain;
        });
    }

    public boolean importToMongo() {
        pull();
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        try {
            accountDomainRepository.save(accountDomains);
        } catch (ConstraintViolationException e) {
            logger.info(e.getMessage() + " with errors: " + StreamSupport.stream(e.getConstraintViolations().spliterator(), false).map(ConstraintViolation::getMessage).collect(Collectors.joining()));
        }
    }
}