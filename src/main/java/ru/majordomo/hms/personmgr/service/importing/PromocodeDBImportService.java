package ru.majordomo.hms.personmgr.service.importing;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.repository.PromocodeRepository;

import static ru.majordomo.hms.personmgr.common.Constants.PARTNER_PROMOCODE_ACTION_ID;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class PromocodeDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PromocodeDBImportService.class);

    private PromocodeRepository promocodeRepository;
    private NamedParameterJdbcTemplate partnersNamedParameterJdbcTemplate;
    private List<Promocode> promocodesList = new ArrayList<>();

    @Autowired
    public PromocodeDBImportService(@Qualifier("partnersNamedParameterJdbcTemplate") NamedParameterJdbcTemplate partnersNamedParameterJdbcTemplate, PromocodeRepository promocodeRepository) {
        this.partnersNamedParameterJdbcTemplate = partnersNamedParameterJdbcTemplate;
        this.promocodeRepository = promocodeRepository;
    }

    private void pull() {
        String query = "SELECT p.id, p.accountid, p.postfix, p.active, p.created FROM promorecord p";
        promocodesList = partnersNamedParameterJdbcTemplate.query(query, this::rowMap);
    }

    private void pull(String accountId) {
        String query = "SELECT p.id, p.accountid, p.postfix, p.active, p.created FROM promorecord p WHERE accountid = :accountid";

        SqlParameterSource namedParametersE = new MapSqlParameterSource("accountid", accountId);

        promocodesList = partnersNamedParameterJdbcTemplate.query(query,
                namedParametersE,
                this::rowMap
        );
    }

    private Promocode rowMap(ResultSet rs, int rowNum) throws SQLException {
//        logger.debug("Found Partner promocode " + rs.getString("postfix") + rs.getString("id"));
        Promocode promocode = new Promocode();
        promocode.setType(PromocodeType.PARTNER);
        promocode.setCode(rs.getString("postfix") + rs.getString("id"));
        promocode.setActive(rs.getBoolean("active"));
        promocode.setCreatedDate(rs.getDate("created").toLocalDate());

        promocode.setActionIds(Collections.singletonList(PARTNER_PROMOCODE_ACTION_ID));

        return promocode;
    }

    public boolean importToMongo() {
        promocodeRepository.deleteAll();
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountId) {
        promocodeRepository.deleteAll();
        pull(accountId);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        logger.debug("pushToMongo promocodesList");
        promocodeRepository.save(promocodesList);
    }
}
