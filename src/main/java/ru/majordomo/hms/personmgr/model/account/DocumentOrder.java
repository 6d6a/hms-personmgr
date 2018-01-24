package ru.majordomo.hms.personmgr.model.account;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.majordomo.hms.personmgr.common.DocumentType;
import ru.majordomo.hms.personmgr.model.ModelBelongsToPersonalAccount;

import java.time.LocalDateTime;
import java.util.*;

@Data
@Document
public class DocumentOrder extends ModelBelongsToPersonalAccount {

    @CreatedDate
    @Indexed
    private LocalDateTime createdDateTime;

    private List<DocumentType> documentTypes = new ArrayList<>();

    private Map<String, String> params = new HashMap<>();

    private List<String> domainIds = new ArrayList<>();

    private Boolean checked = false;

    private Boolean agreement = false;

    private Boolean paid = false;

    private Map<String, String> errors = new HashMap<>();

    private Boolean send = false;

    private String postalAddress;
}