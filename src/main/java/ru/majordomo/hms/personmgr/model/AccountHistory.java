package ru.majordomo.hms.personmgr.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * AccountHistory
 */
@Document
public class AccountHistory extends ModelBelongsToPersonalAccount {
    @Indexed
//    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateTime;

//    @TextIndexed
    private String message;

    private String operator;

    public AccountHistory() {
    }

    public AccountHistory(String id, LocalDateTime dateTime, String message, String operator) {
        super();
        this.setId(id);
        this.dateTime = dateTime;
        this.message = message;
        this.operator = operator;
    }

//    @JsonFormat(pattern="yyyy-MM-dd H:i:s")
//    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "AccountHistory{" +
                ", dateTime=" + dateTime +
                ", message='" + message + '\'' +
                ", operator='" + operator + '\'' +
                "} " + super.toString();
    }
}
