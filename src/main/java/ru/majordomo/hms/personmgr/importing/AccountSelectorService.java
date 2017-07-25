package ru.majordomo.hms.personmgr.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;

@Service
public class AccountSelectorService {
    private final static Logger logger = LoggerFactory.getLogger(AccountSelectorService.class);

    private final NamedParameterJdbcTemplate partnersNamedParameterJdbcTemplate;

    public AccountSelectorService(
            NamedParameterJdbcTemplate partnersNamedParameterJdbcTemplate
    ) {
        this.partnersNamedParameterJdbcTemplate = partnersNamedParameterJdbcTemplate;
    }

    public List<String> selectAccountIdsByServerId(String serverId) {
        List<String> accountIds;

        String query = "SELECT a.id, a.name FROM account a WHERE a.server_id = :serverId ORDER BY a.id ASC";

        SqlParameterSource namedParametersE = new MapSqlParameterSource("serverId", serverId);

        accountIds = partnersNamedParameterJdbcTemplate.query(query,
                namedParametersE,
                this::rowMapAccount
        );

        return accountIds;
    }

    private String rowMapAccount(ResultSet rs, int rowNum) throws SQLException {
        logger.debug("Found Account " + rs.getString("name"));

        return rs.getString("id");
    }
}
