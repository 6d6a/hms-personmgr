package ru.majordomo.hms.personmgr.model.business;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import ru.majordomo.hms.personmgr.common.BusinessActionType;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.common.message.SimpleServiceMessage;
import ru.majordomo.hms.personmgr.common.message.destination.GenericMessageDestination;

import javax.annotation.Nullable;

/**
 * ProcessingBusinessAction
 * Объект описывающий длительную операцию которая выполняется другим сервисом.
 * Объект создается только через: {@link ru.majordomo.hms.personmgr.service.BusinessHelper#buildAction}
 *      {@link ru.majordomo.hms.personmgr.service.BusinessHelper#buildActionAndOperation} и другие методы этого объекта
 * При создании всегда отправляется сообщение в rabbit
 *
 * Множество объектов ProcessingBusinessAction может соответстовать одному {@link ProcessingBusinessOperation}
 *
 * Для объекта сделан partial unique индекс через который можно запрещать создавать операции с одним и тем же ресурсом,
 *  пока не закончены предыдущие
 * {@link ru.majordomo.hms.personmgr.config.MongoConfig#initIndicesAfterStartup}
 *
 * frontend использует именного этот класс для отображения и отслеживания состояния операций.
 */
@Getter
@Setter
@ToString(callSuper = true)
@Document
public class ProcessingBusinessAction extends BusinessAction {
    public static final String EXECUTING_KEY = "executing";
    public static final String KEY_DENY_SAME_KEY = "keyDenySame";
    public static final String RESOURCE_DENY_SAME_KEY = "resourceDenySame";

    @Indexed
    private String personalAccountId;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    @JsonView(Views.Internal.class)
    private Map<String,Object> params = new HashMap<>();

    public ProcessingBusinessAction(BusinessAction businessAction) {
        super();
//        this.setOperationId(ObjectId.get().toHexString());
        this.setDestination(businessAction.getDestination());
        this.setName(businessAction.getName());
        this.setMessage(businessAction.getMessage());
        this.setPriority(businessAction.getPriority());
        this.setState(businessAction.getState());
        this.setBusinessActionType(businessAction.getBusinessActionType());
    }

    /**
     * id ресурса, шаблона, имя, {@link java.util.Objects#hash} или что-то подобное
     * {@link ru.majordomo.hms.personmgr.config.MongoConfig#initIndicesAfterStartup}
     */
    @Nullable
    private String keyDenySame;

    /**
     * simpleName ресурса, BusinessActionType или что-то подобное.
     * {@link ru.majordomo.hms.personmgr.config.MongoConfig#initIndicesAfterStartup}
     */
    @Nullable
    private String resourceDenySame;

    public void setResourceDenySame(@Nullable Class<?> resourceDenySame) {
        this.resourceDenySame = resourceDenySame == null ? null : resourceDenySame.getSimpleName();
    }

    /**
     * {@link ru.majordomo.hms.personmgr.config.MongoConfig#initIndicesAfterStartup}
     * @deprecated only for mongodb's index
     */
    @Deprecated
    @Field(EXECUTING_KEY)
    @JsonIgnore
    @AccessType(AccessType.Type.PROPERTY)
    public boolean isExecuting() {
        return Step.ACTIVE_STATES.contains(this.getState());
    }

    /**
     * {@link ru.majordomo.hms.personmgr.config.MongoConfig#initIndicesAfterStartup(MongoTemplate)}
     * @deprecated only for mongodb's index
     */
    @Deprecated
    @Field(EXECUTING_KEY)
    @JsonIgnore
    @AccessType(AccessType.Type.PROPERTY)
    public void setExecuting(boolean ignore) {}

    @PersistenceConstructor
    public ProcessingBusinessAction(
            String id,
            String name,
            State state,
            int priority,
            String operationId,
            BusinessActionType businessActionType,
            GenericMessageDestination destination,
            SimpleServiceMessage message,
            String personalAccountId,
            LocalDateTime createdDate,
            LocalDateTime updatedDate,
            Map<String, Object> params
    ) {
        super(id, name, state, priority, operationId, businessActionType, destination, message);
        this.personalAccountId = personalAccountId;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.params = params;
    }

    public Object getParam(String key) {
        return params.get(key);
    }

    public void addParam(String key, Object value) {
        if (params == null) {
            params = new HashMap<>();
        }

        params.put(key, value);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
