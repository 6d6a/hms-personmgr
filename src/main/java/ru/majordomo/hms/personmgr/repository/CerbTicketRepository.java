package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.cerb.CerbTicket;

public interface CerbTicketRepository extends MongoRepository<CerbTicket, String> {
    CerbTicket findByViolation(String violation);
    CerbTicket findByTicketMessage(String ticketMessage);
}
