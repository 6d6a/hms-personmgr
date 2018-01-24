package ru.majordomo.hms.personmgr.dto.rpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HtmlToPdfResponse extends BaseRpcResponse {

    @JsonProperty("pdf_file")
    private String pdfFileInBase64;
}
