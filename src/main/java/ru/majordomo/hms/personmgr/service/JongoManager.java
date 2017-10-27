package ru.majordomo.hms.personmgr.service;

import com.mongodb.MongoClient;
import org.jongo.Aggregate;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.majordomo.hms.personmgr.dto.IdsContainer;

import java.util.ArrayList;
import java.util.List;

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

        Aggregate.ResultsIterator<IdsContainer> resultsIterator = balanceCollection
                .aggregate("{$match:{}}")
                .and("{$group:{_id:\"class\",ids:{$addToSet:\"$personalAccountId\"}}}")
                .as(IdsContainer.class);


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
        Aggregate.ResultsIterator resultsIterator = aggregate.as(IdsContainer.class);

        List<String> ids = new ArrayList<>();

        while (resultsIterator.hasNext()) {
            IdsContainer element = (IdsContainer) resultsIterator.next();
            ids = element.getIds();
        }
        return ids;
    }

    public List<String> getPlanServiceIds(){
        MongoCollection balanceCollection = this.jongo.getCollection("plan");

        Aggregate.ResultsIterator<IdsContainer> resultsIterator = balanceCollection.aggregate(
                "{$match:{}}")
                .and("{$group:{_id:\"class\",ids:{$addToSet:\"$serviceId\"}}}")
                .as(IdsContainer.class);


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
//                // не работает
//                /*{ $project:{ stringId:{ $concat: [ ObjectId().str ] }}}*/
//                .and("{$group:{_id:'class',ids:{$addToSet:'$_id'}}}");
//        Aggregate.ResultsIterator<IdsContainer> resultsIterator = aggregate.as(IdsContainer.class);
//
//        List<String> ids = new ArrayList<>();
//
//        while (resultsIterator.hasNext()) {
//            IdsContainer element = resultsIterator.next();
//            ids = element.getIds();
//        }
//
//        return ids;
//    }
}

