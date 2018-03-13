package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.revisium.RevisiumRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface RevisiumRequestRepository extends MongoRepository<RevisiumRequest, String> {
    List<RevisiumRequest> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    List<RevisiumRequest> findByPersonalAccountIdAndSuccessGetResultAndCreatedAfter(
            @Param("personalAccountId") String personalAccountId,
            @Param("successGetResult") Boolean successGetResult,
            @Param("created") LocalDateTime created
    );
    List<RevisiumRequest> findByPersonalAccountIdAndRevisiumRequestServiceId(
            @Param("personalAccountId") String personalAccountId,
            @Param("revisiumRequestServiceId") String revisiumRequestServiceId
    );
    List<RevisiumRequest> findByPersonalAccountIdAndRevisiumRequestServiceIdAndSuccessGetResultAndCreatedAfter(
            @Param("personalAccountId") String personalAccountId,
            @Param("revisiumRequestServiceId") String revisiumRequestServiceId,
            @Param("successGetResult") Boolean successGetResult,
            @Param("created") LocalDateTime created
    );
    RevisiumRequest findByPersonalAccountIdAndId(
            @Param("personalAccountId") String personalAccountId,
            @Param("id") String id
    );
    RevisiumRequest findByPersonalAccountIdAndRevisiumRequestServiceIdAndId(
            @Param("personalAccountId") String personalAccountId,
            @Param("personalAccountId") String revisiumRequestServiceId,
            @Param("id") String id
    );
    RevisiumRequest findFirstByPersonalAccountIdAndRevisiumRequestServiceIdAndSuccessGetResultOrderByCreatedDesc(
            @Param("personalAccountId") String personalAccountId,
            @Param("personalAccountId") String revisiumRequestServiceId,
            @Param("successGetResult") Boolean successGetResult
    );
    List<RevisiumRequest> findByPersonalAccountIdAndRevisiumRequestServiceIdOrderByCreatedDesc(
            @Param("personalAccountId") String personalAccountId,
            @Param("revisiumRequestServiceId") String revisiumRequestServiceId
    );
}
