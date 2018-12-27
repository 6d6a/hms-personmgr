package ru.majordomo.hms.personmgr.model.promocode;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.model.BaseModel;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Document
@Data
public class PromocodeTag extends BaseModel {
    @CreatedDate
    private LocalDateTime created;

    private String name;

    @NotBlank(message = "Не может быть пустым")
    @Indexed(unique = true)
    private String internalName;
}
