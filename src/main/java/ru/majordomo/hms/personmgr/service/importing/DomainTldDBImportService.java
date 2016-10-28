package ru.majordomo.hms.personmgr.service.importing;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.DomainCategory;
import ru.majordomo.hms.personmgr.common.FinService;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.repository.DomainTldRepository;
import ru.majordomo.hms.personmgr.service.FinFeignClient;

import static ru.majordomo.hms.personmgr.common.ImportConstants.DOMAIN_CATEGORY_MAP;
import static ru.majordomo.hms.personmgr.common.ImportConstants.DOMAIN_REGISTRATOR_MAP;
import static ru.majordomo.hms.personmgr.common.ImportConstants.DOMAIN_REGISTRATOR_NAME_MAP;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class DomainTldDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DomainTldDBImportService.class);

    private DomainTldRepository domainTldRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private FinFeignClient finFeignClient;
    private List<DomainTld> domainTlds = new ArrayList<>();

    @Autowired
    public DomainTldDBImportService(@Qualifier("billing2NamedParameterJdbcTemplate") NamedParameterJdbcTemplate billing2NamedParameterJdbcTemplate, DomainTldRepository domainTldRepository, FinFeignClient finFeignClient) {
        this.jdbcTemplate = billing2NamedParameterJdbcTemplate;
        this.domainTldRepository = domainTldRepository;
        this.finFeignClient = finFeignClient;
    }

    public void pull() {
        domainTldRepository.deleteAll();

        String query = "SELECT domain_tld, parking_registrator_id, registration_cost, renew_cost, registration_time, renew_time, renew_due_days, renew_after_days, priority, available, permanent, category FROM parking_domains_cost";

        domainTlds.addAll(jdbcTemplate.query(query, (rs, rowNum) -> {
            DomainTld domainTld = new DomainTld();

            logger.info("domain_tld " + rs.getString("domain_tld") + " parking_registrator_id " + rs.getString("parking_registrator_id"));

            domainTld.setActive(rs.getBoolean("available"));
            domainTld.setDomainCategory(DOMAIN_CATEGORY_MAP.get(rs.getString("category")));
            domainTld.setRegistrator(DOMAIN_REGISTRATOR_MAP.get(rs.getInt("parking_registrator_id")));
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

            FinService finService = new FinService();
            finService.setPaymentType(ServicePaymentType.ONE_TIME);
            finService.setAccountType(AccountType.VIRTUAL_HOSTING);
            finService.setActive(domainTld.isActive());
            finService.setCost(rs.getBigDecimal("registration_cost"));
            finService.setLimit(-1);
            finService.setOldId(ObjectId.get().toHexString());
            finService.setName("Регистрация домена в зоне " + domainTld.getTld() + " (" +  DOMAIN_REGISTRATOR_NAME_MAP.get(rs.getInt("parking_registrator_id")) + ")");

            finService = finFeignClient.create(finService);
            logger.info(finService.toString());

            domainTld.setRegistrationServiceId(finService.getId());

            finService = new FinService();
            finService.setPaymentType(ServicePaymentType.ONE_TIME);
            finService.setAccountType(AccountType.VIRTUAL_HOSTING);
            finService.setActive(domainTld.isActive());
            finService.setCost(rs.getBigDecimal("registration_cost"));
            finService.setLimit(-1);
            finService.setOldId(ObjectId.get().toHexString());
            finService.setName("Продление домена в зоне " + domainTld.getTld() + " (" +  DOMAIN_REGISTRATOR_NAME_MAP.get(rs.getInt("parking_registrator_id")) + ")");

            finService = finFeignClient.create(finService);
            logger.info(finService.toString());

            domainTld.setRenewServiceId(finService.getId());

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
