package ru.majordomo.hms.personmgr.model.promocode;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import ru.majordomo.hms.personmgr.common.PromocodeActionType;
import ru.majordomo.hms.personmgr.model.BaseModel;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class PromocodeAction extends BaseModel {
    @NotNull
    @Indexed
    private PromocodeActionType actionType;

    private String description;

    private Map<String, Object> properties = new HashMap<>();

    public PromocodeAction() {
    }

    @PersistenceConstructor
    public PromocodeAction(PromocodeActionType actionType, Map<String, Object> properties) {
        this.actionType = actionType;
        this.properties = properties;
    }
}
