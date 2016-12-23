package ru.majordomo.hms.personmgr.service.importing;

import org.apache.http.util.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.model.AccountHistory;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountHistoryDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountHistoryDBImportService.class);

    private AccountHistoryRepository accountHistoryRepository;
    private PersonalAccountRepository personalAccountRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private List<AccountHistory> accountHistoryList = new ArrayList<>();

    @Autowired
    public AccountHistoryDBImportService(NamedParameterJdbcTemplate jdbcTemplate, AccountHistoryRepository accountHistoryRepository, PersonalAccountRepository personalAccountRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountHistoryRepository = accountHistoryRepository;
        this.personalAccountRepository = personalAccountRepository;
    }

    public void pull() {
        accountHistoryRepository.deleteAll();

        String query = "SELECT id, account, date, action, login FROM client_history WHERE id > 4961960";

        jdbcTemplate.query(query, (rs, rowNum) -> {
            AccountHistory accountHistory = new AccountHistory();

            PersonalAccount account = personalAccountRepository.findByAccountId(rs.getString("account"));

            logger.debug("rs.getString(\"account\") " + rs.getString("account"));

            if (account != null) {
                accountHistory.setPersonalAccountId(account.getId());

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

                accountHistoryList.add(accountHistory);

            }

            return accountHistory;
        });
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
            logger.debug(e.getMessage() + " with errors: " + StreamSupport.stream(e.getConstraintViolations().spliterator(), false).map(ConstraintViolation::getMessage).collect(Collectors.joining()));
        }
    }
}
