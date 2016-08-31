package ru.majordomo.hms.personmgr.service;

import org.springframework.stereotype.Service;

import java.util.Map;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.model.BusinessFlow;

/**
 * BusinessFlowDirector
 */
@Service
public class BusinessFlowDirector {
    public BusinessFlow build(FlowType flowType, Map<String, String> params) {
        return new BusinessFlow();
    }

    public BusinessFlow build(String id) {
        return new BusinessFlow();
    }
}
