package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.stream.Stream;

import ru.majordomo.hms.personmgr.model.service.LongLifeResourceArchive;
import ru.majordomo.hms.rc.user.resources.ResourceArchiveType;

public interface LongLifeResourceArchiveRepository extends MongoRepository<LongLifeResourceArchive, String> {
    LongLifeResourceArchive findByPersonalAccountIdAndResourceArchiveId(String personalAccountId, String resourceArchiveId);
    LongLifeResourceArchive findByResourceArchiveId(String resourceArchiveId);
    List<LongLifeResourceArchive> findByPersonalAccountId(String personalAccountId);
    LongLifeResourceArchive findByAccountServiceId(String accountServiceId);
    List<LongLifeResourceArchive> findByPersonalAccountIdAndTypeAndArchivedResourceId(String personalAccountId, ResourceArchiveType type, String archivedResourceId);
    LongLifeResourceArchive findByPersonalAccountIdAndId(String accountId, String serviceId);
    @Query("{}")
    Stream<LongLifeResourceArchive> findAllStream();
}
