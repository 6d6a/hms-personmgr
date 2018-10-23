package ru.majordomo.hms.personmgr.service.restic;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;
import org.springframework.data.annotation.Transient;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Snapshot {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;

    @JsonSetter("time")
    public void setTime(String time) {
        this.time = LocalDateTime.parse(time, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @JsonProperty("short_id")
    private String shortId;

    private String serverName;

    private Boolean hidden = true;

    private List<String> paths;
}
