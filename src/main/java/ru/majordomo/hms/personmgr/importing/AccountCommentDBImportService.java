package ru.majordomo.hms.personmgr.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.model.account.AccountComment;
import ru.majordomo.hms.personmgr.repository.AccountCommentRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
@ImportProfile
public class AccountCommentDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountCommentDBImportService.class);

    private AccountCommentRepository accountCommentRepository;
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public AccountCommentDBImportService(
            NamedParameterJdbcTemplate jdbcTemplate,
            AccountCommentRepository accountCommentRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountCommentRepository = accountCommentRepository;
    }

    public void pull() {
        String query = "SELECT comment_id, account_id, comment_date, comment_text, login FROM account_comments";

        jdbcTemplate.query(query, this::rowMap);
    }

    public void pull(String accountId) {
        logger.info("[start] Searching for AccountComments for acc " + accountId);

        String query = "SELECT comment_id, account_id, comment_date, comment_text, login FROM account_comments WHERE account_id = :account_id";

        SqlParameterSource namedParameter = new MapSqlParameterSource("account_id", accountId);

        jdbcTemplate.query(query, namedParameter, this::rowMap);

        logger.info("[finish] Searching for AccountComments for acc " + accountId);
    }

    private AccountComment rowMap(ResultSet rs, int rowNum) throws SQLException {
        String accountId = rs.getString("account_id");

        AccountComment accountComment = new AccountComment();

        accountComment.setPersonalAccountId(accountId);
        accountComment.setCreated(LocalDateTime.of(
                rs.getDate("comment_date").toLocalDate(),
                rs.getTime("comment_date").toLocalTime())
        );
        accountComment.setMessage(rs.getString("comment_text"));
        accountComment.setOperator(rs.getString("login"));

        try {
            accountCommentRepository.save(accountComment);
        } catch (ConstraintViolationException e) {
            logger.debug(e.getMessage() + " with errors: " +
                    e.getConstraintViolations()
                            .stream()
                            .map(ConstraintViolation::getMessage)
                            .collect(Collectors.joining())
            );
        }
        logger.info("AccountComment for acc " + accountId + " message: " + accountComment.getMessage());

        return accountComment;
    }

    public void clean() {
        accountCommentRepository.deleteAll();
    }

    public void clean(String accountId) {
        accountCommentRepository.deleteByPersonalAccountId(accountId);
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
