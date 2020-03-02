package ru.majordomo.hms.personmgr.importing;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.util.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.model.account.AccountHistory;
import ru.majordomo.hms.personmgr.repository.AccountHistoryRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
@RequiredArgsConstructor
public class AccountHistoryDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountHistoryDBImportService.class);

    private final AccountHistoryRepository accountHistoryRepository;
    @Qualifier("namedParameterJdbcTemplate")
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final MongoOperations mongoOperations;


    public void pull(String accountId) {
        logger.info("[start] Searching for AccountHistories for acc: " + accountId);

        String query = "SELECT id, account, date, action, login FROM client_history WHERE account = :account_id";

        SqlParameterSource namedParameter = new MapSqlParameterSource("account_id", accountId);

        jdbcTemplate.query(query, namedParameter, this::rowMap);

        logger.info("[finish] Searching for AccountHistories for acc: " + accountId);
    }

    private final static Pattern DOMAIN_FIX_1 = Pattern.compile(".*платное АВТОМАТИЧЕСКОЕ продление домена (.+)");
    private final static Pattern DOMAIN_FIX_2 = Pattern.compile("Автоматическое продление (.+) невозможно.*");

    private AccountHistory rowMap(ResultSet rs, int rowNum) throws SQLException {
        String accountId = rs.getString("account");

        AccountHistory accountHistory = new AccountHistory();

        accountHistory.setPersonalAccountId(accountId);

        long dateTimestamp = rs.getLong("date");
        LocalDateTime created = new Timestamp(dateTimestamp * 1000).toLocalDateTime();

        accountHistory.setCreated(created);

        try {
            String message = EncodingUtils.getString(rs.getString("action").getBytes("windows-1251"), "koi8-r").trim();
            message = StringEscapeUtils.unescapeHtml(message);

            message = message.replaceAll("<nobr>", "");
            message = message.replaceAll("</nobr>", "");
            message = message.replace("оПЕБШЬЕМХЕ ЙБНР", "Превышение квоты");

            if (message.contains("ЮБРН-ОПНДКЕМ ЮАНМЕЛЕМР Я ГЮВХЯКЕМХЕЛ МЮ ЮА. ЯВЕР")) {
                message = new String(message.getBytes("koi8-r"), "cp1251");
            }
            Matcher matcher;
            if ((matcher = DOMAIN_FIX_1.matcher(message)).matches()) {
                String wrongText = matcher.group(1);
                String text = new String(wrongText.getBytes("koi8-r"), StandardCharsets.UTF_8);
                message = message.replace(wrongText, text);
            }
            if ((matcher = DOMAIN_FIX_2.matcher(message)).matches()) {
                String wrongText = matcher.group(1);
                String text = new String(wrongText.getBytes("koi8-r"), StandardCharsets.UTF_8);
                message = message.replace(wrongText, text);
            }

            accountHistory.setMessage(message);
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
            accountHistory = accountHistoryRepository.insert(accountHistory);

            mongoOperations.updateFirst(Query.query(Criteria.where("_id").is(accountHistory.getId())), Update.update("created", created), AccountHistory.class);
            // установить правильное значение created так как spring перезаписывает его из-за анотации @CreatedDate
        } catch (ConstraintViolationException e) {
            logger.error(e.getMessage() + " with errors: "
                    + e.getConstraintViolations()
                    .stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining())
            );
        }

        logger.info("AccountHistory for acc: " + accountId + " message: " + accountHistory.getMessage());

        return accountHistory;
    }

    public void clean(String accountId) {
        accountHistoryRepository.deleteByPersonalAccountId(accountId);
    }

    public boolean importToMongo(String accountId) {
        clean(accountId);
        pull(accountId);
        return true;
    }
}
