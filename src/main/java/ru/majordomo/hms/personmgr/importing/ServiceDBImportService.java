package ru.majordomo.hms.personmgr.importing;

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
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.common.ServicePaymentType;
import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;

import static ru.majordomo.hms.personmgr.common.Constants.FREE_SERVICE_NAME_POSTFIX;
import static ru.majordomo.hms.personmgr.common.Constants.FREE_SERVICE_POSTFIX;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_MONEY_RETURN_PREFIX;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_MONEY_TRANSFER_PREFIX;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_OLD_PREFIX;
import static ru.majordomo.hms.personmgr.common.Constants.SERVICE_PREFIX;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
@ImportProfile
public class ServiceDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(ServiceDBImportService.class);

    private PaymentServiceRepository paymentServiceRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private List<PaymentService> serviceList = new ArrayList<>();

    @Autowired
    public ServiceDBImportService(NamedParameterJdbcTemplate jdbcTemplate, PaymentServiceRepository paymentServiceRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.paymentServiceRepository = paymentServiceRepository;
    }

    public void pull() {
        String query = "SELECT id, usluga, service_cost FROM uslugi WHERE id NOT IN(:usluga_ids)";
        SqlParameterSource namedParameters = new MapSqlParameterSource("usluga_ids", Constants.NOT_NEEDED_SERVICE_IDS);

        serviceList.addAll(jdbcTemplate.query(query, namedParameters, (rs, rowNum) -> {
            PaymentService newService = new PaymentService();

            newService.setPaymentType(ServicePaymentType.MONTH);
            newService.setCost(rs.getString("id").equals("15") ? BigDecimal.valueOf(29L) : rs.getBigDecimal("service_cost"));
            newService.setLimit(0);
            newService.setName(rs.getString("usluga"));
            newService.setAccountType(AccountType.VIRTUAL_HOSTING);
            newService.setActive(true);
            newService.setOldId(SERVICE_PREFIX + rs.getString("id") + (rs.getString("id").equals("20") ? FREE_SERVICE_POSTFIX : ""));

            logger.debug("found PaymentService: " + newService.toString());

            if (Constants.OPTIONALLY_FREE_SERVICE_IDS.contains(Integer.valueOf(rs.getString("id")))) {
                PaymentService newServiceFree = new PaymentService();

                newServiceFree.setPaymentType(ServicePaymentType.MONTH);
                newServiceFree.setCost(BigDecimal.ZERO);
                newServiceFree.setLimit(0);
                newServiceFree.setName(newService.getName() + FREE_SERVICE_NAME_POSTFIX);
                newServiceFree.setAccountType(newService.getAccountType());
                newServiceFree.setActive(true);
                newServiceFree.setOldId(newService.getOldId() + FREE_SERVICE_POSTFIX);

                serviceList.add(newServiceFree);

                logger.debug("found free PaymentService: " + newServiceFree.toString());
            }

            return newService;
        }));

        //Возврат средств
        PaymentService newService = new PaymentService();

        newService.setPaymentType(ServicePaymentType.ONE_TIME);
        newService.setCost(BigDecimal.ZERO);
        newService.setLimit(0);
        newService.setName("Возврат средств");
        newService.setAccountType(AccountType.VIRTUAL_HOSTING);
        newService.setActive(true);
        newService.setOldId(SERVICE_MONEY_RETURN_PREFIX + "1");

        serviceList.add(newService);

        logger.debug("found Возврат средств PaymentService: " + newService.toString());

        //Перенос средств
        newService = new PaymentService();

        newService.setPaymentType(ServicePaymentType.ONE_TIME);
        newService.setCost(BigDecimal.ZERO);
        newService.setLimit(0);
        newService.setName("Перенос средств");
        newService.setAccountType(AccountType.VIRTUAL_HOSTING);
        newService.setActive(true);
        newService.setOldId(SERVICE_MONEY_TRANSFER_PREFIX + "1");

        serviceList.add(newService);

        logger.debug("found Перенос средств PaymentService: " + newService.toString());
    }

    public void pullFix() {
        String query = "SELECT id, usluga, service_cost FROM uslugi WHERE id IN(14)";

        serviceList.addAll(jdbcTemplate.query(query, (rs, rowNum) -> {
//            PaymentService newService = new PaymentService();
//
//            newService.setPaymentType(ServicePaymentType.MONTH);
//            newService.setCost(rs.getBigDecimal("service_cost"));
//            newService.setLimit(0);
//            newService.setName(rs.getString("usluga"));
//            newService.setAccountType(AccountType.VIRTUAL_HOSTING);
//            newService.setActive(true);
//            newService.setOldId(SERVICE_PREFIX + rs.getString("id"));
//
//            logger.debug("found PaymentService: " + newService.toString());

//            return newService;

            PaymentService newServiceFree = new PaymentService();

            newServiceFree.setPaymentType(ServicePaymentType.MONTH);
            newServiceFree.setCost(BigDecimal.ZERO);
            newServiceFree.setLimit(0);
            newServiceFree.setName(rs.getString("usluga") + FREE_SERVICE_NAME_POSTFIX);
            newServiceFree.setAccountType(AccountType.VIRTUAL_HOSTING);
            newServiceFree.setActive(true);
            newServiceFree.setOldId(SERVICE_PREFIX + rs.getString("id") + FREE_SERVICE_POSTFIX);

            return newServiceFree;
        }));
    }


    public void pullOld() {
        String query = "SELECT id, name, active FROM payment_type";

        serviceList.addAll(jdbcTemplate.query(query, (rs, rowNum) -> {
            PaymentService oldService = new PaymentService();

            oldService.setPaymentType(ServicePaymentType.ONE_TIME);
            oldService.setCost(BigDecimal.ZERO);
            oldService.setLimit(0);
            oldService.setName(rs.getString("name"));
            oldService.setAccountType(AccountType.VIRTUAL_HOSTING);
            oldService.setActive(rs.getString("active").equals("1"));
            oldService.setOldId(SERVICE_OLD_PREFIX + rs.getString("id"));

            logger.debug("found Old PaymentService: " + oldService.toString());

            return oldService;
        }));
    }

    public boolean importToMongo() {
        paymentServiceRepository.deleteAll();
        pull();
        pullOld();
        pushToMongo();
        return true;
    }

    public boolean importToMongoOld() {
        pullOld();
        pushToMongo();
        return true;
    }
    public boolean importToMongoFix() {
        pullFix();
        pushToMongo();
        return true;
    }


    private void pushToMongo() {
        try {
            paymentServiceRepository.saveAll(serviceList);
        } catch (ConstraintViolationException e) {
            logger.debug(e.getMessage() + " with errors: " +
                    e.getConstraintViolations()
                            .stream()
                            .map(ConstraintViolation::getMessage)
                            .collect(Collectors.joining())
            );
        }
    }
}
