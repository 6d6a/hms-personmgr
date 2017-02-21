package ru.majordomo.hms.personmgr.model;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
public class AccountHistory extends ModelBelongsToPersonalAccount {
    @Indexed
    private LocalDateTime created;

    @TextIndexed
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
