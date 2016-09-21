package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * AccountHistory
 */
@Document
public class AccountHistory {
    @Id
    private String id;

    @Indexed
    private String accountId;

    @Indexed
    private LocalDateTime dateTime;

//    @TextIndexed
    private String message;

    private String operator;

    public AccountHistory() {
    }

    public AccountHistory(String id, String accountId, LocalDateTime dateTime, String message, String operator) {
        this.id = id;
        this.accountId = accountId;
        this.dateTime = dateTime;
        this.message = message;
        this.operator = operator;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

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
}
