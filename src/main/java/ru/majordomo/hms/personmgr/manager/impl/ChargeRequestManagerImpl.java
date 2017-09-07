package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.manager.ChargeRequestManager;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequestItem;
import ru.majordomo.hms.personmgr.repository.ChargeRequestRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class ChargeRequestManagerImpl implements ChargeRequestManager {
    private final ChargeRequestRepository repository;
    private final MongoOperations mongoOperations;

    public ChargeRequestManagerImpl(
            ChargeRequestRepository repository,
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
    public void delete(ChargeRequest chargeRequest) {
        repository.delete(chargeRequest);
    }

    @Override
    public void delete(Iterable<ChargeRequest> chargeRequests) {
        repository.delete(chargeRequests);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public ChargeRequest save(ChargeRequest chargeRequest) {
        return repository.save(chargeRequest);
    }

    @Override
    public List<ChargeRequest> save(Iterable<ChargeRequest> chargeRequests) {
        return repository.save(chargeRequests);
    }

    @Override
    public ChargeRequest insert(ChargeRequest chargeRequest) {
        return repository.insert(chargeRequest);
    }

    @Override
    public List<ChargeRequest> insert(Iterable<ChargeRequest> chargeRequests) {
        return repository.insert(chargeRequests);
    }

    @Override
    public ChargeRequest findOne(String id) {
        checkById(id);

        return repository.findOne(id);
    }

    @Override
    public List<ChargeRequest> findAll() {
        return repository.findAll();
    }

    @Override
    public List<ChargeRequest> findByPersonalAccountId(String personalAccountId) {
        return repository.findByPersonalAccountId(personalAccountId);
    }

    @Override
    public List<ChargeRequest> findByPersonalAccountIdAndChargeDate(String personalAccountId, LocalDate chargeDate) {
        return repository.findByPersonalAccountIdAndChargeDate(personalAccountId, chargeDate);
    }

    @Override
    public List<ChargeRequest> findByChargeDate(LocalDate chargeDate) {
        return repository.findByChargeDate(chargeDate);
    }

    @Override
    public List<ChargeRequest> findByChargeDateAndStatus(LocalDate chargeDate, ChargeRequest.Status status) {
        return repository.findByChargeDateAndStatus(chargeDate, status);
    }

    @Override
    public List<ChargeRequest> getForProcess(LocalDate chargeDate, Integer limit) {
        List<ChargeRequest> chargeRequests = new ArrayList<>();

        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(ChargeRequestItem.Status.NEW).and("chargeDate").is(chargeDate));

        Update update = new Update();
        update.set("status", ChargeRequestItem.Status.PROCESSING);

        for (int count = 0; count < limit; count++) {
            chargeRequests.add(mongoOperations.findAndModify(query, update, ChargeRequest.class));
        }
        return chargeRequests;
    }

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("ChargeRequest с id: " + id + " не найден");
        }
    }
}
