package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

import ru.majordomo.hms.personmgr.common.AbonementType;
import ru.majordomo.hms.personmgr.model.abonement.Abonement;

public interface AbonementRepository extends MongoRepository<Abonement, String> {
    @RestResource(path = "findListByType", rel = "findListByType")
    List<Abonement> findByType(@Param("type") AbonementType type);
    Page<Abonement> findByType(@Param("type") AbonementType type, Pageable pageable);
    @RestResource(path = "findListByIdIn", rel = "findListByIdIn")
    List<Abonement> findByIdIn(@Param("ids") List<String> ids);
    Page<Abonement> findByIdIn(@Param("ids") List<String> ids, Pageable pageable);
}