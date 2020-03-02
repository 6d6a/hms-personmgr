package ru.majordomo.hms.personmgr.importing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.ResultWithWarning;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.service.BusinessHelper;
import ru.majordomo.hms.rc.user.resources.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
@AllArgsConstructor
public class DatabaseDBImportService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Qualifier("namedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate billingDbJdbcTemplate;
    private final RcUserFeignClient rcUserFeignClient;
    private final BusinessHelper businessHelper;
    private final ImportHelper importHelper;

    public static final Map<String, DomainRegistrar> DOMAIN_REGISTRAR_STRING_MAP = new HashMap<String, DomainRegistrar>();

    static {
        DOMAIN_REGISTRAR_STRING_MAP.put("Registrant", DomainRegistrar.NETHOUSE);
        DOMAIN_REGISTRAR_STRING_MAP.put("GPT", DomainRegistrar.R01);
        DOMAIN_REGISTRAR_STRING_MAP.put("RUCENTER", DomainRegistrar.RUCENTER);
        DOMAIN_REGISTRAR_STRING_MAP.put("Enom", DomainRegistrar.ENOM);
        DOMAIN_REGISTRAR_STRING_MAP.put("GoDaddy", DomainRegistrar.GODADDY);
        DOMAIN_REGISTRAR_STRING_MAP.put("Ukrnames", DomainRegistrar.UKRNAMES);
        DOMAIN_REGISTRAR_STRING_MAP.put("RegRu", DomainRegistrar.REGRU);
        DOMAIN_REGISTRAR_STRING_MAP.put("Webnames", DomainRegistrar.WEBNAMES);
    }

    @Data
    private static class ImportAction {
        private ProcessingBusinessAction action;
        private List<String> warnings;
    }

    public ResultWithWarning importToMongo(String accountId, String mysqlServiceId, String operationId, boolean accountEnabled) {
        ResultWithWarning result = new ResultWithWarning();

        String query = "SELECT a.id, a.server_id, udb.uid, udb.db, udb.host, p.QuotaKB " +
                "FROM account a " +
                "JOIN users_db udb USING(uid) " +
                "JOIN plan p ON p.Plan_ID = a.plan_id " +
                "WHERE (udb.host = 'mdb4.intr' OR udb.host LIKE 'web%') AND a.id = :accountId";
        SqlParameterSource namedParameters1 = new MapSqlParameterSource("accountId", accountId);

        billingDbJdbcTemplate.query(query,
                namedParameters1,
                (rs, rowNum) -> rowMap(rs, rowNum, accountId, operationId, mysqlServiceId, accountEnabled)
        );

        return null;
    }

    private ImportAction rowMap(ResultSet rs, int rowNum, String accountId, String operationId, String mysqlServiceId, boolean accountEnabled) throws SQLException {
        logger.debug("Found Database for id: " + rs.getString("id") +
                " name: " + rs.getString("db") +
                " dbServer: " + rs.getString("host"));

        Database database = new Database();
        database.setAccountId(rs.getString("id"));

        database.setServiceId(mysqlServiceId);

        database.setType(DBType.MYSQL);
        database.setSwitchedOn(accountEnabled);
        database.setName(rs.getString("db"));
        database.setQuota(rs.getLong("QuotaKB") * 1024);
        database.setQuotaUsed(0L);
        database.setWritable(true);

        List<DatabaseUser> databaseUsers = rcUserFeignClient.getDatabaseUsers(accountId);

        database.setDatabaseUserIds(databaseUsers.stream().map(DatabaseUser::getId).collect(Collectors.toList()));

        SimpleServiceMessage message = importHelper.makeServiceMessage(accountId, operationId, database);
        message.addParam("databaseUserIds", database.getDatabaseUserIds());

        businessHelper.buildActionByOperationId(BusinessActionType.DATABASE_CREATE_RC, message, operationId);
        return null;
    }
}
