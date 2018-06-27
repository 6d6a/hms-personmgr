package ru.majordomo.hms.personmgr.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        defaultImpl = FileRestoreRequest.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FileRestoreRequest.class,
                name = "FILE"),
        @JsonSubTypes.Type(value = MysqlRestoreRequest.class,
                name = "MYSQL")
})
public interface RestoreRequest {}


