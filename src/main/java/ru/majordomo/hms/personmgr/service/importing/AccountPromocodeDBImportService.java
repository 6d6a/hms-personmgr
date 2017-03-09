package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.event.accountPromocode.AccountPromocodeCreateEvent;
import ru.majordomo.hms.personmgr.event.accountPromocode.AccountPromocodeImportEvent;
import ru.majordomo.hms.personmgr.event.accountService.AccountServiceCreateEvent;
import ru.majordomo.hms.personmgr.event.accountService.AccountServiceImportEvent;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PromocodeRepository;

import static ru.majordomo.hms.personmgr.common.Constants.PARTNER_PROMOCODE_ACTION_ID;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountPromocodeDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountPromocodeDBImportService.class);

    private PromocodeRepository promocodeRepository;
    private AccountPromocodeRepository accountPromocodeRepository;
    private NamedParameterJdbcTemplate partnersNamedParameterJdbcTemplate;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public AccountPromocodeDBImportService(
            @Qualifier("partnersNamedParameterJdbcTemplate") NamedParameterJdbcTemplate partnersNamedParameterJdbcTemplate,
            PromocodeRepository promocodeRepository,
            AccountPromocodeRepository accountPromocodeRepository,
            @Qualifier("namedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            ApplicationEventPublisher publisher
    ) {
        this.partnersNamedParameterJdbcTemplate = partnersNamedParameterJdbcTemplate;
        this.promocodeRepository = promocodeRepository;
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.publisher = publisher;
    }

    private void pull() {
        String query = "SELECT id, name, plan_id FROM account ORDER BY id ASC";

        namedParameterJdbcTemplate.query(query, resultSet -> {
            publisher.publishEvent(new AccountPromocodeImportEvent(resultSet.getString("id")));
        });
    }

    public void pull(String accountId) {
        logger.debug("Start finding for " + accountId);

        this.pull(accountId, accountId);

        logger.debug("Finish finding for " + accountId);
    }

    private void pull(String accountId, String personalAccountId) {
        logger.debug("Start pull for " + accountId);

        String query = "SELECT p.id, p.accountid, p.postfix, p.active, p.valid_till " +
                "FROM promorecord p WHERE accountid = :accountid";

        SqlParameterSource namedParameters = new MapSqlParameterSource("accountid", accountId);

        partnersNamedParameterJdbcTemplate.query(query,
                namedParameters,
                (rs, rowNum) -> {
                    logger.debug("Found code " + rs.getString("postfix") +
                            rs.getString("id") + " for " + accountId);
                    AccountPromocode accountPromocode = new AccountPromocode();

                    accountPromocode.setPersonalAccountId(personalAccountId);
                    accountPromocode.setOwnedByAccount(true);
                    accountPromocode.setOwnerPersonalAccountId(personalAccountId);

                    Map<String, Boolean> actionsWithStatus = new HashMap<>();
                    actionsWithStatus.put(PARTNER_PROMOCODE_ACTION_ID, true);

                    accountPromocode.setActionsWithStatus(actionsWithStatus);

                    Promocode promocode = promocodeRepository.findByCode(
                            rs.getString("postfix") + rs.getString("id")
                    );

                    if (promocode != null) {
                        logger.debug("Found promocode by code " + rs.getString("postfix") +
                                rs.getString("id")
                        );
                        accountPromocode.setPromocodeId(promocode.getId());
                    } else {
                        return null;
                    }

                    publisher.publishEvent(new AccountPromocodeCreateEvent(accountPromocode));

                    //TODO добавить JOIN account что-бы не барть удаленные акки
                    String queryPromocodes = "SELECT p.acc_id, p.promo_code " +
                            "FROM promocodes p WHERE promo_code = :promo_code";
                    SqlParameterSource namedParametersP = new MapSqlParameterSource(
                            "promo_code",
                            rs.getString("postfix") + rs.getString("id")
                    );

                    namedParameterJdbcTemplate.query(queryPromocodes,
                            namedParametersP,
                            (rsP, rowNumP) -> {
                                String accountIdUser = rsP.getString("acc_id");
                                logger.debug("Found promocodes code " + rs.getString("postfix")
                                        + rs.getString("id") + " for " + accountIdUser
                                );
                                AccountPromocode accountPromocodeP = new AccountPromocode();

                                accountPromocodeP.setPersonalAccountId(accountIdUser);

                                accountPromocodeP.setOwnedByAccount(false);
                                accountPromocodeP.setOwnerPersonalAccountId(personalAccountId);
                                accountPromocodeP.setActionsWithStatus(actionsWithStatus);

                                accountPromocodeP.setPromocodeId(promocode.getId());

                                publisher.publishEvent(new AccountPromocodeCreateEvent(accountPromocodeP));

                                return null;
                            }
                    );
                    return null;
                }
        );
    }

    public boolean importToMongo() {
        accountPromocodeRepository.deleteAll();
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountId) {
        List<AccountPromocode> accountPromocodes = accountPromocodeRepository.findByOwnerPersonalAccountId(accountId);

        if (!accountPromocodes.isEmpty()) {
            accountPromocodeRepository.delete(accountPromocodes);
        }

        pull(accountId);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {

    }
}
