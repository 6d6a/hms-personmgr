package ru.majordomo.hms.personmgr.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.promotion.Promotion;
import ru.majordomo.hms.personmgr.repository.PromotionRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ru.majordomo.hms.personmgr.common.Constants.*;

@Service
public class AccountPromotionDBImportService {

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final AccountHelper accountHelper;
    private final PromotionRepository promotionRepository;
    private final PersonalAccountManager accountManager;
    private final AccountPromotionManager accountPromotionManager;

    private final static Logger logger = LoggerFactory.getLogger(PlanDBImportService.class);

    @Autowired
    public AccountPromotionDBImportService(
            @Qualifier("namedParameterJdbcTemplate") NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            AccountHelper accountHelper,
            PromotionRepository promotionRepository,
            PersonalAccountManager accountManager,
            AccountPromotionManager accountPromotionManager
    ) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.accountHelper = accountHelper;
        this.promotionRepository = promotionRepository;
        this.accountManager = accountManager;
        this.accountPromotionManager = accountPromotionManager;
    }

    public void pull() {
        logger.debug("Start AccountPromotion importing");

        String query = "SELECT account.id, account.plan_id, account.acc_create_date, count(domain.Domain_ID) as domain_count " +
                "FROM account LEFT JOIN domain ON account.uid = domain.UID " +
                "WHERE account.id != 999 AND account.acc_create_date > :acc_create_date " +
                "GROUP BY account.id HAVING COUNT(domain.Domain_ID) = 0";
        // Берём аккаунты зарегистрированные за последние 3 года
        LocalDateTime now = LocalDateTime.now().minusYears(3L);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formatDateTime = now.format(formatter);

        SqlParameterSource parameters_date = new MapSqlParameterSource().addValue("acc_create_date", formatDateTime);

        namedParameterJdbcTemplate.query(query, parameters_date, this::rowMap);

        logger.debug("End AccountPromotion importing");
    }

    public void pull(String accountId) {
        String query = "SELECT a.id, a.plan_id, a.acc_create_date, count(d.Domain_ID) as domain_count " +
                "FROM account a LEFT JOIN domain d ON a.uid = d.UID " +
                "WHERE a.id != 999 AND a.acc_create_date > :acc_create_date AND a.id = :account_id " +
                "GROUP BY a.id HAVING COUNT(d.Domain_ID) = 0";

        SqlParameterSource namedParameter = new MapSqlParameterSource("account_id", accountId);

        namedParameterJdbcTemplate.query(query, namedParameter, this::rowMap);
    }

    private boolean rowMap(ResultSet rs, int rowNum) throws SQLException {
        if (rs.getInt("domain_count") == 0) {

            //Проверка на отсутствие списаний за хостинг
            String sql = "SELECT count(*) FROM Old_payment WHERE Domain_name = :account AND kind = 'r' AND usl_id = '1'";
            SqlParameterSource parameters = new MapSqlParameterSource().addValue("account", rs.getInt("id"));
            int count = namedParameterJdbcTemplate.queryForObject(sql, parameters, Integer.class);

            //Списаний нет
            if (count == 0) {
                // Проверка на наличие промокода с free_domain
                String sql2 = "SELECT free_domain FROM promocodes_mj WHERE acc_id_used = :account";
                Integer count_free_domain;
                try {
                    count_free_domain = namedParameterJdbcTemplate.queryForObject(sql2, parameters, Integer.class);
                    // Промокод найден
                    if (count_free_domain != null && count_free_domain == 1) {
                        Promotion promotion = promotionRepository.findByName(FREE_DOMAIN_PROMOTION);
                        PersonalAccount account = accountManager.findByAccountId(rs.getString("id"));
                        accountHelper.giveGift(account, promotion);
                    }
                } catch (EmptyResultDataAccessException e) {
                    // Промокода нет

                    // При открытии нового аккаунта виртуального хостинга по тарифным планам «Безлимитный», «Безлимитный+», «Бизнес», «Бизнес+»
                    // мы бесплатно зарегистрируем на Вас 1 домен в зоне .ru или .рф при единовременной оплате за
                    // 3 месяца. Бонус предоставляется при открытии аккаунта для первого домена на аккаунте.
                    if (rs.getInt("plan_id") == 9807 // «Бизнес+»
                            || rs.getInt("plan_id") == 9806 // «Бизнес»
                            || rs.getInt("plan_id") == 9805 // «Безлимитный+»
                            || rs.getInt("plan_id") == 9802) { // «Безлимитный»

                        // Наличие пополнения баланса на 3 месяца
                        String sql3 = "SELECT plan.cost " +
                                "FROM plan " +
                                "JOIN account USING(plan_id) " +
                                "JOIN Money ON Money.acc_id = account.id " +
                                "WHERE account.id = :account";
                        String sql4 = "SELECT Money.balance " +
                                "FROM plan " +
                                "JOIN account USING(plan_id) " +
                                "JOIN Money ON Money.acc_id = account.id " +
                                "WHERE account.id = :account";
                        //Проверка на неактивированный абонемент
                        String sql5 = "SELECT count(*) " +
                                "FROM abonement " +
                                "WHERE acc_id = :account AND date_activate = '0000-00-00'";

                        try {
                            Integer billing_plan_cost = namedParameterJdbcTemplate.queryForObject(sql3, parameters, Integer.class);
                            Integer billing_balance = namedParameterJdbcTemplate.queryForObject(sql4, parameters, Integer.class);
                            Integer billing_count_abonement = namedParameterJdbcTemplate.queryForObject(sql5, parameters, Integer.class);

                            if ((billing_balance >= 3 * billing_plan_cost) || (billing_count_abonement > 0)) {
                                Promotion promotion = promotionRepository.findByName(FREE_DOMAIN_PROMOTION);
                                PersonalAccount account = accountManager.findByAccountId(rs.getString("id"));
                                if (account != null) {
                                    accountHelper.giveGift(account, promotion);
                                } else {
                                    logger.debug("AccountPromotion Import Service. Account with id: " +
                                            rs.getString("id") + " not found.");
                                }
                            }
                        } catch (EmptyResultDataAccessException ex) {
                            ex.printStackTrace();
                        }
                    }
                }

            }

        }
        return true;
    }

    public void clean() {
        accountPromotionManager.deleteAll();
    }

    public void clean(String accountId) {
        List<AccountPromotion> accountPromotions = accountPromotionManager.findByPersonalAccountId(accountId);

        if (!accountPromotions.isEmpty()) {
            accountPromotionManager.delete(accountPromotions);
        }
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
