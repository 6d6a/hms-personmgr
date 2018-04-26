package ru.majordomo.hms.personmgr.controller.rest;

import com.google.common.collect.ImmutableMap;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.config.AppConfigTest;
import ru.majordomo.hms.personmgr.config.MongoConfigTest;
import ru.majordomo.hms.personmgr.manager.AccountHistoryManager;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.manager.impl.PersonalAccountManagerImpl;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanPropertyLimit;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.AccountServiceRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.service.BusinessHelper;
import ru.majordomo.hms.personmgr.service.PlanBuilder;
import ru.majordomo.hms.personmgr.service.PlanCheckerService;
import ru.majordomo.hms.personmgr.service.ResourceChecker;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.any;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.majordomo.hms.personmgr.common.DBType.MYSQL;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = PlanRestController.class, secure = false)
@ActiveProfiles("test")
public class PlanRestControllerTest {
    private List<Plan> batchOfPlans = new ArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name="paymentServiceRepository")
    private PaymentServiceRepository paymentServiceRepository;

    @MockBean(name="accountHistoryManager")
    private AccountHistoryManager accountHistoryManager;

    @MockBean(name="accountServiceRepository")
    private AccountServiceRepository accountServiceRepository;

    @MockBean(name="planRepository")
    private PlanRepository planRepository;

    @MockBean(name="planBuilder")
    private PlanBuilder planBuilder;

    @MockBean(name="accountManager")
    private PersonalAccountManager accountManager;

    @MockBean(name="planCheckerService")
    private PlanCheckerService planCheckerService;

    @MockBean(name="businessHelper")
    private BusinessHelper businessHelper;

    @MockBean(name = "resourceChecker")
    private ResourceChecker resourceChecker;

    @MockBean
    private Tracer tracer;

    @Before
    public void setUp() throws Exception {
        generateBatchOfPlan();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void listAllForAccount() throws Exception {
        Mockito
                .when(this.planRepository.findByActive(true))
                .thenReturn(this.batchOfPlans.stream().filter(Plan::isActive).collect(Collectors.toList()));

        Mockito
                .when(this.planBuilder.build(any(Plan.class)))
                .then(returnsFirstArg());

        RequestBuilder requestBuilder = MockMvcRequestBuilders
                .get("/" + ObjectId.get().toString() + "/plans")
                .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andReturn();

        System.out.println("[listAll] result.getResponse().getContentAsString() "
                + result.getResponse().getContentAsString());
    }

    @Test
    public void listAll() throws Exception {
    }

    @Test
    public void get() throws Exception {
    }

    private void generateBatchOfPlan() {
        Plan plan = new Plan();
        for (int i = 0; i < 9; i++) {
            plan.setName("План " + i);
            plan.setInternalName("Внутреннее имя плана " + i);
            plan.setServiceId(ObjectId.get().toString());
            plan.setOldId(ObjectId.get().toString());
            plan.setAccountType(AccountType.VIRTUAL_HOSTING);
            plan.setActive(true);
            plan.setAbonementOnly(false);

            VirtualHostingPlanProperties planProperties = new VirtualHostingPlanProperties();
            planProperties.setSitesLimit(new PlanPropertyLimit(i + 1));
            planProperties.setWebCpuLimit(new PlanPropertyLimit(i + 90));
            planProperties.setDbCpuLimit(new PlanPropertyLimit(i + 100));
            planProperties.setQuotaKBLimit(new PlanPropertyLimit(i + 10));
            planProperties.setFtpLimit(new PlanPropertyLimit(i + 3));
            planProperties.setSshLimit(new PlanPropertyLimit(i + 1));
            planProperties.setDb(ImmutableMap.of(MYSQL, new PlanPropertyLimit(i + 1)));

            plan.setPlanProperties(planProperties);

            this.batchOfPlans.add(plan);
        }
    }
}