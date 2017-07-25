package ru.majordomo.hms.personmgr.model.account;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

@Document
public class AccountHistory extends ModelBelongsToPersonalAccount {
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @CreatedDate
    @Indexed
    private LocalDateTime created;

//    @TextIndexed
    private String message;

    private String operator;

    public AccountHistory() {
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
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
                ", created=" + created +
                ", message='" + message + '\'' +
                ", operator='" + operator + '\'' +
                "} " + super.toString();
    }
}
