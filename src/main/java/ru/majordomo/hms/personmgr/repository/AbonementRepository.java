package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.plan.Feature;

public interface AbonementRepository extends MongoRepository<Abonement, String> {
    List<Abonement> findByPeriodAndTypeAndInternal(String period, Feature type, boolean internal);
    List<Abonement> findByIdInAndInternalAndPeriod(
            List<String> ids,
            boolean internal,
            String period
    );
}