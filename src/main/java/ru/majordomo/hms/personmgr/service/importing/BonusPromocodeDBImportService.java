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
import java.util.List;

import ru.majordomo.hms.personmgr.common.PromocodeType;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.repository.PromocodeRepository;

import static ru.majordomo.hms.personmgr.common.Constants.BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID;
import static ru.majordomo.hms.personmgr.common.Constants.BONUS_PARKING_3_M_PROMOCODE_ACTION_ID;
import static ru.majordomo.hms.personmgr.common.Constants.BONUS_UNLIMITED_1_M_PROMOCODE_ACTION_ID;
import static ru.majordomo.hms.personmgr.common.Constants.BONUS_UNLIMITED_3_M_PROMOCODE_ACTION_ID;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class BonusPromocodeDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(BonusPromocodeDBImportService.class);

    private PromocodeRepository promocodeRepository;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private List<Promocode> promocodesList = new ArrayList<>();

    @Autowired
    public BonusPromocodeDBImportService(
            @Qualifier("namedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            PromocodeRepository promocodeRepository
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.promocodeRepository = promocodeRepository;
    }

    private void pull() {
        String query = "SELECT p.promo_code, p.is_used, p.date_created, p.date_used, p.free_months, " +
                "p.free_domain, p.type " +
                "FROM promocodes_mj p";
        promocodesList = namedParameterJdbcTemplate.query(query, this::rowMap);
    }

    private void pull(String promoCode) {
        String query = "SELECT p.promo_code, p.is_used, p.date_created, p.date_used, p.free_months, " +
                "p.free_domain, p.type " +
                "FROM promocodes_mj p " +
                "WHERE promo_code = :promo_code";

        SqlParameterSource namedParametersE = new MapSqlParameterSource("promo_code", promoCode);

        promocodesList = namedParameterJdbcTemplate.query(query,
                namedParametersE,
                this::rowMap
        );
    }

    private Promocode rowMap(ResultSet rs, int rowNum) throws SQLException {
        Promocode promocode = new Promocode();
        promocode.setType(PromocodeType.BONUS);
        promocode.setCode(rs.getString("promo_code"));
        promocode.setActive(!rs.getBoolean("is_used"));
        promocode.setCreatedDate(rs.getDate("date_created").toLocalDate());
        if (rs.getDate("date_used") != null) {
            promocode.setUsedDate(rs.getDate("date_used").toLocalDate());
        }

        List<String> actionIds = new ArrayList<>();

        switch (rs.getString("type")) {
            case "unlimited":
                switch (rs.getInt("free_months")) {
                    case 1:
                        actionIds.add(BONUS_UNLIMITED_1_M_PROMOCODE_ACTION_ID);
                        break;
                    case 3:
                        actionIds.add(BONUS_UNLIMITED_3_M_PROMOCODE_ACTION_ID);
                        break;
                }
                break;
            case "parking-domains":
                actionIds.add(BONUS_PARKING_3_M_PROMOCODE_ACTION_ID);
                break;
        }

        if (rs.getInt("free_domain") == 1) {
            actionIds.add(BONUS_FREE_DOMAIN_PROMOCODE_ACTION_ID);
        }

        promocode.setActionIds(actionIds);

        return promocode;
    }

    public boolean importToMongo() {
//        promocodeRepository.deleteAll();
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountName) {
//        promocodeRepository.deleteAll();
        pull(accountName);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        promocodeRepository.save(promocodesList);
    }
}
