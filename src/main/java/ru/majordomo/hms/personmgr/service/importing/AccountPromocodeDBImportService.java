package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.AccountPromocode;
import ru.majordomo.hms.personmgr.model.promocode.Promocode;
import ru.majordomo.hms.personmgr.repository.AccountPromocodeRepository;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.repository.PromocodeRepository;

import static ru.majordomo.hms.personmgr.common.ImportConstants.getPartnerPromocodeActionId;

/**
 * Сервис для загрузки первичных данных в БД
 */
@Service
public class AccountPromocodeDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(AccountPromocodeDBImportService.class);

    private PromocodeRepository promocodeRepository;
    private AccountPromocodeRepository accountPromocodeRepository;
    private PersonalAccountRepository personalAccountRepository;
    private NamedParameterJdbcTemplate partnersNamedParameterJdbcTemplate;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private List<AccountPromocode> accountPromocodes = new ArrayList<>();

    @Autowired
    public AccountPromocodeDBImportService(@Qualifier("partnersNamedParameterJdbcTemplate") NamedParameterJdbcTemplate partnersNamedParameterJdbcTemplate, PromocodeRepository promocodeRepository, AccountPromocodeRepository accountPromocodeRepository, PersonalAccountRepository personalAccountRepository, @Qualifier("namedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.partnersNamedParameterJdbcTemplate = partnersNamedParameterJdbcTemplate;
        this.promocodeRepository = promocodeRepository;
        this.accountPromocodeRepository = accountPromocodeRepository;
        this.personalAccountRepository = personalAccountRepository;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    //TODO Ускорить - сейчас тупит загрузка
    private void pull() {
        List<PersonalAccount> personalAccounts = personalAccountRepository.findAll();

        for (PersonalAccount personalAccount : personalAccounts) {
            this.pull(personalAccount.getAccountId(), personalAccount.getId());
        }
    }

    private void pull(String accountId) {
        logger.info("Start finding for " + accountId);
        PersonalAccount personalAccount = personalAccountRepository.findByAccountId(accountId);

        if (personalAccount != null) {
            this.pull(accountId, personalAccount.getId());
        }
        logger.info("Finish finding for " + accountId);
    }
    private void pull(String accountId, String personalAccountId) {
        logger.info("Start pull for " + accountId);

        String query = "SELECT p.id, p.accountid, p.postfix, p.active, p.valid_till FROM promorecord p WHERE accountid = :accountid";

        SqlParameterSource namedParameters = new MapSqlParameterSource("accountid", accountId);

        partnersNamedParameterJdbcTemplate.query(query,
                namedParameters,
                (rs, rowNum) -> {
                    logger.info("Found code " + rs.getString("postfix") + rs.getString("id") + " for " + accountId);
                    AccountPromocode accountPromocode = new AccountPromocode();

                    String personalAccountIdLocal = personalAccountId;

                    if (personalAccountIdLocal == null) {
                        PersonalAccount personalAccount = personalAccountRepository.findByAccountId(rs.getString("accountid"));

                        if (personalAccount != null) {
                            logger.info("Found personalAccount for " + accountId);
                            personalAccountIdLocal = personalAccount.getId();
                        }
                    }

                    accountPromocode.setPersonalAccountId(personalAccountIdLocal);

                    accountPromocode.setOwnedByAccount(true);

                    Map<String, Boolean> actionsWithStatus = new HashMap<>();
                    actionsWithStatus.put(getPartnerPromocodeActionId(), true);

                    accountPromocode.setActionsWithStatus(actionsWithStatus);

                    Promocode promocode = promocodeRepository.findByCode(rs.getString("postfix") + rs.getString("id"));

                    if (promocode != null) {
                        logger.info("Found promocode by code " + rs.getString("postfix") + rs.getString("id"));
                        accountPromocode.setPromocodeId(promocode.getId());
                    }

                    accountPromocodes.add(accountPromocode);

                    String queryPromocodes = "SELECT p.acc_id, p.promo_code FROM promocodes p WHERE promo_code = :promo_code";
                    SqlParameterSource namedParametersP = new MapSqlParameterSource("promo_code", rs.getString("postfix") + rs.getString("id"));

                    String finalPersonalAccountIdLocal = personalAccountIdLocal;
                    namedParameterJdbcTemplate.query(queryPromocodes,
                            namedParametersP,
                            (rsP, rowNumP) -> {
                                logger.info("Found promocodes code " + rs.getString("postfix") + rs.getString("id") + " for " + accountId);
                                AccountPromocode accountPromocodeP = new AccountPromocode();

                                PersonalAccount personalAccountP = personalAccountRepository.findByAccountId(rs.getString("accountid"));

                                if (personalAccountP != null) {
                                    logger.info("Found personalAccount for " + accountId);

                                    accountPromocodeP.setPersonalAccountId(personalAccountP.getId());
                                } else {
                                    accountPromocodeP.setPersonalAccountId(finalPersonalAccountIdLocal);
                                }

                                accountPromocodeP.setOwnedByAccount(false);

                                accountPromocodeP.setActionsWithStatus(actionsWithStatus);

                                if (promocode != null) {
                                    logger.info("Found promocode by code " + rs.getString("postfix") + rs.getString("id"));
                                    accountPromocodeP.setPromocodeId(promocode.getId());
                                }

                                accountPromocodes.add(accountPromocodeP);

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

    public boolean importToMongo(String accountName) {
        accountPromocodeRepository.deleteAll();
        pull(accountName);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        accountPromocodeRepository.save(accountPromocodes);
    }
}
