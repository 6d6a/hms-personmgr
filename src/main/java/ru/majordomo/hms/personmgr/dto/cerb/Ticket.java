package ru.majordomo.hms.personmgr.dto.cerb;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Ticket {
    private Integer ticketId;
    private List<Message> messages = new ArrayList<>();
    private String mask;
    private TicketStatus status;
    private String subject;
    private Integer senderId;
}
