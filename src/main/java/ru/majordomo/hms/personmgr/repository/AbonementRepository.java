package ru.majordomo.hms.personmgr.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

import ru.majordomo.hms.personmgr.common.AbonementType;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;

public interface AbonementRepository extends MongoRepository<Abonement, String> {
    Abonement findOne(String id);
    List<Abonement> findAll();
    List<Abonement> findByType(@Param("type") AbonementType type);
    Page<Abonement> findByType(@Param("type") AbonementType type, Pageable pageable);
}