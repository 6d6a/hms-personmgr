package ru.majordomo.hms.personmgr.manager.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.common.AccountSetting;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;

import static ru.majordomo.hms.personmgr.common.AccountSetting.ADD_QUOTA_IF_OVERQUOTED;
import static ru.majordomo.hms.personmgr.common.AccountSetting.AUTO_BILL_SENDING;
import static ru.majordomo.hms.personmgr.common.AccountSetting.CREDIT;
import static ru.majordomo.hms.personmgr.common.AccountSetting.CREDIT_ACTIVATION_DATE;
import static ru.majordomo.hms.personmgr.common.AccountSetting.NEW_ACCOUNT;
import static ru.majordomo.hms.personmgr.common.AccountSetting.NOTIFY_DAYS;
import static ru.majordomo.hms.personmgr.common.AccountSetting.OVERQUOTED;
import static ru.majordomo.hms.personmgr.common.AccountSetting.SMS_PHONE_NUMBER;

@Component
public class PersonalAccountManagerImpl implements PersonalAccountManager {
    private final PersonalAccountRepository repository;
    private final MongoOperations mongoOperations;

    @Autowired
    public PersonalAccountManagerImpl(
            PersonalAccountRepository repository,
            MongoOperations mongoOperations
    ) {
        this.repository = repository;
        this.mongoOperations = mongoOperations;
    }

