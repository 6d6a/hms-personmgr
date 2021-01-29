package ru.majordomo.hms.personmgr.service.Document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

/** опции wkhttptopdf https://wkhtmltopdf.org/usage/wkhtmltopdf.txt */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WkHtmlToPdfOptions {
    @Nullable
    @JsonProperty("margin-left")
    private String marginLeft;
    @Nullable
    @JsonProperty("margin-right")
    private String marginRight;
    @Nullable
    @JsonProperty("margin-top")
    private String marginTop;
    @Nullable
    @JsonProperty("margin-bottom")
    private String marginBottom;
    @Nullable
    private Double zoom;
    @Nullable
    private Integer dpi = 300;
    /** Disable the intelligent shrinking strategy used by WebKit that makes the pixel/dpi ratio non-constant */
    @JsonProperty("disable-smart-shrinking")
    private boolean disableSmartShrinking = true;
    @JsonProperty("print-media-type")
    private boolean printMediaType = true;
    @Nullable
    @JsonProperty("page-size")
    private String pageSize = "A4";
    /** Не генерировать содержание */
    @JsonProperty("no-outline")
    private boolean noOutline = true;
}