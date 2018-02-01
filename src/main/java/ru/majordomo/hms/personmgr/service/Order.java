package ru.majordomo.hms.personmgr.service;

import org.apache.commons.lang.NotImplementedException;
import ru.majordomo.hms.personmgr.common.OrderState;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;
import ru.majordomo.hms.personmgr.model.order.AccountOrder;

import java.time.LocalDateTime;

public abstract class Order<T extends AccountOrder> {

    protected T accountOrder;

    private void setUpdatedNow() {
        accountOrder.setUpdated(LocalDateTime.now());
    }

    Order() {}

    public void setAccountOrder(T accountOrder) {
        this.accountOrder = accountOrder;
    }

    protected void updateState(OrderState newState, String operator) {
        accountOrder.setState(newState);
        accountOrder.setOperator(operator);
        setUpdatedNow();
    }

    protected void save() {
        throw new NotImplementedException();
    }

    protected void onCreate() {
        throw new NotImplementedException();
    }

    protected void onProcess() {
        throw new NotImplementedException();
    }

    protected void onDecline() {
        throw new NotImplementedException();
    }

    protected void onFinish() {
        throw new NotImplementedException();
    }

    public void create(String operator) {

        accountOrder.setCreated(LocalDateTime.now());
        this.updateState(OrderState.NEW, operator);
        this.onCreate();

        this.save();
    }

    public void decline(String operator) {

        this.updateState(OrderState.DECLINED, operator);
        this.onDecline();

        this.save();
    }

    public void finish(String operator) {

        this.updateState(OrderState.FINISHED, operator);
        this.onFinish();

        this.save();
    }

    public void process(String operator) {

        this.updateState(OrderState.IN_PROGRESS, operator);
        this.onProcess();

        this.save();
    }

//    private Boolean isStateChangeAllowed(OrderState initialState, OrderState finalState) {
//
//        if (initialState == finalState) {
//            return false;
//        }
//
//        switch (initialState) {
//            case NEW:
//                return true;
//            case IN_PROGRESS:
//                return finalState != OrderState.NEW;
//            case DECLINED:
//            case FINISHED:
//                return false;
//            default:
//                return true; //initialState == null
//        }
//    }

}
