package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.manager.ChargeRequestManager;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.Status;
import ru.majordomo.hms.personmgr.repository.ChargeRequestRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        return repository.save(setUpdated(chargeRequest));
    }

    @Override
    public List<ChargeRequest> save(Iterable<ChargeRequest> chargeRequests) {
        return repository.save(
                StreamSupport
                        .stream(chargeRequests.spliterator(), false)
                        .map(this::setUpdated)
                        .collect(Collectors.toSet()));
    }

    @Override
    public ChargeRequest insert(ChargeRequest chargeRequest) {
        return repository.insert(setUpdated(setCreated(chargeRequest)));
    }

    @Override
    public List<ChargeRequest> insert(Iterable<ChargeRequest> chargeRequests) {
        return repository.insert(
                StreamSupport
                        .stream(chargeRequests.spliterator(), false)
                        .map(this::setCreated)
                        .map(this::setUpdated)
                        .collect(Collectors.toSet())
        );
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
    public ChargeRequest findByPersonalAccountIdAndChargeDate(String personalAccountId, LocalDate chargeDate) {
        return repository.findByPersonalAccountIdAndChargeDate(personalAccountId, chargeDate);
    }

    @Override
    public List<ChargeRequest> findByChargeDate(LocalDate chargeDate) {
        return repository.findByChargeDate(chargeDate);
    }

    @Override
    public List<ChargeRequest> findByChargeDateAndStatus(LocalDate chargeDate, Status status) {
        return repository.findByChargeDateAndStatus(chargeDate, status);
    }


    @Override
    public int countNeedToProcessChargeRequests(LocalDate chargeDate) {
        return (int) mongoOperations.count(getNeedToProcessChargeRequestsQuery(chargeDate), ChargeRequest.class);
    }

    @Override
    public List<ChargeRequest> pullNeedToProcessChargeRequests(LocalDate chargeDate) {
        int needToProcess = countNeedToProcessChargeRequests(chargeDate);

        List<ChargeRequest> chargeRequests = new ArrayList<>();

        Update update = new Update();
        update.set("status", Status.PROCESSING);
        update.currentDate("updated");

        for (int count = 0; count <= needToProcess; count++) {
            ChargeRequest chargeRequest = mongoOperations.findAndModify(
                    getNeedToProcessChargeRequestsQuery(chargeDate),
                    update,
                    FindAndModifyOptions.options().returnNew(true),
                    ChargeRequest.class
            );
            if (chargeRequest != null) {
                chargeRequests.add(chargeRequest);
            } else {
                break;
            }
        }
        return chargeRequests;
    }


    @Override
    public int countChargeRequestsWithErrors(LocalDate chargeDate) {
        return (int) mongoOperations.count(getChargeRequestsWithErrorsQuery(chargeDate), ChargeRequest.class);
    }

    @Override
    public List<ChargeRequest> pullChargeRequestsWithErrors(LocalDate chargeDate) {
        int needToProcess = countChargeRequestsWithErrors(chargeDate);

        List<ChargeRequest> chargeRequests = new ArrayList<>();

        Update update = new Update();
        update.set("status", Status.PROCESSING);
        update.currentDate("updated");

        for (int count = 0; count <= needToProcess; count++) {
            ChargeRequest chargeRequest = mongoOperations.findAndModify(
                    getChargeRequestsWithErrorsQuery(chargeDate),
                    update,
                    FindAndModifyOptions.options().returnNew(true),
                    ChargeRequest.class
            );
            if (chargeRequest != null) {
                chargeRequests.add(chargeRequest);
            } else {
                break;
            }
        }
        return chargeRequests;
    }

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("ChargeRequest с id: " + id + " не найден");
        }
    }

    private ChargeRequest setCreated(ChargeRequest chargeRequest) {
        chargeRequest.setCreated(LocalDateTime.now());

        return chargeRequest;
    }

    private ChargeRequest setUpdated(ChargeRequest chargeRequest) {
        chargeRequest.setUpdated(LocalDateTime.now());

        return chargeRequest;
    }

    private Query getChargeRequestsWithErrorsQuery(LocalDate chargeDate) {
        LocalDateTime nowMinus30Minutes = LocalDateTime.now().minusMinutes(30);

        Query query = new Query();
        query.addCriteria(
                new Criteria()
                        .orOperator(
                                Criteria
                                        .where("status").is(Status.ERROR)
                                        .and("chargeDate").is(chargeDate),
                                Criteria
                                        .where("status").is(Status.PROCESSING)
                                        .and("chargeDate").is(chargeDate)
                                        .and("updated").lt(nowMinus30Minutes)
                        ));

        return query;
    }

    private Query getNeedToProcessChargeRequestsQuery(LocalDate chargeDate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(Status.NEW).and("chargeDate").is(chargeDate));

        return query;
    }
}
