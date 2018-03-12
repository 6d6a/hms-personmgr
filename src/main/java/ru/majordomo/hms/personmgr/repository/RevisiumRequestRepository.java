package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;

import java.util.List;

public interface RevisiumRequestRepository extends MongoRepository<RevisiumRequest, String> {
    List<RevisiumRequest> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    List<RevisiumRequest> findByPersonalAccountIdAndRevisiumRequestServiceId(
            @Param("personalAccountId") String personalAccountId,
            @Param("revisiumRequestServiceId") String revisiumRequestServiceId
    );
    RevisiumRequest findByPersonalAccountIdAndId(@Param("personalAccountId") String personalAccountId, @Param("id") String id);
    List<RevisiumRequest> findByPersonalAccountIdAndRevisiumRequestServiceIdOrderByCreatedDesc(
            @Param("personalAccountId") String personalAccountId,
            @Param("revisiumRequestServiceId") String revisiumRequestServiceId
    );
}
