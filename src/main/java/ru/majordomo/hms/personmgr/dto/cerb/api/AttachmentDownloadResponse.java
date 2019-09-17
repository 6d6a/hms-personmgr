package ru.majordomo.hms.personmgr.dto.cerb.api;

import lombok.Data;
import org.springframework.http.MediaType;

@Data
public class AttachmentDownloadResponse {
    byte[] body;
    String contentType;
    String characterEncoding;
}
