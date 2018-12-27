package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.promocode.PromocodeTag;

public interface PromoTagRepository extends MongoRepository<PromocodeTag, String> {
    PromocodeTag findOneByInternalName(String internalName);
}
