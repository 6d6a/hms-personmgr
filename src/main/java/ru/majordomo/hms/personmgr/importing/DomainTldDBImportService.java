package ru.majordomo.hms.personmgr.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.DomainCategory;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.model.domain.DomainTld;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.DomainTldRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;

import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_CATEGORY_MAP;
import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_REGISTRAR_MAP;
import static ru.majordomo.hms.personmgr.common.Constants.DOMAIN_REGISTRATOR_NAME_MAP;
import static ru.majordomo.hms.personmgr.common.Constants.REGISTRATION_COST_SERVICE_PREFIX;
import static ru.majordomo.hms.personmgr.common.Constants.RENEW_COST_SERVICE_PREFIX;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
@ImportProfile
public class DomainTldDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(DomainTldDBImportService.class);

    private DomainTldRepository domainTldRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private PaymentServiceRepository paymentServiceRepository;
    private List<DomainTld> domainTlds = new ArrayList<>();

    @Autowired
    public DomainTldDBImportService(
            @Qualifier("billing2NamedParameterJdbcTemplate") NamedParameterJdbcTemplate billing2NamedParameterJdbcTemplate,
            DomainTldRepository domainTldRepository,
            PaymentServiceRepository paymentServiceRepository
    ) {
        this.jdbcTemplate = billing2NamedParameterJdbcTemplate;
        this.domainTldRepository = domainTldRepository;
        this.paymentServiceRepository = paymentServiceRepository;
    }

    public void pull() {
        String query = "SELECT domain_tld, parking_registrator_id, registration_cost, renew_cost, " +
                "registration_time, renew_time, renew_due_days, renew_after_days, priority, " +
                "available, permanent, category " +
                "FROM parking_domains_cost";

        domainTlds.addAll(jdbcTemplate.query(query, (rs, rowNum) -> {
            DomainTld domainTld = new DomainTld();

            logger.debug("domain_tld " + rs.getString("domain_tld") +
                    " parking_registrator_id " + rs.getString("parking_registrator_id")
            );

            domainTld.setActive(rs.getBoolean("available"));
            domainTld.setDomainCategory(DOMAIN_CATEGORY_MAP.get(rs.getString("category")));
            domainTld.setRegistrar(DOMAIN_REGISTRAR_MAP.get(rs.getInt("parking_registrator_id")));
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

            PaymentService paymentService = new PaymentService();
            paymentService.setPaymentType(ServicePaymentType.ONE_TIME);
            paymentService.setAccountType(AccountType.VIRTUAL_HOSTING);
            paymentService.setActive(domainTld.isActive());
            paymentService.setCost(rs.getBigDecimal("registration_cost"));
            paymentService.setLimit(-1);
            paymentService.setOldId(REGISTRATION_COST_SERVICE_PREFIX +  domainTld.getTld() + "_" +
                    DOMAIN_REGISTRAR_MAP.get(rs.getInt("parking_registrator_id"))
            );
            paymentService.setName("Регистрация домена в зоне " + domainTld.getEncodedTld() + " (" +
                    DOMAIN_REGISTRATOR_NAME_MAP.get(rs.getInt("parking_registrator_id")) + ")"
            );

            paymentServiceRepository.save(paymentService);
            logger.debug(paymentService.toString());

            domainTld.setRegistrationServiceId(paymentService.getId());

            paymentService = new PaymentService();
            paymentService.setPaymentType(ServicePaymentType.ONE_TIME);
            paymentService.setAccountType(AccountType.VIRTUAL_HOSTING);
            paymentService.setActive(domainTld.isActive());
            paymentService.setCost(rs.getBigDecimal("renew_cost"));
            paymentService.setLimit(-1);
            paymentService.setOldId(RENEW_COST_SERVICE_PREFIX +  domainTld.getTld() + "_" +
                    DOMAIN_REGISTRAR_MAP.get(rs.getInt("parking_registrator_id"))
            );
            paymentService.setName("Продление домена в зоне " + domainTld.getEncodedTld() + " (" +
                    DOMAIN_REGISTRATOR_NAME_MAP.get(rs.getInt("parking_registrator_id")) + ")"
            );

            paymentServiceRepository.save(paymentService);
            logger.debug(paymentService.toString());

            domainTld.setRenewServiceId(paymentService.getId());

            return domainTld;
        }));
    }

    public boolean importToMongo() {
        domainTldRepository.deleteAll();

        try (Stream<PaymentService> paymentServiceStream = paymentServiceRepository.findByOldIdRegex(REGISTRATION_COST_SERVICE_PREFIX + ".*|"
                + RENEW_COST_SERVICE_PREFIX + ".*")) {
            paymentServiceStream.forEach(
                    paymentServiceRepository::delete
            );
        }

        pull();
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        try {
            domainTldRepository.saveAll(domainTlds);
        } catch (ConstraintViolationException e) {
            logger.debug(e.getMessage() + " with errors: " + e.getConstraintViolations()
                    .stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining())
            );
        }
    }
}
