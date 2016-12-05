package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.springframework.beans.factory.annotation.Autowired;

import ru.majordomo.hms.personmgr.controller.rest.CommonRestController;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.PlanCheckerService;

public class CommonRestResourceController extends CommonRestController {

    protected BusinessActionBuilder businessActionBuilder;

    protected PlanCheckerService planCheckerService;

    @Autowired
    public void setPlanCheckerService(PlanCheckerService planCheckerService) {
        this.planCheckerService = planCheckerService;
    }

    @Autowired
    public void setBusinessActionBuilder(BusinessActionBuilder businessActionBuilder) {
        this.businessActionBuilder = businessActionBuilder;
    }
}
