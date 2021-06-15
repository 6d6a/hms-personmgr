package ru.majordomo.hms.personmgr.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@ParametersAreNonnullByDefault
public class DocumentPreviewRequest {
    @NotEmpty
    @JsonProperty("body")
    private String bodyHtml;
    @Nullable
    @JsonProperty("footer")
    private String footerHtml;

    @NotNull
    private int[] noFooterPages = {}; // todo validation @Min(1)
    private boolean withoutStamp = false;
}
