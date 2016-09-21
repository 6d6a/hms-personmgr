package ru.majordomo.hms.personmgr.service;

import org.apache.http.util.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.model.AccountHistory;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountHistoryDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountHistoryDBImportService.class);

    private AccountHistoryRepository accountHistoryRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private List<AccountHistory> accountHistoryList = new ArrayList<>();

    @Autowired
    public AccountHistoryDBImportService(NamedParameterJdbcTemplate jdbcTemplate, AccountHistoryRepository accountHistoryRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountHistoryRepository = accountHistoryRepository;
    }

    public void pull() {
        accountHistoryRepository.deleteAll();

        String query = "SELECT id, account, date, action, login FROM client_history WHERE id > 4961960";

        accountHistoryList.addAll(jdbcTemplate.query(query, (rs, rowNum) -> {
            AccountHistory accountHistory = new AccountHistory();

            accountHistory.setAccountId(rs.getString("account"));
            accountHistory.setDateTime(new Timestamp(rs.getLong("date") * 1000).toLocalDateTime());

            try {
                accountHistory.setMessage(EncodingUtils.getString(rs.getString("action").getBytes("windows-1251"), "koi8-r"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                accountHistory.setOperator(EncodingUtils.getString(rs.getString("login").getBytes("windows-1251"), "koi8-r"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return accountHistory;
        }));
    }

    public boolean importToMongo() {
        pull();
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        try {
            accountHistoryRepository.save(accountHistoryList);
        } catch (ConstraintViolationException e) {
            logger.info(e.getMessage() + " with errors: " + StreamSupport.stream(e.getConstraintViolations().spliterator(), false).map(ConstraintViolation::getMessage).collect(Collectors.joining()));
        }
    }
}
