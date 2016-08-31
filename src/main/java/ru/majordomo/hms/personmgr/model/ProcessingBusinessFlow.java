package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.Transient;
import org.springframework.data.redis.core.RedisHash;

import java.util.ArrayList;
import java.util.List;

/**
 * ProcessingBusinessFlow
 */
@RedisHash("processingBusinessFlow")
public class ProcessingBusinessFlow extends BusinessFlow {
    @Transient
    private List<ProcessingBusinessAction> businessActions = new ArrayList<>();

//    @Override
//    public List<ProcessingBusinessAction> getBusinessActions() {
//        return businessActions;
//    }
//
//    @Override
//    public void setBusinessActions() {
//        setBusinessActions();
//    }
//
//    @Override
//    public void setBusinessActions(List<ProcessingBusinessAction> actions) {
//        this.businessActions = actions;
//    }
//
//    public void addBusinessAction(ProcessingBusinessAction action) {
//        this.businessActions.add(action);
//    }
//
//    public void deleteBusinessAction(ProcessingBusinessAction action) {
//        this.businessActions.remove(action);
//    }
}
