package ru.majordomo.hms.personmgr.dto.cerb.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachmentCerberus {
    @JsonProperty("id")
    private Integer attachmentId;
    @JsonProperty("file_name")
    private String name;
    @JsonProperty("file_type")
    private String type;
    @JsonProperty("file_size")
    private Integer size;
    @JsonProperty("file_sha1hash")
    private String sha1hash;
}
