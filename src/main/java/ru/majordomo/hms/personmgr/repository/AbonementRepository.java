package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.model.abonement.Abonement;

public interface AbonementRepository extends MongoRepository<Abonement, String> {
    List<Abonement> findByIdInAndInternalAndPeriod(
            List<String> ids,
            boolean internal,
            String period
    );
}