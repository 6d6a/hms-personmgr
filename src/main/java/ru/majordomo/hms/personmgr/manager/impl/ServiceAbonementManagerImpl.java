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
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.repository.ServiceAbonementRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ServiceAbonementManagerImpl implements AbonementManager<AccountServiceAbonement> {
    private final ServiceAbonementRepository repository;
    private final MongoOperations mongoOperations;

    @Autowired
    public ServiceAbonementManagerImpl(
            ServiceAbonementRepository repository,
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
    public void delete(AccountServiceAbonement serviceAbonement) {
        repository.delete(serviceAbonement);
    }

    @Override
    public void delete(Iterable<AccountServiceAbonement> serviceAbonements) {
        repository.deleteAll(serviceAbonements);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public AccountServiceAbonement save(AccountServiceAbonement serviceAbonement) {
        return repository.save(serviceAbonement);
    }

    @Override
    public List<AccountServiceAbonement> save(Iterable<AccountServiceAbonement> serviceAbonements) {
        return repository.saveAll(serviceAbonements);
    }

    @Override
    public AccountServiceAbonement insert(AccountServiceAbonement serviceAbonement) {
        return repository.insert(serviceAbonement);
    }

    @Override
    public List<AccountServiceAbonement> insert(Iterable<AccountServiceAbonement> serviceAbonements) {
        return repository.insert(serviceAbonements);
    }

    @Override
    public AccountServiceAbonement findOne(String id) {
        checkById(id);
        return repository.findById(id).orElse(null);
    }

    @Override
    public AccountServiceAbonement findByIdAndPersonalAccountId(String id, String personalAccountId) {
        checkById(id);
        return repository.findByIdAndPersonalAccountId(id, personalAccountId);
    }

    @Override
    public List<AccountServiceAbonement> findAll() {
        return repository.findAll();
    }

    @Override
    public List<AccountServiceAbonement> findByAbonementId(String abonementId) {
        return repository.findByAbonementId(abonementId);
    }

    @Override
    public AccountServiceAbonement findByPersonalAccountId(String personalAccountId) {
        return repository.findByPersonalAccountId(personalAccountId);
    }

    @Override
    public List<AccountServiceAbonement> findAllByPersonalAccountId(String personalAccountId) {
        return repository.findAllByPersonalAccountId(personalAccountId);
    }

    @Override
    public boolean existsByPersonalAccountId(String personalAccountId) {
        return repository.existsByPersonalAccountId(personalAccountId);
    }

    @Override
    public void deleteByPersonalAccountId(String personalAccountId) {
        repository.deleteByPersonalAccountId(personalAccountId);
    }

    @Override
    public Page<AccountServiceAbonement> findByPersonalAccountId(String personalAccountId, Pageable pageable) {
        return repository.findByPersonalAccountId(personalAccountId, pageable);
    }

    @Override
    public List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired) {
        return repository.findByPersonalAccountIdAndExpiredAfter(personalAccountId, expired);
    }

    @Override
    public List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredBefore(String personalAccountId, LocalDateTime expired) {
        return repository.findByPersonalAccountIdAndExpiredBefore(personalAccountId, expired);
    }

    @Override
    public List<AccountServiceAbonement> findByPersonalAccountIdAndAbonementIdIn(String personalAccountId, List<String> abonementIds) {
        return repository.findByPersonalAccountIdAndAbonementIdIn(personalAccountId, abonementIds);
    }

    @Override
    public void setExpired(String id, LocalDateTime expired) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update().set("expired", expired);

        mongoOperations.updateFirst(query, update, AccountServiceAbonement.class);
    }

    @Override
    public void setAutorenew(String id, boolean autorenew) {
        checkById(id);

        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update().set("autorenew", autorenew);

        mongoOperations.updateFirst(query, update, AccountServiceAbonement.class);
    }

    @Override
    public boolean existsByPersonalAccountIdAndExpiredAfter(String personalAccountId, LocalDateTime expired) {
        return repository.existsByPersonalAccountIdAndExpiredAfter(personalAccountId, expired);
    }

    @Override
    public List<String> findPersonalAccountIdsByExpiredBefore(LocalDateTime expired) {
        return null;
    }

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("ServiceAbonement с id: " + id + " не найден");
        }
    }
}
