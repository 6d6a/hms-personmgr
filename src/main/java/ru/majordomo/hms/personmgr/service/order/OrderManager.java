package ru.majordomo.hms.personmgr.service.order;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;
import ru.majordomo.hms.personmgr.exception.ResourceNotFoundException;
import ru.majordomo.hms.personmgr.model.order.AccountOrder;
import ru.majordomo.hms.personmgr.repository.AccountOrderRepository;

import java.util.ArrayList;
import java.util.List;

public abstract class OrderManager<T extends AccountOrder> {
    protected AccountOrderRepository<T> repository;

    @Autowired
    public void setAccountOrderRepository(AccountOrderRepository<T> accountOrderRepository) {
        this.repository = accountOrderRepository;
    }

    protected void onCreate(T accountOrder) {}

    protected void onProcess(T accountOrder) {}

    protected void onDecline(T accountOrder) {}

    protected void onFinish(T accountOrder) {}

    protected void save(T accountOrder) {
        repository.save(accountOrder);
    }

    protected void updateState(T accountOrder, OrderState newState, String operator) {
        accountOrder.setState(newState);
        accountOrder.setOperator(operator);
    }

    public void create(T accountOrder, String operator) {
        updateState(accountOrder, OrderState.NEW, operator);
        onCreate(accountOrder);

        save(accountOrder);
    }

    public void decline(T accountOrder, String operator) {
        updateState(accountOrder, OrderState.DECLINED, operator);
        onDecline(accountOrder);

        save(accountOrder);
    }

    public void finish(T accountOrder, String operator) {
        updateState(accountOrder, OrderState.FINISHED, operator);
        onFinish(accountOrder);

        save(accountOrder);
    }

    public void process(T accountOrder, String operator) {
        updateState(accountOrder, OrderState.IN_PROGRESS, operator);
        onProcess(accountOrder);

        save(accountOrder);
    }

    public void changeState(T accountOrder, OrderState orderState, String operator) {
        switch (orderState) {
            case FINISHED:
                finish(accountOrder, operator);
                break;
            case DECLINED:
                decline(accountOrder, operator);
                break;
            case IN_PROGRESS:
                throw new ParameterValidationException("Состояние не может быть изменено на " + OrderState.IN_PROGRESS.name());
        }
    }

    public Page<T> findByPersonalAccountId(String accountId, Pageable pageable){
        return repository.findByPersonalAccountId(accountId, pageable);
    }

    public T findOneByIdAndPersonalAccountId(String id, String personalAccountId){
        return repository.findOneByIdAndPersonalAccountId(id, personalAccountId);
    }

    public boolean exists(Predicate predicate){
        return repository.exists(predicate);
    }

    public Page<T> findAll(Predicate predicate, Pageable pageable){
        if (predicate == null) predicate = new BooleanBuilder();
        return repository.findAll(predicate, pageable);
    }

    public List<T> findAll(Predicate predicate){
        if (predicate == null) predicate = new BooleanBuilder();
        List<T> result = new ArrayList<>();
        repository.findAll(predicate).iterator().forEachRemaining(result::add);
        return result;
    }

    public Page<T> findAll(Pageable pageable){
        return repository.findAll(pageable);
    }

    public T findOne(String id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Не найден заказ с id " + id));
    }
}
