package ru.majordomo.hms.personmgr.manager.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
public class PlanManagerImpl implements PlanManager {
    private final PlanRepository repository;
    private final MongoOperations mongoOperations;

    @Autowired
    public PlanManagerImpl(
            PlanRepository repository,
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
    public void delete(String id) {
        repository.deleteById(id);
    }

    @Override
    public <S extends Plan> S save(S plan) {
        return repository.save(plan);
    }

    @Override
    public void addAbonementId(String id, String abonementId) {
        checkById(id);

        Abonement abonement = mongoOperations.findOne(new Query(where("_id").is(abonementId)), Abonement.class);
        if (abonement == null) {
            throw new ParameterValidationException("Not found abonement with id " + abonementId + " in PlanManagerImpl.addAbonementId");
        }

        Plan plan = findOne(id);

        plan.getAbonementIds().add(abonementId);
        plan.getAbonements().add(abonement);

        save(plan);
    }

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("Тарифный план с id: " + id + " не найден");
        }
    }

    @Override
    public Plan findOne(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<Plan> findAll() {
        return repository.findAll();
    }

    @Override
    public List<Plan> findByActive(boolean active){
        return repository.findByActive(active);
    }

    @Override
    public Plan findByName(String name){
        return repository.findByName(name);
    }

    @Override
    public List<Plan> findByAccountType(AccountType accountType){
        return repository.findByAccountType(accountType);
    }

    @Override
    public Plan findByServiceId(String serviceId){
        return repository.findByServiceId(serviceId);
    }

    @Override
    public Plan findByOldId(String oldId){
        return repository.findByOldId(oldId);
    }

    @Override
    public <S extends Plan> List<S> save(Iterable<S> entites){
        return repository.saveAll(entites);
    }

    @Override
    public void delete(Iterable<? extends Plan> entities){
        repository.deleteAll(entities);
    }

    @Override
    public void delete(Plan entity){
        repository.delete(entity);
    }

    @Override
    public void deleteAll(){
        repository.deleteAll();
    }

    @Override
    public Page<Plan> findAll(Predicate predicate, Pageable pageable) {
        if (predicate == null) predicate = new BooleanBuilder();

        return repository.findAll(predicate, pageable);
    }
}