    @Override
    public boolean exists(String id) {
        return repository.exists(id);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void delete(String id) {
        repository.delete(id);
    }

    @Override
    public void delete(PersonalAccount account) {
        repository.delete(account);
    }

    @Override
    public void delete(Iterable<PersonalAccount> accounts) {
        repository.delete(accounts);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public PersonalAccount save(PersonalAccount account) {
        return repository.save(account);
    }

    @Override
    public List<PersonalAccount> save(Iterable<PersonalAccount> accounts) {
        return repository.save(accounts);
    }

    @Override
    public PersonalAccount insert(PersonalAccount account) {
        return repository.insert(account);
    }

    @Override
    public List<PersonalAccount> insert(Iterable<PersonalAccount> accounts) {
        return repository.insert(accounts);
    }

    @Override
    public PersonalAccount findOne(String id) {
        checkById(id);

        return repository.findOne(id);
    }

    @Override
    public PersonalAccount findOneByIdIncludeIdAndActiveAndDeactivated(String id) {
        checkById(id);

        return repository.findOneByIdIncludeIdAndActiveAndDeactivated(id);
    }

    @Override
    public PersonalAccount findOneByIdIncludeId(String id) {
        checkById(id);

        return repository.findOneByIdIncludeId(id);
    }

    @Override
    public PersonalAccount findByName(String name) {
        PersonalAccount account = repository.findByName(name);

        if (account == null) {
            throw new ResourceNotFoundException("Аккаунт с name '" + name + "' не найден");
        }

        return account;
    }

    @Override
    public PersonalAccount findByClientId(String clientId) {
        PersonalAccount account = repository.findByClientId(clientId);

        if (account == null) {
            throw new ResourceNotFoundException("Аккаунт с clientId '" + clientId + "' не найден");
        }

        return account;
    }

    @Override
    public PersonalAccount findByAccountId(String accountId) {
        PersonalAccount account = repository.findByAccountId(accountId);

        if (account == null) {
            throw new ResourceNotFoundException("Аккаунт с accountId '" + accountId + "' не найден");
        }

        return account;
    }

    @Override
    public List<PersonalAccount> findAll() {
        return repository.findAll();
    }

    @Override
    public List<PersonalAccount> findByAccountType(AccountType accountType) {
        return repository.findByAccountType(accountType);
    }

    @Override
    public List<PersonalAccount> findByActive(boolean active) {
        return repository.findByActive(active);
    }

    @Override
    public List<PersonalAccount> findByAccountIdContaining(String accountId) {
        List<PersonalAccount> accounts = repository.findByAccountIdContaining(accountId);

        if (accounts == null || accounts.isEmpty()) {
            throw new ResourceNotFoundException("Аккаунты содержащие в id строку '" + accountId + "' не найдены");
        }

        return accounts;
    }

    @Override
    public Stream<PersonalAccount> findAllStream() {
        return repository.findAllStream();
    }

    @Override
    public Stream<PersonalAccount> findByActiveAndDeactivatedAfter(boolean active, LocalDateTime deactivated) {
        return repository.findByActiveAndDeactivatedAfter(active, deactivated);
    }

    @Override
    public Stream<PersonalAccount> findByNotificationsEquals(MailManagerMessageType messageType) {
        return repository.findByNotificationsEquals(messageType);
    }

    @Override
    public Stream<PersonalAccount> findByIdNotIn(List<String> ids) {
        return repository.findByIdNotIn(ids);
    }

    @Override
    public Page<PersonalAccount> findByAccountIdContaining(String accountId, Pageable pageable) {
        return repository.findByAccountIdContaining(accountId, pageable);
    }

    @Override
    public Page<PersonalAccount> findByActive(boolean active, Pageable pageable) {
        return repository.findByActive(active, pageable);
    }

    @Override
    public void setActive(String id, Boolean active) {
        PersonalAccount account = findOneByIdIncludeIdAndActiveAndDeactivated(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update().set("active", active);

        if (!active) {
            if (account.getDeactivated() == null) {
                update.currentDate("deactivated");
            }
        } else {
            update.unset("deactivated");
        }

        mongoOperations.updateFirst(query, update, PersonalAccount.class);
    }

    @Override
    public void setOwnerPersonId(String id, String personId) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update().set("ownerPersonId", personId);

        mongoOperations.updateFirst(query, update, PersonalAccount.class);
    }

    @Override
    public void setPlanId(String id, String planId) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update().set("planId", planId);

        mongoOperations.updateFirst(query, update, PersonalAccount.class);
    }

    @Override
    public void setAccountNew(String id, Boolean accountNew) {
        setSettingByName(id, NEW_ACCOUNT, accountNew);
    }

    @Override
    public void setCredit(String id, Boolean credit) {
        setSettingByName(id, CREDIT, credit);
    }

    @Override
    public void setAddQuotaIfOverquoted(String id, Boolean addQuotaIfOverquoted) {
        setSettingByName(id, ADD_QUOTA_IF_OVERQUOTED, addQuotaIfOverquoted);
    }

    @Override
    public void setOverquoted(String id, Boolean overquoted) {
        setSettingByName(id, OVERQUOTED, overquoted);
    }

    @Override
    public void setAutoBillSending(String id, Boolean autoBillSending) {
        setSettingByName(id, AUTO_BILL_SENDING, autoBillSending);
    }

    @Override
    public void setNotifyDays(String id, Integer notifyDays) {
        setSettingByName(id, NOTIFY_DAYS, notifyDays);
    }

    @Override
    public void setSmsPhoneNumber(String id, String smsPhoneNumber) {
        setSettingByName(id, SMS_PHONE_NUMBER, smsPhoneNumber);
    }

    @Override
    public void setCreditActivationDate(String id, LocalDateTime creditActivationDate) {
        setSettingByName(id, CREDIT_ACTIVATION_DATE, creditActivationDate);
    }

    @Override
    public void setSettingByName(String id, AccountSetting name, Object value) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update;

        if (value == null) {
            removeSettingByName(id, name);

            return;
        } else if (value instanceof Integer || value instanceof Boolean) {
            update = new Update().set("settings." + name, String.valueOf(value));
        } else if (value instanceof String) {
            update = new Update().set("settings." + name, value);
        } else if (value instanceof LocalDateTime) {
            update = new Update().set("settings." + name, ((LocalDateTime) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        } else {
            throw new IllegalArgumentException("AccountSetting value must be one of Integer, Boolean or String");
        }

        mongoOperations.updateFirst(query, update, PersonalAccount.class);
    }

    @Override
    public void removeSettingByName(String id, AccountSetting name) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update().unset("settings." + name);

        mongoOperations.updateFirst(query, update, PersonalAccount.class);
    }

    @Override
    public void setNotifications(String id, Set<MailManagerMessageType> notifications) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update().set("notifications", notifications);

        mongoOperations.updateFirst(query, update, PersonalAccount.class);
    }

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("Аккаунт с id: " + id + " не найден");
        }
    }
}