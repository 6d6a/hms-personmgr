package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.integration.annotation.Publisher;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.event.accountHistory.AccountHistoryEvent;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.repository.AccountPromotionRepository;

import static ru.majordomo.hms.personmgr.common.Constants.HISTORY_MESSAGE_KEY;
import static ru.majordomo.hms.personmgr.common.Constants.OPERATOR_KEY;

@Component
public class AccountPromotionManagerImpl implements AccountPromotionManager {
    private final AccountPromotionRepository repository;
    private final MongoOperations mongoOperations;
    private final ApplicationEventPublisher publisher;

    @Autowired
    public AccountPromotionManagerImpl(
            AccountPromotionRepository repository,
            MongoOperations mongoOperations,
            ApplicationEventPublisher publisher
    ) {
        this.repository = repository;
        this.mongoOperations = mongoOperations;
        this.publisher = publisher;
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
    public void delete(AccountPromotion accountPromotion) {
        repository.delete(accountPromotion);
    }

    @Override
    public void delete(Iterable<AccountPromotion> accountPromotions) {
        repository.delete(accountPromotions);
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }

    @Override
    public AccountPromotion save(AccountPromotion accountPromotion) {
        return repository.save(accountPromotion);
    }

    @Override
    public List<AccountPromotion> save(Iterable<AccountPromotion> accountPromotions) {
        return repository.save(accountPromotions);
    }

    @Override
    public AccountPromotion insert(AccountPromotion accountPromotion) {
        return repository.insert(accountPromotion);
    }

    @Override
    public List<AccountPromotion> insert(Iterable<AccountPromotion> accountPromotions) {
        return repository.insert(accountPromotions);
    }

    @Override
    public AccountPromotion findOne(String id) {
        checkById(id);
        return repository.findOne(id);
    }

    @Override
    public List<AccountPromotion> findAll() {
        return repository.findAll();
    }

    @Override
    public List<AccountPromotion> findByPersonalAccountId(String personalAccountId) {
        return repository.findByPersonalAccountId(personalAccountId);
    }

    @Override
    public List<AccountPromotion> findByPersonalAccountIdAndPromotionId(String personalAccountId, String promotionId) {
        return repository.findByPersonalAccountIdAndPromotionId(personalAccountId, promotionId);
    }

    @Override
    public Long countByPersonalAccountIdAndPromotionId(String personalAccountId, String promotionId) {
        return repository.countByPersonalAccountIdAndPromotionId(personalAccountId, promotionId);
    }

    @Override
    public void activateAccountPromotionByIdAndActionId(String id, String actionId) {
        setAccountPromotionStatusByIdAndActionId(id, actionId, true);
    }

    @Override
    public void deactivateAccountPromotionByIdAndActionId(String id, String actionId) {
        setAccountPromotionStatusByIdAndActionId(id, actionId, false);
    }

    @Override
    public void switchAccountPromotionById(String id) {
        Map<String, Boolean> map = repository.findOne(id).getActionsWithStatus();
        map.forEach((k,v) -> setAccountPromotionStatusByIdAndActionId(id, k, !v));
    }

    private void setAccountPromotionStatusByIdAndActionId(String id, String actionId, boolean status) {
        checkById(id);

        AccountPromotion accountPromotion = findOne(id);
        Map<String, Boolean> map = accountPromotion.getActionsWithStatus();
        if (map.get(actionId) != null) {
            Query query = new Query(new Criteria("_id").is(id));
            Update update = new Update().set("actionsWithStatus." + actionId, status);

            mongoOperations.updateFirst(query, update, AccountPromotion.class);

            Map<String, String> params = new HashMap<>();
            params.put(HISTORY_MESSAGE_KEY, "AccountPromotion Id: " + id + " Action Id: " + actionId + " помечен как " +
                    (status ? "активный" : "неактивный")
            );
            params.put(OPERATOR_KEY, "service");

            publisher.publishEvent(new AccountHistoryEvent(accountPromotion.getPersonalAccountId(), params));
        }
    }

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("AccountPromotion с id: " + id + " не найден");
        }
    }
}
