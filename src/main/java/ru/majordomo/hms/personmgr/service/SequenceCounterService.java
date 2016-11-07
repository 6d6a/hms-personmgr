package ru.majordomo.hms.personmgr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;


import ru.majordomo.hms.personmgr.model.counter.Counter;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
public class SequenceCounterService {
    private final MongoOperations mongoOperations;

    @Autowired
    public SequenceCounterService(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public int getNextSequence(String counterName) {
        Counter counter = mongoOperations.findAndModify(
                query(where("counterName").is(counterName)),
                new Update().inc("seq", 1),
                options().returnNew(true).upsert(true),
                Counter.class);

        return counter.getSeq();
    }
}