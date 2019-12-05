package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import ru.majordomo.hms.personmgr.model.service.DedicatedAppService;

import java.util.List;
import java.util.stream.Stream;

public interface DedicatedAppServiceRepository extends MongoRepository<DedicatedAppService, String> {
    DedicatedAppService findByPersonalAccountIdAndTemplateId(String personalAccountId, String templateId);
    List<DedicatedAppService> findByPersonalAccountId(String personalAccountId);
    DedicatedAppService findByIdAndPersonalAccountId(String id, String personalAccountId);
    DedicatedAppService findByPersonalAccountIdAndAccountServiceId(String personalAccountId, String accountServiceId);
    DedicatedAppService findByAccountServiceId(String accountServiceId);
    @Query("{}")
    Stream<DedicatedAppService> findAllStream();
}
