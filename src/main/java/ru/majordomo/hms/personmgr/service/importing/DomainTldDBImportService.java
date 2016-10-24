package ru.majordomo.hms.personmgr.service.importing;

import org.apache.http.util.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.DomainCategory;
import ru.majordomo.hms.personmgr.model.AccountHistory;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;
import ru.majordomo.hms.personmgr.repository.DomainTldRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static ru.majordomo.hms.personmgr.common.ImportConstants.DOMAIN_CATEGORY_MAP;
import static ru.majordomo.hms.personmgr.common.ImportConstants.DOMAIN_REGISTRATOR_MAP;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class DomainTldDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DomainTldDBImportService.class);

    private DomainTldRepository domainTldRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private List<DomainTld> domainTlds = new ArrayList<>();

    @Autowired
    public DomainTldDBImportService(@Qualifier("billing2NamedParameterJdbcTemplate") NamedParameterJdbcTemplate billing2NamedParameterJdbcTemplate, DomainTldRepository domainTldRepository) {
        this.jdbcTemplate = billing2NamedParameterJdbcTemplate;
        this.domainTldRepository = domainTldRepository;
    }

    public void pull() {
        domainTldRepository.deleteAll();

        String query = "SELECT domain_tld, parking_registrator_id, registration_cost, renew_cost, registration_time, renew_time, renew_due_days, renew_after_days, priority, available, permanent, category FROM parking_domains_cost";

        domainTlds.addAll(jdbcTemplate.query(query, (rs, rowNum) -> {
            DomainTld domainTld = new DomainTld();

            logger.info("domain_tld " + rs.getString("domain_tld") + " parking_registrator_id " + rs.getString("parking_registrator_id"));

            domainTld.setActive(rs.getBoolean("available"));
            domainTld.setDomainCategory(DOMAIN_CATEGORY_MAP.get(rs.getString("category")));
            domainTld.setDomainRegistrator(DOMAIN_REGISTRATOR_MAP.get(rs.getInt("parking_registrator_id")));
            domainTld.setPriority(rs.getShort("priority"));
            domainTld.setRegisterYears(rs.getByte("registration_time"));
            domainTld.setRenewYears(rs.getByte("renew_time"));
            domainTld.setRenewStartDays(rs.getShort("renew_due_days"));
            domainTld.setRenewEndDays(rs.getShort("renew_after_days"));
            domainTld.setTld(rs.getString("domain_tld"));
            domainTld.setVariablePrice(!rs.getBoolean("permanent"));

            if (domainTld.getDomainCategory() == null) {
                domainTld.setDomainCategory(DomainCategory.NONE);
            }

            return domainTld;
        }));
    }

    public boolean importToMongo() {
        pull();
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        try {
            domainTldRepository.save(domainTlds);
        } catch (ConstraintViolationException e) {
            logger.info(e.getMessage() + " with errors: " + StreamSupport.stream(e.getConstraintViolations().spliterator(), false).map(ConstraintViolation::getMessage).collect(Collectors.joining()));
        }
    }
}
