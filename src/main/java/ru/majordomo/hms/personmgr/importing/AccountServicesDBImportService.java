package ru.majordomo.hms.personmgr.importing;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.Constants;
import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.event.accountService.AccountServiceCreateEvent;
import ru.majordomo.hms.personmgr.event.accountService.AccountServiceImportEvent;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.Plans;
import ru.majordomo.hms.personmgr.model.service.AccountService;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.service.BusinessHelper;

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
 * Импорт услуг с ежедневным списанием
 */
@Service
@RequiredArgsConstructor
public class AccountServicesDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountServicesDBImportService.class);

    @Qualifier("namedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PaymentServiceRepository paymentServiceRepository;
    private final AccountServiceRepository accountServiceRepository;
    private final ApplicationEventPublisher publisher;
    private final PersonalAccountRepository personalAccountRepository;
    private final BusinessHelper businessHelper;

    private final static List<String> DENY_ADDITIONAL_QUOTA_OLD_IDS = Arrays.asList(
            Constants.PLAN_PARKING_PLUS_ID_STRING,
            Constants.PLAN_PARKING_ID_STRING,
            Plans.PARKING_DOMAIN.oldIdStr(),
            Constants.MAIL_PLAN_OLD_ID
    );

    public void pull(String accountId, Plan plan, String operationId) {
        logger.info("[start] Searching for AccountService for acc " + accountId);

        PersonalAccount personalAccount = personalAccountRepository.findById(accountId).orElseThrow(InternalApiException::new);
        boolean active = personalAccount.isActive();
        boolean allowMail = plan.isMailboxAllowed();
        boolean allowAdditionQuota = !DENY_ADDITIONAL_QUOTA_OLD_IDS.contains(plan.getOldId());
        boolean notPhone = StringUtils.isEmpty(personalAccount.getSmsPhoneNumber());

        String query = "SELECT id, name, plan_id, client_type FROM account WHERE id = :accountId";
        SqlParameterSource sqlParams = new MapSqlParameterSource("accountId", accountId);

        SqlRowSet rs = jdbcTemplate.queryForRowSet(query, sqlParams);
        if (!rs.next()) {
            throw new InternalApiException("Не удалось загрузить тип аккаунта");
        }
        /*
         * тип клиента
         * 1 - деньги
         * 4 - абонемент
         * 5 - фтп-вход
         * 2 - бартер
         * 3 - бесплатно
         */
        String clientType = rs.getString("client_type");
        AccountService planDailyService;
        if (!clientType.equals("4")) {
            planDailyService = new AccountService(plan.getService());
            planDailyService.setPersonalAccountId(accountId);
            if (active) {
                planDailyService.setLastBilled(LocalDateTime.now()); // чтобы избежать второго списания
            }

            if (plan.isAbonementOnly()) {
                planDailyService.setComment("посуточные списания для тарифа только с абонементами");
            }
            accountServiceRepository.save(planDailyService);
        }

        String queryExtend = "SELECT acc_id, Domain_name, usluga, cost, value, promo FROM extend WHERE acc_id = :accountId";

        List<AccountService> services = jdbcTemplate.query(queryExtend,
                sqlParams,
                (rsE, rowNumE) -> {

                    String usluga = rsE.getString("usluga");
                    BigDecimal cost = rsE.getBigDecimal("cost");
                    String value = rsE.getString("value");

                    AccountService accountService = new AccountService();
                    PaymentService paymentService;
                    accountService.setPersonalAccountId(accountId);
                    if (active) {
                        accountService.setLastBilled(LocalDateTime.now()); // чтобы избежать второго списания
                    }

                    if (StringUtils.isNotEmpty(value)) {
                        accountService.setComment(value);
                    }
                    boolean isAntispamService = false;
                    switch (usluga) {
                        case "13": // Защита от спама и вирусов
                            if (!allowMail) {
                                return null;
                            }
                            paymentService = paymentServiceRepository.findByOldId(Constants.ANTI_SPAM_SERVICE_ID);
                            isAntispamService = true;
                            break;
                        case "14": // Дополнительный Webalizer (платно)
                            paymentService = paymentServiceRepository.findByOldId("service_14_free"); // бесплатно
                            break;
                        case "15": // Доп. дисковое пространство
                            if (!allowAdditionQuota) {
                                return null;
                            }
                            paymentService = paymentServiceRepository.findByOldId(Constants.ADDITIONAL_QUOTA_100_SERVICE_ID);
                            break;
                        case "18": // СМС уведомления 0 или 49 р
                            if (notPhone) {
                                businessHelper.addWarning(operationId, "Услуга SMS-уведомления пропущена, так как не задан телефон");
                                return null;
                            }
                            paymentService = paymentServiceRepository.findByOldId(cost.signum() == 0 ? SMS_NOTIFICATIONS_29_RUB_SERVICE_ID : SMS_NOTIFICATIONS_FREE_SERVICE_ID);
                            break;
                        default:
                            return null;
                    }

                    if (paymentService == null) {
                        return null;
                    }

                    accountService.setServiceId(paymentService.getId());
                    accountService.setPaymentService(paymentService);

                    if (isAntispamService) {
                        businessHelper.setParam(operationId, "allowAntispam", true);
                    }

                    return accountServiceRepository.save(accountService);
                }
        );

        logger.info("[finish] Searching for AccountService for acc " + accountId);
    }

    public void clean(String accountId) {
        accountServiceRepository.deleteByPersonalAccountId(accountId);
    }

    public boolean importToMongo(String accountId, Plan plan, String operationId) {
        clean(accountId);
        pull(accountId, plan, operationId);
        return true;
    }
}
