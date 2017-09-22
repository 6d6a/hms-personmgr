package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.counter.AbonementCounter;
import ru.majordomo.hms.personmgr.model.counter.PlanCounter;
import ru.majordomo.hms.personmgr.model.plan.Plan;
import ru.majordomo.hms.personmgr.repository.AbonementRepository;
import ru.majordomo.hms.personmgr.repository.PlanRepository;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@Service
public class StatServiceHelper {

    private final MongoOperations mongoOperations;
    private final AbonementRepository abonementRepository;
    private final PlanRepository planRepository;

    @Autowired
    public StatServiceHelper(
            MongoOperations mongoOperations,
            AbonementRepository abonementRepository,
            PlanRepository planRepository
    ) {
        this.mongoOperations = mongoOperations;
        this.abonementRepository = abonementRepository;
        this.planRepository = planRepository;
    }

    public List<PlanCounter> getAllPlanCounters() {
        return this.getPlanCountersFilterByAbonementOnlyPlan(true);
    }

    public List<PlanCounter> getPlanCountersWithoutAbonement() {
        return this.getPlanCountersFilterByAbonementOnlyPlan(false);
    }

    public List<PlanCounter> getPlanCountersFilterByAbonementOnlyPlan(Boolean withoutFilter) {
        //        db.getCollection('personalAccount').aggregate(
        //                {$match: {"active": true}},
        //        {$project: {planId: 1, "_id":0, count: {$add: [1]}}}
        //        ,{$group: {_id: "$planId", "sum":{$sum: "$count"}}}
        //        ,{$sort: {sum:-1}}
        //)

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

        all.forEach(counter -> {
                    Plan plan = planRepository.findOne(counter.getPlanId());
                    if (withoutFilter || !plan.isAbonementOnly()) {
                        counter.unSetId();
                        counter.setName(plan.getName());
                        result.add(counter);
                    }
                }
        );

        return result;
    }



    //Выполняется дольше 17 секунд
    public List<AbonementCounter> getAbonementCounters() {

        /*db.getCollection('plan').aggregate({$unwind:"$abonementIds"}
        ,{$project:{abonementIds:1,name:1}}
        ,{$lookup:{from: "accountAbonement",
               localField: "abonementIds",
               foreignField: "abonementId",
               as: "accountAbonements"}}
        ,{$project:{abonementIds:1, name:1, "count": { $size: "$accountAbonements" }}}
//        ,{$group:{_id:"$_id", "count":{$sum:"$count"}}}
        ,{$sort: {"count":-1}}
        )*/

        UnwindOperation unwind = unwind("abonementIds");
        ProjectionOperation project = project("abonementIds", "name");

        LookupOperation lookup = lookup(
                "accountAbonement",
                "abonementIds",
                "abonementId",
                "accountAbonements");

        ProjectionOperation countProject = project("name")
                .and("accountAbonements").size().as("count")
                .and("abonementIds").as("abonementId")
//                .and("id").as("planId")
                ;

//  GroupOperation group = group("_id").sum("count").as("count");
        SortOperation sort = sort(new Sort(Sort.Direction.DESC, "count"));
//        ProjectionOperation finalProject = project().andExclude("_id");

//        ProjectionOperation finalProject = project("count", "name")
//                .and("abonementIds").as("abonementId");


        Aggregation aggregation = newAggregation(
                unwind,
                project,
                lookup,
                countProject,
//                group,
                sort/*,
                finalProject*/
        );

        AggregationResults<AbonementCounter> abonementCounters = mongoOperations.aggregate(
                aggregation,
                "plan",
                AbonementCounter.class
        );

        List<AbonementCounter> abonementCountersByPlanAndAbonement = abonementCounters.getMappedResults();

        for (AbonementCounter counter: abonementCountersByPlanAndAbonement
                ) {

            counter.setPlanId(counter.getId());
            counter.unSetId();

            Abonement abonement = abonementRepository.findOne(counter.getAbonementId());

            if (abonement != null) {
                counter.setInternal(abonement.isInternal());
                counter.setPeriod(abonement.getPeriod());
                counter.setName(abonement.getName());
            }
        }

        return abonementCountersByPlanAndAbonement;
    }

    /*public List<PlanCounter> getAllPlanCounters() {
        return this.getPlanCounters(
                "personalAccount",
                "planId",
                "active",
                true
        );
    }*/

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
}
