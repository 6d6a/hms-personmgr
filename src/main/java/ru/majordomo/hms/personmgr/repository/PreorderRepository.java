package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.Preorder;
import ru.majordomo.hms.personmgr.model.plan.Feature;

import java.util.List;

public interface PreorderRepository extends MongoRepository<Preorder, String> {
    List<Preorder> findByPersonalAccountId(String personalAccountId);
    void deleteByPersonalAccountId(String personalAccountId);
    Preorder findByPersonalAccountIdAndFeature(String personalAccountId, Feature feature);
}
