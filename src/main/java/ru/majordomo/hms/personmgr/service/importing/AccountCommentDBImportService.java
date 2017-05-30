package ru.majordomo.hms.personmgr.service.importing;

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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.AccountComment;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.AccountCommentRepository;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountCommentDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountCommentDBImportService.class);

    private AccountCommentRepository accountCommentRepository;
    private PersonalAccountManager accountManager;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private List<AccountComment> accountComments = new ArrayList<>();

    @Autowired
    public AccountCommentDBImportService(
            NamedParameterJdbcTemplate jdbcTemplate,
            AccountCommentRepository accountCommentRepository,
            PersonalAccountManager accountManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountCommentRepository = accountCommentRepository;
        this.accountManager = accountManager;
    }

    public void pull() {
        accountCommentRepository.deleteAll();

        String query = "SELECT comment_id, account_id, comment_date, comment_text, login FROM account_comments";

        jdbcTemplate.query(query, this::rowMap);
    }

    public void pull(String accountId) {
        List<AccountComment> accountComments = accountCommentRepository.findByPersonalAccountId(accountId);
        if (accountComments != null) {
            accountCommentRepository.delete(accountComments);
        }

        String query = "SELECT comment_id, account_id, comment_date, comment_text, login FROM account_comments WHERE account_id = :account_id";

        SqlParameterSource namedParameter = new MapSqlParameterSource("account_id", accountId);

        jdbcTemplate.query(query, namedParameter, this::rowMap);
    }

    private AccountComment rowMap(ResultSet rs, int rowNum) throws SQLException {
        AccountComment accountComment = new AccountComment();

        PersonalAccount account = accountManager.findByAccountId(rs.getString("account_id"));

        logger.debug("rs.getString(\"account_id\") " + rs.getString("account_id"));

        if (account != null) {
            accountComment.setPersonalAccountId(account.getId());
            accountComment.setCreated(LocalDateTime.of(
                    rs.getDate("comment_date").toLocalDate(),
                    rs.getTime("comment_date").toLocalTime())
            );
            accountComment.setMessage(rs.getString("comment_text"));
            accountComment.setOperator(rs.getString("login"));

            accountComments.add(accountComment);
        }

        return accountComment;
    }

    public boolean importToMongo() {
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountId) {
        pull(accountId);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        try {
            accountCommentRepository.save(accountComments);
        } catch (ConstraintViolationException e) {
            logger.debug(e.getMessage() + " with errors: " + e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining()));
        }
    }
}
