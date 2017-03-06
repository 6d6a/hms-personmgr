package ru.majordomo.hms.personmgr.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.TokenType;

@Document
public class Token extends ModelBelongsToPersonalAccount {
    @Indexed
    @NotNull
    private TokenType type;

    @CreatedDate
    @Indexed
    @NotNull
    private LocalDateTime created;

    @Indexed
    private LocalDateTime deleted;

    private Map<String, Object> params = new HashMap<>();

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getDeleted() {
        return deleted;
    }

    public void setDeleted(LocalDateTime deleted) {
        this.deleted = deleted;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public Object getParam(String param) {
        return params.get(param);
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public void addParam(String name, Object value) {
        if (params == null) {
            params = new HashMap<>();
        }

        params.put(name,value);
    }

    @Override
    public String toString() {
        return "Token{" +
                "type=" + type +
                ", created=" + created +
                ", deleted=" + deleted +
                ", params=" + params +
                "} " + super.toString();
    }
}
