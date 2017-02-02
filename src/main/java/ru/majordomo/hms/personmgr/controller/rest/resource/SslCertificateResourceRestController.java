package ru.majordomo.hms.personmgr.controller.rest.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.ProcessingBusinessAction;

@RestController
@RequestMapping("/{accountId}/ssl-certificate")
public class SslCertificateResourceRestController extends CommonResourceRestController {
    private final static Logger logger = LoggerFactory.getLogger(SslCertificateResourceRestController.class);

    @RequestMapping(value = "", method = RequestMethod.POST)
    public SimpleServiceMessage create(
            @RequestBody SimpleServiceMessage message,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.debug(message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.SSL_CERTIFICATE_CREATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{sslcertificateId}", method = RequestMethod.PATCH)
    public SimpleServiceMessage update(
            @PathVariable String sslcertificateId,
            @RequestBody SimpleServiceMessage message, HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        message.setAccountId(accountId);

        logger.debug("Updating sslcertificate with id " + sslcertificateId + " " + message.toString());

        message.getParams().put("id", sslcertificateId);

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.SSL_CERTIFICATE_UPDATE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }

    @RequestMapping(value = "/{sslcertificateId}", method = RequestMethod.DELETE)
    public SimpleServiceMessage delete(
            @PathVariable String sslcertificateId,
            HttpServletResponse response,
            @PathVariable(value = "accountId", required = false) String accountId) {
        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setAccountId(accountId);
        message.addParam("sslcertificateId", sslcertificateId);
        message.setAccountId(accountId);

        logger.debug("Deleting sslcertificate with id " + sslcertificateId + " " + message.toString());

        ProcessingBusinessAction businessAction = businessActionBuilder.build(BusinessActionType.SSL_CERTIFICATE_DELETE_RC, message);

        response.setStatus(HttpServletResponse.SC_ACCEPTED);

        return this.createSuccessResponse(businessAction);
    }
}
