package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountAbonement;
import ru.majordomo.hms.personmgr.repository.AccountAbonementRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AccountAbonementManagerImpl implements AbonementManager<AccountAbonement> {
    private final AccountAbonementRepository repository;
    private final MongoOperations mongoOperations;

    @Autowired
    public AccountAbonementManagerImpl(
            AccountAbonementRepository repository,
            MongoOperations mongoOperations
    ) {
        this.repository = repository;
        this.mongoOperations = mongoOperations;
    }

    @Override
    public boolean exists(String id) {
        return repository.existsById(id);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    @Override
    public void delete(AccountAbonement accountAbonement) {
        repository.delete(accountAbonement);
    }

    @Override
    public void delete(Iterable<AccountAbonement> accountAbonements) {
        repository.deleteAll(accountAbonements);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public AccountAbonement save(AccountAbonement accountAbonement) {
        return repository.save(accountAbonement);
    }

    @Override
    public List<AccountAbonement> save(Iterable<AccountAbonement> accountAbonements) {
        return repository.saveAll(accountAbonements);
    }

    @Override
    public AccountAbonement insert(AccountAbonement accountAbonement) {
        return repository.insert(accountAbonement);
    }

    @Override
    public List<AccountAbonement> insert(Iterable<AccountAbonement> accountAbonements) {
        return repository.insert(accountAbonements);
    }

    @Override
    public AccountAbonement findOne(String id) {
        checkById(id);
        return repository.findById(id).orElse(null);
    }

    @Override
    public AccountAbonement findByIdAndPersonalAccountId(String id, String personalAccountId) {
        checkById(id);
        return repository.findByIdAndPersonalAccountId(id, personalAccountId);
    }

    @Override
    public List<AccountAbonement> findAll() {
        return repository.findAll();
    }

    @Override
    public List<AccountAbonement> findByAbonementId(String abonementId) {
        return repository.findByAbonementId(abonementId);
    }

    @Override
    public AccountAbonement findByPersonalAccountId(String personalAccountId) {
        return repository.findByPersonalAccountId(personalAccountId);
    }

    @Override
    public List<AccountAbonement> findAllByPersonalAccountId(String personalAccountId) {
        return repository.findAllByPersonalAccountId(personalAccountId);
    }

    @Override
    public void deleteByPersonalAccountId(String personalAccountId) {
        repository.deleteByPersonalAccountId(personalAccountId);
    }

    @Override
    public Page<AccountAbonement> findByPersonalAccountId(String personalAccountId, Pageable pageable) {
        return repository.findByPersonalAccountId(personalAccountId, pageable);
    }

    @Override
    public List<AccountAbonement> findByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired) {
        return repository.findByPersonalAccountIdAndExpiredAfter(personalAccountId, expired);
    }

    @Override
    public List<AccountAbonement> findByPersonalAccountIdAndExpiredBefore(String personalAccountId, LocalDateTime expired) {
        return repository.findByPersonalAccountIdAndExpiredBefore(personalAccountId, expired);
    }

    @Override
    public List<AccountAbonement> findByPersonalAccountIdAndAbonementIdIn(String personalAccountId, List<String> abonementIds) {
        return repository.findByPersonalAccountIdAndAbonementIdIn(personalAccountId, abonementIds);
    }

    @Override
    public void setExpired(String id, LocalDateTime expired) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update().set("expired", expired);

        mongoOperations.updateFirst(query, update, AccountAbonement.class);
    }

    @Override
    public void setAutorenew(String id, boolean autorenew) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update().set("autorenew", autorenew);

        mongoOperations.updateFirst(query, update, AccountAbonement.class);
    }

    @Override
    public boolean existsByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired) {
        return repository.existsByPersonalAccountIdAndExpiredAfter(personalAccountId, expired);
    }

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("AccountAbonement с id: " + id + " не найден");
        }
    }
}
