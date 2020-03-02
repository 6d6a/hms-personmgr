package ru.majordomo.hms.personmgr.importing;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.exception.BaseException;
import ru.majordomo.hms.personmgr.exception.InternalApiException;
import ru.majordomo.hms.personmgr.feign.PartnersFeignClient;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.service.BusinessHelper;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerPromocodeDBImportService {
    @Qualifier("partnersNamedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PartnersFeignClient partnersClient;
    private final PersonalAccountRepository accountRepository;
    private final BusinessHelper businessHelper;

    public boolean importToMongo(String accountId, String operationId) {
        try {
            PersonalAccount account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new InternalApiException("Не удалось найти аккаунт"));

            String query = "SELECT p.id, p.accountid, p.postfix, p.active, p.created " +
                    "FROM promorecord p " +
                    "WHERE accountid = :accountid";

            SqlParameterSource sqlParams = new MapSqlParameterSource("accountid", accountId);

            SqlRowSet rs = jdbcTemplate.queryForRowSet(query, sqlParams);
            if (rs.next()) {
                String code = rs.getString("postfix") + rs.getString("id");
                LocalDateTime created = rs.getDate("created").toLocalDate().atStartOfDay();
                partnersClient.createCode(accountId, account.getName(), code, created);
            }
            return true;
        } catch (BaseException | DataAccessException | FeignException ex) {
            businessHelper.addWarning(operationId, "Не удалось импортировать партнерский промокод, будет создан новый при первом обращении. Исключение: " + ex.toString());
            log.error("Got exception", ex);
            return false;
        } catch (RuntimeException ex) {
            log.error("Got exception", ex);
            throw ex;
        }
    }
}
