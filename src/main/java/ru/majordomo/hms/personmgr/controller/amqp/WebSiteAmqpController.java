package ru.majordomo.hms.personmgr.controller.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.model.PersonalAccount;
import ru.majordomo.hms.personmgr.repository.PersonalAccountRepository;
import ru.majordomo.hms.personmgr.service.AccountHelper;
import ru.majordomo.hms.personmgr.service.BusinessActionBuilder;
import ru.majordomo.hms.personmgr.service.BusinessFlowDirector;
import ru.majordomo.hms.personmgr.service.RcUserFeignClient;
import ru.majordomo.hms.rc.user.resources.WebSite;

@EnableRabbit
@Service
public class WebSiteAmqpController extends CommonAmqpController {

    private final static Logger logger = LoggerFactory.getLogger(WebSiteAmqpController.class);

    private final BusinessActionBuilder businessActionBuilder;
    private final BusinessFlowDirector businessFlowDirector;
    private final AccountHelper accountHelper;
    private final PersonalAccountRepository accountRepository;
    private final RcUserFeignClient rcUserFeignClient;

    @Autowired
    public WebSiteAmqpController(
            BusinessActionBuilder businessActionBuilder,
            BusinessFlowDirector businessFlowDirector,
            AccountHelper accountHelper,
            PersonalAccountRepository accountRepository,
            RcUserFeignClient rcUserFeignClient
    ) {
        this.businessActionBuilder = businessActionBuilder;
        this.businessFlowDirector = businessFlowDirector;
        this.accountHelper = accountHelper;
        this.accountRepository = accountRepository;
        this.rcUserFeignClient = rcUserFeignClient;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.website.create",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "website.create",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void create(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received create message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);

        if (state == State.PROCESSED) {
            PersonalAccount account = accountRepository.findOne(message.getAccountId());

            SimpleServiceMessage mailMessage = new SimpleServiceMessage();
            mailMessage.setAccountId(account.getId());

            String webSiteId = getResourceIdByObjRef(message.getObjRef());

            WebSite webSite = null;

            try {
                webSite = rcUserFeignClient.getWebSite(account.getId(), webSiteId);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (webSite == null) {
                logger.debug("WebSite with id " + webSiteId + " not found");

                return;
            }

            String webSiteName = webSite.getName();

            String emails = accountHelper.getEmail(account);

            mailMessage.setParams(new HashMap<>());
            mailMessage.addParam("email", emails);
            mailMessage.addParam("api_name", "MajordomoVHWebSiteCreated");
            mailMessage.addParam("priority", 10);

            HashMap<String, String> parameters = new HashMap<>();
            parameters.put("client_id", message.getAccountId());
            parameters.put("website_name", webSiteName);

            mailMessage.addParam("parametrs", parameters);

            businessActionBuilder.build(BusinessActionType.WEB_SITE_CREATE_MM, mailMessage);
        }
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.website.update",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "website.update",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void update(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received update message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "pm.website.delete",
                            durable = "true",
                            autoDelete = "false"
                    ),
                    exchange = @Exchange(
                            value = "website.delete",
                            type = ExchangeTypes.TOPIC
                    ),
                    key = "pm"
            )
    )
    public void delete(@Payload SimpleServiceMessage message, @Headers Map<String, String> headers) {
        String provider = headers.get("provider");
        logger.debug("Received delete message from " + provider + ": " + message.toString());

        State state = businessFlowDirector.processMessage(message);
    }
}
