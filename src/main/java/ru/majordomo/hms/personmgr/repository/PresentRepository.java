package ru.majordomo.hms.personmgr.repository;

import feign.Param;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import ru.majordomo.hms.personmgr.model.present.Present;

public interface PresentRepository extends MongoRepository<Present, String> {
    Present findOne(String id);
    List<Present> findAll();
    Present findByNameOfPromotion(@Param("nameOfPromotion") String nameOfPromotion);
}
