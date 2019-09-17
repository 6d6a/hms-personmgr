package ru.majordomo.hms.personmgr.dto.cerb;

import lombok.Data;
import ru.majordomo.hms.personmgr.dto.cerb.api.AttachmentCerberus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Message {
    private Integer messageId;
    private LocalDateTime created;
    private Boolean isOurWorker;
    private String content;
    private List<AttachmentCerberus> attachments;
}
