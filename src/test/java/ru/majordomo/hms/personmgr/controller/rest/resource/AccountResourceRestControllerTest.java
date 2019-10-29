package ru.majordomo.hms.personmgr.controller.rest.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.common.message.destination.AmqpMessageDestination;
import ru.majordomo.hms.personmgr.feign.RcUserFeignClient;
import ru.majordomo.hms.personmgr.feign.SiFeignClient;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.AccountOwnerManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.PlanManager;
import ru.majordomo.hms.personmgr.model.account.PersonalAccount;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessAction;
import ru.majordomo.hms.personmgr.model.business.ProcessingBusinessOperation;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.*;
import ru.majordomo.hms.personmgr.service.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.time.LocalDateTime.now;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = AccountResourceRestController.class, secure = false)
@ActiveProfiles("test")
public class AccountResourceRestControllerTest {

    @MockBean
    private SequenceCounterService sequenceCounterService;
    @MockBean
    private ProcessingBusinessOperationRepository processingBusinessOperationRepository;
    @MockBean
    private PlanManager planManager;
    @MockBean
    private AccountServiceRepository accountServiceRepository;
    @MockBean
    private AccountOwnerManager accountOwnerManager;
    @MockBean
    private SiFeignClient siFeignClient;
    @MockBean
    private RcUserFeignClient rcUserFeignClient;
    @MockBean
    private AccountTransferService accountTransferService;
    @MockBean
    protected PersonalAccountManager accountManager;
    @MockBean
    protected ApplicationEventPublisher publisher;
    @MockBean
    protected PaymentServiceRepository paymentServiceRepository;
    @MockBean
    protected PlanCheckerService planCheckerService;
    @MockBean
    protected BusinessHelper businessHelper;
    @MockBean
    protected ResourceChecker resourceChecker;
    @MockBean
    protected AccountHistoryManager history;
    @MockBean
    protected ServicePlanRepository servicePlanRepository;
    @MockBean
    protected ServiceAbonementRepository serviceAbonementRepository;
    @MockBean
    protected PreorderService preorderService;

    @Autowired
    MockMvc mockMvc;

    @Test
    public void create() throws Exception {
        Plan plan = new Plan();
        plan.setId("planId1");
        plan.setServiceId("serviceId1");
        plan.setActive(true);
        plan.setAccountType(AccountType.VIRTUAL_HOSTING);
        plan.setOldId("9804");

        Mockito.when(planManager.findByOldId(eq("9804")))
                .thenReturn(plan);

        Mockito.when(sequenceCounterService.getNextSequence(eq("PersonalAccount")))
                .thenReturn(123);

//        Mockito.when(accountOwnerManager.insert(any(AccountOwner.class))).thenReturn(null);

        Mockito.when(accountManager.insert(argThat((PersonalAccount pa) -> {
            pa.setId(pa.getAccountId());
            return true;
        }))).thenReturn(null);

        Mockito.when(siFeignClient.createWebAccessAccount(any())).then(sgj -> {
            Map<String, Object> p = new HashMap<>();
            p.put("success", true);
            p.put("token", new HashMap<>());

            SimpleServiceMessage m = new SimpleServiceMessage();
            m.setParams(p);

            return m;
        });

        Mockito.when(businessHelper.buildOperation(any(), any())).thenAnswer(arg -> {
            ProcessingBusinessOperation op = new ProcessingBusinessOperation();
            op.setId(ObjectId.get().toString());
            return op;
        });

        Mockito.when(
                businessHelper.buildActionByOperation(eq(BusinessActionType.ACCOUNT_CREATE_FIN), any(), any())
        ).thenAnswer(arg -> new ProcessingBusinessAction(
                "id", "name", State.PROCESSING, 1, "operationId",
                BusinessActionType.ACCOUNT_CREATE_FIN, new AmqpMessageDestination(),
                new SimpleServiceMessage(), "accountId", now(), now(), new HashMap<>())
        );

        Map<String, Object> params = new HashMap<>();
        params.put("agreement", true);
        params.put("plan", "9804");
        params.put("name", "Mister Tester");
        params.put("type", "INDIVIDUAL");
        params.put("emailAddresses", Collections.singletonList("test@example.com"));

        SimpleServiceMessage message = new SimpleServiceMessage();
        message.setParams(params);

        String content = new ObjectMapper().writeValueAsString(message);

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .post("/register")
                .content(content)
                .contentType(APPLICATION_JSON_UTF8);

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().is(202))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("params.credentials.username").exists())
                .andExpect(jsonPath("params.credentials.password").exists())
                .andExpect(jsonPath("params.token").exists())
                .andExpect(jsonPath("operationIdentity").value("operationId"))
                .andReturn();


    }
}