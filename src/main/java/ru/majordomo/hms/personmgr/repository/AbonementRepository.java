package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;
import ru.majordomo.hms.personmgr.model.plan.Feature;

public interface AbonementRepository extends MongoRepository<Abonement, String> {
    List<Abonement> findByType(Feature type);
    Page<Abonement> findByType(Feature type, Pageable pageable);
    List<Abonement> findByIdIn(List<String> ids);
    Page<Abonement> findByIdIn(List<String> ids, Pageable pageable);
    List<Abonement> findByIdInAndInternalAndPeriod(
            List<String> ids,
            boolean internal,
            String period
    );
    List<Abonement> findByServiceId(
            String serviceId
    );
}