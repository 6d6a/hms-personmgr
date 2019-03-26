package ru.majordomo.hms.personmgr.manager.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.AccountPromotionManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeAction;
import ru.majordomo.hms.personmgr.model.promotion.AccountPromotion;
import ru.majordomo.hms.personmgr.model.service.PaymentService;
import ru.majordomo.hms.personmgr.repository.AccountPromotionRepository;

import static ru.majordomo.hms.personmgr.common.PromocodeActionType.SERVICE_DISCOUNT;

@Component
public class AccountPromotionManagerImpl implements AccountPromotionManager {
    private final AccountPromotionRepository repository;
    private final MongoOperations mongoOperations;
    private final AccountHistoryManager history;

    @Autowired
    public AccountPromotionManagerImpl(
            AccountPromotionRepository repository,
            MongoOperations mongoOperations,
            AccountHistoryManager history
    ) {
        this.repository = repository;
        this.mongoOperations = mongoOperations;
        this.history = history;
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
    public void delete(AccountPromotion accountPromotion) {
        repository.delete(accountPromotion);
    }

    @Override
    public void delete(Iterable<AccountPromotion> accountPromotions) {
        repository.deleteAll(accountPromotions);
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
        return repository.saveAll(accountPromotions);
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
        return repository.findById(id).orElse(null);
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
    public List<AccountPromotion> findByPersonalAccountIdAndActionIdInAndActive(String personalAccountId, List<String> actionIds, boolean active) {
        return repository.findByPersonalAccountIdAndActionIdInAndActive(personalAccountId, actionIds, active);
    }

    @Override
    public Long countByPersonalAccountIdAndPromotionIdAndActionId(String personalAccountId, String promotionId, String actionId) {
        return repository.countByPersonalAccountIdAndPromotionIdAndActionId(personalAccountId, promotionId, actionId);
    }

    @Override
    public void activateAccountPromotionById(String id) {
        setAccountPromotionStatusByIdAndActionId(id, true);
    }

    @Override
    public void deactivateAccountPromotionById(String id) {
        setAccountPromotionStatusByIdAndActionId(id, false);
    }

    public AccountPromotion getServiceDiscountPromotion(PersonalAccount account, PaymentService service) {
        List<AccountPromotion> promotions = findByPersonalAccountId(account.getId());

        for (AccountPromotion accountPromotion : promotions) {
            if (!accountPromotion.getActive()) continue;

            PromocodeAction action = mongoOperations.findById(accountPromotion.getActionId(), PromocodeAction.class);

            accountPromotion.setAction(action);

            if (action == null || !SERVICE_DISCOUNT.equals(action.getActionType())) continue;

            Map<String, Object> properties = action.getProperties();

            if (properties.get("amount") == null
                    || properties.get("type") == null
                    || properties.get("serviceIds") == null
            ) {
                continue;
            }

            Object o = properties.get("serviceIds");

            if (!(o instanceof List)) continue;

            List<String> serviceIds = (List<String>) o;

            if (serviceIds.contains(service.getId())) {
                return accountPromotion;
            }
        }
        return null;
    }

    private void setAccountPromotionStatusByIdAndActionId(String id, boolean status) {
        checkById(id);

        AccountPromotion accountPromotion = findOne(id);
        Query query = new Query(new Criteria("_id").is(id));
        Update update = new Update().set("active", status);

        mongoOperations.updateFirst(query, update, AccountPromotion.class);

        history.save(
                accountPromotion.getPersonalAccountId(),
                "AccountPromotion Id: " + id + " помечен как " + (status ? "активный" : "неактивный"),
                "service"
        );
    }

    private void checkById(String id) {
        if (!exists(id)) {
            throw new ResourceNotFoundException("AccountPromotion с id: " + id + " не найден");
        }
    }
}
