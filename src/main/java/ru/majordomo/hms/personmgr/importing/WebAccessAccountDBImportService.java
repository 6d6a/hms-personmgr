package ru.majordomo.hms.personmgr.importing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.feign.SiFeignClient;
import ru.majordomo.hms.personmgr.service.BusinessHelper;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebAccessAccountDBImportService {
    @Qualifier("namedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final BusinessHelper businessHelper;
    private final SiFeignClient siClient;

    private final static String query = "SELECT id, old_name, name, password FROM account WHERE id = :accountId";

    public void importToMongo(String accountId, String operationId) throws InternalApiException {
        try {
            SqlParameterSource sqlParams = new MapSqlParameterSource("accountId", accountId);
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(query, sqlParams);
            if (rowSet.next()) {
                String username = rowSet.getString("name");
                String oldName = rowSet.getString("old_name");
                String passwordHash = rowSet.getString("password");
                if (StringUtils.isEmpty(username) || StringUtils.isEmpty(passwordHash)) {
                    throw new InternalApiException("Не удалось загрузить параметры аутентификации из базы данных");
                }

                SimpleServiceMessage message = new SimpleServiceMessage();
                message.setAccountId(accountId);
                message.addParam("username", username);
                message.addParam("passwordHash", passwordHash);

                message.addParam("replaceOldResource", true);
                siClient.createWebAccessAccount(message);
                if (StringUtils.isNotEmpty(oldName)) {
                    message.addParam("username", oldName);
                    siClient.createWebAccessAccount(message);
                }
            }
        } catch (InternalApiException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Got exception", ex);
            throw new InternalApiException("Не удалось отправить запрос на добавление данных аутентификации", ex);
        }
    }
}
