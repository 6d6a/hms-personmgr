package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.model.VersionedModelBelongsToPersonalAccount;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@Document
public class AccountDocument extends VersionedModelBelongsToPersonalAccount{

    private String templateId;

    @Indexed
    private DocumentType type;

    private Map<String, String> parameters;

    @CreatedDate
    @Indexed
    private LocalDateTime createdDate;
}
