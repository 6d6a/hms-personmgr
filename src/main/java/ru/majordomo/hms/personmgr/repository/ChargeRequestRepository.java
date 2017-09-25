package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.Status;

import java.time.LocalDate;
import java.util.List;

public interface ChargeRequestRepository extends MongoRepository<ChargeRequest, String> {
    List<ChargeRequest> findByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
    ChargeRequest findByPersonalAccountIdAndChargeDate(@Param("personalAccountId") String personalAccountId, @Param("chargeDate") LocalDate chargeDate);
    List<ChargeRequest> findByChargeDate(@Param("chargeDate") LocalDate chargeDate);
    List<ChargeRequest> findByChargeDateAndStatus(@Param("chargeDate") LocalDate chargeDate, @Param("status") Status status);
    void deleteByPersonalAccountId(@Param("personalAccountId") String personalAccountId);
}