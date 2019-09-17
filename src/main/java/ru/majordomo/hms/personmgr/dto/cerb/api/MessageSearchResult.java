package ru.majordomo.hms.personmgr.dto.cerb.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageSearchResult {
    @JsonProperty("created")
    private Long created;
    @JsonProperty("content")
    private String content;
    @JsonProperty("sender_id")
    private Integer senderId;
    @JsonProperty("worker_id")
    private Integer workerId;
    @JsonProperty("id")
    private Integer messageId;
    @JsonProperty("attachments")
    private Map<Integer, AttachmentCerberus> attachments;
}
