package ru.majordomo.hms.personmgr.model.business;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import ru.majordomo.hms.personmgr.common.BusinessOperationType;
import ru.majordomo.hms.personmgr.common.ExtendedActionStage;
import ru.majordomo.hms.personmgr.common.State;
import ru.majordomo.hms.personmgr.common.Views;
import ru.majordomo.hms.personmgr.importing.DBImportService;
import ru.majordomo.hms.personmgr.service.BusinessHelper;

import javax.annotation.Nullable;

/**
 * Объект описывающий длительную операцию которая не может быть выполнена сразу в том потоке.
 * Например что-то что потребовало обращение по rabbit, а результат придет неизвестно когда.
 * По нему и ProcessingBusinessAction так же происходит запрет на создание повторных операций.
 * 
 * Один объект ProcessingBusinessOperation соответствует множеству {@link ProcessingBusinessAction}
 *
 * frontend использует именного этот класс для отображения и отслеживания состояния операций, а не ProcessingBusinessAction
 *
 * При изменении объекта нужно исправить большое количество написанных в ручную запросов к mongo в классе {@link BusinessHelper}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document
@NoArgsConstructor
@ToString(callSuper = true)
public class ProcessingBusinessOperation extends Step {
    @JsonView(Views.Public.class)
    @Indexed
    private BusinessOperationType type = BusinessOperationType.COMMON_OPERATION;

    @JsonView(Views.Public.class)
    @Indexed
    @Nullable
    private String personalAccountId;

    @JsonView(Views.Public.class)
    @CreatedDate
    @Indexed
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;

    @JsonView(Views.Public.class)
    @LastModifiedDate
    @Indexed
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedDate;

    /**
     * Собственно параметры операции, такие как resourceId, domainId, type, name и так далее.
     * @property {!enum} [stage] стадия для операций соостоящих из нескольких {@link ProcessingBusinessAction} через {@link BusinessHelper#setStage} например: {@link DBImportService.ImportStage} {@link ExtendedActionStage}
     */
    @JsonView(Views.Internal.class)
    private Map<String,Object> params = new HashMap<>();

    /**
     * Дополнительные параметры, которые видно в ответе на rest запросы.
     * @property {!string[]} warnings список некритичных ошибок возникших во время операции {@link BusinessHelper#addWarning(String, String)}
     * @property {?string} name что это такое спросить у Ильи
     * @property {?string} message сообщение об ошибке для frontend
     * @property {any[]} errors ошибка в неопределенном формате
     * @property {string} exceptionClass например ParameterValidationException
     */
    @JsonView(Views.Public.class)
    private Map<String,Object> publicParams = new HashMap<>();

    @PersistenceConstructor
    public ProcessingBusinessOperation(
            String id,
            String name,
            State state,
            int priority,
            BusinessOperationType type,
            @Nullable String personalAccountId,
            LocalDateTime createdDate,
            LocalDateTime updatedDate,
            Map<String, Object> params
    ) {
        super();
        this.setId(id);
        this.setName(name);
        this.setState(state);
        this.setPriority(priority);
        this.type = type;
        this.personalAccountId = personalAccountId;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.params = params;
    }

    public void addParam(String key, Object value) {
        if (params == null) {
            params = new HashMap<>();
        }

        params.put(key, value);
    }

    public Object getParam(String key) {
        return this.params.get(key);
    }

    public void addPublicParam(String key, Object value) {
        if (publicParams == null) {
            publicParams = new HashMap<>();
        }

        publicParams.put(key, value);
    }

    public Object getPublicParam(String key) {
        return this.publicParams.get(key);
    }
}
