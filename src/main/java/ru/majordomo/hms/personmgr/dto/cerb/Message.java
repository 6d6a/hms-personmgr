package ru.majordomo.hms.personmgr.dto.cerb;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Message {
    private Integer messageId;
    private LocalDateTime created;
    private Boolean isOurWorker;
    private String content;
}
