package ru.majordomo.hms.personmgr.service.importing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

/**
 * DBImportService
 */
@Service
public class PersonalAccountDBImportService {
    private final static Logger logger = LoggerFactory.getLogger(PersonalAccountDBImportService.class);

    private JdbcTemplate jdbcTemplate;
    private PersonalAccountRepository personalAccountRepository;
    private List<PersonalAccount> personalAccounts = new ArrayList<>();

    @Autowired
    public PersonalAccountDBImportService(JdbcTemplate jdbcTemplate, PersonalAccountRepository personalAccountRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.personalAccountRepository = personalAccountRepository;
    }

    private void pull() {
        String query = "SELECT a.id, a.name, a.client_id, a.credit, m.notify_days FROM account a LEFT JOIN Money m ON a.id = m.acc_id";
        personalAccounts = jdbcTemplate.query(query, this::rowMap);
    }

    private void pull(String accountName) {
        String query = "SELECT a.id, a.name, a.client_id, a.credit, m.notify_days FROM account a LEFT JOIN Money m ON a.id = m.acc_id WHERE name = ?";
        personalAccounts = jdbcTemplate.query(query,
                new Object[] { accountName },
                this::rowMap
        );
    }

    private PersonalAccount rowMap(ResultSet rs, int rowNum) throws SQLException {
        PersonalAccount personalAccount = new PersonalAccount(rs.getString("id"), rs.getString("client_id"), rs.getString("name"), AccountType.VIRTUAL_HOSTING);
        personalAccount.setSetting("notify_days", rs.getString("notify_days"));
        personalAccount.setSetting("credit", rs.getString("credit").equals("y") ? "1" : "0");
        return personalAccount;
    }

    public boolean importToMongo() {
        personalAccountRepository.deleteAll();
        pull();
        pushToMongo();
        return true;
    }

    public boolean importToMongo(String accountName) {
        personalAccountRepository.deleteAll();
        pull(accountName);
        pushToMongo();
        return true;
    }

    private void pushToMongo() {
        personalAccountRepository.save(personalAccounts);
    }
}
