package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.ResourceCounter;
import ru.majordomo.hms.personmgr.manager.PersonalAccountManager;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.dto.AbonementCounter;
import ru.majordomo.hms.personmgr.dto.PlanCounter;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.repository.PaymentServiceRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static ru.majordomo.hms.personmgr.common.AccountStatType.VIRTUAL_HOSTING_REGISTER_DOMAIN;

@Service
public class StatServiceHelper {

    private final MongoOperations mongoOperations;
    private final AbonementRepository abonementRepository;
    private final PlanRepository planRepository;
    private final PersonalAccountManager accountManager;
    private final PaymentServiceRepository paymentServiceRepository;

    @Autowired
    public StatServiceHelper(
            MongoOperations mongoOperations,
            AbonementRepository abonementRepository,
            PlanRepository planRepository,
            PersonalAccountManager accountManager,
            PaymentServiceRepository paymentServiceRepository
    ) {
        this.mongoOperations = mongoOperations;
        this.abonementRepository = abonementRepository;
        this.planRepository = planRepository;
        this.accountManager = accountManager;
        this.paymentServiceRepository = paymentServiceRepository;
    }

    public List<PlanCounter> getAllPlanCounters() {
        return this.getPlanCountersFilterByAbonementOnlyPlan(true);
    }

    public List<PlanCounter> getPlanCountersWithoutAbonement() {
        return this.getPlanCountersFilterByAbonementOnlyPlan(false);
    }

    public List<PlanCounter> getPlanCountersFilterByAbonementOnlyPlan(Boolean withoutFilter) {
        MatchOperation match = match(Criteria.where("active").is(true));

        ProjectionOperation project = project("planId");

        GroupOperation group = group("planId").count().as("count")
                .first("planId").as("planId");

        SortOperation sort = sort(new Sort(Sort.Direction.DESC, "count"));

        Aggregation aggregation = newAggregation(
                match,
                project,
                group,
                sort
        );

        List<PlanCounter> result = new ArrayList<>();
        List<PlanCounter> all = mongoOperations.aggregate(
                aggregation, "personalAccount", PlanCounter.class).getMappedResults();

        List<Plan> planList = planRepository.findAll();
        Map<String, Plan> planMap = planList.stream().collect(Collectors.toMap(Plan::getId, x->x));

        all.forEach(counter -> {
                    Plan plan = planMap.get(counter.getPlanId());
                    if (withoutFilter || !plan.isAbonementOnly()) {
                        counter.unSetId();
                        counter.setName(plan.getName());
                        counter.setActive(plan.isActive());
                        result.add(counter);
                    }
                }
        );

        return result;
    }



    //Выполняется дольше 17 секунд
    public List<AbonementCounter> getAbonementCounters() {

        UnwindOperation unwind = unwind("abonementIds");
        ProjectionOperation project = project("abonementIds", "name", "active");

        LookupOperation lookup = lookup(
                "accountAbonement",
                "abonementIds",
                "abonementId",
                "accountAbonements");

        ProjectionOperation countProject = project("name", "active")
                .and("accountAbonements").size().as("count")
                .and("abonementIds").as("abonementId")
                ;

        SortOperation sort = sort(new Sort(Sort.Direction.DESC, "count"));

        Aggregation aggregation = newAggregation(
                unwind,
                project,
                lookup,
                countProject,
                sort
        );

        AggregationResults<AbonementCounter> abonementCounters = mongoOperations.aggregate(
                aggregation,
                "plan",
                AbonementCounter.class
        );

        List<AbonementCounter> abonementCountersByPlanAndAbonement = abonementCounters.getMappedResults();

        List<Abonement> abonements = abonementRepository.findAll();
        Map<String, Abonement> abonementMap = abonements.stream().collect(Collectors.toMap(Abonement::getId, x->x));

        for (AbonementCounter counter: abonementCountersByPlanAndAbonement
                ) {

            counter.setPlanId(counter.getId());
            counter.unSetId();

            Abonement abonement = abonementMap.get(counter.getAbonementId());

            if (abonement != null) {
                counter.setInternal(abonement.isInternal());
                counter.setPeriod(abonement.getPeriod());
                counter.setName(abonement.getName());
            }
        }

        return abonementCountersByPlanAndAbonement;
    }

    public List<PlanCounter> getDailyPlanCounters() {
        //все аккаунты с дневными списаниями и абонементами
        List<PlanCounter> all = this.getPlanCountersWithoutAbonement();

        //Только абонементы
        List<AbonementCounter> abonements = this.getAbonementCounters();

        //Вычитаем абонементы из общего числа
        all.forEach(counter ->
                abonements.forEach(abonementCounter -> {
                    if (abonementCounter.getPlanId().equals(counter.getPlanId())) {
                        counter.setCount(counter.getCount() - abonementCounter.getCount());
                    }
                })
        );

        return all;
    }

