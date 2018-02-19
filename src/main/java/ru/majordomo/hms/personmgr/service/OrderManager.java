package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;

import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.exception.IncorrectStateException;
import ru.majordomo.hms.personmgr.model.order.AccountOrder;
import ru.majordomo.hms.personmgr.repository.AccountOrderRepository;

import java.time.LocalDateTime;

public abstract class OrderManager<T extends AccountOrder> {
    private AccountOrderRepository<T> accountOrderRepository;

    protected void updateState(T accountOrder, OrderState newState, String operator) {
        accountOrder.setState(newState);
        accountOrder.setOperator(operator);
        accountOrder.setUpdated(LocalDateTime.now());
    }

    @Autowired
    public void setAccountOrderRepository(AccountOrderRepository<T> accountOrderRepository) {
        this.accountOrderRepository = accountOrderRepository;
    }

    protected void save(T accountOrder) {
        accountOrderRepository.save(accountOrder);
    }

    protected void onCreate(T accountOrder) {}

    protected void onProcess(T accountOrder) {}

    protected void onDecline(T accountOrder) {}

    protected void onFinish(T accountOrder) {}

    public void create(T accountOrder, String operator) {
        accountOrder.setCreated(LocalDateTime.now());
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
                throw new IncorrectStateException("Состояние не может быть изменено на " + OrderState.IN_PROGRESS.name());
        }
    }
}
