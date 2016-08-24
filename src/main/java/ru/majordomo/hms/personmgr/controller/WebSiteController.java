package ru.majordomo.hms.personmgr.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.FlowType;
import ru.majordomo.hms.personmgr.common.RestResponse;
import ru.majordomo.hms.personmgr.model.BusinessFlow;
import ru.majordomo.hms.personmgr.service.BusinessFlowBuilder;

/**
 * WebSiteController
 */
@RestController
@RequestMapping("/website")
public class WebSiteController {
    private final static Logger logger = LoggerFactory.getLogger(WebSiteController.class);

    @Autowired
    private BusinessFlowBuilder businessFlowBuilder;

    @RequestMapping(value = "create", method = RequestMethod.POST)
    public RestResponse createAccount(
            @RequestBody String requestBody,
            HttpServletResponse response
    ) {
        Map<String, String> params = new HashMap<>();
        BusinessFlow businessFlow = businessFlowBuilder.build(FlowType.WEB_SITE_CREATE, params);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        return new RestResponse("11111", "queued");
    }
}
