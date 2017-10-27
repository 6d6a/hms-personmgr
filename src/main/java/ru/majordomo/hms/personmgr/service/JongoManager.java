package ru.majordomo.hms.personmgr.service;

import com.mongodb.MongoClient;
import org.jongo.Aggregate;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.ClassWithListProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class JongoManager {
    private Jongo jongo;

    public JongoManager(
            @Qualifier("jongoMongoClient") MongoClient mongoClient,
            @Value("${spring.data.mongodb.database}") String springDataMongodbDatabase
    ){
        this.jongo = new Jongo(mongoClient.getDB(springDataMongodbDatabase));
    }

    public List<String> getAccountIdsWithAbonement() {

        MongoCollection balanceCollection = this.jongo.getCollection("accountAbonement");

        Aggregate.ResultsIterator<ClassWithListProperty> resultsIterator = balanceCollection
                .aggregate("{$match:{}}")
                .and("{$group:{_id:\"class\",ids:{$addToSet:\"$personalAccountId\"}}}")
                .as(ClassWithListProperty.class);


        List<String> ids = new ArrayList<>();

        while (resultsIterator.hasNext()) {
            ids = resultsIterator.next().getIds();
        }
        return ids;
    }

    public List<String> getAccountIdWithPlanService(){
        MongoCollection balanceCollection = this.jongo.getCollection("accountService");

        Aggregate aggregate = balanceCollection
                .aggregate("{$match:{enabled:true}}")
                .and("{$lookup:{from:\"plan\",localField:\"serviceId\",foreignField:\"serviceId\",as:\"planService\"}}")
                .and("{$match:{planService:{$exists:1}}}")
                .and("{$group:{_id:\"class\",ids:{$addToSet:\"$personalAccountId\"}}}");
        Aggregate.ResultsIterator resultsIterator = aggregate.as(ClassWithListProperty.class);

        List<String> ids = new ArrayList<>();

        while (resultsIterator.hasNext()) {
            ClassWithListProperty element = (ClassWithListProperty) resultsIterator.next();
            ids = element.getIds();
        }
        return ids;
    }

    public List<String> getPlanServiceIds(){
        MongoCollection balanceCollection = this.jongo.getCollection("plan");

        Aggregate.ResultsIterator<ClassWithListProperty> resultsIterator = balanceCollection.aggregate(
                "{$match:{}}")
                .and("{$group:{_id:\"class\",ids:{$addToSet:\"$serviceId\"}}}")
                .as(ClassWithListProperty.class);


        List<String> planIds = new ArrayList<>();

        while (resultsIterator.hasNext()) {
            planIds = resultsIterator.next().getIds();
        }
        return planIds;
    }

    // не получается избавиться от ObjectId
//    public List<String> getActiveAccountIds() {
//        MongoCollection balanceCollection = this.jongo.getCollection("personalAccount");
//
//        Aggregate aggregate = balanceCollection
//                .aggregate("{$match:{active:true}}")
//                .and("{$group:{_id:\"class\",ids:{$addToSet:\"$_id\"}}}");
//        Aggregate.ResultsIterator resultsIterator = aggregate.as(ClassWithListProperty.class);
//
//        List<String> ids = new ArrayList<>();
//
//        while (resultsIterator.hasNext()) {
//            ClassWithListProperty element = (ClassWithListProperty) resultsIterator.next();
//            ids = element.getIds();
//        }
//
//        List<String> result = new ArrayList<>();
//        ids.forEach(e -> result.add(e.toString()));
//        return ids;
//    }
}

