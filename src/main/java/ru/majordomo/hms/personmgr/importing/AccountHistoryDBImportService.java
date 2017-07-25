package ru.majordomo.hms.personmgr.importing;

import org.apache.http.util.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.model.account.AccountHistory;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountHistoryDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountHistoryDBImportService.class);

    private AccountHistoryRepository accountHistoryRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public AccountHistoryDBImportService(
            NamedParameterJdbcTemplate jdbcTemplate,
            AccountHistoryRepository accountHistoryRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountHistoryRepository = accountHistoryRepository;
    }

    public void pull() {
        String query = "SELECT id, account, date, action, login FROM client_history WHERE id > 4961960";

        jdbcTemplate.query(query, this::rowMap);
    }

    public void pull(String accountId) {
        logger.debug("[start] Searching for AccountHistories for acc: " + accountId);

        //        String query = "SELECT id, account, date, action, login FROM client_history WHERE id > 4961960 AND account = :account_id";
        String query = "SELECT id, account, date, action, login FROM client_history WHERE account = :account_id";

        SqlParameterSource namedParameter = new MapSqlParameterSource("account_id", accountId);

        jdbcTemplate.query(query, namedParameter, this::rowMap);

        logger.debug("[finish] Searching for AccountHistories for acc: " + accountId);
    }

    private AccountHistory rowMap(ResultSet rs, int rowNum) throws SQLException {
        String accountId = rs.getString("account");

        AccountHistory accountHistory = new AccountHistory();

        accountHistory.setPersonalAccountId(accountId);

        accountHistory.setCreated(new Timestamp(rs.getLong("date") * 1000).toLocalDateTime());

        try {
            accountHistory.setMessage(EncodingUtils.getString(rs.getString("action").getBytes("windows-1251"), "koi8-r"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error("Exception in accountHistory.setMessage: " + e.getMessage());
        }
        try {
            accountHistory.setOperator(EncodingUtils.getString(rs.getString("login").getBytes("windows-1251"), "koi8-r"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error("Exception in accountHistory.setOperator: " + e.getMessage());
        }

        try {
            accountHistoryRepository.save(accountHistory);
        } catch (ConstraintViolationException e) {
            logger.debug(e.getMessage() + " with errors: "
                    + e.getConstraintViolations()
                    .stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining())
            );
        }

        logger.debug("AccountHistory for acc: " + accountId + " message: " + accountHistory.getMessage());

        return accountHistory;
    }

    public void clean() {
        accountHistoryRepository.deleteAll();
    }

    public void clean(String accountId) {
        accountHistoryRepository.deleteByPersonalAccountId(accountId);
    }

    public boolean importToMongo() {
        clean();
        pull();
        return true;
    }

    public boolean importToMongo(String accountId) {
        clean(accountId);
        pull(accountId);
        return true;
    }
}
