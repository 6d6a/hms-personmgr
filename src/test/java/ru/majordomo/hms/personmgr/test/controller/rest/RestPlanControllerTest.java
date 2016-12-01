package ru.majordomo.hms.personmgr.test.controller.rest;

import com.google.common.collect.ImmutableMap;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import ru.majordomo.hms.personmgr.common.AccountType;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.model.plan.PlanPropertyLimit;
import ru.majordomo.hms.personmgr.model.plan.VirtualHostingPlanProperties;
import ru.majordomo.hms.personmgr.repository.PlanRepository;
import ru.majordomo.hms.personmgr.test.config.ConfigRestPlanController;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.majordomo.hms.personmgr.common.DBType.MYSQL;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ConfigRestPlanController.class, webEnvironment = RANDOM_PORT)
public class RestPlanControllerTest {

    private List<Plan> batchOfPlans = new ArrayList<>();
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext ctx;

    @Autowired
    private PlanRepository planRepository;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).build();
        generateBatchOfPlan();
        planRepository.save(batchOfPlans);
    }

    public void generateBatchOfPlan() {
        Plan plan = new Plan();
        for (int i = 0; i < 9; i++) {
            plan.setName("План " + i);
            plan.setInternalName("Внутреннее имя плана " + i);
            plan.setServiceId(ObjectId.get().toString());
            plan.setOldId(ObjectId.get().toString());
            plan.setAccountType(AccountType.VIRTUAL_HOSTING);
            plan.setActive(true);

            VirtualHostingPlanProperties planProperties = new VirtualHostingPlanProperties();
            planProperties.setSitesLimit(new PlanPropertyLimit(i + 1));
            planProperties.setWebCpuLimit(new PlanPropertyLimit(i + 90));
            planProperties.setDbCpuLimit(new PlanPropertyLimit(i + 100));
            planProperties.setQuotaKBLimit(new PlanPropertyLimit(i + 10));
            planProperties.setFtpLimit(new PlanPropertyLimit(i + 3));
            planProperties.setSshLimit(new PlanPropertyLimit(i + 1));
            planProperties.setPhpEnabled(true);
            planProperties.setDb(ImmutableMap.of(MYSQL, new PlanPropertyLimit(i + 1)));

            plan.setPlanProperties(planProperties);

            this.batchOfPlans.add(plan);
        }
    }

    @Test
    public void listAll() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/" + ObjectId.get().toString() + "/plans").accept(APPLICATION_JSON_UTF8);
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8));

    }

}
