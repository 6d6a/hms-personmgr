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
import ru.majordomo.hms.personmgr.manager.AbonementManager;
import ru.majordomo.hms.personmgr.model.abonement.AccountServiceAbonement;
import ru.majordomo.hms.personmgr.repository.ServiceAbonementRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

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
    public void delete(AccountServiceAbonement serviceAbonement) {
        repository.delete(serviceAbonement);
    }

    @Override
    public void delete(Iterable<AccountServiceAbonement> serviceAbonements) {
        repository.delete(serviceAbonements);
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
        return repository.save(serviceAbonements);
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
        return repository.findOne(id);
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
    public Stream<AccountServiceAbonement> findAllStream() {
        return repository.findAllStream();
    }

    @Override
    public List<AccountServiceAbonement> findByAbonementId(String abonementId) {
        return repository.findByAbonementId(abonementId);
    }

    @Override
    public Page<AccountServiceAbonement> findByAbonementId(String abonementId, Pageable pageable) {
        return repository.findByAbonementId(abonementId, pageable);
    }

    @Override
    public AccountServiceAbonement findByPersonalAccountId(String personalAccountId) {
        return repository.findByPersonalAccountId(personalAccountId);
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
    public List<AccountServiceAbonement> findByAbonementIdAndAutorenew(String abonementId, String autorenew) {
        return repository.findByAbonementIdAndAutorenew(abonementId, autorenew);
    }

    @Override
    public Page<AccountServiceAbonement> findByAbonementIdAndAutorenew(String abonementId, String autorenew, Pageable pageable) {
        return repository.findByAbonementIdAndAutorenew(abonementId, autorenew, pageable);
    }

    @Override
    public List<AccountServiceAbonement> findByPersonalAccountIdAndAutorenew(String personalAccountId, String autorenew) {
        return repository.findByPersonalAccountIdAndAutorenew(personalAccountId, autorenew);
    }

    @Override
    public Page<AccountServiceAbonement> findByPersonalAccountIdAndAutorenew(String personalAccountId, String autorenew, Pageable pageable) {
        return repository.findByPersonalAccountIdAndAutorenew(personalAccountId, autorenew, pageable);
    }

    @Override
    public List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredAfterAndAutorenew(String personalAccountId, LocalDateTime expired, String autorenew) {
        return repository.findByPersonalAccountIdAndExpiredAfterAndAutorenew(personalAccountId, expired, autorenew);
    }

    @Override
    public List<AccountServiceAbonement> findByPersonalAccountIdAndExpiredBeforeAndAutorenew(String personalAccountId, LocalDateTime expired, String autorenew) {
        return repository.findByPersonalAccountIdAndExpiredBeforeAndAutorenew(personalAccountId, expired, autorenew);
    }

    @Override
    public List<AccountServiceAbonement> findByPersonalAccountIdAndAbonementId(String personalAccountId, String abonementId) {
        return repository.findByPersonalAccountIdAndAbonementId(personalAccountId, abonementId);
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

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("ServiceAbonement с id: " + id + " не найден");
        }
    }
}