    public List<ResourceCounter> getActiveAccountServiceCounters() {
        List<String> accountIds = accountManager.findAccountIdsByActive(true);

        // Получаем количество включенных услуг без количества одновременно подключенных на одном аккаунте

        /*
        db.getCollection('accountService').aggregate(
        {$lookup:{from:"plan", localField:"serviceId", foreignField: "serviceId", as: "plan"}}
        ,{$project:{"serviceId":"$serviceId","plan":{$size:"$plan"}}}
        ,{$match:{"plan":0}}
        ,{$addFields:{count:1, serviceId:"$serviceId"}}
        ,{$group:{_id:"$serviceId", "count":{$sum:"$count"}}}
        )
        */

        MatchOperation match = match(
                Criteria.where("enabled")
                        .is(true)
                        .and("quantity").gte(1)
                        .and("personalAccountId").in(accountIds)
        );

        LookupOperation lookup = lookup(
                "plan",
                "serviceId",
                "serviceId",
                "plan");

        ProjectionOperation project = project("serviceId")
                .and("plan").size().as("plan");

        MatchOperation planFilter = match(Criteria.where("plan").is(0));

        GroupOperation group = group(
                "serviceId").count().as("count")
                .first("serviceId").as("resourceId")
                .first("plan").as("plan");

        SortOperation sort = sort(Sort.Direction.DESC,"count");

        Aggregation aggregation = newAggregation(
                match,
                lookup,
                project,
                planFilter,
                group,
                sort
        );

        List<ResourceCounter> accountServiceCounters = mongoOperations.aggregate(
                aggregation, "accountService", ResourceCounter.class
        ).getMappedResults();

        accountServiceCounters.forEach(element -> element.setName(paymentServiceRepository.findOne(element.getResourceId()).getName()));

        return accountServiceCounters;
    }

    public List<ResourceCounter> getQuantityForActiveAccountService() {
        List<String> accountIds = accountManager.findAccountIdsByActive(true);

        MatchOperation match = match(
                Criteria.where("enabled")
                        .is(true)
                        .and("quantity").gte(1)
                        .and("personalAccountId").in(accountIds)
        );

        LookupOperation lookup = lookup(
                "plan",
                "serviceId",
                "serviceId",
                "plan");

        ProjectionOperation project = project("serviceId", "quantity")
                .and("plan").size().as("plan");

        MatchOperation planFilter = match(Criteria.where("plan").is(0));

        GroupOperation group = group(
                "serviceId")
                .first("serviceId").as("resourceId")
                .first("plan").as("plan")
                .sum("quantity").as("count");

        SortOperation sort = sort(Sort.Direction.DESC,"count");

        Aggregation aggregation = newAggregation(
                match,
                lookup,
                project,
                planFilter,
                group,
                sort
        );

        List<ResourceCounter> accountServiceCounters = mongoOperations.aggregate(
                aggregation, "accountService", ResourceCounter.class
        ).getMappedResults();

        accountServiceCounters.forEach(element -> element.setName(paymentServiceRepository.findOne(element.getResourceId()).getName()));

        return accountServiceCounters;
    }

    public List<ResourceCounter> getDomainRegistrationCounters(LocalDate date) {

        LocalDateTime startDateTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endDateTime = LocalDateTime.of(date, LocalTime.MAX);

        MatchOperation match = match(
                Criteria.where("created")
                        .gte(Date.from(startDateTime.toInstant(ZoneOffset.ofHours(3))))
                        .lte(Date.from(endDateTime.toInstant(ZoneOffset.ofHours(3))))
                        .and("type").is(VIRTUAL_HOSTING_REGISTER_DOMAIN.name())
        );

        ProjectionOperation project = project()
                .and("data.domainName").as("name");

        Aggregation aggregation = newAggregation(
                match,
                project
        );

        List<ResourceCounter> accountServiceCounters = mongoOperations.aggregate(
                aggregation, "accountStat", ResourceCounter.class
        ).getMappedResults();

        Map<String, ResourceCounter> tldMap = new HashMap<>();
        List<ResourceCounter> result = new ArrayList<>();
        for(ResourceCounter element: accountServiceCounters) {
            String[] splitDomain = element.getName().split("\\.", 2);
            String tld = splitDomain[1];
            element.setResourceId(tld);
            if (!tldMap.containsKey(tld)) {
                ResourceCounter counter = new ResourceCounter();
                counter.setResourceId(tld);
                counter.setDateTime(LocalDateTime.of(date, LocalTime.MIN));
                counter.setCount(1);
                tldMap.put(tld, counter);
                result.add(counter);
            } else {
                tldMap.get(tld).countPlusOne();
            }
        }
        return result;
    }
}
