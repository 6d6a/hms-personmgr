package ru.majordomo.hms.personmgr.service;

import com.mongodb.LazyDBList;
import com.mongodb.MongoClient;

import org.bson.types.ObjectId;
import org.jongo.Aggregate;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.MongoCursor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.majordomo.hms.personmgr.common.MailManagerMessageType;
import ru.majordomo.hms.personmgr.dto.IdsContainer;
import ru.majordomo.hms.personmgr.dto.stat.Options;
import ru.majordomo.hms.personmgr.model.account.projection.PersonalAccountWithNotificationsProjection;

@Service
public class JongoManager {
    private final Jongo jongo;

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

    public List<String> getAccountIdsWithAbonementExpiredNull() {
            MongoCollection collection = this.jongo.getCollection("accountAbonement");

            Aggregate aggregate = collection
                    .aggregate("{$match:{expired:null}}")
                    .and("{$group:{_id:'class',ids:{$addToSet:'$personalAccountId'}}}");

            return getIds(aggregate);
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

    @SuppressWarnings(value = "unchecked")
    public Map<String, PersonalAccountWithNotificationsProjection> getAccountsWithNotifications() {
        Map<String, PersonalAccountWithNotificationsProjection> accountMap = new HashMap<>();
        MongoCollection personalAccountCollection = jongo.getCollection("personalAccount");

        try (MongoCursor<PersonalAccountWithNotificationsProjection> accountCursor = personalAccountCollection
                .find()
                .projection("{notifications: 1, accountId: 1, active: 1}")
                .map(
                        result -> {
                            PersonalAccountWithNotificationsProjection account = new PersonalAccountWithNotificationsProjection();
                            try {
                                if (result.get("_id") instanceof ObjectId) {
                                    account.setId(((ObjectId) result.get("_id")).toString());
                                } else if (result.get("_id") instanceof String) {
                                    account.setId((String) result.get("_id"));
                                }

                                account.setAccountId((String) result.get("accountId"));
                                Set<MailManagerMessageType> notifications = new HashSet<>();

                                ((LazyDBList) result.get("notifications"))
                                        .forEach(element -> notifications.add(MailManagerMessageType.valueOf((String) element)));

                                account.setNotifications(notifications);

                                account.setActive((boolean) result.get("active"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return account;
                        }
                )
        ) {
            while (accountCursor.hasNext()) {
                PersonalAccountWithNotificationsProjection account = accountCursor.next();
                accountMap.put(account.getId(), account);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return accountMap;
    }

    public List<Options> getMetaOptions() {
        MongoCollection collection = this.jongo.getCollection("processingBusinessOperation");

        Aggregate aggregate = collection
                .aggregate("{$match:{'type' : 'ACCOUNT_CREATE', 'params.meta':{$exists: true}}}")
                .and("{$project: {_id: 0, 'metaarr':{$objectToArray: '$params.meta'}}}")
                .and("{$unwind:'$metaarr'}")
                .and("{$group: {_id:'$metaarr.k', label: {$first:'$metaarr.k' }, values: {$addToSet:'$metaarr.v'}}}")
                ;

        List<Options> r = new ArrayList<>();

        aggregate.as(Options.class).forEach(r::add);

        return r;
    }
}

