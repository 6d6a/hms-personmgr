package ru.majordomo.hms.personmgr.service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.model.BusinessFlow;

/**
 * BusinessFlowBuilder
 */
public class BusinessFlowBuilder {
    public BusinessFlow build(FlowType flowType, Map<String, String> params) {
        return new BusinessFlow();
    }
}
