package ru.majordomo.hms.personmgr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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
        String query = "SELECT id, name, client_id FROM account";
        personalAccounts = jdbcTemplate.query(query, (rs, rowNum) -> new PersonalAccount(rs.getString("id"), rs.getString("client_id"), rs.getString("name"), AccountType.VIRTUAL_HOSTING));
    }

    private void pull(String accountName) {
        String query = "SELECT id, name, client_id FROM account WHERE name = ?";
        personalAccounts = jdbcTemplate.query(query,
                new Object[] { accountName },
                (rs, rowNum) -> new PersonalAccount(rs.getString("id"), rs.getString("client_id"), rs.getString("name"), AccountType.VIRTUAL_HOSTING)
        );
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
