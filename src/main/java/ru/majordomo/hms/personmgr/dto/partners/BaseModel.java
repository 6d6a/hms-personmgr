package ru.majordomo.hms.personmgr.dto.partners;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class BaseModel {
    private String id;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING,
             pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime created;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING,
             pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updated;
}
