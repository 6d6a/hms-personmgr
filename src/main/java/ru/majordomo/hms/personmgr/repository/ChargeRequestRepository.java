package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import ru.majordomo.hms.personmgr.model.charge.ChargeRequest;
import ru.majordomo.hms.personmgr.model.charge.Status;

import java.time.LocalDate;
import java.util.List;

public interface ChargeRequestRepository extends MongoRepository<ChargeRequest, String> {
    List<ChargeRequest> findByPersonalAccountId(String personalAccountId);
    ChargeRequest findByPersonalAccountIdAndChargeDate(String personalAccountId, LocalDate chargeDate);
    List<ChargeRequest> findByChargeDate(LocalDate chargeDate);
    List<ChargeRequest> findByChargeDateAndStatus(LocalDate chargeDate, Status status);
    void deleteByPersonalAccountId(String personalAccountId);
}