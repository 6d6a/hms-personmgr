package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.promocode.UnknownPromocode;

import java.util.List;

public interface UnknownPromocodeRepository extends MongoRepository<UnknownPromocode, String> {
    UnknownPromocode findOne(String id);
    List<UnknownPromocode> findAll();
}
