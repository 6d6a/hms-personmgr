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

        MongoCollection collection = this.jongo.getCollection("accountAbonement");

        Aggregate aggregate = collection
                .aggregate("{$match:{}}")
                .and("{$group:{_id:'class',ids:{$addToSet:'$personalAccountId'}}}");

        return getIds(aggregate);
    }

    public List<String> getAccountIdWithPlanService(){
        MongoCollection collection = this.jongo.getCollection("accountService");

        Aggregate aggregate = collection
                .aggregate("{$match:{enabled:true}}")
                .and("{$lookup:{from:'plan',localField:'serviceId',foreignField:'serviceId',as:'planService'}}")
                .and("{$match:{planService:{$size:1}}}")
                .and("{$group:{_id:'class',ids:{$addToSet:'$personalAccountId'}}}");

        return getIds(aggregate);
    }

    public List<String> getPlanServiceIds(){
        MongoCollection collection = this.jongo.getCollection("plan");

        Aggregate aggregate = collection.aggregate(
                "{$match:{}}")
                .and("{$group:{_id:'class',ids:{$addToSet:'$serviceId'}}}");

        return getIds(aggregate);
    }

    public List<String> getAccountIdsWithMoreThanOnePlanService(){
        MongoCollection collection = this.jongo.getCollection("accountService");

        Aggregate aggregate = collection
                .aggregate("{$match:{enabled:true}}")
                .and("{$lookup:{from:'plan',localField:'serviceId',foreignField:'serviceId',as:'planService'}}")
                .and("{$match:{planService:{$size:1}}}")
                .and("{$group:{_id:'$personalAccountId', class:{$first:'$_class'}, quantity:{$sum:'$quantity'}}}")
                .and("{$match:{quantity:{$gt:1}}}")
                .and("{$group:{_id:'class',ids:{$addToSet:'$_id'}}}");

        return getIds(aggregate);
    }

    private List<String> getIds(Aggregate aggregate){
        Aggregate.ResultsIterator<IdsContainer> resultsIterator = aggregate.as(IdsContainer.class);

        if (resultsIterator.hasNext()) {
            return resultsIterator.next().getIds();
        } else {
            return new ArrayList<>();
        }
    }

    // не получается избавиться от ObjectId
//    public List<String> getActiveAccountIds() {
//        MongoCollection collection = this.jongo.getCollection("personalAccount");
//
//        Aggregate aggregate = collection
//                .aggregate("{$match:{active:true}}")
//                // не работает
//                /*{ $project:{ stringId:{ $concat: [ ObjectId().str ] }}}*/
//                .and("{$group:{_id:'class',ids:{$addToSet:'$_id'}}}");
//
//        return getIds(aggregate);
//    }
}

