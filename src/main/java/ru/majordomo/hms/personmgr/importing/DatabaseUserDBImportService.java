package ru.majordomo.hms.personmgr.importing;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.dto.ResultWithWarning;
import ru.majordomo.hms.personmgr.service.BusinessHelper;
import ru.majordomo.hms.rc.user.resources.DBType;
import ru.majordomo.hms.rc.user.resources.DatabaseUser;


@Component
@AllArgsConstructor
public class DatabaseUserDBImportService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Qualifier("namedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate billingDbJdbcTemplate;
    private final BusinessHelper businessHelper;
    private final ImportHelper importHelper;

    public ResultWithWarning importToMongo(String accountId, String mysqlServiceId, String operationId, boolean accountEnabled) {
        String query = "SELECT a.id, a.name, a.plan_id, a.old_name, p.db, a.server_id, s.jail, s.localdb " +
                "FROM account a " +
                "JOIN servers s ON s.id = a.server_id " +
                "JOIN plan p ON p.Plan_ID = a.plan_id " +
                "WHERE a.id = :accountId";
        SqlParameterSource sqlParams = new MapSqlParameterSource("accountId", accountId);

        ResultWithWarning result = new ResultWithWarning();
        SqlRowSet rs = billingDbJdbcTemplate.queryForRowSet(query, sqlParams);
        while (rs.next()) {
            logger.debug("Found DatabaseUser for id: " + rs.getString("id") + " old_name: " + rs.getString("old_name"));

            String userName = "u" + accountId;

            int oldServerId =  rs.getInt("server_id");
            String oldName = rs.getString("old_name");
            boolean localDb = "1".equals(rs.getString("localdb"));

            if (!oldName.equals("")) {
                userName = oldName.replaceAll("\\.", "");
                userName = userName.substring(0, Math.min(userName.length(), 15));
            }

            DatabaseUser databaseUser = new DatabaseUser();
            databaseUser.setAccountId(accountId);


            databaseUser.setPasswordHash("someReallyHardToCrackPasswordHash1111!");

            boolean pwdLoadWarning = false;
            if (!localDb) {
                pwdLoadWarning = true;
            } else {
                pwdLoadWarning = true;
            }
            if (pwdLoadWarning) {
                result.addWarning(String.format("Не удалось загрузить пароль для mysql пользователя %s. Необходимо установить пароль вручную", userName));
            }

            databaseUser.setServiceId(mysqlServiceId);

            databaseUser.setType(DBType.MYSQL);
            databaseUser.setSwitchedOn(accountEnabled);
            databaseUser.setName(userName);

            SimpleServiceMessage message = importHelper.makeServiceMessage(accountId, operationId, databaseUser);

            businessHelper.buildActionByOperationId(BusinessActionType.DATABASE_USER_CREATE_RC, message, operationId);
        }

        return result;
    }
}
