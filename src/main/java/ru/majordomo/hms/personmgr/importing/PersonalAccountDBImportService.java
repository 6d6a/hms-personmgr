package ru.majordomo.hms.personmgr.importing;


import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.Language;
import ru.majordomo.hms.personmgr.common.ResourceType;
import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.service.BusinessHelper;

import javax.annotation.Nullable;

import static ru.majordomo.hms.personmgr.common.PhoneNumberManager.formatPhone;
import static ru.majordomo.hms.personmgr.common.PhoneNumberManager.phoneValid;

@Service
@RequiredArgsConstructor
public class PersonalAccountDBImportService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Qualifier("namedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PersonalAccountManager accountManager;
    private final PersonalAccountRepository personalAccountRepository;
    private final PlanManager planManager;
    private final BusinessHelper businessHelper;

    @Nullable
    public Set<Language> getWebSiteAllowedServiceTypes(String accountId) {
        Plan plan;
        PersonalAccount account = personalAccountRepository.findById(accountId).orElse(null);
        if (account != null) {
            plan = planManager.findOne(account.getPlanId());
        } else {
            try {
                String query = "SELECT plan_id FROM account WHERE id = :accountId";
                SqlParameterSource params = new MapSqlParameterSource("accountId", accountId);
                String oldId = jdbcTemplate.queryForObject(query, params, String.class);
                plan = planManager.findByOldId(oldId);
            } catch (EmptyResultDataAccessException ex) {
                return null;
            }
        }
        if (plan == null) {
            return null;
        }
        if (plan.getProhibitedResourceTypes().contains(ResourceType.WEB_SITE)
                || !(plan.getPlanProperties() instanceof VirtualHostingPlanProperties)) {
            return Collections.emptySet();
        }
        VirtualHostingPlanProperties prop = (VirtualHostingPlanProperties) plan.getPlanProperties();
        if (prop.getSitesLimit().getFreeLimit() == 0 || CollectionUtils.isEmpty(prop.getWebSiteAllowedServiceTypes())) {
            return Collections.emptySet();
        }

        return prop.getWebSiteAllowedServiceTypes().stream()
                .map(s -> s.startsWith("WEBSITE_APACHE2_PHP") ? Language.PHP : s.startsWith("WEBSITE_APACHE2_PERL") ? Language.PERL :
                "STAFF_NGINX".equals(s) ? Language.STATIC : null).filter(Objects::nonNull).collect(Collectors.toSet());
    }



    public Plan importToMongo(String accountId, String operationId) {
        clean(accountId);
        logger.info("[start] Searching for PersonalAccount for acc " + accountId);

        String query = "SELECT a.id, a.name, a.client_id, a.credit, a.plan_id, m.notify_days, " +
                "a.status, a.acc_create_date, a.date_negative_balance, " +
                "c.client_auto_bill, a.overquoted, a.overquot_addcost, e.value as sms_phone " +
                "FROM account a " +
                "LEFT JOIN Money m ON a.id = m.acc_id " +
                "LEFT JOIN client c ON a.client_id = c.Client_ID " +
                "LEFT JOIN extend e ON (a.id = e.acc_id AND e.usluga = 18) " +
                "WHERE a.client_id != 0 AND a.id = :accountId";

        SqlParameterSource namedParametersE = new MapSqlParameterSource("accountId", accountId);

        SqlRowSet rs = jdbcTemplate.queryForRowSet(query, namedParametersE);
        if (!rs.next()) {
            throw new InternalApiException("Не удалось загрузить PersonalAccount с id: " + accountId);
        }
        String planId = rs.getString("plan_id");
        String clientId = rs.getString("client_id");
        String nameRaw = rs.getString("name");
        boolean status = rs.getBoolean("status");
        Date accCreateDate = rs.getDate("acc_create_date");
        @Nullable
        Date dateNegativeBalance = rs.getDate("date_negative_balance");
        int notifyDays = rs.getInt("notify_days");
        boolean credit = rs.getString("credit").equals("y");
        boolean clientAutoBill = rs.getString("client_auto_bill").equals("1");
        boolean overquotAddcost = rs.getString("overquot_addcost").equals("1");
        @Nullable
        String smsPhone = rs.getString("sms_phone");

        Plan plan = planManager.findByOldId(planId);
        if (plan == null) {
            throw new InternalApiException("Не удалось найти тарифный план");
        }
        String name = nameRaw.startsWith("ac_") ? nameRaw.toUpperCase() : nameRaw;
        String formattedPhone = StringUtils.isNotEmpty(smsPhone) && phoneValid(smsPhone) ? formatPhone(smsPhone) : null;

        PersonalAccount personalAccount = new PersonalAccount();
        personalAccount.setPlanId(plan.getId());
        personalAccount.setId(accountId);
        personalAccount.setAccountId(accountId);
        personalAccount.setAccountNew(false);
        personalAccount.setAccountType(AccountType.VIRTUAL_HOSTING);
        personalAccount.setCreditPeriod("P14D");
        personalAccount.setNotifyDays(notifyDays);
        personalAccount.setClientId(clientId);
        personalAccount.setActive(status);
        personalAccount.setAutoBillSending(clientAutoBill);
        personalAccount.setAddQuotaIfOverquoted(overquotAddcost);
        personalAccount.setCredit(credit);
        personalAccount.setName(name);
        personalAccount.setCreated(LocalDateTime.of(accCreateDate.toLocalDate(), LocalTime.MAX));

        if (!status && dateNegativeBalance != null) {
            personalAccount.setDeactivated(LocalDateTime.of(dateNegativeBalance.toLocalDate(), LocalTime.MAX));
        }
        if (StringUtils.isNotEmpty(formattedPhone)) {
            personalAccount.setSmsPhoneNumber(formattedPhone);
        }

        long quotaKb;
        boolean unixAccountDenied;
        boolean ftpUserDenied = !plan.isFtpUserAllowed();
        boolean websiteDenied = !plan.isWebSiteAllowed();
        boolean databaseDenied = !plan.isDatabaseAllowed();
        if (plan.getPlanProperties() instanceof VirtualHostingPlanProperties) {
            VirtualHostingPlanProperties properties = (VirtualHostingPlanProperties) plan.getPlanProperties();
            quotaKb = properties.getQuotaKBLimit().getFreeLimit();
        } else {
            throw new InternalApiException("Некорректный тип тарифного плана");
        }
        unixAccountDenied = !plan.isUnixAccountAllowed();

        accountManager.save(personalAccount);

        businessHelper.setParam(operationId, "accountEnabled", status);
        businessHelper.setParam(operationId, "quotaBytes", quotaKb * 1024);
        businessHelper.setParam(operationId, "unixAccountDenied", unixAccountDenied);
        businessHelper.setParam(operationId, "ftpUserDenied", ftpUserDenied);
        businessHelper.setParam(operationId, "websiteDenied", websiteDenied);
        businessHelper.setParam(operationId, "databaseDenied", databaseDenied);

        logger.info("[finish] Searching for PersonalAccount for acc " + accountId);
        return plan;
    }

    public void clean(String accountId) {
        accountManager.delete(accountId);
    }
}
