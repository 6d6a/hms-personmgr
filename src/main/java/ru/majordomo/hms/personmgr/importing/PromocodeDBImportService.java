package ru.majordomo.hms.personmgr.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.config.ImportProfile;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.repository.PromocodeRepository;

import static ru.majordomo.hms.personmgr.common.Constants.PARTNER_PROMOCODE_ACTION_ID;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
@ImportProfile
public class PromocodeDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PromocodeDBImportService.class);

    private PromocodeRepository promocodeRepository;
    private NamedParameterJdbcTemplate partnersNamedParameterJdbcTemplate;

    @Autowired
    public PromocodeDBImportService(
            @Qualifier("partnersNamedParameterJdbcTemplate") NamedParameterJdbcTemplate partnersNamedParameterJdbcTemplate,
            PromocodeRepository promocodeRepository) {
        this.partnersNamedParameterJdbcTemplate = partnersNamedParameterJdbcTemplate;
        this.promocodeRepository = promocodeRepository;
    }

    private void pull() {
        String query = "SELECT p.id, p.accountid, p.postfix, p.active, p.created FROM promorecord p";
        partnersNamedParameterJdbcTemplate.query(query, this::rowMap);
    }

    private void pull(String accountId) {
        logger.info("[start] Searching for Promocode for acc " + accountId);

        String query = "SELECT p.id, p.accountid, p.postfix, p.active, p.created " +
                "FROM promorecord p " +
                "WHERE accountid = :accountid";

        SqlParameterSource namedParametersE = new MapSqlParameterSource("accountid", accountId);

        partnersNamedParameterJdbcTemplate.query(query,
                namedParametersE,
                this::rowMap
        );

        logger.info("[finish] Searching for Promocode for acc " + accountId);
    }

    private Promocode rowMap(ResultSet rs, int rowNum) throws SQLException {
//        logger.debug("Found Partner promocode " + rs.getString("postfix") + rs.getString("id"));
        Promocode promocode = new Promocode();
        promocode.setType(PromocodeType.PARTNER);
        promocode.setCode(rs.getString("postfix") + rs.getString("id"));
        promocode.setActive(rs.getBoolean("active"));
        promocode.setCreatedDate(rs.getDate("created").toLocalDate());

        promocode.setActionIds(Collections.singletonList(PARTNER_PROMOCODE_ACTION_ID));

        promocodeRepository.save(promocode);

        return promocode;
    }

    private Promocode rowMapClean(ResultSet rs, int rowNum) throws SQLException {
        promocodeRepository.deleteByCode(rs.getString("postfix") + rs.getString("id"));

        return null;
    }

    public void clean() {
        promocodeRepository.deleteAll();
    }

    public void clean(String accountId) {
        logger.info("clean of promocodesList for acc: " + accountId);

        String query = "SELECT p.id, p.accountid, p.postfix, p.active, p.created " +
                "FROM promorecord p " +
                "WHERE accountid = :accountid";

        SqlParameterSource namedParametersE = new MapSqlParameterSource("accountid", accountId);

        partnersNamedParameterJdbcTemplate.query(query,
                namedParametersE,
                this::rowMapClean
        );
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
