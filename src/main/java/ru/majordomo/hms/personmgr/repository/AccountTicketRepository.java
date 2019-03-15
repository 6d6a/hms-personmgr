package ru.majordomo.hms.personmgr.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.majordomo.hms.personmgr.model.account.AccountTicket;

import java.util.List;

public interface AccountTicketRepository extends MongoRepository<AccountTicket, String> {
    List<AccountTicket> findByPersonalAccountId(String personalAccountId);
    AccountTicket findByPersonalAccountIdAndTicketId(String personalAccountId, Integer ticketId);
}